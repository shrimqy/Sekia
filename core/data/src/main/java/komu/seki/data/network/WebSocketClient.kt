package komu.seki.data.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.*
import io.ktor.websocket.*
import komu.seki.domain.models.SocketMessage
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WebSocketClient {
    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: WebSocketSession? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun connect(hostAddress: String, port: Int) {
        session = client.webSocketSession {
            url("ws://$hostAddress:$port/SekiService")
        }
    }

    suspend fun sendMessage(message: SocketMessage) {
        val jsonString = json.encodeToString(message)
        session?.send(Frame.Text(jsonString))
    }

    fun receiveMessages(): Flow<SocketMessage> = flow {
        session?.incoming?.consumeAsFlow()?.collect { frame ->
            if (frame is Frame.Text) {
                val jsonString = frame.readText()
                val message = json.decodeFromString<SocketMessage>(jsonString)
                emit(message)
            }
        }
    }

    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}
