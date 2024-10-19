package komu.seki.domain.repository

import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.SocketMessage
import java.nio.ByteBuffer

interface WebSocketRepository {
    suspend fun connect(hostAddress: String, deviceInfo: DeviceInfo?): Boolean
    suspend fun startListening(onDisconnect: () -> Unit)
    suspend fun disconnect()
    suspend fun sendMessage(message: SocketMessage)
    suspend fun sendBinary(message: ByteArray)
    suspend fun sendBuffer(message: ByteBuffer)
}