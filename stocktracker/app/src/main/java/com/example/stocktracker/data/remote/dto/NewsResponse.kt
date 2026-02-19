package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("feed")
    val feed: List<NewsItem>
)

data class NewsItem(
    @SerializedName("title")
    val title: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("time_published")
    val timePublished: String,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("banner_image")
    val bannerImage: String?,
    @SerializedName("source")
    val source: String,
    @SerializedName("category_within_source")
    val category: String?,
    @SerializedName("overall_sentiment_score")
    val sentimentScore: Double,
    @SerializedName("overall_sentiment_label")
    val sentimentLabel: String,
    @SerializedName("ticker_sentiment")
    val tickerSentiment: List<TickerSentiment>?
)

data class TickerSentiment(
    @SerializedName("ticker")
    val ticker: String,
    @SerializedName("relevance_score")
    val relevanceScore: String,
    @SerializedName("ticker_sentiment_score")
    val sentimentScore: String,
    @SerializedName("ticker_sentiment_label")
    val sentimentLabel: String
)