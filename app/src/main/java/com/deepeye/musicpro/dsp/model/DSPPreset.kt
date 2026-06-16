// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.model

enum class DSPPreset(
    val presetName: String,
    val description: String,
    val params: DspParams,
    val requiredRank: Int = 999999
) {
    PREMIUM_BASS(
        presetName = "Premium Bass",
        description = "Fata-fat bass with harmonic saturation",
        params = DspParams(
            viperBassEnabled = true,
            viperBassGain = 12f,
            viperBassFreq = 60,
            viperBassMode = ViperBassMode.PURE,
            bassBoostEnabled = true,
            bassBoostStrength = 800,
            limiterEnabled = true,
            limiterThreshold = -1f
        )
    ),
    
    THREE_D_AUDIO(
        presetName = "3D Audio",
        description = "Spatial audio with crossfeed + reverb",
        params = DspParams(
            crossfeedEnabled = true,
            reverbEnabled = true,
            reverbPreset = ReverbPreset.LARGE_ROOM,
            virtualizerEnabled = true,
            virtualizerStrength = 500
        )
    ),
    
    VOCAL_CLEAR(
        presetName = "Vocal Clear",
        description = "Enhanced vocals for clarity",
        params = DspParams(
            eqEnabled = true,
            eqBands = arrayOf(0f, 0f, 3f, 6f, 3f, 0f, 0f, 0f, 0f, 0f).toFloatArray(),
            karaokeModeEnabled = false
        )
    ),
    
    TUBE_WARMTH(
        presetName = "Tube Warmth",
        description = "Classic tube amplifier warmth",
        params = DspParams(
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 70
        )
    ),
    
    DEFAULT(
        presetName = "Default Tuning",
        description = "Balanced sound",
        params = DspParams()
    ),

    // Rank Exclusives
    LEGEND_BASS(
        presetName = "Legend Bass",
        description = "Exclusive +15dB bass + harmony (Top 10 only)",
        params = DspParams(
            viperBassEnabled = true,
            viperBassGain = 15f,
            viperBassFreq = 80,
            viperBassMode = ViperBassMode.DYNAMIC,
            bassBoostEnabled = true,
            bassBoostStrength = 1000
        ),
        requiredRank = 10
    ),

    ELITE_WARMTH(
        presetName = "Elite Warmth",
        description = "Enhanced tube sim (Top 100 only)",
        params = DspParams(
            tubeEnabled = true,
            tubeMode = TubeMode.PENTODE,
            tubeDrive = 90
        ),
        requiredRank = 100
    ),

    RISING_AUDIO(
        presetName = "Rising 3D Audio",
        description = "Balanced 3D spatial (Top 1000 only)",
        params = DspParams(
            crossfeedEnabled = true,
            virtualizerEnabled = true,
            virtualizerStrength = 750
        ),
        requiredRank = 1000
    )
}
