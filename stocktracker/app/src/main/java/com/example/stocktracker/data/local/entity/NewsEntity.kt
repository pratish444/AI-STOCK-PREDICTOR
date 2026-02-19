package com.example.stocktracker.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey
    val url: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val source: String,
    val publishedAt: Long,
    val sentiment: String, // positive, negative, neutral
    val sentimentScore: Double,
    val relatedSymbols: String, // Comma-separated symbols
    val category: String?
)