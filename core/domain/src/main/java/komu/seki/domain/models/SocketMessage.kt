package komu.seki.domain.models

import android.os.Parcelable
import android.util.Base64
import komu.seki.common.models.FileMetadata
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


enum class NotificationType {
    ACTIVE,
    NEW,
    REMOVED
}
enum class MediaAction {
    RESUME,
    PAUSE,
    NEXT_QUEUE,
    PREV_QUEUE,
    VOLUME
}

enum class CommandType {
    LOCK,
    SHUTDOWN,
    SLEEP,
    HIBERNATE,
}

enum class Transfer

enum class TransferType {
    WEBSOCKET,
    P2P,
    UDP,
}

enum class DataTransferType {
    METADATA, CHUNK
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
    val notificationKey: String,
    val notificationType: NotificationType,
    val timestamp: String? = null,
    val appName: String? = null,
    val title: String? = null,
    val text: String? = null,
    val messages: List<Message>? = emptyList(),
    val groupKey: String? = null,
    val tag: String?,
    val appIcon: String? = null,
    val largeIcon: String? = null,
    val bigPicture: String? = null,
    val actions: List<NotificationAction>? = emptyList(),
) : SocketMessage()

@Serializable
data class NotificationAction(
    val label: String,
    val actionId: String
)

@Serializable
data class Message(
    val sender: String,
    val text: String
)

@Serializable
@SerialName("3")
data class DeviceInfo(
    val deviceName: String,
    val userAvatar: String? = null,
) : SocketMessage()

@Serializable
@SerialName("4")
data class DeviceStatus(
    val batteryStatus: Int?,
    val chargingStatus: Boolean?,
    val wifiStatus: Boolean?,
    val bluetoothStatus: Boolean?,
) : SocketMessage()

@Parcelize
@Serializable
@SerialName("5")
data class PlaybackData(
    val appName: String? = null,
    val trackTitle: String? = null,
    val artist: String? = null,
    var volume: Float,
    var isPlaying: Boolean? = null,
    var mediaAction: MediaAction?,
    val thumbnail: String? = null,
    val appIcon: String? = null,
) : SocketMessage(), Parcelable

@Serializable
@SerialName("6")
data class Command(
    val commandType: CommandType
) : SocketMessage()

@Serializable
@SerialName("7")
data class FileTransfer(
    val transferType: TransferType,
    val dataTransferType: DataTransferType,
    val metadata: FileMetadata? = null,
    val progress: Float? = null,
    val chunkData: String? = null,
) : SocketMessage()

