package com.speedwatch.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lab_audits")
data class LabAudit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val testType: String, // "THROTTLING", "BUFFERBLOAT", "DNS"
    val mainResult: String, // e.g., "Optimal", "Grade A", "Cloudflare"
    val technicalDetails: String, // JSON or formatted string
    val networkSnapshot: String // e.g., "Wi-Fi 5GHz -50dBm"
)
