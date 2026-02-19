package com.example.stocktracker.data.local.dao

import androidx.room.*
import com.example.stocktracker.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE symbol = :symbol")
    fun getAlertsForSymbol(symbol: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isEnabled = 1")
    suspend fun getEnabledAlerts(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isEnabled = :isEnabled WHERE id = :alertId")
    suspend fun updateAlertStatus(alertId: Int, isEnabled: Boolean)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlertById(alertId: Int)
}