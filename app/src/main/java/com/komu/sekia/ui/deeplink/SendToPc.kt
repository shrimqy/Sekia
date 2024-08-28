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
import komu.seki.common.models.FileMetadata
import komu.seki.common.util.storage.extractMetadata
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.FileTransferContent
import komu.seki.domain.models.TransferType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

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
                        intent.type?.startsWith("image/") == true -> handleFileShare(intent)
                        intent.type?.startsWith("video/") == true -> handleFileShare(intent)
                        intent.type?.startsWith("application/") == true -> handleFileShare(intent)
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
                        webSocketService?.sendMessage(ClipboardMessage(text))
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

    private fun handleFileShare(intent: Intent) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        Log.d("ShareToPc", "Handling file share: $uri")

        if (uri != null) {
            val metadata = extractMetadata(applicationContext, uri)
//            val maxWebSocketFileSize = 50 * 1024 * 1024 // 10 MB
//            if (metadata.fileSize < maxWebSocketFileSize) {
            handleSmallFileTransfer(uri, metadata)
//            } else {
//                handleLargeFileTransfer(uri, metadata)
//            }
        } else {
            Log.e("ShareToPc", "Received null URI")
            showToast("No file to share")
            finishAffinity()
        }
    }

    private fun handleSmallFileTransfer(uri: Uri, metadata: FileMetadata) {
        scope.launch {
            try {
                // Send the file in chunks
                val bufferSize = 1024 * 1024 // 8KB
                val inputStream = contentResolver.openInputStream(uri)
                webSocketService?.sendMessage(FileTransfer(TransferType.WEBSOCKET, metadata))
                inputStream?.buffered()?.let { bufferedInput ->
                    val buffer = ByteArray(bufferSize)
                    var bytesRead: Int
                    while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
                        val chunk = buffer.copyOf(bytesRead)
                        webSocketService?.sendMessage(FileTransferContent(chunk))
                    }
                }
                inputStream?.close()
                Log.d("ShareToPc", "Small file sent successfully")
                runOnUiThread {
                    showToast("File shared successfully")
                    finishAffinity()
                }
            } catch (e: Exception) {
                Log.e("ShareToPc", "Failed to send small file", e)
                runOnUiThread {
                    showToast("Failed to share file")
                    finishAffinity()
                }
            }
        }
    }

//    private fun handleLargeFileTransfer(uri: Uri, metadata: FileMetadata) {
//        try {
//            scope.launch {
//                webSocketService?.sendMessage(FileTransfer(TransferType.HTTP))
//                delay(300)
//            }
//            FileTransferService.startService(this, uri, metadata)
//            Log.d("ShareToPc", "Started large file transfer service")
//            runOnUiThread {
//                showToast("File transfer started")
//            }
//        } catch (e: Exception) {
//            Log.e("ShareToPc", "Failed to start file transfer service", e)
//            runOnUiThread {
//                showToast("Failed to start file transfer")
//            }
//            finishAffinity()
//        }
//    }

    private val handler = Handler(Looper.getMainLooper())

    // Use this method to show Toasts from the service
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

}