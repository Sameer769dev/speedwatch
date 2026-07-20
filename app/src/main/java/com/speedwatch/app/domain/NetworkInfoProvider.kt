package com.speedwatch.app.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class NetworkInfoProvider(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getNetworkDetails(): NetworkDetails {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val transport = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Offline"
        }

        val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
        
        var signalStrength: Int? = null
        var detailInfo = ""

        if (transport == "Wi-Fi") {
            try {
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                signalStrength = wifiInfo.rssi
                val freq = wifiInfo.frequency
                detailInfo = when {
                    freq in 2400..2500 -> "2.4 GHz"
                    freq in 4900..5900 -> "5 GHz"
                    freq > 5900 -> "6 GHz"
                    else -> ""
                }
                if (wifiInfo.linkSpeed > 0) {
                    detailInfo += " (${wifiInfo.linkSpeed} Mbps link)"
                }
            } catch (e: Exception) {
                // Log if needed
            }
        } else if (transport == "Cellular") {
            detailInfo = getCellularType()
        }

        return NetworkDetails(
            transport = transport,
            isValidated = isValidated,
            isMetered = isMetered,
            signalStrength = signalStrength,
            detailInfo = detailInfo,
            bandwidthDownKbps = capabilities?.linkDownstreamBandwidthKbps ?: 0
        )
    }

    private fun getCellularType(): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Cellular"
        }
        
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            @Suppress("DEPRECATION")
            when (tm.networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "Mobile Data"
            }
        } catch (e: SecurityException) {
            "Cellular"
        }
    }
}

data class NetworkDetails(
    val transport: String,
    val isValidated: Boolean,
    val isMetered: Boolean,
    val signalStrength: Int?,
    val detailInfo: String,
    val bandwidthDownKbps: Int
)
