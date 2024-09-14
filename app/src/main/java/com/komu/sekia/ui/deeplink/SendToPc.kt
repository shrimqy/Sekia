package com.komu.sekia.ui.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.komu.sekia.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import komu.seki.common.util.storage.extractMetadata
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.DataTransferType
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.TransferType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class ShareToPc : BaseActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ShareToPc", "onCreate called")
        handleIntent(intent)
    }

    // Clipboard Handle
    private fun handleIntent(intent: Intent?) {
        // Wait until the service is bound
        scope.launch {
            withTimeoutOrNull(5000) { // wait for up to 5 seconds
                while (!bound) {
                    delay(100) // poll every 100ms
                }
            }

            if (bound) {
                if (intent?.action == Intent.ACTION_SEND) {
                    when {
                        intent.type?.startsWith("text/") == true -> handleTextShare(intent)
                        intent.type?.startsWith("image/") == true -> handleFileTransfer(intent)
                        intent.type?.startsWith("video/") == true -> handleFileTransfer(intent)
                        intent.type?.startsWith("application/") == true -> handleFileTransfer(intent)
                        else -> {
                            Log.e("ShareToPc", "Unsupported content type: ${intent.type}")
                            finishAffinity()
                        }
                    }
                } else {
                    Log.e("ShareToPc", "Unsupported intent action: ${intent?.action}")
                    finishAffinity()
                }
            } else {
                Log.e("ShareToPc", "Service not bound in time")
                finishAffinity()
            }
        }
    }


    private fun handleTextShare(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d("ShareToPc", "Handling text share: $text")

        if (text != null) {
                scope.launch {
                    try {
                        networkService?.sendMessage(ClipboardMessage(text))
                        Log.d("ShareToPc", "Text message sent successfully")
                        runOnUiThread {
                            finishAffinity()
                        }
                    } catch (e: Exception) {
                        Log.e("ShareToPc", "Failed to send text message", e)
                        runOnUiThread {
                            finishAffinity()
                        }
                    }
                }
        } else {
            Log.e("ShareToPc", "Received null text")
            finishAffinity()
        }
    }

    private fun handleFileTransfer(intent: Intent) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        Log.d("ShareToPc", "Handling file share: $uri")

        if (uri != null) {
            scope.launch {
                val bufferSize = 512 * 1024 // 512KB
                val inputStream = contentResolver.openInputStream(uri)
                try {
                    val metadata = extractMetadata(applicationContext, uri)
                    networkService?.sendMessage(FileTransfer(TransferType.WEBSOCKET, DataTransferType.METADATA, metadata))
                    inputStream?.buffered()?.let { bufferedInput ->
                        val buffer = ByteArray(bufferSize)
                        var bytesRead: Int
                        while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
                            val chunk = buffer.copyOf(bytesRead)
                            val encodedChunk = Base64.encodeToString(chunk, Base64.DEFAULT)
                            networkService?.sendMessage(FileTransfer(TransferType.WEBSOCKET, DataTransferType.CHUNK, chunkData = encodedChunk))
                        }
                    }
                    inputStream?.close()
                }
                catch (e: Exception) {
                    Log.e("ShareToPc", "Failed to send small file", e)
                    runOnUiThread {
                        showToast("Failed to share file")
                        finishAffinity()
                    }
                }
                Log.d("ShareToPc", "Small file sent successfully")
            }
        } else {
            Log.e("ShareToPc", "Received null URI")
            showToast("Error")
            finishAffinity()
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    // Use this method to show Toasts from the service
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

}