package com.speedwatch.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.speedwatch.app.SpeedWatchApplication
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class UpsellWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SpeedWatchApplication
        val settings = app.repository.ispSettings.firstOrNull()
        
        if (settings?.isPremium == true) {
            return Result.success() // Already pro
        }

        val highlights = listOf(
            "Go Pro" to "Unlock background checks every hour to stay informed about outages.",
            "Pro Exclusive" to "Export professional PDF reports to prove network issues to your ISP.",
            "Upgrade to Pro" to "Track your network stability with Jitter and Stability analytics.",
            "Pro Features" to "Keep an unlimited history of all your network performance tests."
        )

        val (title, message) = highlights[Random.nextInt(highlights.size)]
        app.notificationHelper.showUpgradeUpsell(title, message)
        
        return Result.success()
    }
}
