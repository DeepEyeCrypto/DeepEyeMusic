// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.model

enum class ViperBassMode { NATURAL, PURE, DYNAMIC }
enum class ViperClarityMode { NATURAL, OZONE_PLUS, FEELING }
enum class TubeMode { TRIODE, PENTODE }
enum class DynamicMode { V1, V2, SPEAKER, SUBWOOFER }
enum class AudioRoute { WIRED_HEADSET, BLUETOOTH_A2DP, SPEAKER, USB_AUDIO, UNKNOWN }

/**
 * Complete DSP parameter state for the V4A engine.
 * All 14 modules are represented as fields.
 */
data class DspParams(
    // ── Master ──
    val enabled: Boolean = false,
    val masterGain: Float = 0f,          // dB, range: -12..+12

    // ── 1. Pre-Gain Control (PGC) ──
    val pgcEnabled: Boolean = true,
    val pgcGain: Float = -6f,             // dB default headroom

    // ── 2. 10-Band Equalizer ──
    val eqEnabled: Boolean = false,
    val eqBands: FloatArray = FloatArray(10) { 0f },  // dB per band, range: -12..+12

    // ── 3. Bass Boost ──
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,      // 0–1000

    // ── 4. Virtualizer ──
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,    // 0–1000

    // ── 5. Reverb ──
    val reverbEnabled: Boolean = false,
    val reverbPreset: ReverbPreset = ReverbPreset.NONE,
    val reverbRoomLevel: Int = -1000,    // millibels
    val reverbDecayTime: Int = 1500,     // ms
    val reverbDiffusion: Int = 1000,     // per mille
    val reverbRoomSize: Float = 0f,
    val reverbDensity: Float = 0f,
    val reverbWetMix: Float = 0f,

    // ── 6. Loudness Enhancer ──
    val loudnessEnabled: Boolean = false,
    val loudnessGain: Float = 0f,        // dB, range: 0..+15
    val loudnessTargetGainMb: Int = 0,   // millibels

    // ── 7. Dynamics Processing ──
    val dynamicsEnabled: Boolean = false,
    val compressorThreshold: Float = -24f,  // dB
    val compressorRatio: Float = 4f,        // ratio
    val compressorAttack: Float = 3f,       // ms
    val compressorRelease: Float = 250f,    // ms
    val limiterEnabled: Boolean = true,
    val limiterThreshold: Float = -1f,      // dB
    val dynamicSystemEnabled: Boolean = false,
    val dynamicSystemMode: DynamicMode = DynamicMode.V1,
    val dynamicSystemStrength: Int = 0,

    // ── 8. Field Surround ──
    val surroundEnabled: Boolean = false,
    val surroundStrength: Int = 500,     // 0–1000
    val surroundMode: SurroundMode = SurroundMode.WIDE,
    val fieldSurroundEnabled: Boolean = false,
    val fieldSurroundStrength: Int = 0,
    val fieldMidImageStrength: Int = 0,

    // ── 9. Convolver (IRS) ──
    val convolverEnabled: Boolean = false,
    val convolverIrsPath: String = "",   // path to impulse response file
    val convolverMix: Float = 1.0f,      // wet/dry, 0..1

    // ── 10. Tube Simulator ──
    val tubeEnabled: Boolean = false,
    val tubeDrive: Int = 0,
    val tubeMode: TubeMode = TubeMode.TRIODE,

    // ── 11. Clarity / Exciter ──
    val clarityEnabled: Boolean = false,
    val clarityStrength: Float = 0.5f,   // 0..1
    val viperClarityEnabled: Boolean = false,
    val viperClarityMode: ViperClarityMode = ViperClarityMode.NATURAL,
    val viperClarityGain: Float = 0f,

    // ── 12. HRTF (Head Related Transfer Function) ──
    val hrtfEnabled: Boolean = false,
    val hrtfPreset: HrtfPreset = HrtfPreset.DEFAULT,

    // ── 13. Speaker Protection ──
    val speakerProtectionEnabled: Boolean = true,
    val speakerMaxDb: Float = 0f,        // max output dB

    // ── 14. Noise Gate ──
    val noiseGateEnabled: Boolean = false,
    val noiseGateThreshold: Float = -60f, // dB
    val noiseGateAttack: Float = 1f,      // ms
    val noiseGateRelease: Float = 100f,    // ms

    // ── Viper Specifics ──
    val viperBassEnabled: Boolean = false,
    val viperBassMode: ViperBassMode = ViperBassMode.NATURAL,
    val viperBassFreq: Int = 60,
    val viperBassGain: Float = 0f,
    
    val auditoryProtectionEnabled: Boolean = false,
    val auditoryBinauralLevel: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DspParams

        if (enabled != other.enabled) return false
        if (masterGain != other.masterGain) return false
        if (pgcEnabled != other.pgcEnabled) return false
        if (pgcGain != other.pgcGain) return false
        if (eqEnabled != other.eqEnabled) return false
        if (!eqBands.contentEquals(other.eqBands)) return false
        if (bassBoostEnabled != other.bassBoostEnabled) return false
        if (bassBoostStrength != other.bassBoostStrength) return false
        if (virtualizerEnabled != other.virtualizerEnabled) return false
        if (virtualizerStrength != other.virtualizerStrength) return false
        if (reverbEnabled != other.reverbEnabled) return false
        if (reverbPreset != other.reverbPreset) return false
        if (reverbRoomLevel != other.reverbRoomLevel) return false
        if (reverbDecayTime != other.reverbDecayTime) return false
        if (reverbDiffusion != other.reverbDiffusion) return false
        if (reverbRoomSize != other.reverbRoomSize) return false
        if (reverbDensity != other.reverbDensity) return false
        if (reverbWetMix != other.reverbWetMix) return false
        if (loudnessEnabled != other.loudnessEnabled) return false
        if (loudnessGain != other.loudnessGain) return false
        if (loudnessTargetGainMb != other.loudnessTargetGainMb) return false
        if (dynamicsEnabled != other.dynamicsEnabled) return false
        if (compressorThreshold != other.compressorThreshold) return false
        if (compressorRatio != other.compressorRatio) return false
        if (compressorAttack != other.compressorAttack) return false
        if (compressorRelease != other.compressorRelease) return false
        if (limiterEnabled != other.limiterEnabled) return false
        if (limiterThreshold != other.limiterThreshold) return false
        if (dynamicSystemEnabled != other.dynamicSystemEnabled) return false
        if (dynamicSystemMode != other.dynamicSystemMode) return false
        if (dynamicSystemStrength != other.dynamicSystemStrength) return false
        if (surroundEnabled != other.surroundEnabled) return false
        if (surroundStrength != other.surroundStrength) return false
        if (surroundMode != other.surroundMode) return false
        if (fieldSurroundEnabled != other.fieldSurroundEnabled) return false
        if (fieldSurroundStrength != other.fieldSurroundStrength) return false
        if (fieldMidImageStrength != other.fieldMidImageStrength) return false
        if (convolverEnabled != other.convolverEnabled) return false
        if (convolverIrsPath != other.convolverIrsPath) return false
        if (convolverMix != other.convolverMix) return false
        if (tubeEnabled != other.tubeEnabled) return false
        if (tubeDrive != other.tubeDrive) return false
        if (tubeMode != other.tubeMode) return false
        if (clarityEnabled != other.clarityEnabled) return false
        if (clarityStrength != other.clarityStrength) return false
        if (viperClarityEnabled != other.viperClarityEnabled) return false
        if (viperClarityMode != other.viperClarityMode) return false
        if (viperClarityGain != other.viperClarityGain) return false
        if (hrtfEnabled != other.hrtfEnabled) return false
        if (hrtfPreset != other.hrtfPreset) return false
        if (speakerProtectionEnabled != other.speakerProtectionEnabled) return false
        if (speakerMaxDb != other.speakerMaxDb) return false
        if (noiseGateEnabled != other.noiseGateEnabled) return false
        if (noiseGateThreshold != other.noiseGateThreshold) return false
        if (noiseGateAttack != other.noiseGateAttack) return false
        if (noiseGateRelease != other.noiseGateRelease) return false
        if (viperBassEnabled != other.viperBassEnabled) return false
        if (viperBassMode != other.viperBassMode) return false
        if (viperBassFreq != other.viperBassFreq) return false
        if (viperBassGain != other.viperBassGain) return false
        if (auditoryProtectionEnabled != other.auditoryProtectionEnabled) return false
        if (auditoryBinauralLevel != other.auditoryBinauralLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + masterGain.hashCode()
        result = 31 * result + pgcEnabled.hashCode()
        result = 31 * result + pgcGain.hashCode()
        result = 31 * result + eqEnabled.hashCode()
        result = 31 * result + eqBands.contentHashCode()
        result = 31 * result + bassBoostEnabled.hashCode()
        result = 31 * result + bassBoostStrength
        result = 31 * result + virtualizerEnabled.hashCode()
        result = 31 * result + virtualizerStrength
        result = 31 * result + reverbEnabled.hashCode()
        result = 31 * result + reverbPreset.hashCode()
        result = 31 * result + reverbRoomLevel
        result = 31 * result + reverbDecayTime
        result = 31 * result + reverbDiffusion
        result = 31 * result + reverbRoomSize.hashCode()
        result = 31 * result + reverbDensity.hashCode()
        result = 31 * result + reverbWetMix.hashCode()
        result = 31 * result + loudnessEnabled.hashCode()
        result = 31 * result + loudnessGain.hashCode()
        result = 31 * result + loudnessTargetGainMb
        result = 31 * result + dynamicsEnabled.hashCode()
        result = 31 * result + compressorThreshold.hashCode()
        result = 31 * result + compressorRatio.hashCode()
        result = 31 * result + compressorAttack.hashCode()
        result = 31 * result + compressorRelease.hashCode()
        result = 31 * result + limiterEnabled.hashCode()
        result = 31 * result + limiterThreshold.hashCode()
        result = 31 * result + dynamicSystemEnabled.hashCode()
        result = 31 * result + dynamicSystemMode.hashCode()
        result = 31 * result + dynamicSystemStrength
        result = 31 * result + surroundEnabled.hashCode()
        result = 31 * result + surroundStrength
        result = 31 * result + surroundMode.hashCode()
        result = 31 * result + fieldSurroundEnabled.hashCode()
        result = 31 * result + fieldSurroundStrength
        result = 31 * result + fieldMidImageStrength
        result = 31 * result + convolverEnabled.hashCode()
        result = 31 * result + convolverIrsPath.hashCode()
        result = 31 * result + convolverMix.hashCode()
        result = 31 * result + tubeEnabled.hashCode()
        result = 31 * result + tubeDrive
        result = 31 * result + tubeMode.hashCode()
        result = 31 * result + clarityEnabled.hashCode()
        result = 31 * result + clarityStrength.hashCode()
        result = 31 * result + viperClarityEnabled.hashCode()
        result = 31 * result + viperClarityMode.hashCode()
        result = 31 * result + viperClarityGain.hashCode()
        result = 31 * result + hrtfEnabled.hashCode()
        result = 31 * result + hrtfPreset.hashCode()
        result = 31 * result + speakerProtectionEnabled.hashCode()
        result = 31 * result + speakerMaxDb.hashCode()
        result = 31 * result + noiseGateEnabled.hashCode()
        result = 31 * result + noiseGateThreshold.hashCode()
        result = 31 * result + noiseGateAttack.hashCode()
        result = 31 * result + noiseGateRelease.hashCode()
        result = 31 * result + viperBassEnabled.hashCode()
        result = 31 * result + viperBassMode.hashCode()
        result = 31 * result + viperBassFreq
        result = 31 * result + viperBassGain.hashCode()
        result = 31 * result + auditoryProtectionEnabled.hashCode()
        result = 31 * result + auditoryBinauralLevel
        return result
    }

    companion object {
        fun flat() = DspParams(enabled = true, pgcGain = 0f)

        fun premiumHeadphoneBass() = DspParams(
            enabled = true,
            pgcGain = -6f,
            eqEnabled = true,
            eqBands = floatArrayOf(
                4.5f, 5.5f, 4.0f, 2.0f, -1.0f,
                0.0f, -1.5f, 1.5f, 2.5f, 3.0f
            ),
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 60,
            viperBassGain = 10.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.OZONE_PLUS,
            viperClarityGain = 8.5f,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 18,
            bassBoostEnabled = true,
            bassBoostStrength = 600,
            loudnessEnabled = true,
            loudnessTargetGainMb = 800,
            virtualizerEnabled = true,
            virtualizerStrength = 500,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 6,
            fieldMidImageStrength = 8,
            limiterEnabled = true,
            auditoryProtectionEnabled = true,
            auditoryBinauralLevel = 2
        )

        fun bollywoodVocals() = DspParams(
            enabled = true,
            pgcGain = -6f,
            eqEnabled = true,
            eqBands = floatArrayOf(
                3.0f, 4.0f, 3.5f, 2.0f, 1.0f,
                2.5f, 1.5f, 1.0f, 2.0f, 1.5f
            ),
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 80,
            viperBassGain = 8.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.NATURAL,
            viperClarityGain = 10.0f,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 12,
            reverbEnabled = true,
            reverbRoomSize = 0.25f,
            reverbDensity = 0.3f,
            reverbWetMix = 0.18f,
            limiterEnabled = true
        )

        fun nightMode() = DspParams(
            enabled = true,
            pgcGain = -4f,
            eqEnabled = true,
            eqBands = floatArrayOf(
                2.0f, 3.0f, 3.0f, 1.5f, 0.0f,
                0.5f, 0.0f, 0.5f, 1.0f, 1.5f
            ),
            loudnessEnabled = true,
            loudnessTargetGainMb = 1500,
            bassBoostEnabled = true,
            bassBoostStrength = 400,
            virtualizerEnabled = true,
            virtualizerStrength = 300,
            limiterEnabled = true,
            auditoryProtectionEnabled = true
        )

        fun bassMonster() = DspParams(
            enabled = true,
            pgcGain = -8f, // extra headroom for max bass
            eqEnabled = true,
            eqBands = floatArrayOf(
                6.0f, 7.0f, 5.5f, 3.0f, -2.0f,
                -1.5f, -1.0f, 2.0f, 3.0f, 3.5f
            ),
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.DYNAMIC,
            viperBassFreq = 40,
            viperBassGain = 14.0f,
            bassBoostEnabled = true,
            bassBoostStrength = 800,
            dynamicSystemEnabled = true,
            dynamicSystemMode = DynamicMode.SUBWOOFER,
            dynamicSystemStrength = 70,
            loudnessEnabled = true,
            loudnessTargetGainMb = 600,
            limiterEnabled = true,
            pgcEnabled = true,
            auditoryProtectionEnabled = true
        )

        fun speakerSafe() = DspParams(
            enabled = true,
            pgcGain = -2f,
            eqEnabled = true,
            eqBands = floatArrayOf(
                -2.0f, 0.0f, 1.5f, 2.0f, 1.5f,
                1.0f, 0.5f, 1.0f, 2.0f, 1.5f
            ),
            loudnessEnabled = true,
            loudnessTargetGainMb = 400,
            limiterEnabled = true
        )

        fun bluetoothOptimized() = DspParams(
            enabled = true,
            pgcGain = -5f,
            eqEnabled = true,
            eqBands = floatArrayOf(
                3.0f, 4.5f, 4.0f, 2.5f, 0.0f,
                0.5f, 0.0f, 1.5f, 2.5f, 2.0f
            ),
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 80,
            viperBassGain = 9.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.NATURAL,
            viperClarityGain = 7.0f,
            bassBoostEnabled = true,
            bassBoostStrength = 500,
            loudnessEnabled = true,
            loudnessTargetGainMb = 600,
            limiterEnabled = true
        )
    }
}

