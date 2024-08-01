package komu.seki.domain.repository

import komu.seki.domain.models.SocketMessage

interface WebSocketRepository {
    suspend fun connect(hostAddress: String): Boolean
    suspend fun startListening(onDisconnect: () -> Unit)
    suspend fun disconnect()
    suspend fun sendMessage(message: SocketMessage)
}