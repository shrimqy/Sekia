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
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.DeviceDetails
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _deviceDetails = MutableStateFlow<DeviceDetails?>(null)
    val deviceDetails: StateFlow<DeviceDetails?> = _deviceDetails.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _messages = MutableStateFlow<List<SocketMessage>>(emptyList())
    val messages: StateFlow<List<SocketMessage>> = _messages.asStateFlow()

    private var webSocketService: WeakReference<WebSocketService>? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = WeakReference(binder.getService())
            _isConnected.value = true
            connectToWebSocket()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webSocketService = null
            _isConnected.value = false
        }
    }

    init {
        viewModelScope.launch {
            preferencesRepository.readDeviceDetails().collectLatest {
                _deviceDetails.value = it
                bindWebSocketService()
            }
        }
    }

    private fun bindWebSocketService() {
        val intent = Intent(getApplication(), WebSocketService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun connectToWebSocket() {
        val hostAddress = deviceDetails.value?.hostAddress
        val port = deviceDetails.value?.port
        if (hostAddress != null && port != null) {
            webSocketService?.get()?.connect(hostAddress, port)
        }
    }

    fun sendMessage(message: SocketMessage) {
        webSocketService?.get()?.sendMessage(message)
    }

    fun disconnect() {
        webSocketService?.get()?.disconnect()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            // Handle the exception gracefully, e.g., log the error or notify the user
        }
        _isConnected.value = false
    }

    private fun handleIncomingMessage(message: SocketMessage) {
        // Handle incoming messages...
    }


    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}