package com.speedwatch.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_logs")
data class SpeedLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Int = 0,
    val networkType: String,
    val jitterMs: Double = 0.0,
    val signalStrength: Int? = null,
    val dataUsageBytes: Long = 0L
)
