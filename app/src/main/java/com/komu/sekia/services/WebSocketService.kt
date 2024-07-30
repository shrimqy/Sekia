package com.komu.sekia.services

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.komu.sekia.R
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.data.network.WebSocketClient
import komu.seki.data.repository.WebSocketRepositoryImpl
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.reflect.KProperty

@AndroidEntryPoint
class WebSocketService : Service() {
    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    companion object {
        private const val NOTIFICATION_ID = 1
    }
    private var job: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    suspend fun connect(hostAddress: String): Boolean {
        return webSocketRepository.connect(hostAddress)
    }

    suspend fun startListening() {
        webSocketRepository.startListening()
    }

    suspend fun disconnect() {
        webSocketRepository.disconnect()
    }

    suspend fun sendMessage(message: SocketMessage) {
        webSocketRepository.sendMessage(message)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
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
}


