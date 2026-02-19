package com.example.stocktracker.data.mapper

import com.example.stocktracker.data.local.entity.AlertEntity
import com.example.stocktracker.data.local.entity.NewsEntity
import com.example.stocktracker.data.local.entity.StockEntity
import com.example.stocktracker.data.remote.dto.*
import com.example.stocktracker.domain.model.Alert
import com.example.stocktracker.domain.model.News
import com.example.stocktracker.domain.model.Stock
import java.text.SimpleDateFormat
import java.util.*

// Stock Entity to Domain
fun StockEntity.toStock(): Stock {
    return Stock(
        symbol = symbol,
        name = name,
        currentPrice = currentPrice,
        changeAmount = changeAmount,
        changePercent = changePercent,
        dayHigh = dayHigh,
        dayLow = dayLow,
        openPrice = openPrice,
        previousClose = previousClose,
        volume = volume,
        isInWatchlist = isInWatchlist
    )
}

// Stock Domain to Entity
fun Stock.toEntity(): StockEntity {
    return StockEntity(
        symbol = symbol,
        name = name,
        currentPrice = currentPrice,
        changeAmount = changeAmount,
        changePercent = changePercent,
        dayHigh = dayHigh,
        dayLow = dayLow,
        openPrice = openPrice,
        previousClose = previousClose,
        volume = volume,
        lastUpdated = System.currentTimeMillis(),
        isInWatchlist = isInWatchlist
    )
}

// Quote Response to Stock Entity
fun GlobalQuote.toStockEntity(name: String, isInWatchlist: Boolean = false): StockEntity {
    val changePercentValue = changePercent.removeSuffix("%").toDoubleOrNull() ?: 0.0
    return StockEntity(
        symbol = symbol,
        name = name,
        currentPrice = price.toDoubleOrNull() ?: 0.0,
        changeAmount = change.toDoubleOrNull() ?: 0.0,
        changePercent = changePercentValue,
        dayHigh = high.toDoubleOrNull() ?: 0.0,
        dayLow = low.toDoubleOrNull() ?: 0.0,
        openPrice = open.toDoubleOrNull() ?: 0.0,
        previousClose = previousClose.toDoubleOrNull() ?: 0.0,
        volume = volume.toLongOrNull() ?: 0L,
        lastUpdated = System.currentTimeMillis(),
        isInWatchlist = isInWatchlist
    )
}

// News Entity to Domain
fun NewsEntity.toNews(): News {
    return News(
        url = url,
        title = title,
        summary = summary,
        imageUrl = imageUrl,
        source = source,
        publishedAt = publishedAt,
        sentiment = sentiment,
        sentimentScore = sentimentScore,
        relatedSymbols = relatedSymbols.split(",").filter { it.isNotBlank() },
        category = category
    )
}

// News Item to Entity
fun NewsItem.toNewsEntity(): NewsEntity {
    val tickers = tickerSentiment?.joinToString(",") { it.ticker } ?: ""
    val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
    val timestamp = try {
        dateFormat.parse(timePublished)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    return NewsEntity(
        url = url,
        title = title,
        summary = summary,
        imageUrl = bannerImage,
        source = source,
        publishedAt = timestamp,
        sentiment = sentimentLabel,
        sentimentScore = sentimentScore,
        relatedSymbols = tickers,
        category = category
    )
}

// Alert Entity to Domain
fun AlertEntity.toAlert(): Alert {
    return Alert(
        id = id,
        symbol = symbol,
        stockName = stockName,
        alertType = alertType.name,
        targetPrice = targetPrice,
        isEnabled = isEnabled,
        createdAt = createdAt
    )
}

// Alert Domain to Entity
fun Alert.toEntity(): AlertEntity {
    return AlertEntity(
        id = id,
        symbol = symbol,
        stockName = stockName,
        alertType = com.example.stocktracker.data.local.entity.AlertType.valueOf(alertType),
        targetPrice = targetPrice,
        isEnabled = isEnabled,
        createdAt = createdAt
    )
}

// Time Series to Chart Points
fun TimeSeriesResponse.toChartPoints(): List<ChartPoint> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return timeSeries?.map { (date, data) ->
        ChartPoint(
            timestamp = dateFormat.parse(date)?.time ?: 0L,
            date = date,
            open = data.open.toDoubleOrNull() ?: 0.0,
            high = data.high.toDoubleOrNull() ?: 0.0,
            low = data.low.toDoubleOrNull() ?: 0.0,
            close = data.close.toDoubleOrNull() ?: 0.0,
            volume = data.volume.toLongOrNull() ?: 0L
        )
    }?.sortedBy { it.timestamp } ?: emptyList()
}