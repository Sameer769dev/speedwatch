package com.speedwatch.app.domain

import android.content.Context
import android.net.ConnectivityManager
import com.speedwatch.app.data.model.IspSettings

class ThrottlingDetector(private val context: Context) {

    fun checkThrottling(actualDownloadMbps: Double, settings: IspSettings?): ThrottlingStatus {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        val estimatedDownKbps = capabilities?.linkDownstreamBandwidthKbps ?: 0
        val linkMbps = estimatedDownKbps / 1000.0
        
        if (actualDownloadMbps <= 0) return ThrottlingStatus.Unknown
        
        // 1. Check Link Quality (Hardware)
        val isHardwareBottleneck = linkMbps > 0 && actualDownloadMbps >= linkMbps * 0.8
        
        // 2. Check Plan Compliance
        val planMbps = settings?.promisedDownloadMbps ?: 0.0
        val planGap = if (planMbps > 0) actualDownloadMbps / planMbps else 1.0
        
        // 3. Link vs Actual Gap (The "Throttling" indicator)
        val linkGap = if (linkMbps > 0) actualDownloadMbps / linkMbps else 1.0

        return when {
            isHardwareBottleneck -> ThrottlingStatus.HardwareLimited(linkMbps)
            linkGap < 0.2 && planGap < 0.5 -> ThrottlingStatus.HighlyLikely(linkMbps)
            linkGap < 0.5 -> ThrottlingStatus.Suspicious(linkMbps)
            else -> ThrottlingStatus.Optimal(linkMbps)
        }
    }
}

sealed interface ThrottlingStatus {
    data object Unknown : ThrottlingStatus
    data class Optimal(val linkSpeed: Double) : ThrottlingStatus
    data class HardwareLimited(val linkSpeed: Double) : ThrottlingStatus
    data class Suspicious(val linkSpeed: Double) : ThrottlingStatus
    data class HighlyLikely(val linkSpeed: Double) : ThrottlingStatus
}
