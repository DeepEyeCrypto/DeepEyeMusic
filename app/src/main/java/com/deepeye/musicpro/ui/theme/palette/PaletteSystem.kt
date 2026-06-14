// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme.palette

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

// ═══════════════════════════════════════════════════
// Data Classes
// ═══════════════════════════════════════════════════

/**
 * Holds the full extracted color palette from album artwork.
 */
data class PaletteResult(
    val dominant: Color = DefaultPalette.dominant,
    val vibrant: Color = DefaultPalette.vibrant,
    val muted: Color = DefaultPalette.muted,
    val darkVibrant: Color = DefaultPalette.darkVibrant,
    val darkMuted: Color = DefaultPalette.darkMuted,
    val lightVibrant: Color = DefaultPalette.lightVibrant,
)

/** Default neutral dark palette used as fallback. */
object DefaultPalette {
    val dominant = Color(0xFF7B3FE4)
    val vibrant = Color(0xFF9C5FFF)
    val muted = Color(0xFF4A3070)
    val darkVibrant = Color(0xFF3D1F7A)
    val darkMuted = Color(0xFF2A1850)
    val lightVibrant = Color(0xFFBB8FFF)
}

// ═══════════════════════════════════════════════════
// Palette Extractor
// ═══════════════════════════════════════════════════

/**
 * Extracts color palettes from album artwork URIs.
 * Uses an LRU cache (20 entries) to avoid re-extraction on recomposition.
 */
@Singleton
class PaletteExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = LruCache<String, PaletteResult>(20)

    /**
     * Extract palette from artwork URI. Returns cached result if available.
     * Falls back to [DefaultPalette] on any failure.
     */
    suspend fun extract(artworkUri: Uri?): PaletteResult {
        if (artworkUri == null) return PaletteResult()

        val key = artworkUri.toString()
        cache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmap(artworkUri) ?: return@withContext PaletteResult()
                val palette = Palette.from(bitmap).generate()

                val result = PaletteResult(
                    dominant = palette.getDominantColor(DefaultPalette.dominant.toArgb()).toComposeColor(),
                    vibrant = palette.getVibrantColor(DefaultPalette.vibrant.toArgb()).toComposeColor(),
                    muted = palette.getMutedColor(DefaultPalette.muted.toArgb()).toComposeColor(),
                    darkVibrant = palette.getDarkVibrantColor(DefaultPalette.darkVibrant.toArgb()).toComposeColor(),
                    darkMuted = palette.getDarkMutedColor(DefaultPalette.darkMuted.toArgb()).toComposeColor(),
                    lightVibrant = palette.getLightVibrantColor(DefaultPalette.lightVibrant.toArgb()).toComposeColor(),
                )
                cache.put(key, result)
                result
            } catch (e: Exception) {
                PaletteResult()
            }
        }
    }

    fun getCached(artworkUri: Uri?): PaletteResult? {
        if (artworkUri == null) return null
        return cache.get(artworkUri.toString())
    }

    private suspend fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(128, 128) // Small size for fast palette extraction
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.image as? coil3.BitmapImage)?.bitmap
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun Int.toComposeColor(): Color = Color(this)
}

// ═══════════════════════════════════════════════════
// Palette Constrainer (WCAG AA Compliance)
// ═══════════════════════════════════════════════════

/**
 * Constrains palette colors to ensure WCAG AA contrast ratios
 * against dark/AMOLED backgrounds.
 */
object PaletteConstrainer {
    private const val MIN_TEXT_CONTRAST = 4.5f
    private const val MIN_UI_CONTRAST = 3.0f

    /**
     * Adjusts a color to meet minimum contrast against the given background.
     * Lightens the color if needed to achieve the target contrast ratio.
     */
    fun constrainForText(color: Color, background: Color): Color {
        return constrainColor(color, background, MIN_TEXT_CONTRAST)
    }

    fun constrainForTextAAA(color: Color, background: Color): Color {
        return constrainColor(color, background, 7.0f)
    }

    fun constrainForUI(color: Color, background: Color): Color {
        return constrainColor(color, background, MIN_UI_CONTRAST)
    }

    /**
     * Determines if a color is considered light based on WCAG luminance.
     */
    fun isLight(color: Color): Boolean {
        return relativeLuminance(color) > 0.5f
    }

    private fun constrainColor(color: Color, background: Color, minContrast: Float): Color {
        var adjusted = color
        var iterations = 0
        val bgIsLight = isLight(background)
        
        while (contrastRatio(adjusted, background) < minContrast && iterations < 20) {
            adjusted = if (bgIsLight) darken(adjusted, 0.05f) else lighten(adjusted, 0.05f)
            iterations++
        }
        return adjusted
    }

    /**
     * Computes WCAG contrast ratio between two colors.
     * Returns a value between 1 and 21.
     */
    fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun relativeLuminance(color: Color): Float {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun linearize(channel: Float): Float {
        return if (channel <= 0.03928f) {
            channel / 12.92f
        } else {
            Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
    }

    private fun lighten(color: Color, amount: Float): Color {
        return Color(
            red = (color.red + amount).coerceIn(0f, 1f),
            green = (color.green + amount).coerceIn(0f, 1f),
            blue = (color.blue + amount).coerceIn(0f, 1f),
            alpha = color.alpha,
        )
    }

    private fun darken(color: Color, amount: Float): Color {
        return Color(
            red = (color.red - amount).coerceIn(0f, 1f),
            green = (color.green - amount).coerceIn(0f, 1f),
            blue = (color.blue - amount).coerceIn(0f, 1f),
            alpha = color.alpha,
        )
    }
}
