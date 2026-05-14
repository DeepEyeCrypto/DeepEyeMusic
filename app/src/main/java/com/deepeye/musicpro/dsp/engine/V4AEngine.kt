package com.deepeye.musicpro.dsp.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.EngineState
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V4A DSP Engine — manages the AudioEffect chain for the active audio session.
 *
 * Singleton lifecycle: one instance per app lifetime.
 * Session attach/detach MUST be symmetric — never leak an AudioEffect.
 */
@Singleton
class V4AEngine @Inject constructor() {

    companion object {
        private const val TAG = "V4AEngine"
        private const val MAX_HEADROOM_DB = 12f
    }

    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _currentParams = MutableStateFlow(DspParams())
    val currentParams: StateFlow<DspParams> = _currentParams.asStateFlow()

    private val _gainBudget = MutableStateFlow(GainBudget(0f, MAX_HEADROOM_DB, RiskLevel.SAFE, emptyMap()))
    val gainBudget: StateFlow<GainBudget> = _gainBudget.asStateFlow()

    // Audio effects (created when session is attached)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentSessionId: Int = 0

    /**
     * Attaches the DSP engine to an ExoPlayer audio session.
     * Must be called from IO dispatcher.
     */
    suspend fun attachSession(audioSessionId: Int) = withContext(Dispatchers.IO) {
        if (audioSessionId == currentSessionId && _engineState.value == EngineState.ATTACHED) {
            return@withContext
        }

        // Release any existing session first (symmetric detach)
        releaseSession()

        try {
            currentSessionId = audioSessionId

            // Create AudioEffect instances
            equalizer = Equalizer(0, audioSessionId)
            bassBoost = BassBoost(0, audioSessionId)
            virtualizer = Virtualizer(0, audioSessionId)
            presetReverb = PresetReverb(0, audioSessionId)
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)

            // Apply current params
            applyParams(_currentParams.value)

            _engineState.value = EngineState.ATTACHED
            Log.i(TAG, "DSP engine attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach DSP engine", e)
            _engineState.value = EngineState.ERROR
            releaseSession()
        }
    }

    /**
     * Releases all AudioEffect instances.
     */
    suspend fun releaseSession() = withContext(Dispatchers.IO) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing effects", e)
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            presetReverb = null
            loudnessEnhancer = null
            currentSessionId = 0
            _engineState.value = EngineState.IDLE
        }
    }

    /**
     * Updates the DSP parameters and applies them to the audio effects.
     */
    suspend fun updateParams(params: DspParams) = withContext(Dispatchers.IO) {
        _currentParams.value = params
        applyParams(params)
        recalculateGainBudget(params)
    }

    /**
     * Applies DSP parameters to the active AudioEffect instances.
     */
    private fun applyParams(params: DspParams) {
        if (_engineState.value != EngineState.ATTACHED) return

        try {
            // ── Equalizer ──
            equalizer?.let { eq ->
                eq.enabled = params.enabled && params.eqEnabled
                if (eq.enabled) {
                    val numBands = eq.numberOfBands.toInt()
                    for (i in 0 until minOf(numBands, params.eqBands.size)) {
                        val millibels = (params.eqBands[i] * 100).toInt().toShort()
                        eq.setBandLevel(i.toShort(), millibels)
                    }
                }
            }

            // ── Bass Boost ──
            bassBoost?.let { bb ->
                bb.enabled = params.enabled && params.bassBoostEnabled
                if (bb.enabled) {
                    bb.setStrength(params.bassBoostStrength.toShort())
                }
            }

            // ── Virtualizer ──
            virtualizer?.let { virt ->
                virt.enabled = params.enabled && params.virtualizerEnabled
                if (virt.enabled) {
                    virt.setStrength(params.virtualizerStrength.toShort())
                }
            }

            // ── Reverb ──
            presetReverb?.let { reverb ->
                reverb.enabled = params.enabled && params.reverbEnabled
                if (reverb.enabled) {
                    reverb.preset = params.reverbPreset.ordinal.toShort()
                }
            }

            // ── Loudness Enhancer ──
            loudnessEnhancer?.let { loud ->
                loud.enabled = params.enabled && params.loudnessEnabled
                if (loud.enabled) {
                    loud.setTargetGain((params.loudnessGain * 100).toInt())
                }
            }

            _engineState.value = EngineState.PROCESSING
        } catch (e: Exception) {
            Log.e(TAG, "Error applying DSP params", e)
        }
    }

    /**
     * Recalculates the gain budget based on all active DSP modules.
     */
    private fun recalculateGainBudget(params: DspParams) {
        val breakdown = mutableMapOf<String, Float>()

        if (params.pgcEnabled) breakdown["PGC"] = params.pgcGain
        if (params.eqEnabled) breakdown["EQ"] = params.eqBands.maxOrNull() ?: 0f
        if (params.bassBoostEnabled) breakdown["Bass"] = params.bassBoostStrength / 100f
        if (params.loudnessEnabled) breakdown["Loudness"] = params.loudnessGain
        breakdown["Master"] = params.masterGain

        val totalGain = breakdown.values.sum()
        val headroom = MAX_HEADROOM_DB - totalGain

        val riskLevel = when {
            headroom > 6f -> RiskLevel.SAFE
            headroom >= 0f -> RiskLevel.MODERATE
            else -> RiskLevel.DANGER
        }

        _gainBudget.value = GainBudget(
            totalGain = totalGain,
            headroom = headroom,
            riskLevel = riskLevel,
            breakdown = breakdown
        )
    }
}
