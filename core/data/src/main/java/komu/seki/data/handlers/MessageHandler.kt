package komu.seki.data.handlers

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import komu.seki.common.util.Constants.ACTION_CLEAR_NOTIFICATIONS
import komu.seki.common.util.Constants.ACTION_REMOVE_NOTIFICATION
import komu.seki.common.util.Constants.ACTION_SEND_ACTIVE_NOTIFICATIONS
import komu.seki.common.util.Constants.NOTIFICATION_ID
import komu.seki.data.database.Device
import komu.seki.data.repository.AppRepository
import komu.seki.data.services.NotificationService
import komu.seki.data.services.ScreenMirrorService
import komu.seki.data.services.ScreenMirrorService.Companion.ACTION_STOP_SCREEN_CAPTURE
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
import komu.seki.domain.models.NotificationType
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.PreferencesSettings
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessageHandler(
    private val sendMessage: suspend (SocketMessage) -> Unit,
    private val playbackRepository: PlaybackRepository,
    private val appRepository: AppRepository,
    private val lastConnected: String,
    private val preferencesSettings: PreferencesSettings,
) {

    fun handleMessages(context: Context, message: SocketMessage) {
        when (message) {
            is Response -> handleResponse(message)
            is ClipboardMessage -> handleClipboardMessage(context, message)
            is NotificationMessage -> handleNotificationMessage(context, message)
            is DeviceInfo -> handleDeviceInfo(message)
            is DeviceStatus -> handleDeviceStatus(message)
            is PlaybackData -> handlePlaybackData(context, message, sendMessage)
            is FileTransfer -> handleFileTransfer(context, preferencesSettings, message)
            is Command -> handleCommands(context, message)
            is InteractiveControlMessage -> handleInteractiveControlMessage(context, message)
            else -> {

            }
        }
    }

    private fun handleInteractiveControlMessage(context: Context, message: InteractiveControlMessage) {
        val screenHandler = ScreenHandler.getInstance()
        // Get KeyguardManager and PowerManager services
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // Check if the device is locked or the screen is off
        val isLocked = keyguardManager.isKeyguardLocked
        val isScreenOff = !powerManager.isInteractive

        if (screenHandler != null) {
            // If the device is locked or the screen is off, acquire a wake lock
            if (isLocked || isScreenOff) {
                screenHandler.wakeDevice()
            }


            when(val control = message.control) {
                is InteractiveControl.SingleTap -> {
                    Log.d("MessageHandler", "Mouse control: x=${control.x}, y=${control.y}")
                    screenHandler.performTap(control)
                }

                is InteractiveControl.HoldTap -> screenHandler.performHoldTap(control)
                is InteractiveControl.KeyEvent -> screenHandler.performTextInput(control.key)
                is InteractiveControl.ScrollEvent -> screenHandler.performScroll(control)
                is InteractiveControl.SwipeEvent -> {
//                    Log.d("ScreenHandler", "Swipe coordinates: startX=${control.startX}, startY=${control.startY} endX=${control.endX}, endY=${control.endY}, duration:${control.duration}}, WillContinue: ${control.willContinue}")
                    screenHandler.performSwipe(control)
                }

                is InteractiveControl.KeyboardAction -> screenHandler.performKeyboardAction(control.action)
            }
        }
    }



    private fun handleCommands(context: Context, message: Command) {
        Log.d("Command", message.toString())
        when (message.commandType) {
            CommandType.MIRROR -> {
                Log.d("Handle", "Starting permission activity")
                // Start the permission activity
                val intent = Intent(context, PermissionRequestActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            CommandType.CLEAR_NOTIFICATIONS -> {
                val intent = Intent(ACTION_CLEAR_NOTIFICATIONS).also {
                    it.setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
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

    private fun handleNotificationMessage(context: Context, notificationMessage: NotificationMessage) {
        when(notificationMessage.notificationType) {
            NotificationType.REMOVED -> {
                val intent = Intent(ACTION_REMOVE_NOTIFICATION).also {
                    it.putExtra(NOTIFICATION_ID, notificationMessage.notificationKey)
                    it.setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
            }
            else -> { return }
        }
    }

    private fun handleDeviceInfo(deviceInfo: DeviceInfo) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("deviceInfo", deviceInfo.deviceName)
            appRepository.addDevice(Device(deviceName = deviceInfo.deviceName, avatar = deviceInfo.userAvatar, ipAddress = lastConnected))
        }
    }
}


