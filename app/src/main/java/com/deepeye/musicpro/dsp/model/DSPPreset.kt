package com.deepeye.musicpro.dsp.model

import android.media.audiofx.PresetReverb

enum class DSPPreset {
    FLAT,
    BALANCED,
    PREMIUM_BASS,
    BASS_BOOSTER_MAX,
    VOCAL_CLARITY,
    TREBLE_BOOST,
    DEEP_HOUSE,
    HIFI_HEADPHONES,
    LOUDNESS_MAX,
    CUSTOM
}

data class DSPSettings(
    val name: String,
    val eqBands: IntArray,        // 10 bands in milliBel
    val bassStrength: Short,      // 0-1000
    val virtualizerStrength: Short, // 0-1000
    val loudnessGain: Float,      // dB
    val reverbPreset: Short,
    val description: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DSPSettings

        if (name != other.name) return false
        if (!eqBands.contentEquals(other.eqBands)) return false
        if (bassStrength != other.bassStrength) return false
        if (virtualizerStrength != other.virtualizerStrength) return false
        if (loudnessGain != other.loudnessGain) return false
        if (reverbPreset != other.reverbPreset) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + eqBands.contentHashCode()
        result = 31 * result + bassStrength
        result = 31 * result + virtualizerStrength
        result = 31 * result + loudnessGain.hashCode()
        result = 31 * result + reverbPreset
        result = 31 * result + description.hashCode()
        return result
    }
}

object DSPPresets {

    // ════════════════════════════════════════
    // FLAT — No DSP
    // ════════════════════════════════════════
    val FLAT = DSPSettings(
        name = "Flat",
        eqBands = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        bassStrength = 0,
        virtualizerStrength = 0,
        loudnessGain = 0.0f,
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Unprocessed original audio"
    )

    // ════════════════════════════════════════
    // BALANCED — Slight enhancement
    // ════════════════════════════════════════
    val BALANCED = DSPSettings(
        name = "Balanced",
        eqBands = intArrayOf(200, 100, 0, 0, 0, 0, 100, 200, 100, 0),
        bassStrength = 150,
        virtualizerStrength = 200,
        loudnessGain = 1.0f,
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Balanced enhancement for everyday listening"
    )

    // ════════════════════════════════════════
    // PREMIUM BASS — Punchy, deep, clean bass
    // Best for: EDM, Hip-hop, Bollywood
    // ════════════════════════════════════════
    val PREMIUM_BASS = DSPSettings(
        name = "Premium Bass",
        eqBands = intArrayOf(
            600,   // 60Hz   Sub-bass    +6dB  → felt bass
            500,   // 170Hz  Bass        +5dB  → punch
            200,   // 310Hz  Low-mid     +2dB  → warmth
            -100,  // 600Hz  Mid         -1dB  → clear
            -200,  // 1kHz   Upper-mid   -2dB  → not muddy
            0,     // 3kHz   Presence     0dB
            100,   // 6kHz   Brilliance  +1dB
            200,   // 12kHz  Air         +2dB
            300,   // 14kHz  Sparkle     +3dB
            200    // 16kHz  Shimmer     +2dB
        ),
        bassStrength = 750,
        virtualizerStrength = 500,
        loudnessGain = 3.0f,
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Deep punchy bass with crystal highs"
    )

    // ════════════════════════════════════════
    // BASS BOOSTER MAX — Maximum bass impact
    // Best for: Club music, workout
    // ════════════════════════════════════════
    val BASS_BOOSTER_MAX = DSPSettings(
        name = "Bass Boost MAX",
        eqBands = intArrayOf(
            900,   // 60Hz   Sub-bass    +9dB  → THUMP
            800,   // 170Hz  Bass        +8dB  → PUNCH
            400,   // 310Hz  Low-mid     +4dB  → BODY
            0,     // 600Hz  Mid          0dB
            -300,  // 1kHz   Upper-mid   -3dB  → not harsh
            -100,  // 3kHz   Presence    -1dB
            200,   // 6kHz   Brilliance  +2dB
            300,   // 12kHz  Air         +3dB
            400,   // 14kHz  Sparkle     +4dB
            300    // 16kHz  Shimmer     +3dB
        ),
        bassStrength = 1000,
        virtualizerStrength = 600,
        loudnessGain = 4.0f,
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Maximum bass for club and workout"
    )

    // ════════════════════════════════════════
    // HIFI HEADPHONES — Reference quality
    // Best for: All genres, audiophile
    // ════════════════════════════════════════
    val HIFI_HEADPHONES = DSPSettings(
        name = "HiFi Headphones",
        eqBands = intArrayOf(
            200,   // 60Hz   Sub-bass    +2dB  → balanced
            100,   // 170Hz  Bass        +1dB  → natural
            0,     // 310Hz  Low-mid      0dB  → flat
            -100,  // 600Hz  Mid         -1dB  → reference
            0,     // 1kHz   Upper-mid    0dB
            100,   // 3kHz   Presence    +1dB  → detail
            200,   // 6kHz   Brilliance  +2dB  → airiness
            300,   // 12kHz  Air         +3dB  → sparkle
            200,   // 14kHz  Top          +2dB
            100    // 16kHz  Extension   +1dB
        ),
        bassStrength = 200,
        virtualizerStrength = 700,  // 3D width
        loudnessGain = 2.0f,
        reverbPreset = PresetReverb.PRESET_SMALLROOM,
        description = "Reference HiFi for audiophiles"
    )

