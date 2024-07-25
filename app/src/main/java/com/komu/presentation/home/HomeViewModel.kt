package com.komu.presentation.home

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val webSocketRepository: WebSocketRepository,
    private val preferencesRepository: PreferencesRepository,
    application: Application
) : AndroidViewModel(application) {

    private var webSocketService: WebSocketService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            _isConnected.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webSocketService = null
            _isConnected.value = false
        }
    }

    private val _messages = MutableStateFlow<List<SocketMessage>>(emptyList())
    val messages: StateFlow<List<SocketMessage>> = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(hostAddress: String, port: Int) {
        val intent = Intent(getApplication(), WebSocketService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            webSocketService?.connect(hostAddress, port) { message ->
                handleIncomingMessage(message)
            }
        }
    }

    fun sendMessage(message: SocketMessage) {
        viewModelScope.launch {
            webSocketService?.sendMessage(message)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            webSocketService?.disconnect()
            getApplication<Application>().unbindService(serviceConnection)
            _isConnected.value = false
        }
    }

    private fun handleIncomingMessage(message: SocketMessage) {
        when (message) {
            is Response -> {
                // Handle Response message
            }
            is NotificationMessage -> {
                // Handle Notification message
            }
            is ClipboardMessage -> {
                // Handle Clipboard message
            }
            else -> {
                // Handle unknown message types
            }
        }
        _messages.update { it + message }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}