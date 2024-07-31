package com.komu.presentation.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.di.AppCoroutineScope
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.models.DeviceDetails
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val appScope: AppCoroutineScope,
    application: Application
) : AndroidViewModel(application) {

    val syncStatus: StateFlow<Boolean> = preferencesRepository.readSyncStatus()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _deviceDetails = MutableStateFlow<DeviceDetails?>(null)
    val deviceDetails: StateFlow<DeviceDetails?> = _deviceDetails.asStateFlow()

    init {
        appScope.launch {
            preferencesRepository.readDeviceDetails()?.collectLatest {
                _deviceDetails.value = it
            }
        }
    }
}
