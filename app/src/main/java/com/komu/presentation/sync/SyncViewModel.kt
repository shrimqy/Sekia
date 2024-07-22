package com.komu.presentation.sync

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.network.services.KtorWebSocketService
import komu.seki.data.network.services.Message
import komu.seki.data.network.services.MessageType
import komu.seki.data.network.services.NsdService
import komu.seki.domain.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds


@HiltViewModel
class SyncViewModel @Inject constructor(
    private val webSocketService: KtorWebSocketService,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> get() = _messages

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> get() = _isConnected

    fun connect(hostAddress: String, port: Int) {
        viewModelScope.launch {
            try {
                webSocketService.connect(hostAddress, port)
                _isConnected.value = true
                webSocketService.receiveMessages().collect { message ->
                    handleIncomingMessage(message)
                }
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Error connecting to WebSocket: ${e.message}", e)
                _isConnected.value = false
            }
        }
    }

    fun sendMessage(type: String, content: String) {
        viewModelScope.launch {
            val message = Message(type, content)
            webSocketService.sendMessage(message)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            webSocketService.disconnect()
            _isConnected.value = false
        }
    }

    private fun handleIncomingMessage(message: Message) {
        // Handle incoming messages and update UI
        when (message.type) {
            MessageType.Clipboard -> {
                // Handle Clipboard message
            }
            MessageType.Response -> {
                // Handle Response message
            }
            else -> {
                // Handle Error or unknown message types
            }
        }
        _messages.value += message
    }
}
