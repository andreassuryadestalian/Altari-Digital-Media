package com.example.server

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

data class IpInfo(
    val primaryIp: String,
    val wifiIps: List<String> = emptyList(),
    val hotspotIps: List<String> = emptyList(),
    val allIps: List<String> = emptyList(),
    val isEmulator: Boolean = false
)

fun getLocalIpAddress(context: Context? = null): String {
    return getLocalIpInfo(context).primaryIp
}

fun getLocalIpInfo(context: Context? = null): IpInfo {
    val wifiIps = mutableListOf<String>()
    val hotspotIps = mutableListOf<String>()
    val otherIps = mutableListOf<String>()
    var isEmulator = false

    if (context != null) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ipStr = String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
                if (ipStr != "0.0.0.0") {
                    wifiIps.add(ipStr)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalIpAddress", "Error getting IP from WifiManager", e)
        }
    }

    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces?.hasMoreElements() == true) {
            val intf = interfaces.nextElement()
            val ifName = intf.name.lowercase()

            if (!intf.isUp || intf.isLoopback) continue

            val addrs = intf.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    val host = addr.hostAddress ?: continue
                    if (host == "10.0.2.15") {
                        isEmulator = true
                        otherIps.add(host)
                    } else if (ifName.contains("ap") || ifName.contains("softap") || ifName.contains("p2p") || ifName.contains("rndis") || host.startsWith("192.168.43.") || host.startsWith("192.168.49.")) {
                        hotspotIps.add(host)
                    } else if (ifName.contains("wlan") || ifName.contains("eth") || ifName.contains("swlan")) {
                        wifiIps.add(host)
                    } else if (!ifName.contains("dummy") && !ifName.contains("tun") && !ifName.contains("rmnet") && !ifName.contains("ccmni")) {
                        otherIps.add(host)
                    } else {
                        otherIps.add(host)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("LocalIpAddress", "Error getting IP address", e)
    }

    val distinctWifi = wifiIps.distinct()
    val distinctHotspot = hotspotIps.distinct()
    val distinctOther = otherIps.distinct()
    val all = (distinctWifi + distinctHotspot + distinctOther).distinct()

    val primary = distinctWifi.firstOrNull() 
        ?: distinctHotspot.firstOrNull()
        ?: distinctOther.firstOrNull { it != "10.0.2.15" } 
        ?: if (isEmulator) "localhost" else (all.firstOrNull() ?: "127.0.0.1")

    return IpInfo(
        primaryIp = primary,
        wifiIps = distinctWifi,
        hotspotIps = distinctHotspot,
        allIps = all,
        isEmulator = isEmulator
    )
}


