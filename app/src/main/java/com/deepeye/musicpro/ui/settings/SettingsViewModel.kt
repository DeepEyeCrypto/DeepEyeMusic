package com.deepeye.musicpro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.AppSettings
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.prefs.ThemeMode
import com.deepeye.musicpro.data.prefs.TasteProfile
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import com.deepeye.musicpro.domain.usecase.SyncLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isRescanningLibrary: Boolean = false,
    val tasteProfile: TasteProfile = TasteProfile()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val syncLibraryUseCase: SyncLibraryUseCase,
    private val tasteProfileRepository: TasteProfileRepository
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
    }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settingsDataStore.setThemeMode(mode) } }
    fun setDynamicColor(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) } }
    fun setCrossfadeDuration(seconds: Int) { viewModelScope.launch { settingsDataStore.setCrossfadeDuration(seconds) } }
    fun setShowVisualizer(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setShowVisualizer(enabled) } }

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
            try { syncLibraryUseCase() } catch (_: Exception) { }
            _uiState.value = _uiState.value.copy(isRescanningLibrary = false)
        }
    }
}
