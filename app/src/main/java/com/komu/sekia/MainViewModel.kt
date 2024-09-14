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
import com.komu.sekia.navigation.SyncRoute
import com.komu.sekia.services.Actions
import com.komu.sekia.services.NetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    context: Context,
    preferencesRepository: PreferencesRepository,
): ViewModel() {
    var splashCondition by mutableStateOf(true)
        private set

    var startDestination by mutableStateOf(Graph.MainScreenGraph)
        private set

    private val _hostAddress = MutableStateFlow<String?>(null)
    private val hostAddress: StateFlow<String?> = _hostAddress.asStateFlow()

    fun startWebSocketService(context: Context) {
        viewModelScope.launch {
            hostAddress.filterNotNull().last().let {
                Intent(context, NetworkService::class.java).also { intent ->
                    intent.action = Actions.START.name
                    intent.putExtra(NetworkService.EXTRA_HOST_ADDRESS, it)
                    context.startForegroundService(intent)
                }
                Log.d("MainViewModel", "Starting WebSocket service with host: $hostAddress")
            }
        }
    }

    init {
        Log.d("MainViewModel", "ViewModel initialized")
        viewModelScope.launch {

            preferencesRepository.readLastConnected().collectLatest { hostAddress ->
                Log.d("MainViewModel", "last used hostAddress: $hostAddress")
                startDestination = if (hostAddress != null) {
                    Graph.MainScreenGraph
                } else {
                    SyncRoute.SyncScreen.route
                }
                delay(150)
                _hostAddress.value = hostAddress
                splashCondition = false
            }
        }
    }
}