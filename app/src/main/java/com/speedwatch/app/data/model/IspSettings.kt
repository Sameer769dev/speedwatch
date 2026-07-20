package com.speedwatch.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "isp_settings")
data class IspSettings(
    @PrimaryKey val id: Int = 0, // Singleton setting
    val promisedDownloadMbps: Double,
    val promisedUploadMbps: Double,
    val ispName: String = "",
    val isPremium: Boolean = false,
    val checkFrequencyHours: Int = 6, // Default for free
    val allowMobileBackgroundTests: Boolean = false,
    val dataUsageCapMB: Int = 0, // 0 = unlimited
    val dataUsageAlertEnabled: Boolean = true,
    val themePreference: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val speedDropAlertsEnabled: Boolean = true,
    val reportAlertsEnabled: Boolean = true
)
