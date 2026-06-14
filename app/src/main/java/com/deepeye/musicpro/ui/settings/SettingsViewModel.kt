// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.AppSettings
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.data.prefs.ThemeMode
import com.deepeye.musicpro.data.source.remote.update.AutoUpdateManager
import com.deepeye.musicpro.data.source.remote.update.UpdateState
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import com.deepeye.musicpro.domain.usecase.SyncLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isRescanningLibrary: Boolean = false,
    val tasteProfile: TasteProfile = TasteProfile(),
    val updateState: UpdateState = UpdateState.Idle,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsDataStore: SettingsDataStore,
    private val syncLibraryUseCase: SyncLibraryUseCase,
    private val tasteProfileRepository: TasteProfileRepository,
    private val autoUpdateManager: AutoUpdateManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            tasteProfileRepository.getTasteProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(tasteProfile = profile)
            }
        }
        viewModelScope.launch {
            autoUpdateManager.updateState.collect { updateState ->
                _uiState.value = _uiState.value.copy(updateState = updateState)
            }
        }
    }

    fun checkForUpdate() {
        autoUpdateManager.checkForUpdate()
    }

    fun downloadUpdate(
        apkUrl: String,
        version: String,
    ) {
        autoUpdateManager.downloadUpdate(apkUrl, version)
    }

    fun installApk(file: File) {
        autoUpdateManager.installApk(file)
    }

    fun resetUpdateState() {
        autoUpdateManager.resetState()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAmoledMode(enabled) }
    }

    fun setCrossfadeDuration(seconds: Int) {
        viewModelScope.launch { settingsDataStore.setCrossfadeDuration(seconds) }
    }

    fun setShowVisualizer(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowVisualizer(enabled) }
    }

    fun setPreferredLanguages(languages: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updatePreferredLanguages(languages)
        }
    }

    fun setFavoriteArtists(artists: Set<String>) {
        viewModelScope.launch {
            tasteProfileRepository.updateFavoriteArtists(artists)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRescanningLibrary = true)
            try {
                syncLibraryUseCase()
            } catch (_: Exception) {
            }
            _uiState.value = _uiState.value.copy(isRescanningLibrary = false)
        }
    }
}
