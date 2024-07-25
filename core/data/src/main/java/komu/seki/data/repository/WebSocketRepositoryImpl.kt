package komu.seki.data.repository

import komu.seki.data.network.WebSocketClient
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.flow.Flow

class WebSocketRepositoryImpl(private val webSocketClient: WebSocketClient) : WebSocketRepository {
    override suspend fun connect(hostAddress: String, port: Int) {
        webSocketClient.connect(hostAddress, port)
    }

    override suspend fun disconnect() {
        webSocketClient.disconnect()
    }

    override suspend fun sendMessage(message: SocketMessage) {
        webSocketClient.sendMessage(message)
    }

    override fun receiveMessages(): Flow<SocketMessage> {
        return webSocketClient.receiveMessages()
    }
}