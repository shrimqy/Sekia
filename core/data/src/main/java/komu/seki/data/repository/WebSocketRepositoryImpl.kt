package komu.seki.data.repository

import komu.seki.data.network.WebSocketClient
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.WebSocketRepository

class WebSocketRepositoryImpl(
    private val webSocketClient: WebSocketClient,
) : WebSocketRepository {
    override suspend fun connect(hostAddress: String): Boolean {
        return webSocketClient.connect(hostAddress)
    }

    override suspend fun startListening(onDisconnect: () -> Unit) {
        webSocketClient.startListening(onDisconnect)
    }

    override suspend fun disconnect() {
        webSocketClient.disconnect()
    }

    override suspend fun sendMessage(message: SocketMessage) {
        webSocketClient.sendMessage(message)
    }

}