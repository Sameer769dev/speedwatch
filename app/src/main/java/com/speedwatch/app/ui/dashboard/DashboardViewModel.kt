package com.speedwatch.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedwatch.app.data.model.SpeedLog
import com.speedwatch.app.data.repository.SpeedRepository
import com.speedwatch.app.domain.NetworkDetails
import com.speedwatch.app.domain.NetworkInfoProvider
import com.speedwatch.app.domain.SpeedMeasurer
import com.speedwatch.app.domain.SpeedResult
import com.speedwatch.app.ui.notifications.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SpeedRepository,
    private val speedMeasurer: SpeedMeasurer,
    private val networkInfoProvider: NetworkInfoProvider,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _networkDetails = MutableStateFlow<NetworkDetails?>(null)
    val networkDetails: StateFlow<NetworkDetails?> = _networkDetails.asStateFlow()

    init {
        updateNetworkDetails()
    }

    fun updateNetworkDetails() {
        _networkDetails.value = networkInfoProvider.getNetworkDetails()
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            updateNetworkDetails()
            val currentDetails = _networkDetails.value
            
            _uiState.value = DashboardUiState.Testing("Latency & Jitter", 0.0)
            val latency = speedMeasurer.measureLatency() ?: 0
            val jitter = speedMeasurer.measureJitter() ?: 0.0
            
            _uiState.value = DashboardUiState.Testing("Download", 0.0)
            var lastDownload: SpeedResult? = null
            speedMeasurer.measureDownloadFlow().collect { result ->
                lastDownload = result
                _uiState.value = DashboardUiState.Testing("Download", result.mbps)
            }
            
            if (lastDownload != null) {
                val finalDownload = lastDownload!!
                _uiState.value = DashboardUiState.Testing("Upload", 0.0)
                var lastUpload: SpeedResult? = null
                speedMeasurer.measureUploadFlow().collect { result ->
                    lastUpload = result
                    _uiState.value = DashboardUiState.Testing("Upload", result.mbps)
                }
                
                val finalUpload = lastUpload ?: SpeedResult(0.0, 0L)
                
                val log = SpeedLog(
                    timestamp = System.currentTimeMillis(),
                    downloadSpeedMbps = finalDownload.mbps,
                    uploadSpeedMbps = finalUpload.mbps,
                    latencyMs = latency,
                    networkType = currentDetails?.transport ?: "Manual",
                    jitterMs = jitter,
                    signalStrength = currentDetails?.signalStrength,
                    dataUsageBytes = finalDownload.bytesUsed + finalUpload.bytesUsed
                )
                repository.insertLog(log)

                // History Limit Upsell
                val settings = repository.ispSettings.firstOrNull()
                if (settings?.isPremium == false) {
                    val logs = repository.allLogs.firstOrNull() ?: emptyList()
                    if (logs.size >= 10) {
                        notificationHelper.showUpgradeUpsell(
                            "History Limit Reached",
                            "Free version limits history to 10 logs. Go Pro for unlimited tracking!"
                        )
                    }
                }

                _uiState.value = DashboardUiState.Success(finalDownload.mbps, finalUpload.mbps, latency, jitter)
            } else {
                _uiState.value = DashboardUiState.Error("Failed to measure download speed")
            }
        }
    }
}

sealed interface DashboardUiState {
    object Idle : DashboardUiState
    data class Testing(val stage: String, val lastResult: Double) : DashboardUiState
    data class Success(val download: Double, val upload: Double, val latency: Int, val jitter: Double) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
