// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassCard(
    elevation: Dp = 8.dp,
    borderColor: Color = Color(0xFF333333).copy(alpha = 0.5f),
    gradientTop: Color = Color(0xFF1E1E1E).copy(alpha = 0.7f),
    gradientBottom: Color = Color(0xFF141414).copy(alpha = 0.7f),
    cornerRadius: Dp = 16.dp
): Modifier = composed {
    this
        .shadow(elevation, RoundedCornerShape(cornerRadius), clip = false)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(gradientTop, gradientBottom)
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
        .clip(RoundedCornerShape(cornerRadius))
}
