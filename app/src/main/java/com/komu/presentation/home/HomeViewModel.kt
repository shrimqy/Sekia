package com.komu.presentation.home

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.di.AppCoroutineScope
import com.komu.sekia.services.Actions
import com.komu.sekia.services.NetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.database.Device
import komu.seki.data.repository.AppRepository
import komu.seki.domain.models.MediaAction
import komu.seki.domain.models.PlaybackData
import komu.seki.domain.models.SocketMessage
import komu.seki.domain.repository.PlaybackRepository
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    val preferencesRepository: PreferencesRepository,
    playbackRepository: PlaybackRepository,
    private val webSocketRepository: WebSocketRepository,
    private val appScope: AppCoroutineScope,
    appRepository: AppRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _syncStatus = MutableStateFlow(false)
    val syncStatus: StateFlow<Boolean> = _syncStatus

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _deviceDetails = MutableStateFlow<Device?>(null)
    val deviceDetails: StateFlow<Device?> = _deviceDetails

    val playbackData: StateFlow<PlaybackData?> = playbackRepository.readPlaybackData()

    init {
        appScope.launch {
            preferencesRepository.readLastConnected().collectLatest { lastConnectedValue ->
                if (lastConnectedValue != null) {
                    appRepository.getDevice(lastConnectedValue).collectLatest { device ->
                        Log.d("HomeViewModel", "Device found: ${device.deviceName}")
                        _deviceDetails.value = device
                    }
                }
            }
        }

        appScope.launch {
            preferencesRepository.readSyncStatus().collectLatest {
                _syncStatus.value = it
            }
        }
        viewModelScope.launch {
            val currentStatus = preferencesRepository.readSyncStatus().firstOrNull() ?: false
            if (!currentStatus) {
                toggleSync(true)
            }
        }
    }

    fun toggleSync(syncRequest: Boolean) {
        appScope.launch {
            _isRefreshing.value = true
            if (deviceDetails.value?.ipAddress != null) {
                // Proceed based on current status
                if (syncRequest and !syncStatus.value){
                    val intent = Intent(getApplication(), NetworkService::class.java).apply {
                        action = Actions.START.name
                        putExtra(NetworkService.EXTRA_HOST_ADDRESS, deviceDetails.value?.ipAddress)
                    }
                    getApplication<Application>().startService(intent)
                } else if (syncRequest and syncStatus.value) {
                    var intent = Intent(getApplication(), NetworkService::class.java).apply {
                        action = Actions.STOP.name
                        putExtra(NetworkService.EXTRA_HOST_ADDRESS, deviceDetails.value?.ipAddress)
                    }
                    getApplication<Application>().startService(intent)
                    delay(50)
                    intent = Intent(getApplication(), NetworkService::class.java).apply {
                        action = Actions.START.name
                        putExtra(NetworkService.EXTRA_HOST_ADDRESS, deviceDetails.value?.ipAddress)
                    }
                    getApplication<Application>().startService(intent)
                } else if (!syncRequest and syncStatus.value){
                    val intent = Intent(getApplication(), NetworkService::class.java).apply {
                        action = Actions.STOP.name
                        putExtra(NetworkService.EXTRA_HOST_ADDRESS, deviceDetails.value?.ipAddress)
                    }
                    getApplication<Application>().startService(intent)
                }
            }
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

    fun onVolumeChange(volume: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            sendMessage(PlaybackData(volume = volume.toFloat(), appName = playbackData.value?.appName, mediaAction = MediaAction.VOLUME))
        }
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
