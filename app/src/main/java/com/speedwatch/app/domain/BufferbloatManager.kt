package com.speedwatch.app.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class BufferbloatManager(private val speedMeasurer: SpeedMeasurer) {

    suspend fun measureBufferbloat(): BufferbloatResult = coroutineScope {
        // 1. Measure base latency (multiple samples for precision)
        val baseSamples = mutableListOf<Int>()
        repeat(3) {
            speedMeasurer.measureLatency()?.let { baseSamples.add(it) }
            delay(200)
        }
        val basePing = if (baseSamples.isNotEmpty()) baseSamples.average().toInt() else 0
        
        // 2. Start a background download to saturate the link
        val downloadJob = async {
            // Using a slightly larger download for bufferbloat saturation
            speedMeasurer.measureDownloadSpeed("https://speed.cloudflare.com/__down?bytes=10485760")
        }

        // 3. Measure latency while downloading (10 samples)
        val loadedSamples = mutableListOf<Int>()
        delay(1000) // Wait for ramp-up
        repeat(10) {
            speedMeasurer.measureLatency()?.let { loadedSamples.add(it) }
            delay(300)
        }
        
        val downloadResult = downloadJob.await()
        
        // Use 90th percentile for loaded ping (worst case scenario)
        val loadedPing = if (loadedSamples.isNotEmpty()) {
            loadedSamples.sorted()[(loadedSamples.size * 0.9).toInt().coerceAtMost(loadedSamples.size - 1)]
        } else basePing

        val increase = loadedPing - basePing
        val totalBytes = downloadResult?.bytesUsed ?: 0L
        
        val grade = when {
            increase <= 10 -> "A+"
            increase <= 30 -> "A"
            increase <= 60 -> "B"
            increase <= 100 -> "C"
            increase <= 200 -> "D"
            else -> "F"
        }
        
        BufferbloatResult(basePing, loadedPing, increase, grade, totalBytes)
    }
}

data class BufferbloatResult(
    val basePingMs: Int,
    val loadedPingMs: Int,
    val increaseMs: Int,
    val grade: String,
    val dataUsageBytes: Long
)
