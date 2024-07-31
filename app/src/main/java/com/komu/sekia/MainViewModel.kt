package com.komu.sekia

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.navigation.Graph
import com.komu.sekia.services.Actions
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.models.DeviceDetails
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
): ViewModel() {
    var splashCondition by mutableStateOf(true)
        private set

    var startDestination by mutableStateOf(Graph.MainScreenGraph)
        private set

    private val _deviceDetails = MutableStateFlow<DeviceDetails?>(null)
    private val deviceDetails: StateFlow<DeviceDetails?> = _deviceDetails.asStateFlow()

    fun startWebSocketService(context: Context) {
        viewModelScope.launch {
            deviceDetails.filterNotNull().first().let { details ->
                val hostAddress = details.hostAddress
                if (hostAddress != null) {
                    Intent(context, WebSocketService::class.java).also { intent ->
                        intent.action = Actions.START.name
                        intent.putExtra(WebSocketService.EXTRA_HOST_ADDRESS, hostAddress)
                        context.startForegroundService(intent)
                    }
                    Log.d("MainViewModel", "Starting WebSocket service with host: $hostAddress")
                } else {
                    Log.e("MainViewModel", "Host address is null, cannot start WebSocket service")
                }
            }
        }
    }

    init {
        Log.d("MainViewModel", "ViewModel initialized")
        viewModelScope.launch {
            preferencesRepository.readDeviceDetails()?.collectLatest { device ->
                Log.d("MainViewModel", "Onboarding status: $device")
                startDestination = if (device.hostAddress != null) {
                    Graph.MainScreenGraph
                } else {
                    Graph.SyncGraph
                }
                delay(150)
                _deviceDetails.value = device
                splashCondition = false
            }
        }
    }
}