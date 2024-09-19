package komu.seki.data.handlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import komu.seki.data.database.Device
import komu.seki.data.repository.AppRepository
import komu.seki.data.services.PermissionRequestActivity
import komu.seki.data.services.mediaController
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.Command
import komu.seki.domain.models.CommandType
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.DeviceStatus
import komu.seki.domain.models.FileTransfer
import komu.seki.domain.models.InteractiveControl
import komu.seki.domain.models.InteractiveControlMessage
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
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
            is FileTransfer -> handleFileTransfer(context, message)
            is Command -> handleCommands(context, message)
            is InteractiveControlMessage -> handleInteractiveControlMessage(message)
            else -> {

            }
        }
    }

    private fun handleInteractiveControlMessage(message: InteractiveControlMessage) {
        Log.d("MessageHandler", "Handling InteractiveControlMessage")
        val screenHandler = ScreenHandler.getInstance()
        if (screenHandler == null) {
            Log.e("MessageHandler", "ScreenHandler is not initialized")
        } else {
            Log.d("MessageHandler", "ScreenHandler instance found")
            when(val control = message.control) {
                is InteractiveControl.SingleTap -> {
                    Log.d("MessageHandler", "Mouse control: x=${control.x}, y=${control.y}")
                    screenHandler.performTap(control)
                }

                is InteractiveControl.HoldTap -> screenHandler.performHoldTap(control)
                is InteractiveControl.KeyboardEvent -> TODO()
                is InteractiveControl.ScrollEvent -> screenHandler.performScroll(control)
                is InteractiveControl.SwipeEvent -> screenHandler.performSwipe(control)
            }
        }
    }



    private fun handleCommands(context: Context, message: Command) {
        Log.d("Command", message.toString())
        when (message.commandType) {
            CommandType.MIRROR -> {
                Log.d("Handle", "Starting permission activity")
                val intent = Intent(context, PermissionRequestActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            else -> { }
        }
    }



    private fun handleFileTransfer(context: Context, message: FileTransfer) {
        receivingFileHandler(context, message)
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


