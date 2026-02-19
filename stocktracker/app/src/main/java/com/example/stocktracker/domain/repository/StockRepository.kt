package com.example.stocktracker.domain.repository


import com.example.stocktracker.data.remote.dto.ChartPoint
import com.example.stocktracker.domain.model.Alert
import com.example.stocktracker.domain.model.News
import com.example.stocktracker.domain.model.Stock
import kotlinx.coroutines.flow.Flow

interface StockRepository {

    // Stock operations
    fun getWatchlistStocks(): Flow<List<Stock>>
    suspend fun getStock(symbol: String): com.example.stocktracker.domain.util.Resource<Stock>
    fun getStockFlow(symbol: String): Flow<Stock?>
    suspend fun addToWatchlist(symbol: String)
    suspend fun removeFromWatchlist(symbol: String)
    suspend fun searchStocks(query: String): com.example.stocktracker.domain.util.Resource<List<Stock>>
    suspend fun refreshStock(symbol: String): com.example.stocktracker.domain.util.Resource<Stock>

    // Chart data
    suspend fun getChartData(symbol: String, interval: String): com.example.stocktracker.domain.util.Resource<List<ChartPoint>>

    // News operations
    fun getAllNews(): Flow<List<News>>
    fun getNewsForSymbol(symbol: String): Flow<List<News>>
    suspend fun refreshNews(symbol: String? = null): com.example.stocktracker.domain.util.Resource<Unit>

    // Alert operations
    fun getAllAlerts(): Flow<List<Alert>>
    fun getAlertsForSymbol(symbol: String): Flow<List<Alert>>
    suspend fun createAlert(alert: Alert): com.example.stocktracker.domain.util.Resource<Unit>
    suspend fun updateAlert(alert: Alert): com.example.stocktracker.domain.util.Resource<Unit>
    suspend fun deleteAlert(alertId: Int): com.example.stocktracker.domain.util.Resource<Unit>
    suspend fun toggleAlertStatus(alertId: Int, isEnabled: Boolean): com.example.stocktracker.domain.util.Resource<Unit>
    suspend fun checkAlerts(): List<Alert>
}