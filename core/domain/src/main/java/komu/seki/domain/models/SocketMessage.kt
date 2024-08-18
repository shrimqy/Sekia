package komu.seki.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class NotificationType {
    ACTIVE,
    NEW
}

@Serializable
sealed class SocketMessage

@Serializable
@SerialName("0")
data class Response(
    val resType: String,
    val content: String,
) : SocketMessage()

@Serializable
@SerialName("1")
data class ClipboardMessage(
    val content: String,
) : SocketMessage()

@Serializable
@SerialName("2")
data class NotificationMessage(
    val notificationType: NotificationType,
    val appName: String,
    val title: String?,
    val text: String?,
    val groupKey: String?,
    val tag: String?,
    val appIcon: String?,
    val largeIcon: String?,
    val bigPicture: String?,
    val actions: List<NotificationAction>?,
) : SocketMessage()

@Serializable
data class NotificationAction(
    val label: String,
    val actionId: String
)

@Serializable
@SerialName("3")
data class DeviceInfo(
    val id: String,
    val deviceName: String?,
) : SocketMessage()

@Serializable
@SerialName("4")
data class DeviceStatus(
    val batteryStatus: Int?,
    val chargingStatus: Boolean?,
    val wifiStatus: Boolean?,
    val bluetoothStatus: Boolean?,
) : SocketMessage()

@Serializable
@SerialName("5")
data class PlaybackData(
    val appName: String?,
    val trackTitle: String,
    val artist: String?,
    val thumbnail: String?,
    val volume: Double?,
    val isPlaying: Boolean
) : SocketMessage()



