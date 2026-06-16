// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.bass

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log
import com.deepeye.musicpro.dsp.profile.AudioDeviceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium bass processor with independent sub-bass and mid-bass control,
 * harmonic saturation, and a safety limiter.
 *
 * Uses Android's DynamicsProcessing multi-band EQ for precise frequency targeting:
 * - Sub-bass band: 20–60Hz (deep rumble, felt more than heard)
 * - Mid-bass band: 60–250Hz (punch, body, warmth)
 *
 * Harmonic saturation adds even-order harmonics (2nd, 4th) to make bass
 * feel warmer and fuller without odd-harmonic distortion.
 */
@Singleton
class BassProcessor @Inject constructor() {

    companion object {
        private const val TAG = "BassProcessor"
        private const val SUB_BASS_LOW_HZ = 20f
        private const val SUB_BASS_HIGH_HZ = 60f
        private const val MID_BASS_LOW_HZ = 60f
        private const val MID_BASS_HIGH_HZ = 250f
    }

    /**
     * Bass configuration with independent sub-bass and mid-bass controls.
     */
    data class BassConfig(
        val subBassGain: Float = 0f,          // 20–60Hz, range -12..+12 dB
        val subBassCutoff: Float = 60f,       // Configurable cutoff frequency (e.g. 30Hz - 120Hz)
        val midBassGain: Float = 0f,          // 60–250Hz, range -12..+12 dB
        val harmonicSaturation: Float = 0f,   // 0.0..1.0 intensity
        val limiterEnabled: Boolean = true,
        val limiterThreshold: Float = -0.5f,  // dBFS, prevents clipping
        val preset: BassPreset = BassPreset.CUSTOM,
    ) {
        /** Validates that all values are within acceptable ranges. */
        val isValid: Boolean
            get() = subBassGain in -12f..12f &&
                midBassGain in -12f..12f &&
                harmonicSaturation in 0f..1f &&
                limiterThreshold in -6f..0f
    }

    enum class BassPreset {
        HEADPHONE,
        SPEAKER,
        CAR,
        CUSTOM,
    }

    /**
     * Returns the default bass configuration for a given audio output type.
     */
    fun getDefaultPreset(deviceType: AudioDeviceType): BassConfig {
        return when (deviceType) {
            AudioDeviceType.WIRED_HEADSET, AudioDeviceType.BLUETOOTH -> PRESET_HEADPHONE
            AudioDeviceType.SPEAKER -> PRESET_SPEAKER
            AudioDeviceType.CAR -> PRESET_CAR
            AudioDeviceType.USB -> PRESET_HEADPHONE // USB DACs typically used with headphones
            AudioDeviceType.OTHER -> BassConfig()
        }
    }

    /**
     * Applies bass configuration to a DynamicsProcessing instance.
     * Requires API 28+ (Android P).
     */
    fun applyBassConfig(config: BassConfig, dp: DynamicsProcessing?) {
        if (dp == null) {
            Log.w(TAG, "DynamicsProcessing is null, cannot apply bass config")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "DynamicsProcessing requires API 28+")
            return
        }

        try {
            // Apply limiter to prevent clipping
            if (config.limiterEnabled) {
                val limiter = DynamicsProcessing.Limiter(
                    true,                    // inUse
                    true,                    // enabled
                    0,                       // linkGroup
                    1f,                      // attackTime ms
                    50f,                     // releaseTime ms
                    10f,                     // ratio
                    config.limiterThreshold, // threshold dBFS
                    0f,                      // postGain
                )
                dp.setLimiterAllChannelsTo(limiter)
            }

            // Apply Hardware Sub-Bass and Mid-Bass using PreEQ
            val preEq = dp.getPreEqByChannelIndex(0)
            if (preEq != null && preEq.bandCount >= 2) {
                val subBassBand = preEq.getBand(0)
                subBassBand.gain = config.subBassGain
                subBassBand.cutoffFrequency = config.subBassCutoff
                preEq.setBand(0, subBassBand)

                val midBassBand = preEq.getBand(1)
                midBassBand.gain = config.midBassGain
                midBassBand.cutoffFrequency = config.subBassCutoff + 150f // Keep mid-bass relatively above sub
                preEq.setBand(1, midBassBand)

                dp.setPreEqAllChannelsTo(preEq)
            }

            dp.enabled = true
            Log.d(TAG, "Applied bass config: sub=${config.subBassGain}dB, mid=${config.midBassGain}dB, sat=${config.harmonicSaturation}, limiter=${config.limiterEnabled}")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying bass config", e)
        }
    }

    /**
     * Computes the effective bass boost strength (0–1000) for the legacy BassBoost effect,
     * combining sub-bass and mid-bass gains into a single value.
     */
    fun computeLegacyBassBoostStrength(config: BassConfig): Int {
        val combinedGain = (config.subBassGain + config.midBassGain) / 2f
        // Map -12..+12 dB range to 0..1000
        val normalized = ((combinedGain + 12f) / 24f).coerceIn(0f, 1f)
        return (normalized * 1000).toInt()
    }

    /**
     * Applies harmonic saturation to simulate warm, tube-like bass.
     * Even-harmonic only (2nd, 4th) — avoids harsh odd-harmonic distortion.
     *
     * This is a soft-clipping function: tanh(x * drive) where drive is
     * proportional to saturation intensity.
     *
     * Note: This is a mathematical model. Actual audio processing happens
     * in the DSP chain via the tube simulator module or custom processing.
     */
    fun computeSaturationDrive(intensity: Float): Float {
        // Map 0..1 intensity to 1.0..4.0 drive factor
        return 1f + (intensity.coerceIn(0f, 1f) * 3f)
    }

    /**
     * Validates that the limiter threshold prevents output from exceeding 0dBFS.
     * Returns true if the configuration is safe from clipping.
     */
    fun isClipSafe(config: BassConfig): Boolean {
        if (!config.limiterEnabled) {
            // Without limiter, check if total gain could clip
            val maxGain = maxOf(config.subBassGain, config.midBassGain)
            return maxGain <= 0f
        }
        // With limiter enabled and threshold <= 0dBFS, output is safe
        return config.limiterThreshold <= 0f
    }

    // ═══════════════════════════════════════════════════
    // Default Presets
    // ═══════════════════════════════════════════════════

    /** Headphone preset: balanced sub-bass boost with controlled mid-bass. */
    private val PRESET_HEADPHONE = BassConfig(
        subBassGain = 4f,
        midBassGain = 2f,
        harmonicSaturation = 0.2f,
        limiterEnabled = true,
        limiterThreshold = -0.5f,
        preset = BassPreset.HEADPHONE,
    )

    /** Speaker preset: stronger mid-bass for small drivers, less sub-bass (can't reproduce). */
    private val PRESET_SPEAKER = BassConfig(
        subBassGain = 2f,
        midBassGain = 5f,
        harmonicSaturation = 0.3f,
        limiterEnabled = true,
        limiterThreshold = -1f,
        preset = BassPreset.SPEAKER,
    )

    /** Car preset: heavy sub-bass for subwoofers, moderate mid-bass. */
    private val PRESET_CAR = BassConfig(
        subBassGain = 8f,
        midBassGain = 4f,
        harmonicSaturation = 0.15f,
        limiterEnabled = true,
        limiterThreshold = -0.3f,
        preset = BassPreset.CAR,
    )
}
