package com.komu.sekia.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService : Service() {
    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun connect(hostAddress: String, port: Int) {
        scope.launch {
            webSocketRepository.connect(hostAddress, port)
        }
    }

    fun disconnect() {
        scope.launch {
            webSocketRepository.disconnect()
        }
    }

    fun sendMessage(message: SocketMessage) {
        scope.launch {
            webSocketRepository.sendMessage(message)
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
            .setContentTitle("Device Connected")
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