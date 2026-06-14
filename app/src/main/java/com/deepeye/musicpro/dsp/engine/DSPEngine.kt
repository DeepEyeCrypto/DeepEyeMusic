// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.deepeye.musicpro.dsp.model.AudioRoute
import com.deepeye.musicpro.dsp.model.DspParams
import com.deepeye.musicpro.dsp.model.EngineState
import com.deepeye.musicpro.dsp.model.GainBudget
import com.deepeye.musicpro.dsp.model.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DSPEngine
@Inject
constructor(
    private val bassProcessor: com.deepeye.musicpro.dsp.bass.BassProcessor,
    private val vocalRemoverProcessor: com.deepeye.musicpro.dsp.processor.VocalRemoverProcessor,
    private val crossfeedProcessor: com.deepeye.musicpro.dsp.processor.CrossfeedProcessor
) {
    companion object {
        private const val TAG = "DSPEngine"
        private const val EFFECT_PRIORITY = 100 // High priority
    }

    private var audioSessionId: Int = 0

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState = _engineState.asStateFlow()

    private val _currentParams = MutableStateFlow(DspParams())
    val currentParams = _currentParams.asStateFlow()

    private val _gainBudget = MutableStateFlow(GainBudget(0f, RiskLevel.SAFE))
    val gainBudget = _gainBudget.asStateFlow()

    private val _currentPresetName = MutableStateFlow("Default Tuning")
    val currentPresetName = _currentPresetName.asStateFlow()

    private val _currentRoute = MutableStateFlow(AudioRoute.UNKNOWN)
    val currentRoute = _currentRoute.asStateFlow()

    private val _currentSessionId = MutableStateFlow(0)
    val currentSessionId = _currentSessionId.asStateFlow()

    fun isAttached(): Boolean = _engineState.value == EngineState.ATTACHED || _engineState.value == EngineState.PROCESSING

    fun getCurrentSessionId(): Int = _currentSessionId.value

    fun updateRoute(route: AudioRoute) {
        _currentRoute.value = route
    }

    fun attachSession(sessionId: Int, force: Boolean = false) {
        if (sessionId == 0 || (!force && this.audioSessionId == sessionId)) return
        Log.d(TAG, "Attaching DSP Engine to Session: $sessionId")

        releaseSession()
        this.audioSessionId = sessionId
        _currentSessionId.value = sessionId

        try {
            equalizer =
                try {
                    Equalizer(EFFECT_PRIORITY, sessionId)
                } catch (e: Exception) {
                    null
                }
            bassBoost =
                try {
                    BassBoost(EFFECT_PRIORITY, sessionId)
                } catch (e: Exception) {
                    null
                }
            virtualizer =
                try {
                    Virtualizer(EFFECT_PRIORITY, sessionId)
                } catch (e: Exception) {
                    null
                }
            presetReverb =
                try {
                    PresetReverb(EFFECT_PRIORITY, sessionId)
                } catch (e: Exception) {
                    null
                }
            loudnessEnhancer =
                try {
                    LoudnessEnhancer(sessionId)
                } catch (e: Exception) {
                    null
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing =
                    try {
                        val builder =
                            DynamicsProcessing.Config.Builder(
                                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                                1, // Channels
                                true, 0, // PreEQ
                                true, 0, // MultiBandCompressor
                                true, 0, // PostEQ
                                true, // Limiter
                            )
                        DynamicsProcessing(EFFECT_PRIORITY, sessionId, builder.build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create DynamicsProcessing", e)
                        null
                    }
            }

            _engineState.value = EngineState.ATTACHED
            applyParams(_currentParams.value)
            Log.d(TAG, "✅ V4A DSP Engine successfully attached to session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DSP effects", e)
            releaseSession()
            _engineState.value = EngineState.ERROR
        }
    }

    fun releaseSession() {
        Log.d(TAG, "Releasing DSP Engine Session: $audioSessionId")
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

        audioSessionId = 0
        _currentSessionId.value = 0
        _engineState.value = EngineState.IDLE
    }

    fun updateParams(
        params: DspParams,
        presetName: String? = null,
    ) {
        // Run auto-correction if clipping risk is in DANGER zone
        val correctedParams = GainBudgetCalculator.autoCorrect(params)

        _currentParams.value = correctedParams
        presetName?.let { _currentPresetName.value = it }
        applyParams(correctedParams)
        _gainBudget.value = GainBudgetCalculator.calculate(correctedParams)
    }

    private fun applyParams(params: DspParams) {
        if (!isAttached()) return
        Log.d(TAG, "Applying DSP Params: enabled=${params.enabled}")

        try {
            val isEnabled = params.enabled

            val bassConfig = com.deepeye.musicpro.dsp.bass.BassProcessor.BassConfig(
                subBassGain = if (params.viperBassEnabled) params.viperBassGain else 0f,
                midBassGain = if (params.bassBoostEnabled) (params.bassBoostStrength / 1000f) * 12f else 0f,
                harmonicSaturation = if (params.viperBassMode == com.deepeye.musicpro.dsp.model.ViperBassMode.PURE) 0.5f else 0.2f,
                limiterEnabled = params.limiterEnabled,
                limiterThreshold = params.limiterThreshold
            )

            // ── Equalizer ──
            equalizer?.let { eq ->
                eq.enabled = isEnabled && params.eqEnabled
                if (eq.enabled) {
                    val numBands = eq.numberOfBands.toInt()
                    for (i in 0 until minOf(numBands, params.eqBands.size)) {
                        // Convert float dB (-12..+12) to milliBel
                        val millibels = (params.eqBands[i] * 100).toInt().toShort()
                        eq.setBandLevel(i.toShort(), millibels)
                    }
                }
            }

            // ── Bass Boost ──
            bassBoost?.let { bb ->
                bb.enabled = isEnabled && (params.bassBoostEnabled || params.viperBassEnabled)
                if (bb.enabled) {
                    val strength = bassProcessor.computeLegacyBassBoostStrength(bassConfig)
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

            // ── Gain Distribution ──
            var dynamicsPostGain = 0f
            var loudnessGainMb = 0

            val totalGainDb = params.pgcGain + params.masterGain + (if (params.loudnessEnabled) params.loudnessGain else 0f)
            
            if (totalGainDb > 0) {
                loudnessGainMb = (totalGainDb * 100).toInt()
            } else {
                // Apply negative headroom in DynamicsProcessing so it actually works
                dynamicsPostGain = totalGainDb
            }

            // ── Loudness / Master Gain ──
            loudnessEnhancer?.let { loud ->
                loud.enabled = isEnabled
                if (isEnabled) {
                    loud.setTargetGain(loudnessGainMb)
                }
            }

            // ── Dynamics Processing (API 28+) ──
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.let { dp ->
                    if (isEnabled && (params.viperBassEnabled || params.bassBoostEnabled)) {
                        bassProcessor.applyBassConfig(bassConfig, dp)
                        // Override the limiter postGain with our computed negative headroom
                        val limiter = DynamicsProcessing.Limiter(
                            true, true, 0, 1f, 50f, 10f, params.limiterThreshold, dynamicsPostGain
                        )
                        dp.setLimiterAllChannelsTo(limiter)
                    } else {
                        dp.enabled = isEnabled && params.dynamicsEnabled
                        if (dp.enabled || dynamicsPostGain < 0f) {
                            dp.enabled = true
                            val limiter =
                                DynamicsProcessing.Limiter(
                                    true, // inUse
                                    params.limiterEnabled || dynamicsPostGain < 0f, // enabled
                                    0, // linkGroup
                                    params.compressorAttack, // attackTime
                                    params.compressorRelease, // releaseTime
                                    10f, // ratio (placeholder)
                                    params.limiterThreshold, // threshold
                                    dynamicsPostGain, // postGain
                                )
                            dp.setLimiterAllChannelsTo(limiter)
                        }
                    }
                }
            }

            // ── Custom ExoPlayer Processors ──
            vocalRemoverProcessor.setEnabled(isEnabled && params.karaokeModeEnabled)
            crossfeedProcessor.setEnabled(isEnabled && params.crossfeedEnabled)

            _engineState.value = EngineState.PROCESSING
        } catch (e: Exception) {
            Log.e(TAG, "Error applying DSP params", e)
            _engineState.value = EngineState.ERROR
        }
    }
}
