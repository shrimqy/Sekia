package komu.seki.domain.repository

import komu.seki.domain.models.SocketMessage
import kotlinx.coroutines.flow.Flow

interface WebSocketRepository {
    suspend fun connect(hostAddress: String): Boolean
    suspend fun startListening()
    suspend fun disconnect()
    suspend fun sendMessage(message: SocketMessage)
}