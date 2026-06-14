// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.util

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GlassPalette(
    val dominant: Color,
    val vibrant: Color,
    val muted: Color,
    val onSurface: Color,
    val adaptiveTint: Color
)

@Singleton
class AlbumArtColorExtractor @Inject constructor() {

    suspend fun extract(bitmap: Bitmap, isDarkMode: Boolean): GlassPalette =
        withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).generate()

            val dominantVal = palette.getDominantColor(0xFF1A1A2E.toInt())
            val vibrantVal = palette.getVibrantColor(dominantVal)
            val mutedVal = palette.getMutedColor(dominantVal)

            val dominant = Color(dominantVal)
            val vibrant = Color(vibrantVal)
            val muted = Color(mutedVal)

            // Compute adaptive tint based on background luminance and environment mode
            val isBgLight = dominant.calculateLuminance() > 0.5
            val rawTint = if (isDarkMode) {
                // Dim, cool dark mode tint
                vibrant.copy(alpha = 0.15f)
            } else {
                // Brighter light mode tint
                vibrant.copy(alpha = 0.10f)
            }

            // Contrast safety check
            val onSurface = if (isBgLight) Color.Black else Color.White
            val safeOnSurface = ensureContrast(onSurface, dominant)

            GlassPalette(
                dominant = dominant,
                vibrant = vibrant,
                muted = muted,
                onSurface = safeOnSurface,
                adaptiveTint = rawTint
            )
        }

    private fun Color.calculateLuminance(): Double {
        val r = red
        val g = green
        val b = blue
        return 0.2126 * (if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)) +
            0.7152 * (if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)) +
            0.0722 * (if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4))
    }

    private fun calculateContrast(color1: Color, color2: Color): Double {
        val l1 = color1.calculateLuminance()
        val l2 = color2.calculateLuminance()
        return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)
    }

    private fun ensureContrast(textColor: Color, backgroundColor: Color, minRatio: Float = 4.5f): Color {
        val contrast = calculateContrast(textColor, backgroundColor)
        if (contrast >= minRatio) return textColor
        return if (backgroundColor.calculateLuminance() > 0.5) Color.Black else Color.White
    }
}

fun defaultPalette(): GlassPalette {
    return GlassPalette(
        dominant = Color(0xFF1A1A2E),
        vibrant = Color(0xFF8A2BE2),
        muted = Color(0xFF4B0082),
        onSurface = Color.White,
        adaptiveTint = Color(0xFF8A2BE2).copy(alpha = 0.15f)
    )
}

@Composable
fun rememberAnimatedGlassPalette(palette: GlassPalette): GlassPalette {
    val dominantAnim = animateColorAsState(palette.dominant, tween(600), label = "dominant")
    val vibrantAnim = animateColorAsState(palette.vibrant, tween(600), label = "vibrant")
    val mutedAnim = animateColorAsState(palette.muted, tween(600), label = "muted")
    val onSurfaceAnim = animateColorAsState(palette.onSurface, tween(600), label = "onSurface")
    val tintAnim = animateColorAsState(palette.adaptiveTint, tween(600), label = "adaptiveTint")

    return remember(palette) {
        derivedStateOf {
            GlassPalette(
                dominant = dominantAnim.value,
                vibrant = vibrantAnim.value,
                muted = mutedAnim.value,
                onSurface = onSurfaceAnim.value,
                adaptiveTint = tintAnim.value
            )
        }
    }.value
}
