package com.example.stocktracker.data.remote.api

import com.example.stocktracker.data.remote.dto.NewsResponse
import com.example.stocktracker.data.remote.dto.QuoteResponse
import com.example.stocktracker.data.remote.dto.SearchResponse
import com.example.stocktracker.data.remote.dto.TimeSeriesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {

    @GET("query")
    suspend fun getQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): QuoteResponse

    @GET("query")
    suspend fun searchSymbol(
        @Query("function") function: String = "SYMBOL_SEARCH",
        @Query("keywords") keywords: String,
        @Query("apikey") apiKey: String
    ): SearchResponse

    @GET("query")
    suspend fun getTimeSeriesDaily(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact", // compact or full
        @Query("apikey") apiKey: String
    ): TimeSeriesResponse

    @GET("query")
    suspend fun getNews(
        @Query("function") function: String = "NEWS_SENTIMENT",
        @Query("tickers") tickers: String? = null,
        @Query("topics") topics: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("sort") sort: String = "LATEST",
        @Query("apikey") apiKey: String
    ): NewsResponse

    companion object {
        const val BASE_URL = "https://www.alphavantage.co/"
    }
}