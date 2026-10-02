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
    val activePreset: com.deepeye.musicpro.dsp.model.DSPPreset = com.deepeye.musicpro.dsp.model.DSPPreset.SUBWOOFER_30HZ_INFRA,
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

    // ── EQ dispatch plumbing ────────────────────────────────────────────────
    // Declared ABOVE `init` on purpose: Kotlin initialises properties in
    // declaration order, and `init` calls observeEqDispatch(), which reads
    // these. Declaring them after `init` left the flow null and crashed the
    // app on every launch.
    /** The curve the fader renders from, updated per frame during a drag. */
    private var stagedEqBands = FloatArray(EQ_BAND_COUNT)

    /** Latest gesture value per band, pending dispatch. */
    private var pendingEqBands = FloatArray(EQ_BAND_COUNT)

    /** Bumped by [setPendingEqBand]; the dispatcher acts only when it changes. */
    private val _eqDispatchSignal = MutableStateFlow(0L)
    private val eqDispatchSignal = _eqDispatchSignal.asStateFlow()

    private var eqDispatcherJob: kotlinx.coroutines.Job? = null

    init {
        observeEngineState()
        observeEqDispatch()
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
            // Hydrate the fader's local mirror so the first frame after launch
            // renders the persisted curve rather than a flat bank.
            val persistedBands = FloatArray(EQ_BAND_COUNT)
            val src = finalParams.eqBands
            for (i in 0 until minOf(src.size, EQ_BAND_COUNT)) persistedBands[i] = src[i]
            stagedEqBands = persistedBands.copyOf()
            pendingEqBands = persistedBands.copyOf()
            dspEngine.updateParams(finalParams)
        }
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    companion object {
        /** Number of hardware EQ bands the fader bank renders and stages. */
        const val EQ_BAND_COUNT = 10
    }

    fun updateParams(transform: (DspParams) -> DspParams) {
        val current = _uiState.value.params
        val next = transform(current)

        // Push update to engine immediately for 0ms audio latency
        dspEngine.updateParams(next)

        // Debounce DataStore disk I/O to avoid UI stuttering during slider drags
        saveJob?.cancel()
        saveJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
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
        val nextParams = preset.params.copy(enabled = _uiState.value.params.enabled)
        if (preset.params.eqBands.size == EQ_BAND_COUNT) {
            stagedEqBands = preset.params.eqBands.copyOf()
            pendingEqBands = preset.params.eqBands.copyOf()
            dspEngine.updateEqBands(preset.params.eqBands)
        }
        updateParams { nextParams }
        _uiState.value = _uiState.value.copy(activePreset = preset)
    }

    /**
     * Per-frame drag entry point.
     *
     * Stages the value and raises the dispatch signal; it deliberately does NOT
     * touch the engine. [observeEqDispatch] collapses a burst of frames into a
     * single push, so the main thread pays for one equalizer update per idle
     * gap rather than one per pixel of finger travel.
     */
    fun updateEqBand(
        bandIndex: Int,
        value: Float,
    ) {
        setPendingEqBand(bandIndex, value)
    }

    /**
     * Replaces the whole EQ curve (preset tap, "Flat" reset).
     *
     * Bypasses the conflated drag queue on purpose: these are discrete,
     * one-shot commands where every band must land, so coalescing could
     * legally drop a band the user just selected.
     */
    fun setEqBands(bands: FloatArray) {
        val size = minOf(bands.size, EQ_BAND_COUNT)
        val staged = FloatArray(EQ_BAND_COUNT)
        for (i in 0 until size) staged[i] = bands[i]
        commitEqBands(staged)
    }

    /**
     * Queues one band's gain for dispatch, dropping superseded values.
     *
     * [stagedEqBands] is the local source of truth the fader UI reads, so the
     * thumb tracks the finger at frame rate. The expensive half — re-deriving
     * the gain budget and pushing the curve to the audio effect — is funnelled
     * through a conflated signal: a fast drag produces far more values than the
     * engine needs, and only the newest value per band is meaningful.
     */
    fun setPendingEqBand(bandIndex: Int, value: Float) {
        if (bandIndex !in stagedEqBands.indices) return
        val clamped = value.coerceIn(-12f, 12f)
        if (stagedEqBands[bandIndex] == clamped) return
        stagedEqBands[bandIndex] = clamped
        pendingEqBands[bandIndex] = clamped
        _eqDispatchSignal.value = System.nanoTime()
    }

    /** The EQ curve the fader UI should render right now (staged, pre-dispatch). */
    fun stagedEqBands(): FloatArray = stagedEqBands.copyOf()

    /**
     * Flushes every staged band to the engine and persists.
     *
     * Called when the gesture ends so the final position is never lost to
     * coalescing, and when a preset is applied.
     */
    fun commitEqBands(bands: FloatArray) {
        if (bands.size != EQ_BAND_COUNT) return
        stagedEqBands = bands.copyOf()
        pendingEqBands = bands.copyOf()

        val current = _uiState.value.params
        val next = current.copy(
            eqBands = bands.copyOf(),
            eqEnabled = true,
        )
        _uiState.value = _uiState.value.copy(params = next)
        dspEngine.updateEqBands(next.eqBands)
        persistParams(next)
    }

    /**
     * Drains the conflated pending values into the engine.
     *
     * `collectLatest` cancels an in-flight dispatch when a newer signal arrives,
     * so a rapid drag never queues a backlog: at most one dispatch is ever
     * pending, and it is always the most recent value per band.
     */
    private fun observeEqDispatch() {
        eqDispatcherJob = viewModelScope.launch {
            eqDispatchSignal.collectLatest {
                val params = _uiState.value.params
                val next = params.copy(
                    eqBands = pendingEqBands.copyOf(),
                    eqEnabled = true,
                )
                dspEngine.updateEqBands(next.eqBands)
                _uiState.value = _uiState.value.copy(params = next)
                persistParams(next)
            }
        }
    }

    /** Extracted so the drag path and the one-shot path persist identically. */
    private fun persistParams(next: DspParams) {
        // Debounce DataStore disk I/O to avoid UI stuttering during slider drags
        saveJob?.cancel()
        saveJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
            dataStore.edit { prefs ->
                prefs[DSPKeys.ACTIVE_PARAMS_JSON] = gson.toJson(next)
                prefs[DSPKeys.ENABLED] = next.enabled
            }
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
