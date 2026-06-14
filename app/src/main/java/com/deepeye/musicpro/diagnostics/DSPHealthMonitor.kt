// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics

import android.util.Log
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.model.EngineState
import com.deepeye.musicpro.dsp.profile.AudioRouteDetector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DSPHealthMonitor @Inject constructor(
    private val dspEngine: DSPEngine,
    private val audioRouteDetector: AudioRouteDetector
) {
    companion object {
        private const val TAG = "DSPHealthMonitor"
    }

    data class HealthStatus(
        val enabled: Boolean,
        val active: Boolean,
        val sessionId: Int,
        val engineState: EngineState,
        val outputDevice: String,
        val routeType: String,
        val effectsLoaded: List<String>
    )

    fun getHealthStatus(): HealthStatus {
        val params = dspEngine.currentParams.value
        val sessionId = dspEngine.getCurrentSessionId()
        val state = dspEngine.engineState.value
        val device = audioRouteDetector.currentDevice.value

        val loadedEffects = mutableListOf<String>()
        if (params.eqEnabled) loadedEffects.add("Equalizer")
        if (params.bassBoostEnabled || params.viperBassEnabled) loadedEffects.add("BassBoost")
        if (params.virtualizerEnabled) loadedEffects.add("Virtualizer")
        if (params.reverbEnabled) loadedEffects.add("PresetReverb")
        if (params.loudnessEnabled) loadedEffects.add("LoudnessEnhancer")
        if (params.dynamicsEnabled) loadedEffects.add("DynamicsProcessing")

        return HealthStatus(
            enabled = params.enabled,
            active = dspEngine.isAttached(),
            sessionId = sessionId,
            engineState = state,
            outputDevice = device.name,
            routeType = device.type.name,
            effectsLoaded = loadedEffects
        )
    }

    /**
     * Generates a Root Cause Analysis (RCA) report if the DSP engine is inactive.
     */
    fun generateRootCauseAnalysis(): String {
        val health = getHealthStatus()
        val sb = StringBuilder()
        sb.append("# DSP Root Cause Analysis (RCA) Report\n\n")

        if (health.enabled && health.active) {
            sb.append("✅ DSP is fully healthy and active.\n")
            sb.append("- Session ID: ${health.sessionId}\n")
            sb.append("- Device: ${health.outputDevice} (${health.routeType})\n")
            return sb.toString()
        }

        sb.append("🚨 DSP INACTIVE DETECTED!\n\n")
        sb.append("## System Diagnostics\n")
        sb.append("- DSP Enabled in Settings: ${health.enabled}\n")
        sb.append("- DSP Active in Engine: ${health.active}\n")
        sb.append("- Engine State: ${health.engineState}\n")
        sb.append("- Session ID: ${health.sessionId}\n")
        sb.append("- Output Device: ${health.outputDevice} (${health.routeType})\n")
        sb.append("- Configured Effects: ${health.effectsLoaded.joinToString(", ").ifEmpty { "None" }}\n\n")

        sb.append("## Root Cause & Action Items\n")
        when {
            !health.enabled -> {
                sb.append("👉 **Root Cause**: DSP is disabled in the app settings.\n")
                sb.append("🔧 **Fix**: Enable the DSP controller switch in the Equalizer settings panel.\n")
            }
            health.sessionId == 0 -> {
                sb.append("👉 **Root Cause**: No active audio session ID. Player is likely idle, paused, or stopped.\n")
                sb.append("🔧 **Fix**: Play a music/video track to initialize the ExoPlayer audio session.\n")
            }
            health.engineState == EngineState.ERROR -> {
                sb.append("👉 **Root Cause**: Android AudioEffect framework failed to load effect classes on session ${health.sessionId}.\n")
                sb.append("🔧 **Fix**: Re-prepare/restart playback to let AudioSessionGuardian recreate the effects. Check device logs for effect engine limits.\n")
            }
            else -> {
                sb.append("👉 **Root Cause**: Media session active but DSP engine is in IDLE/attached state without active processing.\n")
                sb.append("🔧 **Fix**: Auto-verify if the effects need to be re-applied or route changed.\n")
            }
        }

        val report = sb.toString()
        Log.e(TAG, report)
        return report
    }
}
