package com.speedwatch.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.data.model.SpeedLog
import com.speedwatch.app.data.repository.SpeedRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

class ReportsViewModel(private val repository: SpeedRepository) : ViewModel() {

    private val _logs = repository.allLogs
    val settings = repository.ispSettings

    val reportState = combine(_logs, settings) { logs, settings ->
        calculateReports(logs, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun calculateReports(logs: List<SpeedLog>, settings: IspSettings?): ReportsData {
        if (logs.isEmpty()) {
            return ReportsData(
                isDataAvailable = false,
                dayReport = null,
                weekReport = null,
                monthReport = null,
                overallStatus = OverallStatus("", "", androidx.compose.ui.graphics.Color.Gray),
                gamingStatus = GamingStatus("", ""),
                streamingStatus = StreamingStatus("", "")
            )
        }

        val now = System.currentTimeMillis()
        val dayAgo = now - 24 * 60 * 60 * 1000
        val weekAgo = now - 7 * 24 * 60 * 60 * 1000
        val monthAgo = now - 30L * 24 * 60 * 60 * 1000

        val dayLogs = logs.filter { it.timestamp >= dayAgo }
        val weekLogs = logs.filter { it.timestamp >= weekAgo }
        val monthLogs = logs.filter { it.timestamp >= monthAgo }

        // Progressive Reporting: Only show larger periods if they contain more/different data
        val dayReport = if (dayLogs.isNotEmpty()) generateSummary(dayLogs, settings, "Last 24 Hours") else null
        
        val weekReport = if (weekLogs.isNotEmpty() && weekLogs.size > dayLogs.size) {
            generateSummary(weekLogs, settings, "Last 7 Days")
        } else null

        val monthReport = if (monthLogs.isNotEmpty() && monthLogs.size > weekLogs.size) {
            generateSummary(monthLogs, settings, "Last 30 Days")
        } else null

        return ReportsData(
            isDataAvailable = true,
            dayReport = dayReport,
            weekReport = weekReport,
            monthReport = monthReport,
            overallStatus = calculateOverallStatus(logs.take(5), settings),
            gamingStatus = calculateGamingStatus(logs.take(5)),
            streamingStatus = calculateStreamingStatus(logs.take(5))
        )
    }

    private fun generateSummary(logs: List<SpeedLog>, settings: IspSettings?, title: String): PeriodSummary {
        if (logs.isEmpty()) return PeriodSummary(title, 0.0, 0.0, 0, "No data")
        
        val avgDown = logs.map { it.downloadSpeedMbps }.average()
        val avgUp = logs.map { it.uploadSpeedMbps }.average()
        val avgPing = logs.map { it.latencyMs }.average().toInt()
        
        val status = if (settings != null) {
            val ratio = avgDown / settings.promisedDownloadMbps
            when {
                ratio >= 0.9 -> "Excellent"
                ratio >= 0.7 -> "Good"
                ratio >= 0.5 -> "Fair"
                else -> "Poor"
            }
        } else "Logged"

        return PeriodSummary(title, avgDown, avgUp, avgPing, status)
    }

    private fun calculateOverallStatus(recentLogs: List<SpeedLog>, settings: IspSettings?): OverallStatus {
        if (recentLogs.isEmpty()) return OverallStatus("Unknown", "Run a test to see status", androidx.compose.ui.graphics.Color.Gray)
        
        val latest = recentLogs.first()
        val statusText: String
        val detail: String
        val color: androidx.compose.ui.graphics.Color

        if (settings != null) {
            val ratio = latest.downloadSpeedMbps / settings.promisedDownloadMbps
            when {
                ratio >= 0.9 -> {
                    statusText = "Healthy"
                    detail = "Your internet is performing as promised."
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                }
                ratio >= 0.7 -> {
                    statusText = "Stable"
                    detail = "Slightly below peak, but still good."
                    color = androidx.compose.ui.graphics.Color(0xFF8BC34A)
                }
                else -> {
                    statusText = "Issues Detected"
                    detail = "Speed is significantly lower than promised."
                    color = androidx.compose.ui.graphics.Color(0xFFF44336)
                }
            }
        } else {
            statusText = "Setup Required"
            detail = "Configure your ISP plan in Settings for a full health check."
            color = androidx.compose.ui.graphics.Color(0xFFFF9800) // Warning Orange
        }
        
        return OverallStatus(statusText, detail, color)
    }
    private fun calculateGamingStatus(recentLogs: List<SpeedLog>): GamingStatus {
        if (recentLogs.isEmpty()) return GamingStatus("Unknown", "No ping data")
        
        val avgPing = recentLogs.map { it.latencyMs }.average()
        return when {
            avgPing < 20 -> GamingStatus("Pro Grade", "Ultra-low latency for competitive gaming.")
            avgPing < 50 -> GamingStatus("Smooth", "Great for most online games.")
            avgPing < 100 -> GamingStatus("Playable", "Occasional lag possible.")
            else -> GamingStatus("Poor", "High latency will affect gameplay.")
        }
    }

    private fun calculateStreamingStatus(recentLogs: List<SpeedLog>): StreamingStatus {
        if (recentLogs.isEmpty()) return StreamingStatus("Unknown", "No speed data")
        
        val avgDown = recentLogs.map { it.downloadSpeedMbps }.average()
        return when {
            avgDown >= 50 -> StreamingStatus("8K Ultra HD", "Flawless streaming on multiple 8K devices.")
            avgDown >= 25 -> StreamingStatus("4K Ultra HD", "Perfect for high-quality 4K HDR content.")
            avgDown >= 10 -> StreamingStatus("Full HD", "Smooth 1080p streaming experience.")
            else -> StreamingStatus("Basic", "Limited to SD or single HD stream.")
        }
    }
}

data class ReportsData(
    val isDataAvailable: Boolean,
    val dayReport: PeriodSummary?,
    val weekReport: PeriodSummary?,
    val monthReport: PeriodSummary?,
    val overallStatus: OverallStatus,
    val gamingStatus: GamingStatus,
    val streamingStatus: StreamingStatus
)

data class GamingStatus(
    val grade: String,
    val description: String
)

data class StreamingStatus(
    val grade: String,
    val description: String
)

data class PeriodSummary(
    val title: String,
    val avgDownload: Double,
    val avgUpload: Double,
    val avgPing: Int,
    val status: String
)

data class OverallStatus(
    val title: String,
    val description: String,
    val color: androidx.compose.ui.graphics.Color
)
