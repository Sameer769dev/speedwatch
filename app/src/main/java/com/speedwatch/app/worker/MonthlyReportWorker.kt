package com.speedwatch.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.domain.ReportGenerator
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class MonthlyReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SpeedWatchApplication
        val repository = app.repository
        
        // Get data for the month that just ended
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        
        val logs = repository.allLogs.firstOrNull() ?: emptyList()
        val settings = repository.ispSettings.firstOrNull()
        
        val report = ReportGenerator().generateMonthlyReport(settings, logs, month, year)
        
        // Notify user that report is ready
        if (settings?.reportAlertsEnabled != false) {
            app.notificationHelper.showReportReadyAlert(monthName(month))
        }
        
        return Result.success()
    }
    
    private fun monthName(month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month)
        return java.text.SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
    }
}
