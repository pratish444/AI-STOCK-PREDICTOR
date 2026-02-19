package com.example.stocktracker.data.ml

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import com.example.stocktracker.domain.model.*
import com.example.stocktracker.domain.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridMLManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trendPredictor: TrendPredictor,
    private val mlRepository: MLRepository
) {
    companion object {
        private const val TAG = "HybridMLManager"
        private const val MIN_CLOUD_DATA_POINTS = 20
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val POOR_CONNECTION_THRESHOLD_KBPS = 100
    }

    data class MLConfig(
        val preferCloud: Boolean = true,
        val allowOnDeviceFallback: Boolean = true,
        val minConfidenceThreshold: Float = 0.6f,
        val maxLatencyMs: Long = 2000
    )

    suspend fun getOptimalPrediction(
        symbol: String,
        historicalData: List<PricePoint>,
        config: MLConfig = MLConfig()
    ): Resource<PredictionResult> = withContext(Dispatchers.IO) {

        val strategy = determineStrategy(historicalData, config)
        Log.d(TAG, "Using strategy: $strategy for $symbol")

        when (strategy) {
            is MLStrategy.Cloud -> {
                when (val result = mlRepository.predictLSTM(symbol, historicalData)) {
                    is Resource.Success -> result
                    is Resource.Error -> {
                        if (config.allowOnDeviceFallback) {
                            Log.w(TAG, "Cloud failed, falling back to on-device")
                            getOnDevicePrediction(symbol, historicalData)
                        } else {
                            result
                        }
                    }
                    is Resource.Loading -> Resource.Loading()
                }
            }
            is MLStrategy.OnDevice -> getOnDevicePrediction(symbol, historicalData)
            is MLStrategy.Hybrid -> getEnsemblePrediction(symbol, historicalData)
        }
    }

    private suspend fun getOnDevicePrediction(
        symbol: String,
        data: List<PricePoint>
    ): Resource<PredictionResult> {
        val features = FeatureExtractor.extract(data)

        return trendPredictor.predict(features)?.let { trend ->
            val lastPrice = data.last().close
            val predictions = generatePredictions(lastPrice, trend.direction, trend.confidence)

            Resource.Success(
                PredictionResult(
                    symbol = symbol,
                    predictions = predictions,
                    confidenceScores = List(predictions.size) { i ->
                        (trend.confidence * (1 - i * 0.1)).coerceAtLeast(0.5).toDouble()
                    },
                    trend = trend.direction.name.lowercase(),
                    currentPrice = lastPrice,
                    predictedChangePercent = ((predictions.lastOrNull() ?: lastPrice) - lastPrice) / lastPrice * 100,
                    predictedHigh = predictions.maxOrNull() ?: lastPrice,
                    predictedLow = predictions.minOrNull() ?: lastPrice,
                    generatedAt = java.time.Instant.now().toString(),
                    modelVersion = "tflite-v1.0",
                    source = "on-device",
                    isOffline = !isNetworkAvailable()
                )
            )
        } ?: Resource.Error("On-device prediction failed")
    }

    private suspend fun getEnsemblePrediction(
        symbol: String,
        data: List<PricePoint>
    ): Resource<PredictionResult> {
        // Run both predictions and combine results
        val cloudResult = mlRepository.predictLSTM(symbol, data)
        val localResult = getOnDevicePrediction(symbol, data)

        return if (cloudResult is Resource.Success && localResult is Resource.Success) {
            // Weighted average based on confidence
            val cloud = cloudResult.data!!
            val local = localResult.data!!

            val ensemblePredictions = cloud.predictions.zip(local.predictions) { c, l ->
                c * 0.7 + l * 0.3 // Weight cloud higher
            }

            Resource.Success(
                cloud.copy(
                    predictions = ensemblePredictions,
                    source = "ensemble",
                    modelVersion = "ensemble-v1.0"
                )
            )
        } else {
            // Return whichever succeeded
            cloudResult.takeIf { it is Resource.Success }
                ?: localResult
                ?: Resource.Error("Both predictions failed")
        }
    }

    private fun determineStrategy(data: List<PricePoint>, config: MLConfig): MLStrategy {
        return when {
            !config.preferCloud -> MLStrategy.OnDevice
            !isNetworkAvailable() -> MLStrategy.OnDevice
            isBatteryLow() -> MLStrategy.OnDevice
            data.size < MIN_CLOUD_DATA_POINTS -> MLStrategy.OnDevice
            isConnectionPoor() -> MLStrategy.OnDevice
            data.size >= 60 && config.preferCloud -> MLStrategy.Hybrid
            else -> MLStrategy.Cloud
        }
    }

    private fun generatePredictions(
        lastPrice: Double,
        direction: TrendDirection,
        confidence: Float
    ): List<Double> {
        val predictions = mutableListOf<Double>()
        var currentPrice = lastPrice

        // ✅ FIX: All branches return Double (removed 'f' suffix from 0.0)
        val dailyDrift = when (direction) {
            TrendDirection.UP -> 0.005 * confidence
            TrendDirection.DOWN -> -0.005 * confidence
            TrendDirection.NEUTRAL -> 0.0
        }

        repeat(7) {
            currentPrice *= (1 + dailyDrift + ((Math.random() - 0.5) * 0.01))
            predictions.add(currentPrice)
        }

        return predictions
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isBatteryLow(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < LOW_BATTERY_THRESHOLD
    }

    private fun isConnectionPoor(): Boolean {
        // Simplified check - could be enhanced with actual speed test
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return true
        val capabilities = cm.getNetworkCapabilities(network) ?: return true

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Check if 4G/5G
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }
            else -> true
        }
    }

    sealed class MLStrategy {
        object Cloud : MLStrategy()
        object OnDevice : MLStrategy()
        object Hybrid : MLStrategy()
    }
}

object FeatureExtractor {
    fun extract(data: List<PricePoint>): FloatArray {
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
            val change = data[i].close - data[i-1].close
            if (change > 0) gains.add(change) else losses.add(kotlin.math.abs(change))
        }

        val avgGain = if (gains.isNotEmpty()) gains.average() else 0.0
        val avgLoss = if (losses.isNotEmpty()) losses.average() else 0.001

        if (avgLoss == 0.0) return 100f

        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).toFloat()
    }

    private fun calculateMACD(data: List<PricePoint>): Float {
        val closes = data.map { it.close }
        val ema12 = closes.takeLast(12).average()
        val ema26 = closes.takeLast(26).average()
        return (ema12 - ema26).toFloat()
    }
}