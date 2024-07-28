package komu.seki.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage

class MessageHandler(
    private val context: Context
) {
    fun handleMessage(message: SocketMessage) {
        when (message) {
            is Response -> handleResponse(message)
            is ClipboardMessage -> handleClipboardMessage(message)
            is NotificationMessage -> handleNotificationMessage(message)
        }
    }

    private fun handleResponse(response: Response) {
        Log.d(response.resType, response.content)
    }

    private fun handleClipboardMessage(clipboardMessage: ClipboardMessage) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", clipboardMessage.content)
        clipboard.setPrimaryClip(clip)
        Log.d("ClipboardMessage", "Copied to clipboard: ${clipboardMessage.content}")
    }

    private fun handleNotificationMessage(notificationMessage: NotificationMessage) {
        // Handle notification message
    }
}