package com.speedwatch.app

import com.speedwatch.app.domain.SpeedMeasurer
import com.speedwatch.app.domain.SpeedResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class SpeedMeasurerTest {

    private lateinit var client: OkHttpClient
    private lateinit var speedMeasurer: SpeedMeasurer

    @Before
    fun setUp() {
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        speedMeasurer = SpeedMeasurer(client)
    }

    @Test
    fun testLatencyMeasurementReturnsValidValue() = runBlocking {
        val latency = speedMeasurer.measureLatency()
        assertNotNull("Latency should not be null on active network", latency)
        assertTrue("Latency should be positive", latency!! > 0)
        assertTrue("Latency should be reasonable (< 2000ms)", latency < 2000)
    }

    @Test
    fun testJitterMeasurementReturnsValidValue() = runBlocking {
        val jitter = speedMeasurer.measureJitter()
        assertNotNull("Jitter should not be null", jitter)
        assertTrue("Jitter should be non-negative", jitter!! >= 0.0)
    }

    @Test
    fun testDownloadSpeedMeasurement() = runBlocking {
        val result = speedMeasurer.measureDownloadSpeed(
            url = "https://speed.cloudflare.com/__down?bytes=1048576",
            durationMs = 3000L
        )
        assertNotNull("Download result should not be null", result)
        assertTrue("Downloaded bytes should be > 0", result!!.bytesUsed > 0)
        assertTrue("Download Mbps should be > 0", result.mbps > 0.0)
        println("Measured Download Speed: ${result.mbps} Mbps (${result.bytesUsed} bytes)")
    }

    @Test
    fun testUploadSpeedMeasurement() = runBlocking {
        val result = speedMeasurer.measureUploadSpeed(durationMs = 3000L)
        assertNotNull("Upload result should not be null", result)
        assertTrue("Uploaded bytes should be > 0", result!!.bytesUsed > 0)
        assertTrue("Upload Mbps should be realistic (> 0 and < 2000)", result.mbps in 0.1..2000.0)
        println("Measured Upload Speed: ${result.mbps} Mbps (${result.bytesUsed} bytes)")
    }
}
