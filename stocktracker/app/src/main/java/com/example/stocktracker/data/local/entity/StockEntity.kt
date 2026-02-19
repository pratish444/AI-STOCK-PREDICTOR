package com.example.stocktracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey
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
    val lastUpdated: Long,
    val isInWatchlist: Boolean = false
)
