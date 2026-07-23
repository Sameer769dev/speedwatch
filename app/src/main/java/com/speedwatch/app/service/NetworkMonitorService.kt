package com.speedwatch.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.ui.notifications.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class NetworkMonitorService : Service() {

    private val TAG = "NetworkMonitorService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.MONITOR_NOTIFICATION_ID,
                createNotification("Initialising Monitor..."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NotificationHelper.MONITOR_NOTIFICATION_ID,
                createNotification("Initialising Monitor...")
            )
        }
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val app = application as SpeedWatchApplication
            val repository = app.repository
            val speedMeasurer = app.speedMeasurer

            lastRxBytes = TrafficStats.getTotalRxBytes()
            lastTxBytes = TrafficStats.getTotalTxBytes()
            lastTime = System.currentTimeMillis()

            var pingValue = 0
            var pingCounter = 0

            while (isActive) {
                val settings = repository.ispSettings.firstOrNull() ?: break
                if (!settings.statusBarMonitorEnabled) {
                    Log.d(TAG, "Monitor disabled in settings, stopping service")
                    stopSelf()
                    break
                }

                val currentTime = System.currentTimeMillis()
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                
                val timeDiff = (currentTime - lastTime) / 1000.0
                
                if (currentRx != TrafficStats.UNSUPPORTED.toLong() && timeDiff >= 1.0) {
                    val rxDelta = currentRx - lastRxBytes
                    val txDelta = currentTx - lastTxBytes
                    
                    Log.d(TAG, "Delta RX: $rxDelta, TX: $txDelta over ${"%.2f".format(timeDiff)}s")

                    // Update ping every 5 iterations (~5 seconds) to save battery
                    if (settings.showPing && pingCounter % 5 == 0) {
                        pingValue = speedMeasurer.measureLatency() ?: pingValue
                    }
                    pingCounter++

                    updateNotification(settings, rxDelta, txDelta, timeDiff, pingValue)

                    lastRxBytes = currentRx
                    lastTxBytes = currentTx
                    lastTime = currentTime
                }
                
                delay(1000)
            }
        }
    }

    private fun updateNotification(settings: IspSettings, rxDelta: Long, txDelta: Long, timeDiff: Double, ping: Int) {
        val parts = mutableListOf<String>()
        if (settings.showDownloadSpeed) {
            parts.add("D: ${formatSpeed(rxDelta, timeDiff)}")
        }
        if (settings.showUploadSpeed) {
            parts.add("U: ${formatSpeed(txDelta, timeDiff)}")
        }
        if (settings.showPing && ping > 0) {
            parts.add("P: $ping ms")
        }

        val content = if (parts.isEmpty()) "Monitoring Active" else parts.joinToString("  ")
        
        val notification = createNotification(content)
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NotificationHelper.MONITOR_NOTIFICATION_ID, notification)
    }

    private fun formatSpeed(bytes: Long, timeDiff: Double): String {
        if (bytes <= 0 || timeDiff <= 0) return "0 B/s"
        
        val bytesPerSecond = bytes.toDouble() / timeDiff
        val bitsPerSecond = bytesPerSecond * 8.0
        
        return when {
            bitsPerSecond >= 1_000_000.0 -> {
                "%.1f Mbps".format(bitsPerSecond / 1_000_000.0)
            }
            bytesPerSecond >= 1024.0 -> {
                "%.0f KB/s".format(bytesPerSecond / 1024.0)
            }
            else -> {
                "%.0f B/s".format(bytesPerSecond)
            }
        }
    }

    private fun createNotification(content: String): android.app.Notification {
        return NotificationCompat.Builder(this, NotificationHelper.MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Real-time Network Speed")
            .setContentText(content)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        super.onDestroy()
        serviceScope.cancel()
    }
}
