package com.example.stocktracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TimeSeriesResponse(
    @SerializedName("Meta Data")
    val metaData: MetaData?,
    @SerializedName("Time Series (Daily)")
    val timeSeries: Map<String, TimeSeriesData>?
)

data class MetaData(
    @SerializedName("1. Information")
    val information: String,
    @SerializedName("2. Symbol")
    val symbol: String,
    @SerializedName("3. Last Refreshed")
    val lastRefreshed: String
)

data class TimeSeriesData(
    @SerializedName("1. open")
    val open: String,
    @SerializedName("2. high")
    val high: String,
    @SerializedName("3. low")
    val low: String,
    @SerializedName("4. close")
    val close: String,
    @SerializedName("5. volume")
    val volume: String
)

// For chart data
data class ChartPoint(
    val timestamp: Long,
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)