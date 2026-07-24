package com.speedwatch.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.speedwatch.app.data.local.SpeedWatchDatabase
import com.speedwatch.app.data.repository.SpeedRepository
import com.speedwatch.app.domain.NetworkInfoProvider
import com.speedwatch.app.domain.SpeedMeasurer
import com.speedwatch.app.monetization.MonetizationManager
import com.speedwatch.app.service.NetworkMonitorService
import com.speedwatch.app.ui.notifications.NotificationHelper
import com.speedwatch.app.worker.MonthlyReportWorker
import com.speedwatch.app.worker.SpeedCheckWorker
import com.speedwatch.app.worker.UpsellWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit

class SpeedWatchApplication : Application() {

    private val database by lazy { SpeedWatchDatabase.getDatabase(this) }
    val repository by lazy { 
        SpeedRepository(
            database.speedLogDao(), 
            database.ispSettingsDao(),
            database.labAuditDao()
        ) 
    }
    val speedMeasurer by lazy { SpeedMeasurer(OkHttpClient()) }
    val notificationHelper by lazy { NotificationHelper(this) }
    val monetizationManager by lazy { MonetizationManager(this, repository) }
    val adManager by lazy { com.speedwatch.app.monetization.AdManager(this) }
    val networkInfoProvider by lazy { NetworkInfoProvider(this) }
    
    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    private val applicationScope = CoroutineScope(Dispatchers.Main)

    fun triggerNavigation(route: String) {
        applicationScope.launch {
            _navigationEvents.emit(route)
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupBackgroundMonitoring()
        setupMonthlyReporting()
        setupWeeklyUpsell()
        observeFrequencyChanges()
        observeStatusBarMonitor()
        setupNetworkCapabilityListener()
    }

    private fun setupNetworkCapabilityListener() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)) {
                        notificationHelper.showUnmeteredPromoAlert()
                    }
                }
            }
        })
    }

    private fun observeStatusBarMonitor() {
        applicationScope.launch {
            repository.ispSettings
                .map { it?.statusBarMonitorEnabled ?: false }
                .distinctUntilChanged()
                .collect { enabled ->
                    val intent = Intent(this@SpeedWatchApplication, NetworkMonitorService::class.java)
                    if (enabled) {
                        ContextCompat.startForegroundService(this@SpeedWatchApplication, intent)
                    } else {
                        stopService(intent)
                    }
                }
        }
    }

    private fun observeFrequencyChanges() {
        applicationScope.launch {
            repository.ispSettings
                .map { it?.checkFrequencyHours ?: 6 }
                .distinctUntilChanged()
                .collect { frequency ->
                    updateBackgroundMonitoring(frequency)
                }
        }
    }

    private fun setupBackgroundMonitoring() {
        // Initial setup with default or saved value
        updateBackgroundMonitoring(6) 
    }

    private fun updateBackgroundMonitoring(hours: Int) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SpeedCheckWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SpeedCheck",
            ExistingPeriodicWorkPolicy.UPDATE, // Update instead of KEEP to apply new frequency
            workRequest
        )
    }

    private fun setupMonthlyReporting() {
        // Run once every 30 days as a simple proxy for "monthly"
        val workRequest = PeriodicWorkRequestBuilder<MonthlyReportWorker>(30, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MonthlyReport",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun setupWeeklyUpsell() {
        val workRequest = PeriodicWorkRequestBuilder<UpsellWorker>(7, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklyUpsell",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
