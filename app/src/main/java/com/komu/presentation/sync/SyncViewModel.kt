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
import komu.seki.network.services.NsdService
import komu.seki.domain.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds


@HiltViewModel
class SyncViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val nsdService: NsdService
) : ViewModel() {

    val services: StateFlow<List<NsdServiceInfo>> = nsdService.services

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            nsdService.startDiscovery()
            delay(1.seconds)
            stopDiscovery()
        }
    }

    fun connectToService(serviceInfo: NsdServiceInfo) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val hostAddress = serviceInfo.hostAddresses.first().hostAddress
                    preferencesRepository.saveServiceDetails(
                        serviceName = serviceInfo.serviceName,
                        hostAddress = hostAddress!!,
                        port = serviceInfo.port
                    )
                    Log.d(TAG, "Service details saved: ${serviceInfo.serviceName}, ${hostAddress}, ${serviceInfo.port}")
                } else {
                    preferencesRepository.saveServiceDetails(
                        serviceName = serviceInfo.serviceName,
                        hostAddress = serviceInfo.host.hostAddress!!,
                        port = serviceInfo.port
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

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, stopping discovery")
        stopDiscovery()
        nsdService.releaseMulticastLock()
    }

    companion object {
        private const val TAG = "OnboardingViewModel"
        private const val PORT = 5
    }
}
