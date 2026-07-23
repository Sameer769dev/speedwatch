package com.speedwatch.app.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.speedwatch.app.MainActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Speed Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for speed drops and monthly reports"
            }
            notificationManager.createNotificationChannel(channel)

            val monitorChannel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Real-time Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for real-time network speed"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(monitorChannel)
        }
    }

    fun showSpeedDropAlert(currentSpeed: Double, promisedSpeed: Double) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("ISP Promise Breach!")
            .setContentText("Detected speed: %.1f Mbps. Promised: %.1f Mbps.".format(currentSpeed, promisedSpeed))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showReportReadyAlert(monthName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Monthly Performance Report Ready")
            .setContentText("Your network report for $monthName is now available.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(REPORT_NOTIFICATION_ID, notification)
    }

    fun showDataCapAlert(usedMB: Double, capMB: Int) {
        val isFull = usedMB >= capMB
        val title = if (isFull) "Data Cap Reached" else "Approaching Data Cap"
        val message = if (isFull) 
            "You have used all of your %.0f MB testing budget.".format(usedMB)
        else 
            "You have used %.0f MB of your %d MB testing budget.".format(usedMB, capMB)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(DATA_CAP_NOTIFICATION_ID, notification)
    }

    fun showUnmeteredPromoAlert() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Temporary Unlimited Data!")
            .setContentText("Your carrier has granted temporary unmetered data. Great time for high-speed tests!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(PROMO_NOTIFICATION_ID, notification)
    }

    fun showUpgradeUpsell(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NAVIGATE_TO, "premium")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(UPSELL_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "speed_alerts_channel"
        private const val NOTIFICATION_ID = 1001
        private const val REPORT_NOTIFICATION_ID = 1002
        private const val UPSELL_NOTIFICATION_ID = 1003
        private const val DATA_CAP_NOTIFICATION_ID = 1004
        private const val PROMO_NOTIFICATION_ID = 1005
        const val MONITOR_NOTIFICATION_ID = 1006

        const val MONITOR_CHANNEL_ID = "monitor_channel"
        
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
}
