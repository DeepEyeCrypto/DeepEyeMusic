// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.model

/**
 * Authentic ViPER4Android-calibrated Master Audio Presets.
 * Engineered for zero digital clipping (-0.2 dBFS safety ceiling) with
 * precise 64-bit float multi-stage psychoacoustic processing.
 */
enum class DSPPreset(
    val presetName: String,
    val description: String,
    val params: DspParams,
    val requiredRank: Int = 999999
) {
    DEFAULT(
        presetName = "Default Balanced",
        description = "Transparent high-fidelity playback with anti-clip protection",
        params = DspParams(
            enabled = true,
            pgcGain = 0f,
            limiterEnabled = true,
            limiterThreshold = -0.5f
        )
    ),

    VIPER_BASS_XHIFI(
        presetName = "ViPER Bass & X-HiFi",
        description = "Deep subwoofer punch + crystal-clear high-frequency sparkle",
        params = DspParams(
            enabled = true,
            pgcGain = -2f,
            viperBassEnabled = true,
            viperBassGain = 7.5f,
            viperBassFreq = 55,
            viperBassMode = ViperBassMode.DYNAMIC,
            viperClarityEnabled = true,
            viperClarityGain = 6.0f,
            viperClarityMode = ViperClarityMode.X_HIFI,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 12,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            limiterEnabled = true,
            limiterThreshold = -0.5f
        )
    ),

    AUDIOPHILE_PURE(
        presetName = "Audiophile Pure Hi-Res",
        description = "Pristine acoustic soundstage with O-Zone+ air and binaural crossfeed",
        params = DspParams(
            enabled = true,
            pgcGain = -1.5f,
            viperBassEnabled = true,
            viperBassGain = 4.0f,
            viperBassFreq = 40,
            viperBassMode = ViperBassMode.PURE,
            viperClarityEnabled = true,
            viperClarityGain = 5.0f,
            viperClarityMode = ViperClarityMode.OZONE_PLUS,
            crossfeedEnabled = true,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 6,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            limiterEnabled = true,
            limiterThreshold = -0.5f
        )
    ),

    CYBER_SPATIAL_STAGE(
        presetName = "Cyberpunk Spatial 3D",
        description = "Expansive 3D surround soundstage with punchy sub-harmonics",
        params = DspParams(
            enabled = true,
            pgcGain = -2.5f,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 6,
            fieldMidImageStrength = 4,
            viperBassEnabled = true,
            viperBassGain = 6.0f,
            viperBassFreq = 60,
            viperBassMode = ViperBassMode.PURE,
            viperClarityEnabled = true,
            viperClarityGain = 7.5f,
            viperClarityMode = ViperClarityMode.X_HIFI,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            limiterEnabled = true,
            limiterThreshold = -0.5f
        )
    ),

    WARM_TUBE_ANALOG(
        presetName = "Warm 6J1 Vacuum Tube",
        description = "Authentic analog valve saturation with smooth, non-fatiguing warmth",
        params = DspParams(
            enabled = true,
            pgcGain = -2f,
            tubeEnabled = true,
            tubeMode = TubeMode.TRIODE,
            tubeDrive = 38,
            viperBassEnabled = true,
            viperBassGain = 4.5f,
            viperBassFreq = 80,
            viperBassMode = ViperBassMode.NATURAL,
            viperClarityEnabled = true,
            viperClarityGain = 3.5f,
            viperClarityMode = ViperClarityMode.NATURAL,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            limiterEnabled = true,
            limiterThreshold = -0.5f
        )
    ),

    CLUB_EDM_SUBWOOFER(
        presetName = "Club EDM Sub-Woofer",
        description = "High-impact low-end rumble and synthetic transients for electronic music",
        params = DspParams(
            enabled = true,
            pgcGain = -3f,
            viperBassEnabled = true,
            viperBassGain = 9.0f,
            viperBassFreq = 45,
            viperBassMode = ViperBassMode.DYNAMIC,
            viperClarityEnabled = true,
            viperClarityGain = 6.5f,
            viperClarityMode = ViperClarityMode.X_HIFI,
            fieldSurroundEnabled = true,
            fieldSurroundStrength = 4,
            fieldMidImageStrength = 5,
            bassBoostEnabled = false,
            loudnessEnabled = false,
            limiterEnabled = true,
            limiterThreshold = -0.5f
        )
    )
}
