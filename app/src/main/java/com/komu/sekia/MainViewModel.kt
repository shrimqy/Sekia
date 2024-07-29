package com.komu.sekia

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.navigation.Graph
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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

    init {
        Log.d("MainViewModel", "ViewModel initialized")
        viewModelScope.launch {
            preferencesRepository.readSyncStatus().collectLatest { onSyncComplete ->
                Log.d("MainViewModel", "Onboarding status: $onSyncComplete")
                startDestination = if (onSyncComplete) {
                    Graph.MainScreenGraph
                } else {
                    Graph.SyncGraph
                }
                delay(100)
                splashCondition = false
            }
        }
    }
}