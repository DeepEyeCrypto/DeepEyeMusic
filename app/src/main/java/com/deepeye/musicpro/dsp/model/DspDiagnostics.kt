// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.dsp.model

data class DspDiagnostics(
    val sessionId: Int = 0,
    val state: EngineState = EngineState.IDLE,
    val preset: String = "",
    val route: AudioRoute = AudioRoute.UNKNOWN,
    val gainBudget: GainBudget = GainBudget(0f, RiskLevel.SAFE),
    val isVerboseLoggingEnabled: Boolean = false
)