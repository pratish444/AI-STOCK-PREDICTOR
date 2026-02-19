package com.example.stocktracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stocktracker.data.local.dao.AlertDao
import com.example.stocktracker.data.local.dao.NewsDao
import com.example.stocktracker.data.local.dao.StockDao
import com.example.stocktracker.data.local.entity.AlertEntity
import com.example.stocktracker.data.local.entity.NewsEntity
import com.example.stocktracker.data.local.entity.StockEntity

@Database(
    entities = [
        StockEntity::class,
        AlertEntity::class,
        NewsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun alertDao(): AlertDao
    abstract fun newsDao(): NewsDao
}