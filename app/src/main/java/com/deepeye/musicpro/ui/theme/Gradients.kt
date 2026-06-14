// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppGradients {
    val Hero =
        Brush.linearGradient(
            listOf(Color(0xFF0A0A12), Color(0xFF101424), Color(0xFF06070A)),
        )
    val AccentGlow =
        Brush.radialGradient(
            listOf(TealGlow.copy(alpha = 0.45f), Color.Transparent),
        )
    val WarningGlow =
        Brush.radialGradient(
            listOf(AccentHot.copy(alpha = 0.35f), Color.Transparent),
        )
}
