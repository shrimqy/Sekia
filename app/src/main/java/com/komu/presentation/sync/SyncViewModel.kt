package com.komu.presentation.sync

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.services.Actions
import com.komu.sekia.services.NetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.models.SocketMessage
import komu.seki.data.services.NsdService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds


@HiltViewModel
class SyncViewModel @Inject constructor(
    application: Application,
    private val nsdService: NsdService,
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
            nsdService.stopDiscovery()
            if (services.value.isEmpty()) {
                Toast.makeText(application.applicationContext, "Make sure you're connected to the same network as your PC", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Capture the stateflow from the service as the data updates
    val services: StateFlow<List<NsdServiceInfo>> = nsdService.services

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun saveDevice(context: Context, serviceInfo: NsdServiceInfo) {
        viewModelScope.launch {
            try {
                val hostAddress = String(serviceInfo.attributes["ipAddress"]!!, Charsets.UTF_8)
                val intent = Intent(context, NetworkService::class.java).apply {
                    action = Actions.START.name
                    putExtra(NetworkService.NEW_DEVICE, true)
                    putExtra(NetworkService.EXTRA_HOST_ADDRESS, hostAddress)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to service: ${e.message}", e)
            }
        }
    }

    fun findServices(context: Context) {
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
