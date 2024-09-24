package komu.seki.domain.repository

import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.SocketMessage

interface WebSocketRepository {
    suspend fun connect(hostAddress: String, deviceInfo: DeviceInfo?): Boolean
    suspend fun startListening(onDisconnect: () -> Unit)
    suspend fun disconnect()
    suspend fun sendMessage(message: SocketMessage)
    suspend fun sendBinary(message: ByteArray)
}