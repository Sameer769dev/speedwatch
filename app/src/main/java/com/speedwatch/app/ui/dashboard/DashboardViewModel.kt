package com.speedwatch.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedwatch.app.data.model.SpeedLog
import com.speedwatch.app.data.repository.SpeedRepository
import com.speedwatch.app.domain.NetworkDetails
import com.speedwatch.app.domain.NetworkInfoProvider
import com.speedwatch.app.domain.SpeedMeasurer
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
            val downloadResult = speedMeasurer.measureDownloadSpeed()
            
            if (downloadResult != null) {
                _uiState.value = DashboardUiState.Testing("Upload", downloadResult.mbps)
                val uploadResult = speedMeasurer.measureUploadSpeed()
                
                val log = SpeedLog(
                    timestamp = System.currentTimeMillis(),
                    downloadSpeedMbps = downloadResult.mbps,
                    uploadSpeedMbps = uploadResult?.mbps ?: 0.0,
                    latencyMs = latency,
                    networkType = currentDetails?.transport ?: "Manual",
                    jitterMs = jitter,
                    signalStrength = currentDetails?.signalStrength,
                    dataUsageBytes = downloadResult.bytesUsed + (uploadResult?.bytesUsed ?: 0L)
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

                _uiState.value = DashboardUiState.Success(downloadResult.mbps, uploadResult?.mbps ?: 0.0, latency, jitter)
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
