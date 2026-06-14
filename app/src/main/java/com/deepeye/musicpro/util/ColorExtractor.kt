// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of color extraction from album art.
 */
data class ExtractedColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val isDark: Boolean,
)

@Singleton
class ColorExtractor
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val imageLoader = ImageLoader(context)

    /**
     * Extracts a balanced color palette from the given image URI.
     */
    suspend fun extractColors(uri: Uri?): ExtractedColors? =
        withContext(Dispatchers.IO) {
            if (uri == null) return@withContext null

            val request =
                ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false) // Palette needs software bitmap
                    .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.image as? BitmapImage)?.bitmap ?: return@withContext null
                return@withContext processBitmap(bitmap)
            }
            null
        }

    private fun processBitmap(bitmap: Bitmap): ExtractedColors {
        val palette = Palette.from(bitmap).generate()

        // Pick best colors with fallbacks
        val vibrant = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb ?: 0xFF121214.toInt()
        val muted = palette.mutedSwatch?.rgb ?: vibrant
        val lightVibrant = palette.lightVibrantSwatch?.rgb ?: vibrant

        // Apply Premium HSL Clamping to primary color
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(vibrant, hsl)
        // Saturation constraint: Max 40% (0.40f) to prevent eye strain
        hsl[1] = hsl[1].coerceIn(0.15f, 0.40f)
        // Luminance constraint: Max 25% (0.25f) to ensure WCAG contrast safety
        hsl[2] = hsl[2].coerceIn(0.10f, 0.25f)
        val primaryColorVal = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
        val correctedPrimary = Color(primaryColorVal)

        // Deriving premium dark surface background with 15% blend ratio
        val darkBackgroundInt = 0xFF0F0F12.toInt()
        val blendedBackgroundInt = androidx.core.graphics.ColorUtils.blendARGB(
            darkBackgroundInt,
            primaryColorVal,
            0.15f
        )
        val blendedBackground = Color(blendedBackgroundInt)

        return ExtractedColors(
            primary = correctedPrimary,
            secondary = Color(muted),
            tertiary = Color(lightVibrant),
            background = blendedBackground,
            isDark = true,
        )
    }
}
