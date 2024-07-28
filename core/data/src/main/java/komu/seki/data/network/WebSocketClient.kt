package komu.seki.data.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import komu.seki.domain.MessageHandler
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class WebSocketClient(
    private val messageHandler: MessageHandler
) {
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(SocketMessage::class) {
                subclass(Response::class)
                subclass(ClipboardMessage::class)
                subclass(NotificationMessage::class)
            }
        }
        isLenient = true
        classDiscriminator = "type"
    // Name of the field that indicates the type
    }

    private var session: WebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connect(hostAddress: String, port: Int){
        try {
            session = client.webSocketSession {
                url("ws://192.168.1.9:$port/SekiService")
            }
            Log.d("socket", "Client Connected")
            startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    suspend fun startListening() {
        scope.launch {
            try {
                session?.let { session ->
                    for (frame in session.incoming) {
                        if (frame is Frame.Text) {
                            val socketMessage = json.decodeFromString<SocketMessage>(frame.readText())
                            messageHandler.handleMessage(socketMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}
