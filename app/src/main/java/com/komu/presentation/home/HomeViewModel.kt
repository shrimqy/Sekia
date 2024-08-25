package com.komu.presentation.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.di.AppCoroutineScope
import com.komu.sekia.services.Actions
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.repository.PlaybackRepositoryImpl
import komu.seki.domain.models.DeviceDetails
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    playbackRepository: PlaybackRepository,
    private val appScope: AppCoroutineScope,
    application: Application
) : AndroidViewModel(application) {

    val syncStatus: StateFlow<Boolean> = preferencesRepository.readSyncStatus()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _deviceDetails = MutableStateFlow<DeviceDetails?>(null)
    val deviceDetails: StateFlow<DeviceDetails?> = _deviceDetails.asStateFlow()

    val playbackData: StateFlow<PlaybackData?> = playbackRepository.readPlaybackData().also { flow ->
        viewModelScope.launch {
            flow.collect {
                Log.d("HomeViewModel", "PlaybackData updated: $it")
            }
        }
    }

    init {
        appScope.launch {
            preferencesRepository.readDeviceDetails()?.collectLatest {
                _deviceDetails.value = it
            }
        }
    }

    fun toggleSync(context: Context, syncStatus: Boolean, hostAddress: String) {

        val intent = Intent(context, WebSocketService::class.java).apply {
            action = if (syncStatus) Actions.STOP.name else Actions.START.name

            putExtra(WebSocketService.EXTRA_HOST_ADDRESS, hostAddress)
        }
        context.startService(intent)

        // Fake slower refresh so it doesn't seem like it's not doing anything
        appScope.launch {
            _isRefreshing.value = true
            delay(2.seconds)
            _isRefreshing.value = false
        }
    }

    // Handle play/pause, next, previous actions
    fun onPlayPause() {
        // Implement play/pause toggle logic
    }

    fun onNext() {
        // Implement next track logic
    }

    fun onPrevious() {
        // Implement previous track logic
    }
}
