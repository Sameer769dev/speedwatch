package com.speedwatch.app.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.speedwatch.app.data.model.SpeedLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(private val context: Context) {

    fun exportToCsv(logs: List<SpeedLog>): Uri? {
        val fileName = "SpeedWatch_Export_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, fileName)
        
        try {
            file.printWriter().use { out ->
                // Header
                out.println("ID,Timestamp,Date,Download (Mbps),Upload (Mbps),Latency (ms),Network Type")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                
                logs.forEach { log ->
                    out.println(
                        "${log.id}," +
                        "${log.timestamp}," +
                        "${sdf.format(Date(log.timestamp))}," +
                        "${log.downloadSpeedMbps}," +
                        "${log.uploadSpeedMbps}," +
                        "${log.latencyMs}," +
                        "${log.networkType}"
                    )
                }
            }
            
            return FileProvider.getUriForFile(
                context,
                "com.speedwatch.app.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
