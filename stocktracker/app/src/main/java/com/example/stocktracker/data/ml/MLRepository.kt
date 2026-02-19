package com.example.stocktracker.data.ml

import com.example.stocktracker.data.remote.dto.MarketOverviewResponse
import com.example.stocktracker.domain.model.*
import com.example.stocktracker.domain.util.Resource

interface MLRepository {

    // ========== On-Device (TFLite) ==========
    suspend fun predictTrendLocal(features: FloatArray): Resource<TrendPrediction>

    // ========== Cloud API ==========
    suspend fun predictLSTM(
        symbol: String,
        historicalData: List<PricePoint>
    ): Resource<PredictionResult>

    suspend fun analyzeSentiment(
        texts: List<String>,
        symbol: String? = null
    ): Resource<SentimentResult>

    suspend fun calculateIndicators(
        data: List<List<Float>>
    ): Resource<TechnicalIndicators>

    // ========== Hybrid Smart Routing ==========
    suspend fun getPrediction(
        symbol: String,
        historicalData: List<PricePoint>
    ): Resource<PredictionResult>

    suspend fun isBackendAvailable(): Boolean

    suspend fun getMockStockData(symbol: String): Resource<MockStockData>

    // ========== Market Overview ==========
    suspend fun getMarketOverview(): Resource<MarketOverviewResponse>
}

data class MockStockData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val high: Double,
    val low: Double,
    val open: Double,
    val previousClose: Double,
    val marketCap: String,
    val peRatio: Double
)