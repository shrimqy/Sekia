package komu.seki.data.repository

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import komu.seki.data.handlers.MediaSessionManager
import komu.seki.data.handlers.MessageHandler
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.PreferencesSettings
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

class WebSocketRepositoryImpl @Inject constructor(
    val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val preferencesRepository: PreferencesRepository,
    private val appRepository: AppRepository,
) : WebSocketRepository {

    private lateinit var messageHandler: MessageHandler

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = -1L
            maxFrameSize = Long.MAX_VALUE
        }
    }

    private var outputStream: OutputStream? = null

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

    private lateinit var preferencesSettings: PreferencesSettings

    private var socket: Socket? = null
    private var writeChannel: ByteWriteChannel? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var lastHostAddress: String


    override suspend fun connect(hostAddress: String, deviceInfo: DeviceInfo?): Boolean {
        return try {
            // Ensure the host address is set properly
            lastHostAddress = hostAddress
            val port = 5149

            // Set up the connection
            val selectorManager = SelectorManager(Dispatchers.IO)
            socket = aSocket(selectorManager).tcp().connect(lastHostAddress, port)
            Log.d("socket", "Client Connected to $hostAddress")

            // Save the last connection and sync status
            withContext(Dispatchers.IO) {
                preferencesRepository.saveLastConnected(hostAddress)
                preferencesRepository.saveSynStatus(true)
            }

            // Open the write channel after establishing the connection
            writeChannel = socket?.openWriteChannel()

            deviceInfo?.let {
                Log.d("message", "sending deviceInfo $deviceInfo")
                sendMessage(it)
            }

            preferencesSettings = preferencesRepository.preferenceSettings().first()
            Log.d("socket", "preference settings collected: $preferencesSettings")
            messageHandler = MessageHandler(::sendMessage, playbackRepository, appRepository, hostAddress, preferencesSettings)
            true
        } catch (e: Exception) {
            Log.d("connectionError", "Failed to connect to $hostAddress")
            e.printStackTrace()
            false
        }
    }


    override suspend fun startListening(onDisconnect: () -> Unit) {
        Log.d("Socket", "listening started")
        withContext(Dispatchers.IO) {
            try {
                val receiveChannel = socket?.openReadChannel()
                receiveChannel?.let { channel ->
                    while (!channel.isClosedForRead) {
                        try {
                            // Read the incoming data as a line
                            val receivedData = channel.readUTF8Line()

                            // Ensure data isn't null
                            receivedData?.let { jsonMessage ->
                                Log.d("Socket", "Raw received data: $jsonMessage")
                                val socketMessage = json.decodeFromString<SocketMessage>(jsonMessage)
                                messageHandler.handleMessages(context, socketMessage)
                            }
                        } catch (e: Exception) {
                            Log.d("WebSocketClient", "Error while receiving data")
                            e.printStackTrace()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("WebSocketClient", "Session error")
                e.printStackTrace()
//                reconnect()
            } finally {
                Log.d("WebSocketClient", "Session closed")
                preferencesRepository.saveSynStatus(false)
                onDisconnect()
            }
        }
    }

//    private suspend fun reconnect() {
//        session?.close()
//        delay(2000) // Wait before attempting reconnection
//        val port = 5149
//        session = client.webSocketSession {
//            url("ws://$lastHostAddress:$port")
//        }
//        if (session?.isActive == true) startListening { scope.launch { disconnect() }  }
//    }

    override suspend fun sendMessage(message: SocketMessage) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("sendMessage", "Attempting to send message: $message")

                writeChannel?.let { channel ->
                    val jsonMessage = json.encodeToString(SocketMessage.serializer(), message)
                    channel.writeStringUtf8("$jsonMessage\n") // Add newline to separate messages
                    channel.flush()
                    Log.d("sendMessage", "Message sent successfully")
                } ?: run {
                    Log.e("WebSocketClient", "Write channel is not available")
                }
            } catch (e: Exception) {
                Log.e("WebSocketClient", "Failed to send message", e)
            }
        }
    }

    override suspend fun sendBinary(message: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("sendBinary", "Attempting to send binary message of size: ${message.size}")

                // Attach the channel for writing
//                writeChannel?.write(message)
//                writeChannel?.flush()

                Log.d("sendBinary", "Binary message sent successfully")
            } catch (e: Exception) {
                Log.e("WebSocketClient", "Failed to send binary message", e)
            }
        }
    }

    override suspend fun sendBuffer(message: ByteBuffer) {
        withContext(Dispatchers.IO) {
            try {
                // Attach the channel for writing
                writeChannel?.writeFully(message)
                writeChannel?.flush()

                Log.d("sendBinary", "Binary message sent successfully")
            } catch (e: Exception) {
                Log.e("WebSocketClient", "Failed to send binary message", e)
            }
        }
    }

    override suspend fun disconnect() {
        Log.d("socket", "Session Closed")
        MediaSessionManager.release()
        socket?.close()
        writeChannel?.close()
        preferencesRepository.saveSynStatus(false)
    }
}