package com.deepeye.musicpro.dsp.engine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.dsp.data.PresetRepository
import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.EngineState
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.RiskLevel
import com.deepeye.musicpro.player.visualizer.VisualizerEngine
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
    val showConflictWarning: Boolean = false
)

@HiltViewModel
class V4AViewModel @Inject constructor(
    private val engine: V4AEngine,
    private val presetRepository: PresetRepository,
    private val visualizerEngine: VisualizerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(V4AUiState())
    val uiState: StateFlow<V4AUiState> = _uiState.asStateFlow()

    val fftData = visualizerEngine.fftData.map { bytes ->
        if (bytes.isEmpty()) FloatArray(0)
        else {
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
    }

    private fun observeEngineState() {
        // 1. Sync engine flows into UI State
        viewModelScope.launch {
            combine(
                engine.engineState,
                engine.currentParams,
                engine.gainBudget,
                engine.currentRoute,
                engine.currentSessionId
            ) { state, params, budget, route, sid ->
                _uiState.value.copy(
                    engineState = state,
                    params = params,
                    gainBudget = budget,
                    currentRoute = route,
                    sessionId = sid,
                    showConflictWarning = params.surroundEnabled && params.convolverEnabled
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        // 2. Load Presets
        viewModelScope.launch {
            presetRepository.getAllPresets().collect { presets ->
                _uiState.value = _uiState.value.copy(presets = presets)
            }
        }
    }

    fun updateParams(transform: (DspParams) -> DspParams) {
        val current = _uiState.value.params
        val next = transform(current)
        
        // 🚀 Instant visual feedback
        _uiState.value = _uiState.value.copy(params = next)
        
        // 🛠️ Async push to engine
        viewModelScope.launch {
            engine.updateParams(next)
        }
    }

    fun toggleMasterEnabled() {
        updateParams { it.copy(enabled = !it.enabled) }
    }

    fun updateEqBand(bandIndex: Int, value: Float) {
        updateParams { params ->
            val newBands = params.eqBands.copyOf()
            if (bandIndex in newBands.indices) {
                newBands[bandIndex] = value.coerceIn(-12f, 12f)
            }
            params.copy(eqBands = newBands)
        }
    }

    fun activeModuleNames(): List<String> {
        val p = _uiState.value.params
        return listOfNotNull(
            if (p.pgcEnabled) "PGC" else null,
            if (p.eqEnabled) "EQ" else null,
            if (p.bassBoostEnabled) "Bass" else null,
            if (p.virtualizerEnabled) "Virtualizer" else null,
            if (p.reverbEnabled) "Reverb" else null,
            if (p.loudnessEnabled) "Loudness" else null
        )
    }

    fun loadPreset(presetId: Long) {
        viewModelScope.launch {
            val preset = _uiState.value.presets.find { it.first == presetId }
            presetRepository.getPresetParams(presetId).collect { params ->
                params?.let {
                    engine.updateParams(it, preset?.second)
                    _uiState.value = _uiState.value.copy(
                        selectedPresetId = presetId,
                        params = it
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
}
