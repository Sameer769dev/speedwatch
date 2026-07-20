package com.speedwatch.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.data.model.SpeedLog
import kotlinx.coroutines.flow.firstOrNull

class SpeedCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SpeedWatchApplication
        val repository = app.repository
        val measurer = app.speedMeasurer
        val networkInfo = app.networkInfoProvider.getNetworkDetails()

        // 1. Data Guard: Check if we are on metered/cellular and if allowed
        val settings = repository.ispSettings.firstOrNull()
        if (networkInfo.transport == "Cellular" && settings?.allowMobileBackgroundTests == false) {
            return Result.success() // Skip without error
        }

        // 2. Perform full background test
        val latency = measurer.measureLatency() ?: 0
        val jitter = measurer.measureJitter() ?: 0.0
        val downloadResult = measurer.measureDownloadSpeed()
        val uploadResult = measurer.measureUploadSpeed()

        if (downloadResult != null) {
            val log = SpeedLog(
                timestamp = System.currentTimeMillis(),
                downloadSpeedMbps = downloadResult.mbps,
                uploadSpeedMbps = uploadResult?.mbps ?: 0.0,
                latencyMs = latency,
                networkType = "Background",
                jitterMs = jitter,
                signalStrength = networkInfo.signalStrength,
                dataUsageBytes = downloadResult.bytesUsed + (uploadResult?.bytesUsed ?: 0L)
            )
            repository.insertLog(log)

            // Proactive Alert Logic & Pro Upsells
            if (settings != null) {
                // Speed breach alert
                if (settings.speedDropAlertsEnabled) {
                    val isDownloadLow = downloadResult.mbps < settings.promisedDownloadMbps * 0.8 
                    val isUploadLow = (uploadResult?.mbps ?: 0.0) < settings.promisedUploadMbps * 0.8
                    
                    if (isDownloadLow || isUploadLow) {
                        app.notificationHelper.showSpeedDropAlert(
                            currentSpeed = downloadResult.mbps, 
                            promisedSpeed = settings.promisedDownloadMbps
                        )
                    }
                }

                // Stability Upsell (Jitter > 20ms is generally noticeable)
                if (!settings.isPremium && jitter > 20.0) {
                    app.notificationHelper.showUpgradeUpsell(
                        "Unstable Connection Detected",
                        "High jitter found. Upgrade to Pro for detailed stability analytics."
                    )
                }
            }
            
            return Result.success()
        }

        return Result.retry()
    }
}
