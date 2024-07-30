package com.komu.presentation.sync

import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.models.ClipboardMessage
import komu.seki.domain.models.NotificationMessage
import komu.seki.domain.models.Response
import komu.seki.domain.models.SocketMessage
import komu.seki.data.network.NsdService
import komu.seki.data.network.WebSocketClient
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds


@HiltViewModel
class SyncViewModel @Inject constructor(
    private val webWebSocketClient: WebSocketClient,
    private val nsdService: NsdService,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<SocketMessage>>(emptyList())
    val messages: StateFlow<List<SocketMessage>> get() = _messages

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> get() = _isConnected

    init {
        // Starting Nsd Discovery for mDNS services at start
        viewModelScope.launch {
            nsdService.startDiscovery()
            delay(1.seconds)
            stopDiscovery()
        }
    }

    // Capture the stateflow from the service as the data updates
    val services: StateFlow<List<NsdServiceInfo>> = nsdService.services

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun saveDevice(serviceInfo: NsdServiceInfo) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val hostAddress = serviceInfo.hostAddresses.toString()
                    preferencesRepository.saveDeviceDetails(
                        deviceName = serviceInfo.serviceName,
                        hostAddress = hostAddress,
                    )
                    Log.d(TAG, "Service details saved: ${serviceInfo.serviceName}, ${hostAddress}, ${serviceInfo.port}")
                } else {
                    preferencesRepository.saveDeviceDetails(
                        deviceName = serviceInfo.serviceName,
                        hostAddress = serviceInfo.host.hostAddress!!,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to service: ${e.message}", e)
            }
        }
    }

    fun findServices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            nsdService.startDiscovery()
            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)
            stopDiscovery()
            _isRefreshing.value = false
        }
    }

    private fun stopDiscovery() {
        nsdService.stopDiscovery()
    }


    companion object {
        private const val TAG = "OnboardingViewModel"
        private const val PORT = 5149
    }
}
