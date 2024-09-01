package com.komu.sekia.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import komu.seki.data.repository.AppRepository
import komu.seki.data.services.NsdService
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

//@Singleton
//class WifiConnectionService @Inject constructor(
//    private val context: Context,
//    private val appRepository: AppRepository,
//    private val nsdService: NsdService,
//) {
//    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
//    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//    private val networkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
//            override fun onCapabilitiesChanged(
//                network: Network,
//                networkCapabilities: NetworkCapabilities
//            ) {
//                coroutineScope.launch {
//                    val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
//                    if (wifiInfo != null && wifiInfo.ssid != "<unknown ssid>") {
//                        Log.d("WebSocketRepository", "ssid: ${wifiInfo.ssid}")
//                        val knownNetwork = appRepository.getNetwork(wifiInfo.ssid)
//                        if (knownNetwork != null) {
//                            startNSDDiscovery()
//                        }
//                    }
//                }
//                connectivityManager.unregisterNetworkCallback(this)
//            }
//
//            override fun onUnavailable() {
//                Log.d("WebSocketRepository", "Network unavailable")
//                connectivityManager.unregisterNetworkCallback(this)
//            }
//        }
//    } else {
//        object : ConnectivityManager.NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                super.onAvailable(network)
//                coroutineScope.launch {
//                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//                    val wifiInfo = wifiManager.connectionInfo
//                    val ssid = wifiInfo?.ssid?.removeSurrounding("\"")
//                    if (ssid != null) {
//                        val knownNetwork = appRepository.getNetwork(ssid)
//                        if (knownNetwork != null) {
//                            startNSDDiscovery()
//                        }
//                    }
//                }
//            }
//
//            override fun onUnavailable() {
//                Log.d("WebSocketRepository", "Network unavailable")
//                connectivityManager.unregisterNetworkCallback(this)
//            }
//        }
//    }
//
//    fun initiateConnection() {
//        registerNetworkCallback()
//    }
//
//    private fun registerNetworkCallback() {
//        val networkRequest = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .build()
//        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
//    }
//
//    private fun startNSDDiscovery() {
//        nsdService.startDiscovery()
//        coroutineScope.launch {
//            nsdService.services.collect { services ->
//                if (services.isNotEmpty()) {
//                    for (service in services) {
//                        val hostAddress = appRepository.getHostAddress(service.serviceName)
//                        connectToWebSocket(hostAddress)
//                        break
//                    }
//                }
//            }
//        }
//    }
//
//    private fun connectToWebSocket(hostAddress: String) {
//        val intent = Intent(context, WebSocketService::class.java).apply {
//            action = Actions.START.name
//            putExtra(WebSocketService.EXTRA_HOST_ADDRESS, hostAddress)
//        }
//        context.startService(intent)
//    }
//
//    fun cleanup() {
//        connectivityManager.unregisterNetworkCallback(networkCallback)
//        nsdService.stopDiscovery()
//        coroutineScope.cancel()
//    }
//}