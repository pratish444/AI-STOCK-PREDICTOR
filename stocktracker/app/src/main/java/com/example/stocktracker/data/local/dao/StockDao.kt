package com.example.stocktracker.data.local.dao


import androidx.room.*
import com.example.stocktracker.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query("SELECT * FROM stocks WHERE isInWatchlist = 1 ORDER BY symbol ASC")
    fun getWatchlistStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStock(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    fun getStockFlow(symbol: String): Flow<StockEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Query("UPDATE stocks SET isInWatchlist = :isInWatchlist WHERE symbol = :symbol")
    suspend fun updateWatchlistStatus(symbol: String, isInWatchlist: Boolean)

    @Delete
    suspend fun deleteStock(stock: StockEntity)

    @Query("DELETE FROM stocks WHERE symbol = :symbol")
    suspend fun deleteStockBySymbol(symbol: String)

    @Query("SELECT * FROM stocks WHERE symbol LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' LIMIT 20")
    suspend fun searchStocks(query: String): List<StockEntity>

    @Query("DELETE FROM stocks WHERE isInWatchlist = 0 AND lastUpdated < :timestamp")
    suspend fun deleteOldNonWatchlistStocks(timestamp: Long)
}