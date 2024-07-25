package komu.seki.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


enum class SocketMessageType {
    Clipboard,
    Notification,
    Response,
    Permission,
    Media,
    Link,
    Message
}

@Serializable
abstract class SocketMessage {
    abstract val type: SocketMessageType
}

@Serializable
@SerialName("Response")
data class Response(
    val resType: String,
    val content: String,
    override val type: SocketMessageType = SocketMessageType.Response
) : SocketMessage()

@Serializable
@SerialName("ClipboardMessage")
data class ClipboardMessage(
    val content: String,
    override val type: SocketMessageType = SocketMessageType.Clipboard
) : SocketMessage()

@Serializable
@SerialName("NotificationMessage")
data class NotificationMessage(
    val appName: String,
    val header: String,
    val content: String,
    val actions: List<NotificationAction>,
    override val type: SocketMessageType = SocketMessageType.Notification
) : SocketMessage()

@Serializable
data class NotificationAction(
    val label: String,
    val actionId: String
)
