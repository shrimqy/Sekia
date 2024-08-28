package komu.seki.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import komu.seki.data.database.Device
import komu.seki.data.services.mediaController
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.Command
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.DeviceStatus
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageHandler(
    private val sendMessage: suspend (SocketMessage) -> Unit,
    private val playbackRepository: PlaybackRepository,
    private val appRepository: AppRepository,
    private val lastConnected: String,
) {
    fun handleMessages(context: Context, message: SocketMessage) {
        when (message) {
            is Response -> handleResponse(message)
            is ClipboardMessage -> handleClipboardMessage(context, message)
            is NotificationMessage -> handleNotificationMessage(message)
            is DeviceInfo -> handleDeviceInfo(message)
            is DeviceStatus -> handleDeviceStatus(message)
            is PlaybackData -> handlePlaybackData(context, message, sendMessage)
            is FileTransfer -> handleFileTransfer()
            is Command -> handleCommands()
            else -> {

            }
        }

    }

    private fun handleCommands() {
        TODO("Not yet implemented")
    }

    private fun handleFileTransfer() {
        TODO("Not yet implemented")
    }

    private fun handleClipboardMessage(context: Context, clipboardMessage: ClipboardMessage) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", clipboardMessage.content)
        clipboard.setPrimaryClip(clip)
    }

    private fun handlePlaybackData(
        context: Context,
        playbackData: PlaybackData,
        sendMessage: suspend (SocketMessage) -> Unit
    ) {
        playbackRepository.updatePlaybackData(playbackData)
        CoroutineScope(Dispatchers.Main).launch {
            mediaController(
                context,
                playbackData,
                sendMessage
            )
        }
    }

    private fun handleDeviceStatus(deviceStatus: DeviceStatus) {
        TODO("Not yet implemented")
    }

    private fun handleResponse(response: Response) {
        Log.d(response.resType, response.content)
    }

    private fun handleNotificationMessage(notificationMessage: NotificationMessage) {
        TODO("Not yet implemented")
    }

    private fun handleDeviceInfo(deviceInfo: DeviceInfo) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("deviceInfo", deviceInfo.deviceName)
            appRepository.addDevice(Device(deviceName = deviceInfo.deviceName, avatar = deviceInfo.userAvatar, ipAddress = lastConnected))
        }
    }
}


