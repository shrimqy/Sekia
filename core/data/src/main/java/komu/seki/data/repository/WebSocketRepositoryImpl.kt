package komu.seki.data.repository

import android.content.Context
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
import komu.seki.data.handlers.MessageHandler
import komu.seki.data.services.MediaSessionManager
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import javax.inject.Inject

class WebSocketRepositoryImpl @Inject constructor(
    val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val preferencesRepository: PreferencesRepository,
    private val appRepository: AppRepository
) : WebSocketRepository {

    private lateinit var messageHandler: MessageHandler

    private val client = HttpClient(CIO) {
        install(WebSockets){
            maxFrameSize = Long.MAX_VALUE
        }
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
    }

    private var session: WebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun connect(hostAddress: String, deviceInfo: DeviceInfo?): Boolean {
        try {
            val port = 5149
            session = client.webSocketSession {
                url("ws://$hostAddress:$port")
            }
            Log.d("socket", "Client Connected to $hostAddress")
            if (deviceInfo != null) {
                Log.d("message", "sending deviceInfo $deviceInfo")
                sendMessage(deviceInfo)
            }
            scope.launch {
                preferencesRepository.saveLastConnected(hostAddress)
                preferencesRepository.saveSynStatus(true)
            }
            messageHandler = MessageHandler(::sendMessage, playbackRepository, appRepository, hostAddress)
            return true
        } catch (e: Exception) {
            Log.d("connectionError", "Failed to connect to $hostAddress")
            e.printStackTrace()
            return false
        }
    }

    override suspend fun startListening(onDisconnect: () -> Unit) {
        scope.launch {
            try {
                session?.let { session ->
                    for (frame in session.incoming) {
                        if (frame is Frame.Text) {
                            val socketMessage = json.decodeFromString<SocketMessage>(frame.readText())
                            messageHandler.handleMessages(context, socketMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                Log.d("WebSocketClient", "Session closed")
                preferencesRepository.saveSynStatus(false)
                onDisconnect()
            }
        }
    }

    override suspend fun sendMessage(message: SocketMessage) {
        try {
            //send as JSON text
            session?.send(Frame.Text(json.encodeToString(message)))
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Failed to send message", e)
        }
    }

    override suspend fun disconnect() {
        Log.d("socket", "Session Closed")
        MediaSessionManager.release()
        session?.close()
        preferencesRepository.saveSynStatus(false)
    }
}