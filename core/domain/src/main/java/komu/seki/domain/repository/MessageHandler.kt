package komu.seki.domain.repository

import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.DeviceStatus
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.Response

interface MessageHandler {
    fun handlePlaybackData(playbackData: PlaybackData)
    fun handleDeviceStatus(deviceStatus: DeviceStatus)
    fun handleResponse(response: Response)
    fun handleClipboardMessage(clipboardMessage: ClipboardMessage)
    fun handleNotificationMessage(notificationMessage: NotificationMessage)
    fun handleDeviceInfo(deviceInfo: DeviceInfo)
}