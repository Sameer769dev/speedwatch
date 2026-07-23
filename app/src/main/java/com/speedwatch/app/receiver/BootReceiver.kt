package com.speedwatch.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.service.NetworkMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as SpeedWatchApplication
            CoroutineScope(Dispatchers.IO).launch {
                val settings = app.repository.ispSettings.firstOrNull()
                if (settings?.statusBarMonitorEnabled == true) {
                    val serviceIntent = Intent(context, NetworkMonitorService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        }
    }
}
