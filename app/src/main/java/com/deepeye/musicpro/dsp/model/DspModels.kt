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
