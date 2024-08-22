package com.komu.sekia.ui.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.komu.sekia.services.FileTransferService
import com.komu.sekia.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.common.util.storage.extractMetadata
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.TransferType
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

    // Clipboard Handle
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            when (intent.type) {
                "text/plain" -> {
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
                "image/*", "video/*", "application/*" -> {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    val metadata = uri?.let { extractMetadata(applicationContext, it) }

                    if (metadata != null) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val base64Data = inputStream?.buffered()?.use { Base64.encodeToString(it.readBytes(), Base64.DEFAULT) }
                        val maxWebSocketFileSize = 1024 * 1024 // 1 MB
                        if (metadata.fileSize < maxWebSocketFileSize) {
                            scope.launch {
                                try {
                                    webSocketService?.sendMessage(FileTransfer(TransferType.WEBSOCKET, metadata, base64Data))
                                } catch (e: Exception) {
                                    Log.e("ShareToPc", "Failed to send message", e)
                                }
                            }
                        } else {
                            FileTransferService.startService(this, uri, metadata)
                            finish()
                        }
                    } else {
                        finishAffinity()
                    }
                }
            }
        }
    }
}