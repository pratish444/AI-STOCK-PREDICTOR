package com.example.stocktracker.domain.model

/**
 * ML Prediction Result from Cloud API
 */
data class PredictionResult(
    val symbol: String,
    val predictions: List<Double>,
    val confidenceScores: List<Double>,
    val trend: String,  // "bullish", "bearish", "neutral"
    val currentPrice: Double,
    val predictedChangePercent: Double,
    val predictedHigh: Double,
    val predictedLow: Double,
    val generatedAt: String,
    val modelVersion: String,
    val source: String = "cloud",  // "cloud", "on-device", "on-device-fallback"
    val isOffline: Boolean = false
)

/**
 * On-device Trend Prediction (TFLite)
 */
data class TrendPrediction(
    val direction: TrendDirection,
    val confidence: Float,
    val indicators: TechnicalIndicators? = null
)

enum class TrendDirection {
    UP, DOWN, NEUTRAL
}

/**
 * Sentiment Analysis Result
 */
data class SentimentResult(
    val symbol: String?,
    val overallScore: Double,  // -1.0 to 1.0
    val overallLabel: String,  // "positive", "negative", "neutral"
    val recommendation: String, // "buy", "sell", "hold"
    val confidence: Double,
    val sentiments: List<SentimentItem>,
    val bullishCount: Int,
    val bearishCount: Int,
    val neutralCount: Int
)

data class SentimentItem(
    val text: String,
    val label: String,
    val score: Double,
    val keywords: List<String>
)

/**
 * Technical Indicators
 */
data class TechnicalIndicators(
    val rsi: Double?,
    val macd: Double?,
    val sma20: Double?,
    val ema12: Double?,
    val bollingerUpper: Double?,
    val bollingerLower: Double?,
    val signals: Map<String, String>
)

/**
 * Price Point for ML features - USING DOUBLE for precision
 */
data class PricePoint(
    val timestamp: Long = System.currentTimeMillis(),
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)