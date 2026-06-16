// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.model

enum class DSPPreset(
    val presetName: String,
    val description: String,
    val params: DspParams
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
    )
}
