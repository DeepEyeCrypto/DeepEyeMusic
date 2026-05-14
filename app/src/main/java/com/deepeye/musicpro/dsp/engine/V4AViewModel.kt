package com.deepeye.musicpro.dsp.engine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.dsp.data.PresetRepository
import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.EngineState
import com.deepeye.musicpro.dsp.model.GainBudget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the V4A DSP screen.
 */
data class V4AUiState(
    val params: DspParams = DspParams(),
    val engineState: EngineState = EngineState.IDLE,
    val gainBudget: GainBudget = GainBudget(0f, 12f, com.deepeye.musicpro.dsp.model.RiskLevel.SAFE, emptyMap()),
    val presets: List<Pair<Long, String>> = emptyList(),
    val selectedPresetId: Long? = null,
    val showConflictWarning: Boolean = false
)

/**
 * ViewModel for the V4A DSP screen.
 * Bridges UI actions to V4AEngine and PresetRepository.
 */
@HiltViewModel
class V4AViewModel @Inject constructor(
    private val engine: V4AEngine,
    private val presetRepository: PresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(V4AUiState())
    val uiState: StateFlow<V4AUiState> = _uiState.asStateFlow()

    init {
        // Observe engine state
        viewModelScope.launch {
            engine.engineState.collect { state ->
                _uiState.value = _uiState.value.copy(engineState = state)
            }
        }

        // Observe current params
        viewModelScope.launch {
            engine.currentParams.collect { params ->
                val showConflict = params.surroundEnabled && params.convolverEnabled
                _uiState.value = _uiState.value.copy(
                    params = params,
                    showConflictWarning = showConflict
                )
            }
        }

        // Observe gain budget
        viewModelScope.launch {
            engine.gainBudget.collect { budget ->
                _uiState.value = _uiState.value.copy(gainBudget = budget)
            }
        }

        // Load presets
        viewModelScope.launch {
            presetRepository.getAllPresets().collect { presets ->
                _uiState.value = _uiState.value.copy(presets = presets)
            }
        }

        // Seed builtin presets on first launch
        viewModelScope.launch {
            presetRepository.seedBuiltinPresets()
        }
    }

    /**
     * Updates a single DSP parameter — triggers engine re-apply.
     */
    fun updateParams(transform: (DspParams) -> DspParams) {
        viewModelScope.launch {
            val newParams = transform(_uiState.value.params)
            engine.updateParams(newParams)
        }
    }

    /**
     * Toggles the master DSP on/off.
     */
    fun toggleMasterEnabled() {
        updateParams { it.copy(enabled = !it.enabled) }
    }

    /**
     * Updates a single EQ band value.
     */
    fun updateEqBand(bandIndex: Int, value: Float) {
        updateParams { params ->
            val newBands = params.eqBands.toMutableList()
            if (bandIndex in newBands.indices) {
                newBands[bandIndex] = value.coerceIn(-12f, 12f)
            }
            params.copy(eqBands = newBands)
        }
    }

    /**
     * Loads a preset by ID.
     */
    fun loadPreset(presetId: Long) {
        viewModelScope.launch {
            presetRepository.getPresetParams(presetId).collect { params ->
                params?.let {
                    engine.updateParams(it)
                    _uiState.value = _uiState.value.copy(selectedPresetId = presetId)
                }
            }
        }
    }

    /**
     * Saves the current parameters as a new preset.
     */
    fun savePreset(name: String) {
        viewModelScope.launch {
            val id = presetRepository.savePreset(name, _uiState.value.params)
            _uiState.value = _uiState.value.copy(selectedPresetId = id)
        }
    }

    /**
     * Deletes a user preset.
     */
    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            presetRepository.deletePreset(presetId)
            if (_uiState.value.selectedPresetId == presetId) {
                _uiState.value = _uiState.value.copy(selectedPresetId = null)
            }
        }
    }
}
