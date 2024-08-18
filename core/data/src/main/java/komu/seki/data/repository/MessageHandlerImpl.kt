package komu.seki.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import komu.seki.data.services.mediaController
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.DeviceStatus
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.MessageHandler

class MessageHandlerImpl(
    private val context: Context
) : MessageHandler {

    override fun handleClipboardMessage(clipboardMessage: ClipboardMessage) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", clipboardMessage.content)
        clipboard.setPrimaryClip(clip)
        Log.d("ClipboardMessage", "Copied to clipboard: ${clipboardMessage.content}")
    }

    override fun handlePlaybackData(playbackData: PlaybackData) {
        mediaController(context, playbackData)
    }

    override fun handleDeviceStatus(deviceStatus: DeviceStatus) {
        TODO("Not yet implemented")
    }

    override fun handleResponse(response: Response) {
        Log.d(response.resType, response.content)
    }

    override fun handleNotificationMessage(notificationMessage: NotificationMessage) {
        // Handle notification message
    }

    override fun handleDeviceInfo(deviceInfo: DeviceInfo) {
        // Handle notification message
    }
}


