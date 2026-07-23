package com.speedwatch.app.domain

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class SpeedMeasurer(private val client: OkHttpClient) {

    private val threadCount = 4
    private val defaultDurationMs = 8000L
    private val rampUpDurationMs = 2000L

    fun measureDownloadFlow(
        url: String = "https://speed.cloudflare.com/__down?bytes=104857600",
        durationMs: Long = defaultDurationMs
    ): Flow<SpeedResult> = flow {
        val totalBytes = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val rampUpEndTime = startTime + rampUpDurationMs
        val testEndTime = startTime + durationMs

        var bytesAtRampUpEnd = 0L
        var actualRampUpTime = rampUpEndTime

        coroutineScope {
            val jobs = List(threadCount) {
                launch(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(url)
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build()
                    try {
                        while (System.currentTimeMillis() < testEndTime && isActive) {
                            client.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    delay(200)
                                    return@use
                                }
                                val source = response.body?.source() ?: return@use
                                val buffer = ByteArray(16384)
                                var bytesRead: Int
                                while (source.read(buffer).also { bytesRead = it } != -1) {
                                    if (System.currentTimeMillis() >= testEndTime || !isActive) break
                                    totalBytes.addAndGet(bytesRead.toLong())
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Suppress network interruption during test termination
                    }
                }
            }

            var hasCapturedRampUp = false

            while (System.currentTimeMillis() < testEndTime) {
                val now = System.currentTimeMillis()

                if (!hasCapturedRampUp && now >= rampUpEndTime) {
                    bytesAtRampUpEnd = totalBytes.get()
                    actualRampUpTime = now
                    hasCapturedRampUp = true
                }

                val currentTotalBytes = totalBytes.get()
                val mbps = if (hasCapturedRampUp && (now - actualRampUpTime) > 200) {
                    val bytesSinceRampUp = (currentTotalBytes - bytesAtRampUpEnd).coerceAtLeast(0L)
                    val elapsedSec = (now - actualRampUpTime) / 1000.0
                    (bytesSinceRampUp * 8.0) / (elapsedSec * 1_000_000.0)
                } else {
                    val elapsedSec = (now - startTime).coerceAtLeast(100) / 1000.0
                    (currentTotalBytes * 8.0) / (elapsedSec * 1_000_000.0)
                }

                emit(SpeedResult(mbps.coerceAtLeast(0.0), currentTotalBytes))
                delay(200)
            }
            jobs.forEach { it.cancelAndJoin() }

            val finalNow = System.currentTimeMillis()
            val finalTotalBytes = totalBytes.get()
            val finalMbps = if (hasCapturedRampUp && (finalNow - actualRampUpTime) > 200) {
                val bytesSinceRampUp = (finalTotalBytes - bytesAtRampUpEnd).coerceAtLeast(0L)
                val elapsedSec = (finalNow - actualRampUpTime) / 1000.0
                (bytesSinceRampUp * 8.0) / (elapsedSec * 1_000_000.0)
            } else {
                val elapsedSec = (finalNow - startTime).coerceAtLeast(100) / 1000.0
                (finalTotalBytes * 8.0) / (elapsedSec * 1_000_000.0)
            }
            emit(SpeedResult(finalMbps.coerceAtLeast(0.0), finalTotalBytes))
        }
    }

    fun measureUploadFlow(
        url: String = "https://speed.cloudflare.com/__up",
        durationMs: Long = defaultDurationMs
    ): Flow<SpeedResult> = flow {
        val totalBytes = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val rampUpEndTime = startTime + rampUpDurationMs
        val testEndTime = startTime + durationMs
        val uploadChunk = ByteArray(256 * 1024) { 0.toByte() } // 256 KB chunk

        var bytesAtRampUpEnd = 0L
        var actualRampUpTime = rampUpEndTime

        coroutineScope {
            val jobs = List(threadCount) {
                launch(Dispatchers.IO) {
                    while (System.currentTimeMillis() < testEndTime && isActive) {
                        val requestBody = object : RequestBody() {
                            override fun contentType(): MediaType? = "application/octet-stream".toMediaTypeOrNull()
                            override fun contentLength(): Long = uploadChunk.size.toLong()

                            override fun writeTo(sink: BufferedSink) {
                                var offset = 0
                                val blockSize = 16384 // 16 KB blocks
                                while (offset < uploadChunk.size && System.currentTimeMillis() < testEndTime && isActive) {
                                    val toWrite = minOf(blockSize, uploadChunk.size - offset)
                                    sink.write(uploadChunk, offset, toWrite)
                                    sink.flush() // Force network transmission before counting
                                    totalBytes.addAndGet(toWrite.toLong())
                                    offset += toWrite
                                }
                            }
                        }

                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()

                        try {
                            client.newCall(request).execute().use { response ->
                                // Consume response headers and body
                            }
                        } catch (e: Exception) {
                            if (!isActive || System.currentTimeMillis() >= testEndTime) break
                            delay(100)
                        }
                    }
                }
            }

            var hasCapturedRampUp = false

            while (System.currentTimeMillis() < testEndTime) {
                val now = System.currentTimeMillis()

                if (!hasCapturedRampUp && now >= rampUpEndTime) {
                    bytesAtRampUpEnd = totalBytes.get()
                    actualRampUpTime = now
                    hasCapturedRampUp = true
                }

                val currentTotalBytes = totalBytes.get()
                val mbps = if (hasCapturedRampUp && (now - actualRampUpTime) > 200) {
                    val bytesSinceRampUp = (currentTotalBytes - bytesAtRampUpEnd).coerceAtLeast(0L)
                    val elapsedSec = (now - actualRampUpTime) / 1000.0
                    (bytesSinceRampUp * 8.0) / (elapsedSec * 1_000_000.0)
                } else {
                    val elapsedSec = (now - startTime).coerceAtLeast(100) / 1000.0
                    (currentTotalBytes * 8.0) / (elapsedSec * 1_000_000.0)
                }

                emit(SpeedResult(mbps.coerceAtLeast(0.0), currentTotalBytes))
                delay(200)
            }
            jobs.forEach { it.cancelAndJoin() }

            val finalNow = System.currentTimeMillis()
            val finalTotalBytes = totalBytes.get()
            val finalMbps = if (hasCapturedRampUp && (finalNow - actualRampUpTime) > 200) {
                val bytesSinceRampUp = (finalTotalBytes - bytesAtRampUpEnd).coerceAtLeast(0L)
                val elapsedSec = (finalNow - actualRampUpTime) / 1000.0
                (bytesSinceRampUp * 8.0) / (elapsedSec * 1_000_000.0)
            } else {
                val elapsedSec = (finalNow - startTime).coerceAtLeast(100) / 1000.0
                (finalTotalBytes * 8.0) / (elapsedSec * 1_000_000.0)
            }
            emit(SpeedResult(finalMbps.coerceAtLeast(0.0), finalTotalBytes))
        }
    }

    suspend fun measureDownloadSpeed(
        url: String = "https://speed.cloudflare.com/__down?bytes=104857600",
        durationMs: Long = defaultDurationMs
    ): SpeedResult? {
        var lastResult: SpeedResult? = null
        measureDownloadFlow(url, durationMs).collect { lastResult = it }
        return lastResult
    }

    suspend fun measureUploadSpeed(
        url: String = "https://speed.cloudflare.com/__up",
        durationMs: Long = defaultDurationMs
    ): SpeedResult? {
        var lastResult: SpeedResult? = null
        measureUploadFlow(url, durationMs).collect { lastResult = it }
        return lastResult
    }

    suspend fun measureLatency(url: String = "https://1.1.1.1"): Int? = withContext(Dispatchers.IO) {
        try {
            // Warm-up request to establish TLS/TCP connection pool
            executePingRequest(url)

            // Measure warm latency
            val time = measureTimeMillis {
                executePingRequest(url)
            }
            time.toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun executePingRequest(url: String) {
        val request = Request.Builder()
            .url(url)
            .head()
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP response code $response")
        }
    }

    suspend fun measureJitter(url: String = "https://1.1.1.1"): Double? = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Int>()
        // Warm up connection first
        try { executePingRequest(url) } catch (e: Exception) { }

        repeat(6) {
            try {
                val time = measureTimeMillis { executePingRequest(url) }
                latencies.add(time.toInt())
            } catch (e: Exception) { }
            delay(100)
        }

        if (latencies.size < 2) return@withContext null
        var totalDiff = 0.0
        for (i in 0 until latencies.size - 1) {
            totalDiff += abs(latencies[i] - latencies[i + 1])
        }
        totalDiff / (latencies.size - 1)
    }
}

data class SpeedResult(
    val mbps: Double,
    val bytesUsed: Long
)

