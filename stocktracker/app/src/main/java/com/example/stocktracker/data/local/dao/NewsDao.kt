package com.example.stocktracker.data.local.dao

import androidx.room.*
import com.example.stocktracker.data.local.entity.NewsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    @Query("SELECT * FROM news ORDER BY publishedAt DESC LIMIT 100")
    fun getAllNews(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news WHERE relatedSymbols LIKE '%' || :symbol || '%' ORDER BY publishedAt DESC")
    fun getNewsForSymbol(symbol: String): Flow<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    @Query("DELETE FROM news WHERE publishedAt < :timestamp")
    suspend fun deleteOldNews(timestamp: Long)

    @Query("DELETE FROM news")
    suspend fun clearAllNews()
}