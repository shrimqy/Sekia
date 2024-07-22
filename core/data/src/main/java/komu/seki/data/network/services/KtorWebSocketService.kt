package komu.seki.data.network.services

import io.ktor.client.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KtorWebSocketService {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: WebSocketSession? = null

    suspend fun connect(hostAddress: String, port: Int) {
        session = client.webSocketSession {
            url("ws://$hostAddress:$port/SekiService")
        }
    }

    suspend fun sendMessage(message: Message) {
        session?.send(Frame.Text(Json.encodeToString(message)))
    }

    fun receiveMessages(): Flow<Message> = flow {
        session?.incoming?.consumeEach { frame ->
            if (frame is Frame.Text) {
                val message = Json.decodeFromString<Message>(frame.readText())
                emit(message)
            }
        }
    }

    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}

@kotlinx.serialization.Serializable
data class Message(
    val type: String,
    val content: String
)

object MessageType {
    const val Error = "error"
    const val Link = "link"
    const val Clipboard = "clipboard"
    const val Response = "response"
}