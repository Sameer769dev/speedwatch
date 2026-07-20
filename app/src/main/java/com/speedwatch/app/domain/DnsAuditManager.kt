package com.speedwatch.app.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

class DnsAuditManager {

    suspend fun auditDns(): DnsAuditResult = coroutineScope {
        val google = async { measureDnsReal("8.8.8.8", "google.com") }
        val cloudflare = async { measureDnsReal("1.1.1.1", "google.com") }
        val system = async { measureDnsReal(null, "google.com") }
        
        DnsAuditResult(
            googleMs = google.await(),
            cloudflareMs = cloudflare.await(),
            systemMs = system.await()
        )
    }

    private suspend fun measureDnsReal(dnsServer: String?, domain: String): Long = withContext(Dispatchers.IO) {
        if (dnsServer == null) {
            // Measure system default using standard API
            return@withContext try {
                measureTimeMillis {
                    InetAddress.getByName(domain)
                }
            } catch (e: Exception) {
                999L
            }
        }

        // Real UDP DNS Query for specific server
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000
            
            val query = buildDnsQuery(domain)
            val serverAddr = InetAddress.getByName(dnsServer)
            val packet = DatagramPacket(query, query.size, serverAddr, 53)
            
            var time = 999L
            val startTime = System.currentTimeMillis()
            
            socket.send(packet)
            val response = ByteArray(1024)
            val receivePacket = DatagramPacket(response, response.size)
            socket.receive(receivePacket)
            
            time = System.currentTimeMillis() - startTime
            socket.close()
            time
        } catch (e: Exception) {
            999L
        }
    }

    private fun buildDnsQuery(domain: String): ByteArray {
        val buffer = ByteBuffer.allocate(512)
        buffer.putShort(0x1234.toShort()) // ID
        buffer.putShort(0x0100.toShort()) // Flags: standard query
        buffer.putShort(1.toShort()) // Questions
        buffer.putShort(0.toShort()) // Answer RRs
        buffer.putShort(0.toShort()) // Authority RRs
        buffer.putShort(0.toShort()) // Additional RRs

        domain.split(".").forEach { label ->
            buffer.put(label.length.toByte())
            buffer.put(label.toByteArray())
        }
        buffer.put(0.toByte()) // End of domain

        buffer.putShort(1.toShort()) // Type A
        buffer.putShort(1.toShort()) // Class IN

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }
}

data class DnsAuditResult(
    val googleMs: Long,
    val cloudflareMs: Long,
    val systemMs: Long
)
