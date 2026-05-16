package com.deepeye.musicpro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.deepeye.musicpro.util.ExtractedColors

/**
 * DeepEye Music Pro — Material 3 Theme
 *
 * Supports dark/light modes with dynamic color (Material You) on Android 12+.
 * Falls back to the custom DeepEye palette on older devices.
 */

private val DarkColorScheme = darkColorScheme(
    primary            = DeepEyePrimary,
    onPrimary          = DarkOnBackground,
    primaryContainer   = DeepEyePrimaryDark,
    secondary          = DeepEyeSecondary,
    onSecondary        = DarkOnBackground,
    tertiary           = DeepEyeTertiary,
    background         = DarkBackground,
    onBackground       = DarkOnBackground,
    surface            = DarkSurface,
    onSurface          = DarkOnSurface,
    surfaceVariant     = DarkSurfaceVariant,
    onSurfaceVariant   = DarkOnSurfaceVariant,
    outline            = DarkOutline,
    error              = ErrorRed,
    onError            = DarkOnBackground
)

private val LightColorScheme = lightColorScheme(
    primary            = DeepEyePrimary,
    onPrimary          = LightOnBackground,
    primaryContainer   = DeepEyePrimaryDark,
    secondary          = DeepEyeSecondary,
    onSecondary        = LightOnBackground,
    tertiary           = DeepEyeTertiary,
    background         = LightBackground,
    onBackground       = LightOnBackground,
    surface            = LightSurface,
    onSurface          = LightOnSurface,
    surfaceVariant     = LightSurfaceVariant,
    onSurfaceVariant   = LightOnSurfaceVariant,
    outline            = LightOutline,
    error              = ErrorRed,
    onError            = LightOnBackground
)

@Composable
fun DeepEyeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    overrideColors: ExtractedColors? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        overrideColors != null -> {
            darkColorScheme(
                primary = overrideColors.primary,
                secondary = overrideColors.secondary,
                tertiary = overrideColors.tertiary,
                background = overrideColors.background,
                surface = overrideColors.background,
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
        // Use Material You dynamic colors on Android 12+ if enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeepEyeTypography,
        shapes = DeepEyeShapes,
        content = content
    )
}
