package com.speedwatch.app.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class SpeedMeasurer(private val client: OkHttpClient) {

    suspend fun measureDownloadSpeed(url: String = "https://speed.cloudflare.com/__down?bytes=1048576"): SpeedResult? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        try {
            var totalBytes = 0L
            val time = measureTimeMillis {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val body = response.body ?: throw IOException("Empty body")
                    val source = body.source()
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                    }
                }
            }
            if (time > 0) {
                val speedBps = (totalBytes * 8.0) / (time / 1000.0)
                SpeedResult(speedBps / 1_000_000.0, totalBytes)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun measureUploadSpeed(url: String = "https://speed.cloudflare.com/__up"): SpeedResult? = withContext(Dispatchers.IO) {
        // Send 1MB of dummy data
        val data = ByteArray(1024 * 1024) { 0 }
        val requestBody = data.toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        
        try {
            val time = measureTimeMillis {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                }
            }
            if (time > 0) {
                val speedBps = (data.size * 8.0) / (time / 1000.0)
                SpeedResult(speedBps / 1_000_000.0, data.size.toLong())
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun measureLatency(url: String = "https://1.1.1.1"): Int? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).head().build()
        try {
            val time = measureTimeMillis {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                }
            }
            time.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun measureJitter(url: String = "https://1.1.1.1"): Double? = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Int>()
        repeat(5) {
            measureLatency(url)?.let { latencies.add(it) }
        }
        
        if (latencies.size < 2) return@withContext null
        
        var totalDiff = 0.0
        for (i in 0 until latencies.size - 1) {
            totalDiff += abs(latencies[i] - latencies[i+1])
        }
        
        totalDiff / (latencies.size - 1)
    }
}

data class SpeedResult(
    val mbps: Double,
    val bytesUsed: Long
)
