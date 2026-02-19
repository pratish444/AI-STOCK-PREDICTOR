package com.example.stocktracker.domain.model

data class Stock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val changeAmount: Double,
    val changePercent: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val openPrice: Double,
    val previousClose: Double,
    val volume: Long,
    val isInWatchlist: Boolean = false
)

data class Alert(
    val id: Int = 0,
    val symbol: String,
    val stockName: String,
    val alertType: String,
    val targetPrice: Double,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isTriggered: Boolean = false
)

data class News(
    val url: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val source: String,
    val publishedAt: Long,
    val sentiment: String,
    val sentimentScore: Double,
    val relatedSymbols: List<String>,
    val category: String?
)

data class MarketIndex(
    val name: String,
    val symbol: String,
    val value: Double,
    val change: Double,
    val changePercent: Double
)