// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.model

/**
 * Authentic ViPER4Android-calibrated Master Audio Presets.
 * Engineered for zero digital clipping (-0.5 dBFS safety ceiling) with
 * precise 64-bit float multi-stage psychoacoustic processing.
 *
 * 5 Dedicated Sub-Bass & Deep Bass Presets + 5 Studio, Audiophile & Spatial Presets.
 */
enum class DSPPreset(
    val presetName: String,
    val description: String,
    val params: DspParams,
    val requiredRank: Int = 0
) {
    // ══════════════════════════════════════════════════════════════
    // ── 5 PREMIUM DEEP BASS PRESETS ───────────────────────────────
    // ══════════════════════════════════════════════════════════════

    SUBWOOFER_30HZ_INFRA(
        presetName = "Subwoofer 30Hz Infra",
        description = "Sub-audible deep vibration & tactile 30Hz-45Hz low-end rumble",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -3.5f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.DYNAMIC,
            viperBassFreq = 40,
            viperBassGain = 10.0f,
            dynamicSystemEnabled = true,
            dynamicSystemMode = DynamicMode.SUBWOOFER,
            dynamicSystemStrength = 60,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.X_HIFI,
            viperClarityGain = 4.5f,
            eqEnabled = true,
            eqBands = floatArrayOf(6.0f, 4.5f, 2.0f, 0f, 0f, 0f, 0.5f, 1.0f, 1.5f, 2.0f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    EARTHQUAKE_BASS_MONSTER(
        presetName = "Earthquake Bass Monster",
        description = "Heavyweight club sub-bass punch & valve tube saturation for EDM & Trap",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -4.0f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.PURE,
            viperBassFreq = 50,
            viperBassGain = 12.0f,
            bassBoostEnabled = true,
            bassBoostStrength = 450,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 15,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.NATURAL,
            viperClarityGain = 5.0f,
            eqEnabled = true,
            eqBands = floatArrayOf(7.0f, 5.0f, 3.0f, 1.0f, 0f, 0f, 0.5f, 1.5f, 2.5f, 3.0f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    PUNCHY_808_SLAP(
        presetName = "Punchy 808 & Hip-Hop Kick",
        description = "Fast transient attack with tight 60Hz-80Hz kick drum impact and clean mids",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -2.5f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 60,
            viperBassGain = 8.5f,
            dynamicsEnabled = true,
            compressorThreshold = -18f,
            compressorRatio = 3.5f,
            compressorAttack = 5f,
            compressorRelease = 120f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.OZONE_PLUS,
            viperClarityGain = 6.0f,
            eqEnabled = true,
            eqBands = floatArrayOf(3.0f, 6.0f, 4.0f, 0f, -1.0f, 0f, 1.0f, 2.0f, 2.5f, 2.0f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    VELVET_WARM_LOFI_BASS(
        presetName = "Velvet Warm Lo-Fi Bass",
        description = "Smooth, thick analog tube-saturated low-end with rolled-off highs for Soul & R&B",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -2.0f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 75,
            viperBassGain = 6.5f,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 35,
            dynamicSystemEnabled = true,
            dynamicSystemMode = DynamicMode.V1,
            dynamicSystemStrength = 40,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.NATURAL,
            viperClarityGain = 2.0f,
            eqEnabled = true,
            eqBands = floatArrayOf(4.0f, 5.0f, 3.5f, 2.0f, 1.0f, 0f, 0f, -0.5f, -1.5f, -2.5f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    SUB_HARMONIC_EXCITER(
        presetName = "Sub-Harmonic Synthesizer",
        description = "Psychoacoustic harmonic synthesis generating deep 20Hz fundamental sub-bass response",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -3.0f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.DYNAMIC,
            viperBassFreq = 55,
            viperBassGain = 9.0f,
            dynamicSystemEnabled = true,
            dynamicSystemMode = DynamicMode.V2,
            dynamicSystemStrength = 75,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 3,
            fieldMidImageStrength = 4,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.X_HIFI,
            viperClarityGain = 5.5f,
            eqEnabled = true,
            eqBands = floatArrayOf(5.0f, 4.0f, 2.5f, 1.0f, 0f, 0f, 1.0f, 1.5f, 2.0f, 2.5f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    // ══════════════════════════════════════════════════════════════
    // ── 5 PREMIUM STUDIO, AUDIOPHILE & SPATIAL PRESETS ───────────
    // ══════════════════════════════════════════════════════════════

    AUDIOPHILE_STUDIO_MASTER(
        presetName = "Audiophile Studio Master",
        description = "Pristine flat studio reference mastering with binaural crossfeed & zero distortion",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -1.0f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.PURE,
            viperBassFreq = 40,
            viperBassGain = 2.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.OZONE_PLUS,
            viperClarityGain = 4.0f,
            crossfeedEnabled = true,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 5,
            eqEnabled = true,
            eqBands = floatArrayOf(0.5f, 0.2f, 0f, 0f, 0f, 0f, 0.5f, 0.8f, 1.0f, 1.2f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    CRYSTAL_VOCAL_ACOUSTIC_AIR(
        presetName = "Crystal Vocal & Acoustic Air",
        description = "Intimate breathy front-stage vocals, acoustic string detail and 16kHz airy sparkle",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -2.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.X_HIFI,
            viperClarityGain = 8.0f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 90,
            viperBassGain = 3.0f,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 3,
            fieldMidImageStrength = 6,
            dynamicsEnabled = true,
            compressorThreshold = -22f,
            compressorRatio = 2.5f,
            compressorAttack = 10f,
            compressorRelease = 200f,
            eqEnabled = true,
            eqBands = floatArrayOf(-1.0f, -0.5f, 0f, 1.5f, 3.5f, 4.0f, 3.5f, 4.5f, 6.0f, 7.0f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    SPATIAL_3D_HOLOGRAPHIC(
        presetName = "Spatial 3D Holographic Stage",
        description = "Immersive 360° concert hall soundstage with HRTF binaural depth",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -3.0f,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 8,
            fieldMidImageStrength = 5,
            virtualizerEnabled = true,
            virtualizerStrength = 650,
            reverbEnabled = true,
            reverbPreset = ReverbPreset.MEDIUM_HALL,
            reverbRoomLevel = -600,
            reverbDecayTime = 1200,
            reverbDiffusion = 900,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.DYNAMIC,
            viperBassFreq = 50,
            viperBassGain = 5.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.X_HIFI,
            viperClarityGain = 6.5f,
            eqEnabled = true,
            eqBands = floatArrayOf(2.0f, 1.5f, 1.0f, 0f, 0f, 1.0f, 2.0f, 3.0f, 4.0f, 4.5f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    VINTAGE_300B_TRIODE_TUBE(
        presetName = "Vintage 300B Triode Tube",
        description = "Legendary valve amplifier harmonic richness, golden analog warmth & smooth mids",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -2.5f,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 52,
            dynamicSystemEnabled = true,
            dynamicSystemMode = DynamicMode.V1,
            dynamicSystemStrength = 55,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.NATURAL,
            viperBassFreq = 80,
            viperBassGain = 4.5f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.NATURAL,
            viperClarityGain = 3.5f,
            reverbEnabled = true,
            reverbPreset = ReverbPreset.SMALL_ROOM,
            reverbRoomLevel = -1200,
            reverbDecayTime = 600,
            eqEnabled = true,
            eqBands = floatArrayOf(2.5f, 3.5f, 3.0f, 2.0f, 1.5f, 1.0f, 0.5f, 0f, -1.0f, -2.0f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    ),

    CINEMA_IMAX_DOLBY_SURROUND(
        presetName = "Cinema IMAX Dolby Theatre",
        description = "Dynamic movie-theatre surround stage with explosive LFE rumble & clear dialogue",
        params = DspParams(
            enabled = true,
            pgcEnabled = true,
            pgcGain = -3.5f,
            viperBassEnabled = true,
            viperBassMode = ViperBassMode.PURE,
            viperBassFreq = 35,
            viperBassGain = 9.5f,
            loudnessEnabled = true,
            loudnessGain = 4.0f,
            loudnessTargetGainMb = 350,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 7,
            fieldMidImageStrength = 7,
            dynamicsEnabled = true,
            compressorThreshold = -26f,
            compressorRatio = 4.0f,
            viperClarityEnabled = true,
            viperClarityMode = ViperClarityMode.X_HIFI,
            viperClarityGain = 7.0f,
            eqEnabled = true,
            eqBands = floatArrayOf(6.0f, 4.0f, 2.0f, 0f, 1.0f, 2.5f, 2.0f, 3.0f, 4.5f, 5.5f),
            limiterEnabled = true,
            limiterThreshold = -0.5f,
        )
    )
}
