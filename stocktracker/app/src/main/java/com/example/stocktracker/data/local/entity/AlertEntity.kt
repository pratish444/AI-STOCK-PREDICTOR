package com.example.stocktracker.data.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val symbol: String,
    val stockName: String,
    val alertType: AlertType,
    val targetPrice: Double,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AlertType {
    ABOVE,
    BELOW,
    PERCENT_CHANGE_UP,
    PERCENT_CHANGE_DOWN
}
