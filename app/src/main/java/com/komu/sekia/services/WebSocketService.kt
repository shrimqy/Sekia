package com.komu.sekia.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.komu.sekia.R
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.data.network.WebSocketClient
import komu.seki.data.repository.WebSocketRepositoryImpl
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WebSocketService : Service() {

    private val binder = LocalBinder()
    private lateinit var webSocketRepository: WebSocketRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        webSocketRepository = WebSocketRepositoryImpl(WebSocketClient())
    }

    fun connect(hostAddress: String, port: Int, onMessageReceived: (SocketMessage) -> Unit) {
        scope.launch {
            try {
                webSocketRepository.connect(hostAddress, port)
                startForeground(NOTIFICATION_ID, createNotification(), foregroundServiceType)
                webSocketRepository.receiveMessages().collect { message ->
                    onMessageReceived(message)
                }
            } catch (e: Exception) {
                Log.e("WebSocketService", "Error connecting to WebSocket: ${e.message}", e)
                stopSelf()
            }
        }
    }

    fun sendMessage(message: SocketMessage) {
        scope.launch {
            webSocketRepository.sendMessage(message)
        }
    }

    fun disconnect() {
        scope.launch {
            webSocketRepository.disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "WebSocket_Foreground_Service"
        val channel = NotificationChannel(
            channelId,
            "WebSocket Foreground Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WebSocket Active")
            .setContentText("Maintaining connection in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}