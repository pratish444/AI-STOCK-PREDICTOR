package com.stocktracker.app.data.repository

import com.example.stocktracker.BuildConfig
import com.example.stocktracker.data.local.dao.AlertDao
import com.example.stocktracker.data.local.dao.NewsDao
import com.example.stocktracker.data.local.dao.StockDao
import com.example.stocktracker.data.local.entity.AlertType
import com.example.stocktracker.data.mapper.*
import com.example.stocktracker.data.remote.api.AlphaVantageApi
import com.example.stocktracker.data.remote.dto.ChartPoint
import com.example.stocktracker.domain.model.Alert
import com.example.stocktracker.domain.model.News
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.domain.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StockRepositoryImpl @Inject constructor(
    private val api: AlphaVantageApi,
    private val stockDao: StockDao,
    private val newsDao: NewsDao,
    private val alertDao: AlertDao
) : StockRepository {

    private var lastApiCallTime = 0L

    // Rate limiting: Wait at least 2 seconds between API calls
    private suspend fun respectRateLimit() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCall = currentTime - lastApiCallTime

        if (timeSinceLastCall < 2000) { // 2 seconds
            delay(2000 - timeSinceLastCall)
        }

        lastApiCallTime = System.currentTimeMillis()
    }

    override fun getWatchlistStocks(): Flow<List<Stock>> {
        return stockDao.getWatchlistStocks().map { entities ->
            entities.map { it.toStock() }
        }
    }

    override suspend fun getStock(symbol: String): Resource<Stock> {
        return try {
            // Try cache first - use cache if less than 5 minutes old
            val cached = stockDao.getStock(symbol)
            if (cached != null && System.currentTimeMillis() - cached.lastUpdated < 300000) {
                return Resource.Success(cached.toStock())
            }

            // Rate limit before API call
            respectRateLimit()

            // Fetch from API
            val response = api.getQuote(symbol = symbol, apiKey = BuildConfig.ALPHA_VANTAGE_API_KEY)
            val quote = response.globalQuote

            if (quote != null) {
                val stockEntity = quote.toStockEntity(
                    name = cached?.name ?: symbol,
                    isInWatchlist = cached?.isInWatchlist ?: false
                )
                stockDao.insertStock(stockEntity)
                Resource.Success(stockEntity.toStock())
            } else {
                Resource.Error("Stock not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "An error occurred")
        }
    }

    override fun getStockFlow(symbol: String): Flow<Stock?> {
        return stockDao.getStockFlow(symbol).map { it?.toStock() }
    }

    override suspend fun addToWatchlist(symbol: String) {
        stockDao.updateWatchlistStatus(symbol, true)
    }

    override suspend fun removeFromWatchlist(symbol: String) {
        stockDao.updateWatchlistStatus(symbol, false)
    }

    override suspend fun searchStocks(query: String): Resource<List<Stock>> {
        return try {
            respectRateLimit()

            val response = api.searchSymbol(
                keywords = query,
                apiKey = BuildConfig.ALPHA_VANTAGE_API_KEY
            )

            val stocks = response.bestMatches.map { match ->
                Stock(
                    symbol = match.symbol,
                    name = match.name,
                    currentPrice = 0.0,
                    changeAmount = 0.0,
                    changePercent = 0.0,
                    dayHigh = 0.0,
                    dayLow = 0.0,
                    openPrice = 0.0,
                    previousClose = 0.0,
                    volume = 0L,
                    isInWatchlist = false
                )
            }
            Resource.Success(stocks)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Search failed")
        }
    }

    override suspend fun refreshStock(symbol: String): Resource<Stock> {
        return getStock(symbol)
    }

    override suspend fun getChartData(symbol: String, interval: String): Resource<List<ChartPoint>> {
        return try {
            respectRateLimit()

            val response = api.getTimeSeriesDaily(
                symbol = symbol,
                outputSize = if (interval == "1Y" || interval == "5Y") "full" else "compact",
                apiKey = BuildConfig.ALPHA_VANTAGE_API_KEY
            )

            val chartPoints = response.toChartPoints()

            val filtered = when (interval) {
                "1D" -> chartPoints.takeLast(1)
                "1W" -> chartPoints.takeLast(7)
                "1M" -> chartPoints.takeLast(30)
                "3M" -> chartPoints.takeLast(90)
                "1Y" -> chartPoints.takeLast(365)
                "5Y" -> chartPoints
                else -> chartPoints.takeLast(30)
            }

            Resource.Success(filtered)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch chart data")
        }
    }

    override fun getAllNews(): Flow<List<News>> {
        return newsDao.getAllNews().map { entities ->
            entities.map { it.toNews() }
        }
    }

    override fun getNewsForSymbol(symbol: String): Flow<List<News>> {
        return newsDao.getNewsForSymbol(symbol).map { entities ->
            entities.map { it.toNews() }
        }
    }

    override suspend fun refreshNews(symbol: String?): Resource<Unit> {
        return try {
            respectRateLimit()

            val response = api.getNews(
                tickers = symbol,
                apiKey = BuildConfig.ALPHA_VANTAGE_API_KEY
            )

            val newsEntities = response.feed.map { it.toNewsEntity() }
            newsDao.insertNews(newsEntities)

            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            newsDao.deleteOldNews(sevenDaysAgo)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch news")
        }
    }

    override fun getAllAlerts(): Flow<List<Alert>> {
        return alertDao.getAllAlerts().map { entities ->
            entities.map { it.toAlert() }
        }
    }

    override fun getAlertsForSymbol(symbol: String): Flow<List<Alert>> {
        return alertDao.getAlertsForSymbol(symbol).map { entities ->
            entities.map { it.toAlert() }
        }
    }

    override suspend fun createAlert(alert: Alert): Resource<Unit> {
        return try {
            alertDao.insertAlert(alert.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create alert")
        }
    }

    override suspend fun updateAlert(alert: Alert): Resource<Unit> {
        return try {
            alertDao.updateAlert(alert.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update alert")
        }
    }

    override suspend fun deleteAlert(alertId: Int): Resource<Unit> {
        return try {
            alertDao.deleteAlertById(alertId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete alert")
        }
    }

    override suspend fun toggleAlertStatus(alertId: Int, isEnabled: Boolean): Resource<Unit> {
        return try {
            alertDao.updateAlertStatus(alertId, isEnabled)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to toggle alert")
        }
    }

    override suspend fun checkAlerts(): List<Alert> {
        val enabledAlerts = alertDao.getEnabledAlerts()
        val triggeredAlerts = mutableListOf<Alert>()

        for (alertEntity in enabledAlerts) {
            try {
                val stock = getStock(alertEntity.symbol)
                if (stock is Resource.Success && stock.data != null) {
                    val currentPrice = stock.data.currentPrice
                    val triggered = when (alertEntity.alertType) {
                        AlertType.ABOVE -> currentPrice >= alertEntity.targetPrice
                        AlertType.BELOW -> currentPrice <= alertEntity.targetPrice
                        AlertType.PERCENT_CHANGE_UP -> stock.data.changePercent >= alertEntity.targetPrice
                        AlertType.PERCENT_CHANGE_DOWN -> stock.data.changePercent <= -alertEntity.targetPrice
                    }

                    if (triggered) {
                        triggeredAlerts.add(alertEntity.toAlert())
                    }
                }
            } catch (e: Exception) {
                // Continue checking other alerts
            }
        }

        return triggeredAlerts
    }
}