package com.speedwatch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.data.repository.SpeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class SettingsViewModel(private val repository: SpeedRepository) : ViewModel() {

    val settings: StateFlow<IspSettings?> = repository.ispSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveSettings(ispName: String, download: Double, upload: Double) {
        viewModelScope.launch {
            val current = repository.ispSettings.firstOrNull()
            if (current != null) {
                repository.saveIspSettings(
                    current.copy(
                        ispName = ispName,
                        promisedDownloadMbps = download,
                        promisedUploadMbps = upload
                    )
                )
            } else {
                // Should not happen as settings are created during onboarding
                repository.saveIspSettings(
                    IspSettings(
                        ispName = ispName,
                        promisedDownloadMbps = download,
                        promisedUploadMbps = upload
                    )
                )
            }
        }
    }

    fun setPremium(isPremium: Boolean) {
        viewModelScope.launch {
            repository.setPremium(isPremium)
        }
    }

    fun setFrequency(hours: Int) {
        viewModelScope.launch {
            repository.setCheckFrequency(hours)
        }
    }

    fun setAllowMobileBackground(allow: Boolean) {
        viewModelScope.launch {
            repository.setAllowMobileBackground(allow)
        }
    }

    fun setDataUsageCap(capMB: Int) {
        viewModelScope.launch {
            repository.setDataUsageCap(capMB)
        }
    }

    fun setUsageAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUsageAlerts(enabled)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.setThemePreference(theme)
        }
    }

    fun setSpeedDropAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSpeedDropAlertsEnabled(enabled)
        }
    }

    fun setReportAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReportAlertsEnabled(enabled)
        }
    }

    fun setStatusBarMonitor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setStatusBarMonitorEnabled(enabled)
        }
    }

    fun setShowDownload(show: Boolean) {
        viewModelScope.launch {
            repository.setShowDownloadSpeed(show)
        }
    }

    fun setShowUpload(show: Boolean) {
        viewModelScope.launch {
            repository.setShowUploadSpeed(show)
        }
    }

    fun setShowPing(show: Boolean) {
        viewModelScope.launch {
            repository.setShowPing(show)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
