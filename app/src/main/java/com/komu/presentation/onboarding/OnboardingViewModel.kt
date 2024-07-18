package com.komu.presentation.onboarding

import android.content.ContentValues.TAG
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.services.NsdService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val nsdService: NsdService
) : ViewModel() {

    val services: StateFlow<List<NsdServiceInfo>> = nsdService.services

    init {
        nsdService.startDiscovery()
    }

    fun connectToService(serviceInfo: NsdServiceInfo) {
        viewModelScope.launch {
            try {
                nsdService.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        Log.d(TAG, "Service resolved: $resolvedService")
                        // Proceed with connection logic
                    }

                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to service: ${e.message}", e)
            }
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
    }
}