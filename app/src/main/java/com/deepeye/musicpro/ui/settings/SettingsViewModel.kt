// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.AppSettings
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.prefs.ThemeMode
import com.deepeye.musicpro.data.source.remote.update.AutoUpdateManager
import com.deepeye.musicpro.data.source.remote.update.UpdateState
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
    val updateState: UpdateState = UpdateState.Idle,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val settingsDataStore: SettingsDataStore,
    private val syncLibraryUseCase: SyncLibraryUseCase,
    private val autoUpdateManager: AutoUpdateManager,
    private val notificationStateRepo: com.deepeye.musicpro.data.repository.notification.NotificationStateRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val enabled = notificationStateRepo.isNotificationsEnabled()
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
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

    fun setAutoplayOnCellular(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoplayOnCellular(enabled) }
    }

    fun setPreferredLanguages(languages: Set<String>) {
        // No-op: handled by YouTube InnerTube
    }

    fun setFavoriteArtists(artists: Set<String>) {
        // No-op: handled by YouTube InnerTube
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

    fun forceCloudSync() {
        // No-op: synced directly with InnerTube
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationStateRepo.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    fun triggerSubscriptionCheckNow(context: android.content.Context) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.deepeye.musicpro.workers.YouTubeSubscriptionWorker>()
            .build()
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun logoutYouTube() {
        viewModelScope.launch {
            settingsDataStore.setYouTubeTokens("", null)
        }
    }

    fun saveYouTubeTokens(accessToken: String, refreshToken: String?) {
        viewModelScope.launch {
            settingsDataStore.setYouTubeTokens(accessToken, refreshToken)
        }
    }

    fun saveYouTubeProfile(name: String?, avatarUrl: String?, email: String?) {
        viewModelScope.launch {
            settingsDataStore.setYouTubeProfile(name, avatarUrl, email)
        }
    }
}
