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
            pgcGain = -8f,
            viperBassEnabled = true,
            viperBassGain = 8f,
            viperBassFreq = 60,
            viperBassMode = ViperBassMode.PURE,
            bassBoostEnabled = true,
            bassBoostStrength = 500,
            limiterEnabled = true,
            limiterThreshold = -3f
        )
    ),
    
    THREE_D_AUDIO(
        presetName = "3D Audio",
        description = "Spatial audio with crossfeed + reverb",
        params = DspParams(
            pgcGain = -4f,
            crossfeedEnabled = true,
            reverbEnabled = true,
            reverbPreset = ReverbPreset.LARGE_ROOM,
            virtualizerEnabled = true,
            virtualizerStrength = 400
        )
    ),
    
    VOCAL_CLEAR(
        presetName = "Vocal Clear",
        description = "Enhanced vocals for clarity",
        params = DspParams(
            pgcGain = -4f,
            eqEnabled = true,
            eqBands = arrayOf(0f, 0f, 2f, 4f, 2f, 0f, 0f, 0f, 0f, 0f).toFloatArray(),
            karaokeModeEnabled = false
        )
    ),
    
    TUBE_WARMTH(
        presetName = "Tube Warmth",
        description = "Classic tube amplifier warmth",
        params = DspParams(
            pgcGain = -3f,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 45
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
        description = "Exclusive +10dB bass + harmony (Top 10 only)",
        params = DspParams(
            pgcGain = -10f,
            viperBassEnabled = true,
            viperBassGain = 10f,
            viperBassFreq = 80,
            viperBassMode = ViperBassMode.DYNAMIC,
            bassBoostEnabled = true,
            bassBoostStrength = 700,
            limiterEnabled = true,
            limiterThreshold = -3f
        ),
        requiredRank = 10
    ),

    ELITE_WARMTH(
        presetName = "Elite Warmth",
        description = "Enhanced tube sim (Top 100 only)",
        params = DspParams(
            pgcGain = -4f,
            tubeEnabled = true,
            tubeMode = TubeMode.PENTODE,
            tubeDrive = 60
        ),
        requiredRank = 100
    ),

    RISING_AUDIO(
        presetName = "Rising 3D Audio",
        description = "Balanced 3D spatial (Top 1000 only)",
        params = DspParams(
            pgcGain = -4f,
            crossfeedEnabled = true,
            virtualizerEnabled = true,
            virtualizerStrength = 500
        ),
        requiredRank = 1000
    )
}
