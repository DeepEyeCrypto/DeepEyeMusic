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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.RiskLevel

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

/**
 * ViewModel for the V4A DSP screen.
 * Bridges UI actions to V4AEngine and PresetRepository.
 */
@HiltViewModel
class V4AViewModel @Inject constructor(
    private val engine: V4AEngine,
    private val presetRepository: PresetRepository,
    private val visualizerEngine: com.deepeye.musicpro.player.visualizer.VisualizerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(V4AUiState())
    val uiState: StateFlow<V4AUiState> = _uiState.asStateFlow()

    private val _pendingParams = MutableStateFlow<DspParams?>(null)

    // Expose flows for HomeHub/NowPlaying
    val gainBudget = engine.gainBudget
    val currentRoute = engine.currentRoute
    val currentPresetName = engine.currentPresetName

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
        // Observe engine state
        viewModelScope.launch {
            engine.engineState.collect { state ->
                _uiState.value = _uiState.value.copy(engineState = state)
            }
        }

        // Observe current params from engine (e.g. on preset load)
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

        // Observe current route
        viewModelScope.launch {
            engine.currentRoute.collect { route ->
                _uiState.value = _uiState.value.copy(currentRoute = route)
            }
        }

        // Observe current session ID
        viewModelScope.launch {
            engine.currentSessionId.collect { id ->
                _uiState.value = _uiState.value.copy(sessionId = id)
            }
        }

        // Debounced param updates
        viewModelScope.launch {
            _pendingParams.collect { params ->
                params?.let {
                    kotlinx.coroutines.delay(100) // Debounce for HAL stability
                    engine.updateParams(it, "Custom Tuning")
                    
                    val budget = GainBudgetCalculator.calculate(it)
                    if (budget.risk == RiskLevel.DANGER) {
                        val corrected = GainBudgetCalculator.autoCorrect(it)
                        engine.updateParams(corrected, "Custom Tuning (Safe)")
                    }
                }
            }
        }
    }

    fun updateParams(transform: (DspParams) -> DspParams) {
        val next = transform(_uiState.value.params)
        // Update UI immediately for responsiveness
        _uiState.value = _uiState.value.copy(params = next)
        // Schedule engine update
        _pendingParams.value = next
    }

    fun activeModuleNames(): List<String> {
        val p = _uiState.value.params
        val active = mutableListOf<String>()
        if (p.pgcEnabled) active.add("PGC")
        if (p.eqEnabled) active.add("EQ")
        if (p.bassBoostEnabled) active.add("Bass")
        if (p.virtualizerEnabled) active.add("Virtualizer")
        if (p.reverbEnabled) active.add("Reverb")
        if (p.loudnessEnabled) active.add("Loudness")
        if (p.viperBassEnabled) active.add("ViperBass")
        if (p.viperClarityEnabled) active.add("Clarity")
        if (p.tubeEnabled) active.add("Tube")
        return active
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

    fun loadPreset(presetId: Long) {
        viewModelScope.launch {
            val preset = _uiState.value.presets.find { it.first == presetId }
            presetRepository.getPresetParams(presetId).collect { params ->
                params?.let {
                    engine.updateParams(it, preset?.second)
                    _uiState.value = _uiState.value.copy(selectedPresetId = presetId)
                }
            }
        }
    }

    fun loadPresetByName(name: String) {
        viewModelScope.launch {
            val presets = _uiState.value.presets
            val preset = presets.find { it.second == name }
            val params = presetRepository.findByName(name)
            params?.let {
                engine.updateParams(it, name)
                _uiState.value = _uiState.value.copy(selectedPresetId = preset?.first)
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
}