/**
 * Gain budget analysis result.
 */
data class GainBudget(
    val totalDb: Float,             // total accumulated gain in dB
    val risk: RiskLevel
)

/**
 * Engine operational state.
 */
enum class EngineState {
    IDLE,           // No audio session attached
    ATTACHED,       // Audio session attached, DSP active
    PROCESSING,     // Actively processing audio
    ERROR,          // Error state (e.g., AudioEffect creation failed)
    RELEASED        // Engine released, needs re-initialization
}

/**
 * Risk level for the gain budget meter.
 */
enum class RiskLevel {
    SAFE,       // Headroom > 6dB
    MODERATE,   // Headroom 0–6dB
    DANGER      // Headroom < 0dB (clipping risk)
}

/**
 * Reverb presets matching Android's PresetReverb.
 */
enum class ReverbPreset {
    NONE,
    SMALL_ROOM,
    MEDIUM_ROOM,
    LARGE_ROOM,
    MEDIUM_HALL,
    LARGE_HALL,
    PLATE
}

/**
 * Surround mode variants.
 */
enum class SurroundMode {
    WIDE,       // Widened stereo field
    CINEMA,     // Simulated surround
    MUSIC,      // Optimized for music
    GAMING      // Optimized for spatial audio
}

/**
 * HRTF (Head Related Transfer Function) presets.
 */
enum class HrtfPreset {
    DEFAULT,
    WIDE,
    NARROW,
    FORWARD,
    SURROUND
}
