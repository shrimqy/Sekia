package com.komu.sekia.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID
import androidx.core.app.NotificationManagerCompat
import com.komu.sekia.MainActivity
import com.komu.sekia.R
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService : Service() {
    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var context: Context

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = LocalBinder()
    private var isForegroundStarted = false // Flag to track if foreground has been started

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WebSocketService", "onStartCommand called")
        val hostAddress = intent?.getStringExtra(EXTRA_HOST_ADDRESS)
        Log.d("WebSocketService", "Received hostAddress: $hostAddress")
        Log.d("WebSocketService", "Received action: ${intent?.action}")
        when (intent?.action) {
            Actions.START.name -> {
                Log.d("WebSocketService", "START action received")
                hostAddress?.let { start(it) } ?: Log.e("WebSocketService", "hostAddress is null")
            }
            Actions.STOP.name -> {
                Log.d("WebSocketService", "STOP action received")
                stop()
            }
            else -> Log.d("WebSocketService", "Unknown action received")
        }
        return START_NOT_STICKY
    }
    private var isConnected by mutableStateOf(false)
    private fun start(hostAddress: String) {
        scope.launch {
            Log.d("connectedVar", isConnected.toString())
            try {
                val deviceInfo = getDeviceInfo(context)
                Log.d("service", "trying to connect")
                isConnected = connect(hostAddress, deviceInfo)
                if (isConnected) {
                    startListening()
                    preferencesRepository.saveSynStatus(true)
                    if (!isForegroundStarted) {
                        startForeground(NOTIFICATION_ID, createNotification())
                        isForegroundStarted = true
                    }
                    // Trigger sending of active notifications
                    sendActiveNotifications()
                } else {
                    delay(5000)
                }
            } catch (e: Exception) {
                Log.e("WebSocketService", "Error in WebSocket connection", e)
                delay(5000) // Wait before retrying
            }
        }
    }

    private fun stop() {
        scope.launch {
            disconnect()
            stopForeground(true)
            stopSelf()
            preferencesRepository.saveSynStatus(false)
            isForegroundStarted = false // Reset the flag
        }
    }

    private suspend fun connect(hostAddress: String, deviceInfo: DeviceInfo): Boolean {
        return webSocketRepository.connect(hostAddress, deviceInfo)
    }


    private fun getDeviceInfo(context: Context): DeviceInfo {
        val deviceName = Build.MODEL

        val batteryStatus: Int? =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifi = wifiManager.isWifiEnabled

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetooth = bluetoothManager.adapter.isEnabled

        return DeviceInfo(
            deviceName = deviceName,
            batteryStatus = batteryStatus,
            wifiStatus = wifi,
            bluetoothStatus = bluetooth
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
    private fun createNotification(): Notification {
        val channelId = "WebSocket_Foreground_Service"
        val channel = NotificationChannel(
            channelId,
            "WebSocket Foreground Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val disconnectIntent = Intent(this, WebSocketService::class.java).apply {
            action = Actions.STOP.name
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }
        val disconnectPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, disconnectIntent, PendingIntent.FLAG_IMMUTABLE)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Device Connected")
            .setContentText("Maintaining connection in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_launcher_foreground, "Disconnect", disconnectPendingIntent)
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
        super.onDestroy()
        scope.cancel()
        isForegroundStarted = false
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val EXTRA_HOST_ADDRESS = "extra_host_address"
        const val ACTION_SEND_ACTIVE_NOTIFICATIONS = "com.komu.sekia.services.SEND_ACTIVE_NOTIFICATIONS"
    }
}


enum class Actions {
    START,
    STOP
}