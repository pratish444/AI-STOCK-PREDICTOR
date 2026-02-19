package com.example.stocktracker.data.remote.dto

import com.example.stocktracker.presentation.screens.dashboard.MarketIndex
import com.google.gson.annotations.SerializedName

// ========== Request DTOs ==========

data class PredictionRequestDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("features") val features: List<List<Float>>,
    @SerializedName("days_to_predict") val days_to_predict: Int = 7
)

data class SentimentRequestDto(
    @SerializedName("texts") val texts: List<String>,
    @SerializedName("symbol") val symbol: String? = null
)

data class TechnicalIndicatorRequestDto(
    @SerializedName("data") val data: List<List<Float>>,
    @SerializedName("indicators") val indicators: List<String> = listOf("rsi", "macd", "sma", "ema")
)

// ========== Response DTOs ==========

data class HealthCheckDto(
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class PredictionResponseDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("predictions") val predictions: List<Double>,
    @SerializedName("confidence_scores") val confidence_scores: List<Double>,
    @SerializedName("trend") val trend: String,
    @SerializedName("current_price") val current_price: Double,
    @SerializedName("predicted_change_percent") val predicted_change_percent: Double,
    @SerializedName("predicted_high") val predicted_high: Double,
    @SerializedName("predicted_low") val predicted_low: Double,
    @SerializedName("generated_at") val generated_at: String,
    @SerializedName("model_version") val model_version: String,
    @SerializedName("source") val source: String = "cloud-lstm"
)

data class SentimentItemDto(
    @SerializedName("text") val text: String,
    @SerializedName("label") val label: String,
    @SerializedName("score") val score: Double,
    @SerializedName("keywords") val keywords: List<String>
)

data class SentimentResponseDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("sentiments") val sentiments: List<SentimentItemDto>,
    @SerializedName("overall_score") val overall_score: Double,
    @SerializedName("overall_label") val overall_label: String,
    @SerializedName("recommendation") val recommendation: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("bullish_count") val bullish_count: Int,
    @SerializedName("bearish_count") val bearish_count: Int,
    @SerializedName("neutral_count") val neutral_count: Int
)

data class TechnicalIndicatorResponseDto(
    @SerializedName("rsi") val rsi: Double?,
    @SerializedName("macd") val macd: Double?,
    @SerializedName("sma_20") val sma_20: Double?,
    @SerializedName("ema_12") val ema_12: Double?,
    @SerializedName("bollinger_upper") val bollinger_upper: Double?,
    @SerializedName("bollinger_lower") val bollinger_lower: Double?,
    @SerializedName("signals") val signals: Map<String, String>
)

data class MockStockResponseDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("change_percent") val change_percent: Double,
    @SerializedName("volume") val volume: Int,
    @SerializedName("high") val high: Double,
    @SerializedName("low") val low: Double,
    @SerializedName("open") val open: Double,
    @SerializedName("previous_close") val previous_close: Double,
    @SerializedName("market_cap") val market_cap: String,
    @SerializedName("pe_ratio") val pe_ratio: Double,
    @SerializedName("timestamp") val timestamp: String
)

data class MarketIndexDto(
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("value") val value: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("change_percent") val change_percent: Double
)

data class MarketOverviewDto(
    @SerializedName("indices") val indices: List<MarketIndexDto>,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("market_status") val market_status: String
)

// ✅ Moved here from duplicate location - single source of truth
data class MarketOverviewResponse(
    val indices: List<MarketIndex>
)