package komu.seki.domain.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


@Serializable
enum class SocketMessageType {
    @SerialName("0") Clipboard,
    @SerialName("1") Notification,
    @SerialName("2") Response,
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
    val appName: String,
    val header: String,
    val content: String,
    val actions: List<NotificationAction>,
) : SocketMessage()

@Serializable
data class NotificationAction(
    val label: String,
    val actionId: String
)
