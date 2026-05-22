// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.engine

import android.media.audiofx.BassBoost
import android.os.Build
import android.media.audiofx.DynamicsProcessing
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

@Singleton
class V4AEngine @Inject constructor() {

    companion object {
        private const val TAG = "V4AEngine"
        private const val EFFECT_PRIORITY = 100 // High priority
    }

    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _currentParams = MutableStateFlow(DspParams())
    val currentParams: StateFlow<DspParams> = _currentParams.asStateFlow()

    private val _gainBudget = MutableStateFlow(GainBudget(0f, RiskLevel.SAFE))
    val gainBudget: StateFlow<GainBudget> = _gainBudget.asStateFlow()

    private val _currentPresetName = MutableStateFlow("Default Tuning")
    val currentPresetName: StateFlow<String> = _currentPresetName.asStateFlow()

    private val _currentRoute = MutableStateFlow(com.deepeye.musicpro.dsp.model.AudioRoute.UNKNOWN)
    val currentRoute: StateFlow<com.deepeye.musicpro.dsp.model.AudioRoute> = _currentRoute.asStateFlow()

    private val _currentSessionId = MutableStateFlow(0)
    val currentSessionId: StateFlow<Int> = _currentSessionId.asStateFlow()

    fun isAttached(): Boolean = _engineState.value == EngineState.ATTACHED || _engineState.value == EngineState.PROCESSING

    fun getCurrentSessionId(): Int = _currentSessionId.value

    fun updateRoute(route: com.deepeye.musicpro.dsp.model.AudioRoute) {
        _currentRoute.value = route
    }

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    private var activeSessionIdInt: Int = 0

    suspend fun attachSession(audioSessionId: Int) = withContext(Dispatchers.IO) {
        if (audioSessionId == 0) return@withContext
        if (audioSessionId == activeSessionIdInt && isAttached()) return@withContext

        releaseSession()

        try {
            activeSessionIdInt = audioSessionId
            _currentSessionId.value = audioSessionId
            Log.e(TAG, "Attaching DSP effects to session $audioSessionId")

            // Create effects
            equalizer = try { Equalizer(EFFECT_PRIORITY, audioSessionId) } catch (e: Exception) { null }
            bassBoost = try { BassBoost(EFFECT_PRIORITY, audioSessionId) } catch (e: Exception) { null }
            virtualizer = try { Virtualizer(EFFECT_PRIORITY, audioSessionId) } catch (e: Exception) { null }
            presetReverb = try { PresetReverb(EFFECT_PRIORITY, audioSessionId) } catch (e: Exception) { null }
            loudnessEnhancer = try { LoudnessEnhancer(audioSessionId) } catch (e: Exception) { null }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing = try {
                    val builder = DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        1, // Channels
                        true, 0, // PreEQ
                        true, 0, // MultiBandCompressor
                        true, 0, // PostEQ
                        true // Limiter
                    )
                    DynamicsProcessing(EFFECT_PRIORITY, audioSessionId, builder.build())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create DynamicsProcessing", e)
                    null
                }
            }

            _engineState.value = EngineState.ATTACHED
            applyParams(_currentParams.value)
            Log.d(TAG, "✅ V4A Engine successfully attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Failed to attach DSP engine", e)
            _engineState.value = EngineState.ERROR
        }
    }

    suspend fun releaseSession() = withContext(Dispatchers.IO) {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        presetReverb?.release()
        loudnessEnhancer?.release()
        dynamicsProcessing?.release()
        
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
        dynamicsProcessing = null
        activeSessionIdInt = 0
        _currentSessionId.value = 0
        _engineState.value = EngineState.IDLE
    }

    suspend fun updateParams(params: DspParams, presetName: String? = null) = withContext(Dispatchers.IO) {
        _currentParams.value = params
        presetName?.let { _currentPresetName.value = it }
        applyParams(params)
        _gainBudget.value = GainBudgetCalculator.calculate(params)
    }

    private fun applyParams(params: DspParams) {
        if (!isAttached()) return
        Log.d(TAG, "Applying DSP Params: enabled=${params.enabled}")

        try {
            // ── Master Switch ──
            val isEnabled = params.enabled

            // ── Equalizer ──
            equalizer?.let { eq ->
                eq.enabled = isEnabled && params.eqEnabled
                if (eq.enabled) {
                    val numBands = eq.numberOfBands.toInt()
                    Log.d(TAG, "EQ Status: ${eq.enabled}, Bands: $numBands")
                    for (i in 0 until minOf(numBands, params.eqBands.size)) {
                        val millibels = (params.eqBands[i] * 100).toInt().toShort()
                        eq.setBandLevel(i.toShort(), millibels)
                    }
                }
            }

            // ── Bass Boost ──
            bassBoost?.let { bb ->
                bb.enabled = isEnabled && (params.bassBoostEnabled || params.viperBassEnabled)
                if (bb.enabled) {
                    val strength = if (params.viperBassEnabled) (params.viperBassGain * 100).toInt() else params.bassBoostStrength
                    Log.d(TAG, "Bass Status: ${bb.enabled}, Strength: $strength")
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }

            // ── Virtualizer ──
            virtualizer?.let { virt ->
                virt.enabled = isEnabled && params.virtualizerEnabled
                if (virt.enabled) {
                    virt.setStrength(params.virtualizerStrength.toShort())
                }
            }

            // ── Reverb ──
            presetReverb?.let { reverb ->
                reverb.enabled = isEnabled && params.reverbEnabled
                if (reverb.enabled) {
                    reverb.preset = params.reverbPreset.ordinal.toShort()
                }
            }

            // ── Loudness / Master Gain ──
            loudnessEnhancer?.let { loud ->
                // Always enable if master is on to handle PGC and Master Gain
                loud.enabled = isEnabled
                if (isEnabled) {
                    // Combine PGC, Master Gain, and Loudness Enhancer
                    val totalGainDb = params.pgcGain + params.masterGain + (if (params.loudnessEnabled) params.loudnessGain else 0f)
                    val totalGainMb = (totalGainDb * 100).toInt().coerceAtLeast(0)
                    Log.d(TAG, "Loudness Target: $totalGainMb mB")
                    loud.setTargetGain(totalGainMb)
                }
            }

            // ── Dynamics Processing (API 28+) ──
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.let { dp ->
                    dp.enabled = isEnabled && params.dynamicsEnabled
                    if (dp.enabled) {
                        // For simplicity, we just configure the Limiter part as a start
                        val limiter = DynamicsProcessing.Limiter(
                            true, // inUse
                            params.limiterEnabled, // enabled
                            0, // linkGroup
                            params.compressorAttack, // attackTime
                            params.compressorRelease, // releaseTime
                            10f, // ratio (placeholder)
                            params.limiterThreshold, // threshold
                            0f // postGain
                        )
                        dp.setLimiterAllChannelsTo(limiter)
                    }
                }
            }

            _engineState.value = EngineState.PROCESSING
        } catch (e: Exception) {
            Log.e(TAG, "Error applying DSP params", e)
        }
    }
}
