package com.example.stocktracker.data.remote.dto


import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("bestMatches")
    val bestMatches: List<SearchMatch>
)

data class SearchMatch(
    @SerializedName("1. symbol")
    val symbol: String,
    @SerializedName("2. name")
    val name: String,
    @SerializedName("3. type")
    val type: String,
    @SerializedName("4. region")
    val region: String,
    @SerializedName("8. currency")
    val currency: String
)