package com.komu.sekia

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.navigation.Graph
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.repository.PreferencesDatastore
import komu.seki.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
): ViewModel() {
    var splashCondition by mutableStateOf(true)
        private set

    var startDestination by mutableStateOf(Graph.MainScreenGraph)
        private set
    init {
        Log.d("MainViewModel", "ViewModel initialized")
        preferencesRepository.readOnboardingStatus().onEach { onboardingComplete ->
            Log.d("MainViewModel", "Onboarding status: $onboardingComplete")
            startDestination = if (onboardingComplete) {
                Graph.MainScreenGraph
            } else  {
                Graph.OnboardingGraph
            }
            splashCondition = false
        }.launchIn(viewModelScope)
    }
}