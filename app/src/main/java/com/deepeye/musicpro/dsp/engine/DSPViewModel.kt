// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.engine

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.DSPKeys
import com.deepeye.musicpro.data.prefs.dspDataStore
import com.deepeye.musicpro.dsp.data.PresetRepository
import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.EngineState
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel
import com.deepeye.musicpro.player.visualizer.VisualizerEngine
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the V4A DSP screen.
 */
data class V4AUiState(
    val params: DspParams = DspParams(),
    val engineState: EngineState = EngineState.IDLE,
    val gainBudget: GainBudget = GainBudget(0f, RiskLevel.SAFE),
    val presets: List<Pair<Long, String>> = emptyList(),
    val selectedPresetId: Long? = null,
    val sessionId: Int = 0,
    val currentRoute: AudioRoute = AudioRoute.UNKNOWN,
    val showConflictWarning: Boolean = false,
    val activePreset: com.deepeye.musicpro.dsp.model.DSPPreset = com.deepeye.musicpro.dsp.model.DSPPreset.DEFAULT,
)

@HiltViewModel
class DSPViewModel
@Inject
constructor(
    private val application: Application,
    val dspEngine: DSPEngine,
    private val presetRepository: PresetRepository,
    private val visualizerEngine: VisualizerEngine,
    private val gson: Gson,
    private val dspProfileManager: com.deepeye.musicpro.dsp.profile.DspProfileManager,
    private val playerController: com.deepeye.musicpro.player.controller.PlayerController,
    private val rankingRepository: com.deepeye.musicpro.domain.ranking.RankingRepository
) : ViewModel() {
    private val dataStore = application.dspDataStore

    private val _uiState = MutableStateFlow(V4AUiState())
    val uiState: StateFlow<V4AUiState> = _uiState.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userRankFlow = kotlinx.coroutines.flow.flow {
        val currentUserId = rankingRepository.getCurrentUserId()
        if (currentUserId != null) {
            rankingRepository.observeUserRank(currentUserId).collect { rank ->
                emit(rank?.rank ?: 999999)
            }
        } else {
            emit(999999)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 999999)

    val fftData =
        visualizerEngine.fftData.map { bytes ->
            if (bytes.isEmpty()) {
                FloatArray(0)
            } else {
                val magnitudes = FloatArray(bytes.size / 2)
                for (i in magnitudes.indices) {
                    val r = bytes[i * 2].toInt()
                    val im = bytes[i * 2 + 1].toInt()
                    magnitudes[i] = (Math.sqrt((r * r + im * im).toDouble()) / 128f).toFloat().coerceIn(0f, 1f)
                }
                magnitudes
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FloatArray(0))

    init {
        observeEngineState()
        loadInitialState()
    }

    private fun observeEngineState() {
        // 1. Sync engine flows into UI State
        viewModelScope.launch {
            combine(
                dspEngine.engineState,
                dspEngine.currentParams,
                dspEngine.gainBudget,
                dspEngine.currentRoute,
                dspEngine.currentSessionId,
            ) { state, params, budget, route, sid ->
                _uiState.value.copy(
                    engineState = state,
                    params = params,
                    gainBudget = budget,
                    currentRoute = route,
                    sessionId = sid,
                    showConflictWarning = params.surroundEnabled && params.convolverEnabled,
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        // 2. Load Presets list from DB
        viewModelScope.launch {
            presetRepository.getAllPresets().collect { list ->
                _uiState.value = _uiState.value.copy(presets = list)
            }
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val json = prefs[DSPKeys.ACTIVE_PARAMS_JSON]
            val params =
                if (!json.isNullOrBlank()) {
                    try {
                        gson.fromJson(json, DspParams::class.java)
                    } catch (e: Exception) {
                        DspParams()
                    }
                } else {
                    DspParams()
                }

            val enabled = prefs[DSPKeys.ENABLED] ?: false
            val finalParams = params.copy(enabled = enabled)
            dspEngine.updateParams(finalParams)
        }
    }

    fun updateParams(transform: (DspParams) -> DspParams) {
        val current = _uiState.value.params
        val next = transform(current)

        // Push update to engine immediately
        dspEngine.updateParams(next)

        // Persist to DataStore
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DSPKeys.ACTIVE_PARAMS_JSON] = gson.toJson(next)
                prefs[DSPKeys.ENABLED] = next.enabled
            }
        }
    }

    fun toggleMasterEnabled() {
        updateParams { it.copy(enabled = !it.enabled) }
    }

    fun applyDSPPreset(preset: com.deepeye.musicpro.dsp.model.DSPPreset) {
        updateParams { preset.params.copy(enabled = it.enabled) }
        _uiState.value = _uiState.value.copy(activePreset = preset)
    }

    fun updateEqBand(
        bandIndex: Int,
        value: Float,
    ) {
        updateParams { params ->
            val newBands = params.eqBands.copyOf()
            if (bandIndex in newBands.indices) {
                newBands[bandIndex] = value.coerceIn(-12f, 12f)
            }
            params.copy(eqBands = newBands, eqEnabled = true)
        }
    }

    fun activeModuleNames(): List<String> {
        val p = _uiState.value.params
        return listOfNotNull(
            if (p.pgcEnabled) "PGC" else null,
            if (p.eqEnabled) "EQ" else null,
            if (p.bassBoostEnabled || p.viperBassEnabled) "Bass" else null,
            if (p.virtualizerEnabled) "Virtualizer" else null,
            if (p.reverbEnabled) "Reverb" else null,
            if (p.loudnessEnabled) "Loudness" else null,
            if (p.dynamicsEnabled) "Dynamics" else null,
            if (p.surroundEnabled) "Surround" else null,
            if (p.convolverEnabled) "Convolver" else null,
            if (p.tubeEnabled) "Tube" else null,
            if (p.clarityEnabled) "Clarity" else null,
            if (p.hrtfEnabled) "HRTF" else null,
            if (p.speakerProtectionEnabled) "Protection" else null,
            if (p.noiseGateEnabled) "Gate" else null,
        )
    }

    fun loadPreset(presetId: Long) {
        viewModelScope.launch {
            val preset = _uiState.value.presets.find { it.first == presetId }
            presetRepository.getPresetParams(presetId).collect { params ->
                params?.let {
                    val finalParams = it.copy(enabled = _uiState.value.params.enabled)
                    updateParams { finalParams }
                    _uiState.value =
                        _uiState.value.copy(
                            selectedPresetId = presetId,
                        )
                }
            }
        }
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            val id = presetRepository.savePreset(name, _uiState.value.params)
            _uiState.value = _uiState.value.copy(selectedPresetId = id)
        }
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            presetRepository.deletePreset(presetId)
            if (_uiState.value.selectedPresetId == presetId) {
                _uiState.value = _uiState.value.copy(selectedPresetId = null)
            }
        }
    }

    fun saveProfileForCurrentTrack() {
        viewModelScope.launch {
            val trackId = playerController.playerState.value.currentItem?.id
            if (trackId != null) {
                dspProfileManager.saveProfileForTrack(trackId, _uiState.value.params)
                android.widget.Toast.makeText(application, "Saved DSP profile for this track", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(application, "No track currently playing", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
