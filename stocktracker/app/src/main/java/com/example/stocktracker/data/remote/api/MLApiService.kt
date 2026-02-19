package com.example.stocktracker.data.remote.api

import com.example.stocktracker.data.remote.dto.*
import retrofit2.http.*

interface MLApiService {

    @GET("/health")
    suspend fun healthCheck(): HealthCheckDto

    @POST("/api/v1/predict/lstm")
    suspend fun getLSTMPrediction(
        @Body request: PredictionRequestDto
    ): PredictionResponseDto

    @POST("/api/v1/analyze/sentiment")
    suspend fun analyzeSentiment(
        @Body request: SentimentRequestDto
    ): SentimentResponseDto

    @POST("/api/v1/indicators/calculate")
    suspend fun calculateIndicators(
        @Body request: TechnicalIndicatorRequestDto
    ): TechnicalIndicatorResponseDto

    @GET("/api/v1/mock/stock/{symbol}")
    suspend fun getMockStockData(
        @Path("symbol") symbol: String,
        @Query("trend") trend: String? = null
    ): MockStockResponseDto

    @GET("/api/v1/market/overview")
    suspend fun getMarketOverview(): MarketOverviewDto
}