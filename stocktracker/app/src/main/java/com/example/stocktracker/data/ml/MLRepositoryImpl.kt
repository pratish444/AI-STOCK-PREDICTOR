package com.example.stocktracker.data.ml

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import com.example.stocktracker.data.remote.api.MLApiService
import com.example.stocktracker.data.remote.dto.*
import com.example.stocktracker.domain.model.*
import com.example.stocktracker.domain.util.Resource
import com.example.stocktracker.presentation.screens.dashboard.MarketIndex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MLRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mlApiService: MLApiService
) : MLRepository {

    companion object {
        private const val TAG = "MLRepository"
        private const val MODEL_FILE = "trend_model.tflite"
        private const val MIN_DATA_POINTS = 10
    }

    private var tfliteInterpreter: Interpreter? = null
    private var isModelLoaded = false

    init {
        loadLocalModel()
    }

    // ========== Model Management ==========

    private fun loadLocalModel() {
        try {
            val model = loadModelFile(MODEL_FILE)
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = true
            }
            tfliteInterpreter = Interpreter(model, options)
            isModelLoaded = true
            Log.d(TAG, "TFLite model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}")
            isModelLoaded = false
        }
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    // ========== On-Device Predictions ==========

    override suspend fun predictTrendLocal(features: FloatArray): Resource<TrendPrediction> =
        withContext(Dispatchers.Default) {
            try {
                if (!isModelLoaded || tfliteInterpreter == null) {
                    return@withContext calculateFallbackTrend(features)
                }

                val input = Array(1) { features }
                val output = Array(1) { FloatArray(3) }
                tfliteInterpreter?.run(input, output)

                val probabilities = output[0]
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 2

                val direction = when (maxIndex) {
                    0 -> TrendDirection.UP
                    1 -> TrendDirection.DOWN
                    else -> TrendDirection.NEUTRAL
                }

                Resource.Success(
                    TrendPrediction(
                        direction = direction,
                        confidence = probabilities[maxIndex],
                        indicators = extractIndicatorsFromFeatures(features)
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Local prediction error: ${e.message}")
                calculateFallbackTrend(features)
            }
        }

    private fun calculateFallbackTrend(features: FloatArray): Resource<TrendPrediction> {
        val priceChange = features.getOrNull(4) ?: 0f
        val rsi = features.getOrNull(2) ?: 50f

        val direction = when {
            priceChange > 0.02f && rsi < 70 -> TrendDirection.UP
            priceChange < -0.02f && rsi > 30 -> TrendDirection.DOWN
            else -> TrendDirection.NEUTRAL
        }

        val confidence = (abs(priceChange) * 10).coerceIn(0.5f, 0.9f)
        return Resource.Success(TrendPrediction(direction, confidence, null))
    }

    private fun extractIndicatorsFromFeatures(features: FloatArray): TechnicalIndicators {
        val rsi = features.getOrNull(2) ?: 50f
        return TechnicalIndicators(
            rsi = rsi.toDouble(),
            macd = features.getOrNull(3)?.toDouble(),
            sma20 = null,
            ema12 = features.getOrNull(0)?.toDouble(),
            bollingerUpper = null,
            bollingerLower = null,
            signals = mapOf(
                "rsi" to when {
                    rsi > 70 -> "overbought"
                    rsi < 30 -> "oversold"
                    else -> "neutral"
                }
            )
        )
    }

    // ========== Cloud API Calls ==========

    override suspend fun predictLSTM(
        symbol: String,
        historicalData: List<PricePoint>
    ): Resource<PredictionResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting LSTM prediction for $symbol")
            val request = PredictionRequestDto(
                symbol = symbol,
                features = historicalData.map {
                    listOf(
                        it.open.toFloat(),
                        it.high.toFloat(),
                        it.low.toFloat(),
                        it.close.toFloat(),
                        it.volume.toFloat()
                    )
                },
                days_to_predict = 7
            )
            Resource.Success(mlApiService.getLSTMPrediction(request).toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "LSTM prediction failed: ${e.message}")
            Resource.Error(e.localizedMessage ?: "Cloud prediction failed")
        }
    }

    override suspend fun analyzeSentiment(
        texts: List<String>,
        symbol: String?
    ): Resource<SentimentResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing sentiment for ${texts.size} texts")
            val response = mlApiService.analyzeSentiment(SentimentRequestDto(texts, symbol))
            Resource.Success(response.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Sentiment analysis failed: ${e.message}")
            Resource.Error(e.localizedMessage ?: "Sentiment analysis failed")
        }
    }

    override suspend fun calculateIndicators(
        data: List<List<Float>>
    ): Resource<TechnicalIndicators> = withContext(Dispatchers.IO) {
        try {
            val response = mlApiService.calculateIndicators(TechnicalIndicatorRequestDto(data))
            Resource.Success(
                TechnicalIndicators(
                    rsi = response.rsi,
                    macd = response.macd,
                    sma20 = response.sma_20,
                    ema12 = response.ema_12,
                    bollingerUpper = response.bollinger_upper,
                    bollingerLower = response.bollinger_lower,
                    signals = response.signals
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Indicator calculation failed")
        }
    }

    // ========== Market Overview ==========

    override suspend fun getMarketOverview(): Resource<MarketOverviewResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = mlApiService.getMarketOverview()
                Resource.Success(
                    MarketOverviewResponse(
                        indices = response.indices.map {
                            MarketIndex(
                                name = it.name,
                                symbol = it.symbol,
                                value = it.value,
                                change = it.change,
                                changePercent = it.change_percent
                            )
                        }
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Market overview failed: ${e.message}")
                Resource.Error(e.localizedMessage ?: "Failed to load market overview")
            }
        }

    // ========== Hybrid Smart Routing ==========

    override suspend fun getPrediction(
        symbol: String,
        historicalData: List<PricePoint>
    ): Resource<PredictionResult> {

        val shouldUseCloud = shouldUseCloudPrediction(historicalData)

        if (shouldUseCloud) {
            val cloudResult = predictLSTM(symbol, historicalData)
            if (cloudResult is Resource.Success) return cloudResult
            Log.w(TAG, "Cloud prediction failed, falling back to on-device")
        }

        val features = extractFeatures(historicalData)
        val localResult = predictTrendLocal(features)

        return when (localResult) {
            is Resource.Success -> {
                val trend = localResult.data ?: return Resource.Error("Prediction data was null")
                val lastPrice = historicalData.lastOrNull()?.close ?: 0.0
                val predictions = generateSimplePredictions(lastPrice, trend.direction, trend.confidence)

                Resource.Success(
                    PredictionResult(
                        symbol = symbol,
                        predictions = predictions,
                        confidenceScores = List(predictions.size) { index ->
                            (trend.confidence * (1 - index * 0.1)).coerceAtLeast(0.5).toDouble()
                        },
                        trend = trend.direction.name.lowercase(),
                        currentPrice = lastPrice,
                        predictedChangePercent = ((predictions.lastOrNull()
                            ?: lastPrice) - lastPrice) / lastPrice * 100,
                        predictedHigh = predictions.maxOrNull() ?: lastPrice,
                        predictedLow = predictions.minOrNull() ?: lastPrice,
                        generatedAt = java.time.Instant.now().toString(),
                        modelVersion = "tflite-ondevice-v1",
                        source = if (shouldUseCloud) "on-device-fallback" else "on-device",
                        isOffline = !isNetworkAvailable()
                    )
                )
            }
            is Resource.Error -> Resource.Error(
                localResult.message ?: "Both cloud and local prediction failed"
            )
            is Resource.Loading -> Resource.Loading()
        }
    }

    private fun shouldUseCloudPrediction(data: List<PricePoint>): Boolean {
        if (!isNetworkAvailable()) return false
        if (isBatteryLow()) return false
        if (data.size < MIN_DATA_POINTS) return false
        return true
    }

    private fun generateSimplePredictions(
        lastPrice: Double,
        direction: TrendDirection,
        confidence: Float
    ): List<Double> {
        val predictions = mutableListOf<Double>()
        var currentPrice = lastPrice

        val dailyChange = when (direction) {
            TrendDirection.UP -> 0.005 * confidence
            TrendDirection.DOWN -> -0.005 * confidence
            TrendDirection.NEUTRAL -> 0.0
        }

        repeat(7) {
            currentPrice *= (1 + dailyChange + (Math.random() - 0.5) * 0.01)
            predictions.add(currentPrice)
        }
        return predictions
    }

    // ========== Feature Extraction ==========

    private fun extractFeatures(data: List<PricePoint>): FloatArray {
        val recent = data.takeLast(10)
        if (recent.size < 5) return FloatArray(10) { 0f }

        val closes = recent.map { it.close }
        val volumes = recent.map { it.volume }

        return floatArrayOf(
            closes.average().toFloat(),
            (volumes.average() / 1_000_000.0).toFloat(),
            calculateRSI(recent),
            calculateMACD(recent),
            ((closes.last() / closes.first()) - 1.0).toFloat(),
            recent.maxOf { it.high }.toFloat(),
            recent.minOf { it.low }.toFloat(),
            recent.map { it.high - it.low }.average().toFloat(),
            recent.map { (it.close - it.open) / it.open }.average().toFloat(),
            (closes.last() / closes.first()).toFloat()
        )
    }

    private fun calculateRSI(data: List<PricePoint>): Float {
        if (data.size < 2) return 50f

        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()

        for (i in 1 until data.size) {
            val change = data[i].close - data[i - 1].close
            if (change > 0) gains.add(change) else losses.add(abs(change))
        }

        val avgGain = if (gains.isNotEmpty()) gains.average() else 0.0
        val avgLoss = if (losses.isNotEmpty()) losses.average() else 0.001

        if (avgLoss == 0.0) return 100f
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).toFloat()
    }

    private fun calculateMACD(data: List<PricePoint>): Float {
        val closes = data.map { it.close }
        return (closes.takeLast(12).average() - closes.takeLast(26).average()).toFloat()
    }

    // ========== Utility ==========

    override suspend fun isBackendAvailable(): Boolean {
        return try {
            mlApiService.healthCheck().status == "healthy"
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getMockStockData(symbol: String): Resource<MockStockData> =
        withContext(Dispatchers.IO) {
            try {
                val response = mlApiService.getMockStockData(symbol)
                Resource.Success(
                    MockStockData(
                        symbol = response.symbol,
                        name = response.name,
                        price = response.price,
                        change = response.change,
                        changePercent = response.change_percent,
                        volume = response.volume.toLong(),
                        high = response.high,
                        low = response.low,
                        open = response.open,
                        previousClose = response.previous_close,
                        marketCap = response.market_cap,
                        peRatio = response.pe_ratio
                    )
                )
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to fetch mock data")
            }
        }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isBatteryLow(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 20
    }

    // ========== DTO Mappers ==========

    private fun PredictionResponseDto.toDomainModel() = PredictionResult(
        symbol = symbol,
        predictions = predictions,
        confidenceScores = confidence_scores,
        trend = trend,
        currentPrice = current_price,
        predictedChangePercent = predicted_change_percent,
        predictedHigh = predicted_high,
        predictedLow = predicted_low,
        generatedAt = generated_at,
        modelVersion = model_version,
        source = source,
        isOffline = false
    )

    private fun SentimentResponseDto.toDomainModel() = SentimentResult(
        symbol = symbol,
        overallScore = overall_score,
        overallLabel = overall_label,
        recommendation = recommendation,
        confidence = confidence,
        sentiments = sentiments.map {
            SentimentItem(
                text = it.text,
                label = it.label,
                score = it.score,
                keywords = it.keywords
            )
        },
        bullishCount = bullish_count,
        bearishCount = bearish_count,
        neutralCount = neutral_count
    )
}