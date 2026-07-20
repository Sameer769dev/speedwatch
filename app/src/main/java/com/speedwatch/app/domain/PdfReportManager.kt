package com.speedwatch.app.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.data.model.SpeedLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfReportManager(private val context: Context) {

    private val brandColor = Color.parseColor("#1A73E8") // Google Blue style

    fun generateReport(
        settings: IspSettings?,
        logs: List<SpeedLog>,
        monthName: String
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 26f
            color = brandColor
        }
        
        val bodyPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 11f
            color = Color.BLACK
        }

        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = brandColor
        }

        var y = 60f
        
        // Header
        canvas.drawText("SpeedWatch Performance Report", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Generated for: $monthName", 40f, y, bodyPaint)
        canvas.drawText("Professional Network Monitoring", 420f, y, bodyPaint.apply { textSize = 9f; color = Color.GRAY })
        y += 40f
        
        // SLA Status Box
        if (settings != null && logs.isNotEmpty()) {
            val passCount = logs.count { it.downloadSpeedMbps >= settings.promisedDownloadMbps * 0.9 }
            val compliance = (passCount.toDouble() / logs.size) * 100
            val isHealthy = compliance >= 90
            
            val boxColor = if (isHealthy) Color.parseColor("#E8F0FE") else Color.parseColor("#FCE8E6")
            val textColor = if (isHealthy) Color.parseColor("#1E8E3E") else Color.parseColor("#D93025")
            
            val rect = RectF(40f, y, 555f, y + 60f)
            paint.color = boxColor
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            
            val statusTitle = if (isHealthy) "SLA COMPLIANT" else "SLA NON-COMPLIANT"
            canvas.drawText(statusTitle, 60f, y + 25f, Paint().apply { 
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 14f
                color = textColor 
            })
            canvas.drawText("ISP matched or exceeded promised speeds in %.1f%% of tests.".format(compliance), 60f, y + 45f, bodyPaint.apply { 
                textSize = 10f
                color = Color.BLACK
            })
            y += 80f
        }

        // ISP Section
        canvas.drawText("PLAN SPECIFICATIONS", 40f, y, headerPaint)
        y += 25f
        if (settings != null) {
            canvas.drawText("Provider: ${settings.ispName}", 50f, y, bodyPaint)
            y += 18f
            canvas.drawText("Promised Download: ${settings.promisedDownloadMbps} Mbps", 50f, y, bodyPaint)
            y += 18f
            canvas.drawText("Promised Upload: ${settings.promisedUploadMbps} Mbps", 50f, y, bodyPaint)
        } else {
            canvas.drawText("ISP Details: Not configured", 50f, y, bodyPaint)
        }
        y += 40f
        
        // Summary
        canvas.drawText("MONTHLY SUMMARY", 40f, y, headerPaint)
        y += 25f
        if (logs.isNotEmpty()) {
            val avgDown = logs.map { it.downloadSpeedMbps }.average()
            val avgUp = logs.map { it.uploadSpeedMbps }.average()
            val avgPing = logs.map { it.latencyMs }.average()
            
            canvas.drawText("Avg Download: %.1f Mbps".format(avgDown), 50f, y, bodyPaint)
            canvas.drawText("Avg Upload: %.1f Mbps".format(avgUp), 220f, y, bodyPaint)
            canvas.drawText("Avg Latency: %.0f ms".format(avgPing), 400f, y, bodyPaint)
        }
        y += 40f
        
        // Log Table
        canvas.drawText("DETAILED LOGS (MOST RECENT)", 40f, y, headerPaint)
        y += 20f
        
        // Table Header
        paint.color = Color.LTGRAY
        canvas.drawRect(40f, y, 555f, y + 25f, paint)
        canvas.drawText("Date & Time", 50f, y + 17f, bodyPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK })
        canvas.drawText("Download", 180f, y + 17f, bodyPaint)
        canvas.drawText("Upload", 270f, y + 17f, bodyPaint)
        canvas.drawText("Ping", 360f, y + 17f, bodyPaint)
        canvas.drawText("Jitter", 440f, y + 17f, bodyPaint)
        canvas.drawText("Type", 510f, y + 17f, bodyPaint)
        y += 25f
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        logs.take(25).forEach { log ->
            if (y > 780f) { // Simple page break check (not full pagination yet)
                pdfDocument.finishPage(page)
                // In a real app we'd start a new page here, for now we truncate to 25 logs
                return@forEach 
            }
            canvas.drawText(sdf.format(Date(log.timestamp)), 50f, y + 17f, bodyPaint.apply { typeface = Typeface.DEFAULT })
            canvas.drawText("%.1f".format(log.downloadSpeedMbps), 180f, y + 17f, bodyPaint)
            canvas.drawText("%.1f".format(log.uploadSpeedMbps), 270f, y + 17f, bodyPaint)
            canvas.drawText("${log.latencyMs} ms", 360f, y + 17f, bodyPaint)
            canvas.drawText("%.1f ms".format(log.jitterMs), 440f, y + 17f, bodyPaint)
            canvas.drawText(log.networkType, 510f, y + 17f, bodyPaint.apply { textSize = 9f })
            
            y += 22f
            canvas.drawLine(40f, y, 555f, y, Paint().apply { color = Color.parseColor("#EEEEEE") })
        }
        
        // Footer
        canvas.drawText("Generated by SpeedWatch App - Verification of ISP Service Quality", 40f, 810f, bodyPaint.apply { textSize = 8f; color = Color.GRAY })

        pdfDocument.finishPage(page)

        val fileName = "SpeedWatch_Report_${System.currentTimeMillis()}.pdf"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        return try {
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            FileProvider.getUriForFile(context, "com.speedwatch.app.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
