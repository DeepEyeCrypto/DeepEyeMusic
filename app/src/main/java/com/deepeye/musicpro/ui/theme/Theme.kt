// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.deepeye.musicpro.util.ExtractedColors

@Composable
fun DeepEyeMusicTheme(
    darkTheme: Boolean = true,
    useDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    amoledMode: Boolean = false,
    overrideColors: ExtractedColors? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme =
        when {
            overrideColors != null -> {
                val animPrimary by animateColorAsState(
                    targetValue = overrideColors.primary,
                    animationSpec = tween(600),
                    label = "ThemePrimary"
                )
                val animSecondary by animateColorAsState(
                    targetValue = overrideColors.secondary,
                    animationSpec = tween(600),
                    label = "ThemeSecondary"
                )
                val animTertiary by animateColorAsState(
                    targetValue = overrideColors.tertiary,
                    animationSpec = tween(600),
                    label = "ThemeTertiary"
                )
                val animBackground by animateColorAsState(
                    targetValue = if (amoledMode) AmoledBlack else overrideColors.background,
                    animationSpec = tween(600),
                    label = "ThemeBackground"
                )
                val animSurface by animateColorAsState(
                    targetValue = if (amoledMode) AmoledSurface else overrideColors.background,
                    animationSpec = tween(600),
                    label = "ThemeSurface"
                )

                if (darkTheme) {
                    darkColorScheme(
                        primary = animPrimary,
                        secondary = animSecondary,
                        tertiary = animTertiary,
                        background = animBackground,
                        surface = animSurface,
                        onPrimary = Color.Black,
                        onBackground = TextPrimary,
                        onSurface = TextPrimary,
                    )
                } else {
                    lightColorScheme(
                        primary = animPrimary,
                        secondary = animSecondary,
                        tertiary = animTertiary,
                        background = Color(0xFFF5F6FA), // Keep light background for contrast
                        surface = Color.White,
                        surfaceVariant = Color(0xFFF0F1F5),
                        onPrimary = Color.Black, // Dark text on potentially light primary buttons
                        onSecondary = Color.Black,
                        onBackground = Color(0xFF1A1C20),
                        onSurface = Color(0xFF2D3038),
                        onSurfaceVariant = Color(0xFF6B7080), // Proper placeholder text color
                    )
                }
            }
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
                dynamicDarkColorScheme(context)
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ->
                dynamicLightColorScheme(context)
            else ->
                if (darkTheme) {
                    darkColorScheme(
                        primary = ExpressivePrimary,
                        secondary = ExpressiveSecondary,
                        tertiary = ExpressiveTertiary,
                        background = if (amoledMode) AmoledBlack else ExpressiveBackground,
                        surface = if (amoledMode) AmoledSurface else ExpressiveSurface,
                        surfaceVariant = if (amoledMode) AmoledSurface2 else ExpressiveSurfaceVariant,
                        onPrimary = ExpressiveOnPrimary,
                        onSecondary = ExpressiveOnPrimary,
                        onBackground = ExpressiveOnBackground,
                        onSurface = ExpressiveOnBackground,
                    )
                } else {
                    lightColorScheme(
                        primary = TealDim,
                        secondary = AccentHot,
                        tertiary = AccentPink,
                        background = Color(0xFFF5F6FA),
                        surface = Color.White,
                        surfaceVariant = Color(0xFFF0F1F5),
                        onPrimary = Color.Black, // Fix for light theme default text on buttons
                        onSecondary = Color.Black,
                        onBackground = Color(0xFF1A1C20),
                        onSurface = Color(0xFF2D3038),
                        onSurfaceVariant = Color(0xFF6B7080), // Fix for light theme default placeholder text
                        outline = Color(0xFFD0D3DC),
                    )
                }
        }


    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) break
                context = context.baseContext
            }
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
