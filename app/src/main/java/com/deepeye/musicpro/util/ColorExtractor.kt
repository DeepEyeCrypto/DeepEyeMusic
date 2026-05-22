// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
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
    val isDark: Boolean
)

@Singleton
class ColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader = ImageLoader(context)

    /**
     * Extracts a balanced color palette from the given image URI.
     */
    suspend fun extractColors(uri: Uri?): ExtractedColors? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null

        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false) // Palette needs software bitmap
            .build()

        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
            return@withContext processBitmap(bitmap)
        }
        null
    }

    private fun processBitmap(bitmap: Bitmap): ExtractedColors {
        val palette = Palette.from(bitmap).generate()
        
        // Pick best colors with fallbacks
        val vibrant = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb ?: 0xFF000000.toInt()
        val darkVibrant = palette.darkVibrantSwatch?.rgb ?: palette.darkMutedSwatch?.rgb ?: vibrant
        val lightVibrant = palette.lightVibrantSwatch?.rgb ?: palette.lightMutedSwatch?.rgb ?: vibrant
        val muted = palette.mutedSwatch?.rgb ?: vibrant

        return ExtractedColors(
            primary = Color(vibrant),
            secondary = Color(muted),
            tertiary = Color(lightVibrant),
            background = Color(darkVibrant),
            isDark = true // We usually want a dark theme for media apps
        )
    }
}
