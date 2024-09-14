package com.komu.presentation.settings

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import komu.seki.data.repository.AppRepository
import komu.seki.domain.models.PreferencesSettings
import komu.seki.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val preferencesRepository: PreferencesRepository,
    application: Application
) : AndroidViewModel(application) {
    private val _preferencesSettings = MutableStateFlow<PreferencesSettings?>(null)
    val preferencesSettings: StateFlow<PreferencesSettings?> = _preferencesSettings

    init {
        viewModelScope.launch {
            preferencesRepository.preferenceSettings().collectLatest {
                _preferencesSettings.value = it
            }
        }
    }

    fun saveAutoDiscoverySettings(boolean: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveAutoDiscoverySettings(boolean)
        }
    }

    fun saveImageClipboardSettings(boolean: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveImageClipboardSettings(boolean)
        }
    }

    fun updateStorageLocation(string: String) {
        viewModelScope.launch {
            preferencesRepository.updateStorageLocation(string)
        }
    }
}