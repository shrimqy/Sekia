package com.komu.sekia.ui.shortcut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.komu.sekia.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.domain.models.ClipboardMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareToPc : BaseActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null && bound) {
                scope.launch {
                    try {
                        webSocketService?.sendMessage(ClipboardMessage(text))
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