    // ════════════════════════════════════════
    // DEEP HOUSE — Warm sub bass + wide stage
    // Best for: Deep house, lo-fi, chill
    // ════════════════════════════════════════
    val DEEP_HOUSE = DSPSettings(
        name = "Deep House",
        eqBands = intArrayOf(
            700,   // 60Hz   Sub-bass    +7dB  → deep rumble
            400,   // 170Hz  Bass        +4dB  → punch
            100,   // 310Hz  Low-mid     +1dB  → warmth
            -200,  // 600Hz  Mid         -2dB  → recessed
            -300,  // 1kHz   Upper-mid   -3dB  → laid back
            -100,  // 3kHz   Presence    -1dB
            200,   // 6kHz   Brilliance  +2dB
            400,   // 12kHz  Air         +4dB  → wide
            300,   // 14kHz  Shimmer     +3dB
            200    // 16kHz  Extension   +2dB
        ),
        bassStrength = 800,
        virtualizerStrength = 900,  // Very wide stage
        loudnessGain = 2.5f,
        reverbPreset = PresetReverb.PRESET_LARGEHALL,
        description = "Deep sub bass with wide 3D stage"
    )

    // ════════════════════════════════════════
    // VOCAL CLARITY — Voice forward
    // Best for: Bollywood, pop vocals
    // ════════════════════════════════════════
    val VOCAL_CLARITY = DSPSettings(
        name = "Vocal Clarity",
        eqBands = intArrayOf(
            -200,  // 60Hz   Sub-bass    -2dB  → reduce mud
            -100,  // 170Hz  Bass        -1dB
            0,     // 310Hz  Low-mid      0dB
            200,   // 600Hz  Mid         +2dB  → body
            400,   // 1kHz   Presence    +4dB  → vocal
            500,   // 3kHz   Upper-mid   +5dB  → clarity
            300,   // 6kHz   Brilliance  +3dB  → crisp
            200,   // 12kHz  Air         +2dB
            100,   // 14kHz  Shimmer     +1dB
            0      // 16kHz  Extension    0dB
        ),
        bassStrength = 150,
        virtualizerStrength = 400,
        loudnessGain = 3.5f,
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Crystal clear vocals and presence"
    )

    // ════════════════════════════════════════
    // TREBLE BOOST — Crisp highs
    // ════════════════════════════════════════
    val TREBLE_BOOST = DSPSettings(
        name = "Treble Boost",
        eqBands = intArrayOf(
            0, 0, 0, 0, 100, 200, 300, 400, 500, 400
        ),
        bassStrength = 0,
        virtualizerStrength = 300,
        loudnessGain = 2.0f,
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Boosted highs for detailed treble"
    )

    // ════════════════════════════════════════
    // LOUDNESS MAX — Maximum perceived volume
    // Best for: Speaker / outdoor
    // ════════════════════════════════════════
    val LOUDNESS_MAX = DSPSettings(
        name = "Loudness MAX",
        eqBands = intArrayOf(
            500,   // 60Hz   +5dB
            400,   // 170Hz  +4dB
            200,   // 310Hz  +2dB
            100,   // 600Hz  +1dB
            200,   // 1kHz   +2dB
            300,   // 3kHz   +3dB
            400,   // 6kHz   +4dB
            500,   // 12kHz  +5dB
            400,   // 14kHz  +4dB
            300    // 16kHz  +3dB
        ),
        bassStrength = 600,
        virtualizerStrength = 800,
        loudnessGain = 6.0f,  // max safe gain
        reverbPreset = PresetReverb.PRESET_NONE,
        description = "Maximum loudness for speakers"
    )

    fun fromPreset(preset: DSPPreset): DSPSettings {
        return when (preset) {
            DSPPreset.PREMIUM_BASS -> PREMIUM_BASS
            DSPPreset.BASS_BOOSTER_MAX -> BASS_BOOSTER_MAX
            DSPPreset.HIFI_HEADPHONES -> HIFI_HEADPHONES
            DSPPreset.DEEP_HOUSE -> DEEP_HOUSE
            DSPPreset.VOCAL_CLARITY -> VOCAL_CLARITY
            DSPPreset.LOUDNESS_MAX -> LOUDNESS_MAX
            DSPPreset.FLAT -> FLAT
            DSPPreset.BALANCED -> BALANCED
            DSPPreset.TREBLE_BOOST -> TREBLE_BOOST
            DSPPreset.CUSTOM -> FLAT // Fallback for custom, handled separately by viewmodel
        }
    }
}
