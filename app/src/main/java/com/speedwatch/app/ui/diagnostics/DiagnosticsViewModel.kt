package com.speedwatch.app.ui.diagnostics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.data.model.LabAudit
import com.speedwatch.app.data.repository.SpeedRepository
import com.speedwatch.app.domain.*
import com.speedwatch.app.ui.notifications.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiagnosticsViewModel(
    private val repository: SpeedRepository,
    private val speedMeasurer: SpeedMeasurer,
    private val networkInfoProvider: NetworkInfoProvider,
    private val notificationHelper: NotificationHelper,
    context: Context
) : ViewModel() {

    private val throttlingDetector = ThrottlingDetector(context) 
    private val bufferbloatManager = BufferbloatManager(speedMeasurer)
    private val dnsAuditManager = DnsAuditManager()

    private val _labState = MutableStateFlow<LabUiState>(LabUiState.Idle)
    val labState: StateFlow<LabUiState> = _labState.asStateFlow()

    val settings: StateFlow<IspSettings?> = repository.ispSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentAudits: StateFlow<List<LabAudit>> = repository.allAudits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dataUsage = repository.allLogs.map { logs ->
        logs.sumOf { it.dataUsageBytes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val dataUsage: StateFlow<Long> = _dataUsage

    private fun checkMobileData(): Boolean {
        val details = networkInfoProvider.getNetworkDetails()
        if (details.transport != "Cellular") {
            _labState.value = LabUiState.RequireMobileData
            return false
        }
        return true
    }

    private fun checkDataCap() {
        viewModelScope.launch {
            val currentSettings = repository.ispSettings.firstOrNull()
            if (currentSettings?.dataUsageAlertEnabled == true && currentSettings.dataUsageCapMB > 0) {
                val usedMB = _dataUsage.value / (1024.0 * 1024.0)
                val threshold = currentSettings.dataUsageCapMB * 0.9
                if (usedMB >= currentSettings.dataUsageCapMB || usedMB >= threshold) {
                    notificationHelper.showDataCapAlert(usedMB, currentSettings.dataUsageCapMB)
                }
            }
        }
    }

    fun runThrottlingTest() {
        if (!checkMobileData()) return
        viewModelScope.launch {
            _labState.value = LabUiState.Running("Testing Throttling...")
            val result = speedMeasurer.measureDownloadSpeed()
            val settings = repository.ispSettings.firstOrNull()
            
            if (result != null) {
                val status = throttlingDetector.checkThrottling(result.mbps, settings)
                val audit = LabAudit(
                    timestamp = System.currentTimeMillis(),
                    testType = "THROTTLING",
                    mainResult = getThrottlingText(status),
                    technicalDetails = "Real: %.1f Mbps, Link: %.1f Mbps".format(result.mbps, (status as? ThrottlingStatus.Optimal)?.linkSpeed ?: 0.0),
                    networkSnapshot = networkInfoProvider.getNetworkDetails().let { "${it.transport} • ${it.detailInfo}" }
                )
                repository.insertAudit(audit)
                checkDataCap()
                _labState.value = LabUiState.ThrottlingComplete(status)
            } else {
                _labState.value = LabUiState.Error("Test failed")
            }
        }
    }

    private fun getThrottlingText(status: ThrottlingStatus): String = when(status) {
        is ThrottlingStatus.Optimal -> "Optimal"
        is ThrottlingStatus.HardwareLimited -> "Hardware Limited"
        is ThrottlingStatus.Suspicious -> "Suspicious"
        is ThrottlingStatus.HighlyLikely -> "Highly Likely Throttled"
        else -> "Unknown"
    }

    fun runBufferbloatTest() {
        if (!checkMobileData()) return
        viewModelScope.launch {
            _labState.value = LabUiState.Running("Testing Bufferbloat...")
            val result = bufferbloatManager.measureBufferbloat()
            val audit = LabAudit(
                timestamp = System.currentTimeMillis(),
                testType = "BUFFERBLOAT",
                mainResult = "Grade ${result.grade}",
                technicalDetails = "Increase: ${result.increaseMs}ms, Load: ${result.loadedPingMs}ms",
                networkSnapshot = networkInfoProvider.getNetworkDetails().let { "${it.transport} • ${it.detailInfo}" }
            )
            repository.insertAudit(audit)
            checkDataCap()
            _labState.value = LabUiState.BufferbloatComplete(result)
        }
    }

    fun runDnsAudit() {
        if (!checkMobileData()) return
        viewModelScope.launch {
            _labState.value = LabUiState.Running("Auditing DNS...")
            val result = dnsAuditManager.auditDns()
            val audit = LabAudit(
                timestamp = System.currentTimeMillis(),
                testType = "DNS",
                mainResult = "System: ${result.systemMs}ms",
                technicalDetails = "Cloudflare: ${result.cloudflareMs}ms, Google: ${result.googleMs}ms",
                networkSnapshot = networkInfoProvider.getNetworkDetails().let { "${it.transport} • ${it.detailInfo}" }
            )
            repository.insertAudit(audit)
            _labState.value = LabUiState.DnsComplete(result)
        }
    }

    fun runStreamingAudit() {
        if (!checkMobileData()) return
        viewModelScope.launch {
            _labState.value = LabUiState.Running("Auditing Streaming Quality...")
            val result = speedMeasurer.measureDownloadSpeed("https://speed.cloudflare.com/__down?bytes=25165824") // 24MB for 4K test
            if (result != null) {
                val grade = when {
                    result.mbps >= 50 -> "8K Ultra HD"
                    result.mbps >= 25 -> "4K Ultra HD"
                    result.mbps >= 10 -> "Full HD"
                    else -> "Basic"
                }
                _labState.value = LabUiState.QoEComplete("Streaming", grade, "Avg Bitrate: %.1f Mbps".format(result.mbps))
            } else {
                _labState.value = LabUiState.Error("Test failed")
            }
        }
    }

    fun runVideoCallAudit() {
        if (!checkMobileData()) return
        viewModelScope.launch {
            _labState.value = LabUiState.Running("Auditing Video Call Health...")
            val jitter = speedMeasurer.measureJitter() ?: 99.0
            val latency = speedMeasurer.measureLatency() ?: 999
            
            val grade = when {
                latency < 50 && jitter < 10 -> "Excellent"
                latency < 100 && jitter < 20 -> "Good"
                latency < 150 && jitter < 30 -> "Fair"
                else -> "Poor"
            }
            _labState.value = LabUiState.QoEComplete("Video Call", grade, "Latency: ${latency}ms, Jitter: %.1fms".format(jitter))
        }
    }

    fun reset() {
        _labState.value = LabUiState.Idle
    }

    fun clearAuditHistory() {
        viewModelScope.launch {
            repository.clearAudits()
        }
    }
}

sealed interface LabUiState {
    data object Idle : LabUiState
    data object RequireMobileData : LabUiState
    data class Running(val message: String) : LabUiState
    data class ThrottlingComplete(val status: ThrottlingStatus) : LabUiState
    data class BufferbloatComplete(val result: BufferbloatResult) : LabUiState
    data class DnsComplete(val result: DnsAuditResult) : LabUiState
    data class QoEComplete(val activity: String, val grade: String, val details: String) : LabUiState
    data class Error(val message: String) : LabUiState
}
