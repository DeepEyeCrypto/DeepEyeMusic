package com.deepeye.musicpro.dsp.model

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
    val pgcGain: Float = 0f,             // dB, range: -12..+12

    // ── 2. 10-Band Equalizer ──
    val eqEnabled: Boolean = false,
    val eqBands: List<Float> = List(10) { 0f },  // dB per band, range: -12..+12
    // Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz

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

    // ── 6. Loudness Enhancer ──
    val loudnessEnabled: Boolean = false,
    val loudnessGain: Float = 0f,        // dB, range: 0..+15

    // ── 7. Dynamics Processing ──
    val dynamicsEnabled: Boolean = false,
    val compressorThreshold: Float = -24f,  // dB
    val compressorRatio: Float = 4f,        // ratio
    val compressorAttack: Float = 3f,       // ms
    val compressorRelease: Float = 250f,    // ms
    val limiterEnabled: Boolean = true,
    val limiterThreshold: Float = -1f,      // dB

    // ── 8. Field Surround ──
    val surroundEnabled: Boolean = false,
    val surroundStrength: Int = 500,     // 0–1000
    val surroundMode: SurroundMode = SurroundMode.WIDE,

    // ── 9. Convolver (IRS) ──
    val convolverEnabled: Boolean = false,
    val convolverIrsPath: String = "",   // path to impulse response file
    val convolverMix: Float = 1.0f,      // wet/dry, 0..1

    // ── 10. Tube Simulator ──
    val tubeEnabled: Boolean = false,
    val tubeDrive: Float = 0.3f,         // 0..1

    // ── 11. Clarity / Exciter ──
    val clarityEnabled: Boolean = false,
    val clarityStrength: Float = 0.5f,   // 0..1

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
    val noiseGateRelease: Float = 100f    // ms
)

/**
 * Gain budget analysis result.
 */
data class GainBudget(
    val totalGain: Float,           // total accumulated gain in dB
    val headroom: Float,            // available headroom before clipping (dB)
    val riskLevel: RiskLevel,
    val breakdown: Map<String, Float>  // module name → gain contribution
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
