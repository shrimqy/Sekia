package com.komu.sekia.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings.Global
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.komu.sekia.MainActivity
import com.komu.sekia.R
import com.komu.sekia.utils.getStorageInfo
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.common.util.Constants.ACTION_SEND_ACTIVE_NOTIFICATIONS
import komu.seki.common.util.Constants.ACTION_STOP_NOTIFICATION_SERVICE
import komu.seki.data.repository.AppRepository
import komu.seki.data.services.NotificationService
import komu.seki.data.services.NsdService
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.DeviceStatus
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NetworkService : Service() {
    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var nsdService: NsdService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = LocalBinder()

    private var isForegroundStarted = false
    private var isConnected by mutableStateOf(false)

    private var lastBatteryLevel: Int? = null

    private lateinit var connectivityManager: ConnectivityManager

    inner class LocalBinder : Binder() {
        fun getService(): NetworkService = this@NetworkService
    }

    private val _syncStatus = MutableStateFlow(false)
    private val syncStatus: StateFlow<Boolean> = _syncStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerReceivers()
        scope.launch {
            preferencesRepository.saveSynStatus(isConnected)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isConnected) {
                sendDeviceStatus()
            }
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                when (wifiState) {
                    WifiManager.WIFI_STATE_ENABLED -> {
                        Log.d("NetworkService", "Wi-Fi enabled")
                        // Register the network callback when Wi-Fi is enabled
                        registerNetworkCallback()
                    }
                    WifiManager.WIFI_STATE_DISABLED -> {
                        Log.d("NetworkService", "Wi-Fi disabled")
                    }
                    else -> {
                        Log.d("NetworkService", "Wi-Fi state changed but not handled: $wifiState")
                    }
                }
            } else if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                // Optional: Handle network state change like connecting to a new network
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                if (networkInfo != null && networkInfo.isConnected) {
                    // Wi-Fi connected, possibly to a new network
                    Log.d("NetworkService", "Wi-Fi network connected")
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo?.ssid?.removeSurrounding("\"")
                    if (ssid != null) {
                        val knownNetwork = appRepository.getNetwork(ssid)
                        if (knownNetwork != null) {
                            Log.d("NetworkService", "Connected to known network: $ssid")
                            startNSDDiscovery()
                        }
                    }
                }
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isConnected) {
                sendDeviceStatus()
            }
        }
    }

    private fun registerReceivers() {
        val wifiIntentFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        registerReceiver(wifiReceiver, wifiIntentFilter)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification(false)
        val hostAddress = intent?.getStringExtra(EXTRA_HOST_ADDRESS)
        val newDevice = intent?.getBooleanExtra(NEW_DEVICE, false) ?: false
        Log.d("WebSocketService", "Received hostAddress: $hostAddress new Device: $newDevice")
        Log.d("WebSocketService", "Received action: ${intent?.action}")
        when (intent?.action) {
            Actions.START.name -> hostAddress?.let { start(it, newDevice) }
            Actions.STOP.name -> stop()
            else -> Log.d("WebSocketService", "Unknown action received")
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(isConnected: Boolean) {
        // Rebuild and update the notification with the new sync status
        val notification = createNotification(isConnected)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun start(hostAddress: String, newDevice: Boolean) {
        scope.launch {
            val maxRetries = 4
            var attempt = 0
            var connected = false

            while (attempt < maxRetries && !connected) {
                try {
                    attempt++
                    Log.d("WebSocketService", "Attempt $attempt: Trying to connect")

                    // Try to connect, passing deviceInfo if it's a new device
                    val deviceInfo = getDeviceInfo()
                    isConnected = connect(hostAddress, deviceInfo)

                    if (isConnected) {
                        updateNotification(true)
                        Log.d("WebSocketService", "Connected on attempt $attempt")
                        connected = true
                        startListening()
                        preferencesRepository.saveSynStatus(true)
                        sendMessage(getStorageInfo())
                        sendMessage(getDeviceStatus())
                        sendActiveNotifications()
                        break
                    } else {
                        Log.d("WebSocketService", "Connection failed on attempt $attempt")
                        delay(2000) // Optional: Wait for 2 seconds before retrying
                    }

                } catch (e: Exception) {
                    Log.e("WebSocketService", "Error in WebSocket connection on attempt $attempt", e)
                    delay(2000) // Optional: Wait for 2 seconds before retrying
                }
            }

            // If not connected after retries, register for NSD discovery
            if (!connected) {
                Log.d("WebSocketService", "Failed to connect after $maxRetries attempts. Falling back to NSD discovery.")
                CoroutineScope(Dispatchers.IO).launch {
                    preferencesRepository.preferenceSettings().collect { preferences ->
                        val autodiscovery = preferences.autoDiscovery
                        if (autodiscovery) {
                            Log.d("WebSocketService", "Auto-discovery is enabled. Registering network callback.")
                            registerNetworkCallback()
                        } else {
                            Log.d("WebSocketService", "Auto-discovery is disabled. Skipping network callback.")
                            stop()
                        }
                    }
                }
            }
        }
    }

    private fun sendDeviceStatus() {
        scope.launch {
            try {
                val deviceStatus = getDeviceStatus()
                val currentBatteryLevel = deviceStatus.batteryStatus

                // Only send the status if the battery level has changed
                if (currentBatteryLevel != lastBatteryLevel) {
                    lastBatteryLevel = currentBatteryLevel
                    webSocketRepository.sendMessage(deviceStatus)
                }
            } catch (e: Exception) {
                Log.e("WebSocketService", "Failed to send device status", e)
            }
        }
    }

    private fun stop() {
        scope.launch {
            stopNotificationService()
            disconnect()
            stopForeground(true)
            stopSelf()
            preferencesRepository.saveSynStatus(false)
            isForegroundStarted = false
        }
    }

    private fun stopNotificationService() {
        val stopNotificationServiceIntent = Intent(this, NotificationService::class.java)
        stopNotificationServiceIntent.action = ACTION_STOP_NOTIFICATION_SERVICE
        sendBroadcast(stopNotificationServiceIntent)
    }

    private suspend fun connect(hostAddress: String, deviceInfo: DeviceInfo? = null): Boolean {
        return webSocketRepository.connect(hostAddress, deviceInfo)
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.d("NetworkService", "Network callback registered")
        } catch (e: Exception) {
            Log.e("NetworkService", "Failed to register network callback", e)
        }
    }

    private val networkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                scope.launch {
                    val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                    if (wifiInfo != null && wifiInfo.ssid != "<unknown ssid>") {
                        val ssid = wifiInfo.ssid.removeSurrounding("\"")
                        Log.d("NetworkService", "SSID changed to: $ssid")
                        val knownNetwork = appRepository.getNetwork(ssid)
                        if (knownNetwork != null) {
                            startNSDDiscovery()
                        }
                    }
                }
            }

            override fun onUnavailable() {
                Log.d("NetworkService", "Network unavailable")
                connectivityManager.unregisterNetworkCallback(this)
            }
        }
    } else {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo?.ssid?.removeSurrounding("\"")
                    if (ssid != null) {
                        val knownNetwork = appRepository.getNetwork(ssid)
                        if (knownNetwork != null) {
                            startNSDDiscovery()
                        }
                    }
                }
            }

            override fun onUnavailable() {
                Log.d("NetworkService", "Network unavailable")
                connectivityManager.unregisterNetworkCallback(this)
            }
        }
    }

    private fun startNSDDiscovery() {
        CoroutineScope(Dispatchers.Main).launch {
            nsdService.startDiscovery()
            val discoveredServices = mutableSetOf<String>() // Set to track unique services
            nsdService.services.collect { services ->
                if (services.isNotEmpty()) {
                    for (service in services) {
                        if (discoveredServices.add(service.serviceName)) { // Add to set if it's a new service
                            Log.d("service", service.serviceName)
                            val hostAddress = appRepository.getHostAddress(service.serviceName)
                            if (hostAddress != null) {

                                nsdService.stopDiscovery()
                                // Schedule the WorkManager task to start the WebSocketService
                                val constraints = Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()


                                val workRequest = OneTimeWorkRequestBuilder<StartWebSocketWorker>()
                                    .setInputData(workDataOf(
                                        EXTRA_HOST_ADDRESS to hostAddress,
                                        NEW_DEVICE to false
                                    ))
//                                    .setConstraints(constraints)
                                    .build()
                                Log.d("service", "Getting WorkManager Instance")
                                WorkManager.getInstance(applicationContext).enqueue(workRequest)

                            }
                        } else {
                            Log.d("service", "Duplicate service skipped: ${service.serviceName}")
                        }
                    }
                }
            }
        }
    }


    private fun getDeviceInfo(): DeviceInfo {
        val deviceName = Global.getString(context.contentResolver, "device_name")
        return DeviceInfo(
            deviceName = deviceName,
            userAvatar = null,
        )
    }

    private fun getDeviceStatus(): DeviceStatus {
        val batteryStatus: Int? =
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

        val isCharging = batteryStatus != null &&
                registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifi = wifiManager.isWifiEnabled

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetooth = bluetoothManager.adapter.isEnabled

        return DeviceStatus(
            batteryStatus = batteryStatus,
            wifiStatus = wifi,
            bluetoothStatus = bluetooth,
            chargingStatus = isCharging
        )
    }

    private suspend fun startListening() {
        webSocketRepository.startListening {
            scope.launch {
                stop()
            }
        }
    }

    private suspend fun disconnect() {
        Log.d("WebSocketService", "Disconnecting...")
        webSocketRepository.disconnect()
    }

    suspend fun sendMessage(message: SocketMessage) {
        webSocketRepository.sendMessage(message)
    }

    // Notification Builder
    private fun createNotification(isConnected: Boolean): Notification {
        val channelId = "WebSocket_Foreground_Service"
        val channel = NotificationChannel(
            channelId,
            "WebSocket Foreground Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val disconnectIntent = Intent(this, NetworkService::class.java).apply {
            action = Actions.STOP.name
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }
        val disconnectPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, disconnectIntent, PendingIntent.FLAG_IMMUTABLE)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val contentText = if (isConnected) "Connected" else "Trying to connect"

        val actionText = if (isConnected) "Disconnect" else "Stop"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Device Connection Status")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_launcher_foreground, actionText, disconnectPendingIntent)
            .build()
    }


    private fun sendActiveNotifications() {
        val intent = Intent(ACTION_SEND_ACTIVE_NOTIFICATIONS)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        Log.d("WebSocketService", "onDestroy called")
        scope.launch {
            preferencesRepository.saveSynStatus(false)
        }
        // Register Receivers
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(wifiReceiver)
        unregisterReceiver(bluetoothReceiver)
        scope.cancel()
        isForegroundStarted = false
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val EXTRA_HOST_ADDRESS = "extra_host_address"
        const val NEW_DEVICE = "new_device"
        const val ACTION_SEND_ACTIVE_NOTIFICATIONS = "com.komu.sekia.services.NotificationService.SEND_ACTIVE_NOTIFICATIONS"
        const val ACTION_STOP_NOTIFICATION_SERVICE = "com.komu.sekia.services.NotificationService.STOP_NOTIFICATION_SERVICE"
    }
}

enum class Actions {
    START,
    STOP
}