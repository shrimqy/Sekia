package com.komu.sekia.ui.shortcut

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.domain.models.ClipboardMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareToPc : ComponentActivity() {
    private lateinit var webSocketService: WebSocketService
    private var isBound = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
            handleIntent(intent)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindService()
    }

    private fun bindService() {
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null && isBound) {
                scope.launch {
                    try {
                        webSocketService.sendMessage(ClipboardMessage(text))
                        Log.d("ShareToPc", "Message sent: $text")
                        finishAffinity()
                    } catch (e: Exception) {
                        Log.e("ShareToPc", "Failed to send message", e)
                    }
                }
            } else {
                finishAffinity()
            }
        }
    }
}
