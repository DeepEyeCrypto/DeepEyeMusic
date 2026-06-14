// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Mathematically calculates a fluid font size based on the current screen width.
 * This guarantees smooth scaling across all screen sizes without hard jumps.
 */
fun calculateFluidFontSize(
    currentWidthDp: Int,
    minWidthDp: Int = 360,
    maxWidthDp: Int = 1200,
    minFontSize: Float,
    maxFontSize: Float
): TextUnit {
    val clampedWidth = max(minWidthDp, min(currentWidthDp, maxWidthDp))
    val widthFraction = (clampedWidth - minWidthDp).toFloat() / (maxWidthDp - minWidthDp).toFloat()
    val fluidSize = minFontSize + (maxFontSize - minFontSize) * widthFraction
    return fluidSize.sp
}

data class AFDSTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle
)

val LocalFluidTypography = compositionLocalOf {
    AFDSTypography(
        display = TypeScale.Display,
        headline = TypeScale.Headline,
        title = TypeScale.Title,
        body = TypeScale.Body,
        label = TypeScale.Label,
        caption = TypeScale.Caption
    )
}

/**
 * Calculates the exact AFDS fluid typography scale based on the current device screen width.
 * Small Phone (360dp) -> Large Phone (400dp) -> Tablet (840dp) -> Foldable/Desktop (1200dp+)
 */
@Composable
fun ProvideFluidTypography(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val typography = remember(screenWidth) {
        AFDSTypography(
            display = TextStyle(
                fontSize = calculateFluidFontSize(screenWidth, minFontSize = 32f, maxFontSize = 56f), // Hero
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            headline = TextStyle(
                fontSize = calculateFluidFontSize(screenWidth, minFontSize = 24f, maxFontSize = 44f), // Title in user prompt
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            ),
            title = TextStyle(
                fontSize = calculateFluidFontSize(screenWidth, minFontSize = 16f, maxFontSize = 24f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15.sp
            ),
            body = TextStyle(
                fontSize = calculateFluidFontSize(screenWidth, minFontSize = 14f, maxFontSize = 18f),
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.25.sp
            ),
            label = TextStyle(
                fontSize = calculateFluidFontSize(screenWidth, minFontSize = 12f, maxFontSize = 16f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            ),
            caption = TextStyle(
                fontSize = calculateFluidFontSize(screenWidth, minFontSize = 11f, maxFontSize = 14f),
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp
            )
        )
    }

    CompositionLocalProvider(LocalFluidTypography provides typography) {
        content()
    }
}
