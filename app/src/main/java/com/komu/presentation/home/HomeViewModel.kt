package com.komu.presentation.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.komu.sekia.di.AppCoroutineScope
import com.komu.sekia.services.Actions
import com.komu.sekia.services.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.database.Device
import komu.seki.data.repository.AppRepository
import komu.seki.data.repository.PlaybackRepositoryImpl
import komu.seki.data.services.handleMediaAction
import komu.seki.data.services.mediaController
import komu.seki.domain.models.DeviceInfo
import komu.seki.domain.models.MediaAction
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    playbackRepository: PlaybackRepository,
    val webSocketRepository: WebSocketRepository,
    private val appScope: AppCoroutineScope,
    appRepository: AppRepository,
    application: Application
) : AndroidViewModel(application) {

    val syncStatus: StateFlow<Boolean> = preferencesRepository.readSyncStatus()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _deviceDetails = MutableStateFlow<Device?>(null)
    val deviceDetails: StateFlow<Device?> = _deviceDetails

    private val _lastConnected = MutableStateFlow<String?>(null)
    private val lastConnected: StateFlow<String?> = _lastConnected

    val playbackData: StateFlow<PlaybackData?> = playbackRepository.readPlaybackData()

    init {
        viewModelScope.launch {
            preferencesRepository.readLastConnected().collect { lastConnectedValue ->
                _lastConnected.value = lastConnectedValue
                Log.d("HomeViewModel", "Last connected device: $lastConnectedValue")
                lastConnectedValue?.let {
                    appRepository.getDevice(it).collect { device ->
                        Log.d("HomeViewModel", "Device found: $device")
                        _deviceDetails.value = device
                    }
                }
            }
        }
    }

    fun toggleSync(context: Context, syncStatus: Boolean) {
        val intent = Intent(context, WebSocketService::class.java).apply {
            action = if (syncStatus) Actions.STOP.name else Actions.START.name
            putExtra(WebSocketService.EXTRA_HOST_ADDRESS, deviceDetails.value?.ipAddress)
        }
        context.startService(intent)

        // Fake slower refresh so it doesn't seem like it's not doing anything
        appScope.launch {
            _isRefreshing.value = true
            delay(1.seconds)
            _isRefreshing.value = false
        }
    }

    // Handle play/pause, next, previous actions
    fun onPlayPause() {
        val action: MediaAction = if (playbackData.value?.isPlaying == true) MediaAction.PAUSE else MediaAction.RESUME
        sendPlaybackData(playbackData.value!!, action)
    }

    fun onNext() {
        sendPlaybackData(playbackData.value!!, MediaAction.NEXT_QUEUE)
    }

    fun onPrevious() {
        sendPlaybackData(playbackData.value!!, MediaAction.PREV_QUEUE)
    }

    private fun sendPlaybackData(playbackData: PlaybackData, mediaAction: MediaAction) {
        playbackData.mediaAction = mediaAction
        CoroutineScope(Dispatchers.IO).launch {
            sendMessage(playbackData)
        }
        Log.d("MediaSession", "Action received: $mediaAction" + playbackData.trackTitle)
    }

    private suspend fun sendMessage(message: SocketMessage) {
        webSocketRepository.sendMessage(message)
    }
}
