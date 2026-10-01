// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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

    // ── THE GLOBAL ZOOM ───────────────────────────────────────────────────────
    //
    // This is the app's single scaling authority. Everything else computes
    // *authored* dp and sp; this is where those become on-screen sizes, and it
    // is the only place in the app where `Density` is modified.
    //
    // Why here and not in each component:
    //  - Material3 components (Button, Slider, dialog insets, ListItem) size
    //    themselves with hardcoded internal `.dp`. They read `LocalDensity` and
    //    nothing else, so this is the only lever that reaches them. Scaling
    //    `.sdp` per call site could never make a Material slider shrink, which
    //    is precisely why the previous pass produced a UI where our cards sat
    //    at 0.62 and Material's sat at 0.92 — a 1.49x mismatch that read as
    //    "disjointed".
    //  - One axis cannot drift against another. The old arrangement had three
    //    (MainActivity's fontScale cap, DeepEyeMusicApp's 0.92, and `.sdp`'s
    //    viewport ratio), and they compounded.
    //
    // Why `fontScale` is multiplied too:
    //  A zoom that shrinks boxes but not type is not a zoom — it makes text
    //  overflow the containers it labels. Scaling both keeps the whole UI in
    //  one consistent proportion, which is what "zoom out to tablet density"
    //  actually means.
    //
    //  The cost is that a style authored at 12sp would render at 9sp. That is
    //  prevented upstream: `scaledFontSize` *divides* authored sizes by the
    //  zoom so they land back on their intended rendered size, and
    //  `TouchTargets` does the same for the 48dp floor. This block is the
    //  multiplier; those two are the compensation.
    //
    // Ordering — this block is deliberately ABOVE `rememberDynamicTypography()`.
    // The typography compensator *reads* `ZoomState.zoom`, so the zoom must be
    // committed first or the type scale would be computed from the previous
    // orientation's value and lag a frame behind the geometry.
    val isLandscape = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_LANDSCAPE
    val zoom = remember(isLandscape) {
        UiScale.zoomFor(isLandscape).also(ZoomState::update)
    }

    // Viewport-scaled type.
    //
    // `MaterialTheme(typography = ...)` is what feeds `LocalTypography`, and it
    // is passed the *dynamic* scale rather than the static `AppTypography` so
    // that type re-resolves on every zoom change instead of being frozen at
    // class-load time. See rememberDynamicTypography for why a `val` cannot do
    // this.
    val dynamicTypography = rememberDynamicTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        shapes = AppShapes,
    ) {
        val baseDensity = LocalDensity.current
        val customDensity = remember(zoom, baseDensity) {
            Density(
                density = baseDensity.density * zoom,
                fontScale = baseDensity.fontScale * zoom
            )
        }

        CompositionLocalProvider(LocalDensity provides customDensity, content = content)
    }
}

/**
 * Applies the app's global zoom to a **separate window**.
 *
 * ## Why this exists — `Dialog` does not inherit `LocalDensity`
 *
 * The zoom installed by [DeepEyeMusicTheme] wraps the main composition. Compose's
 * [Dialog][androidx.compose.ui.window.Dialog] does not compose its content into
 * that composition: it creates a **new Android window** with its own
 * `ViewRootImpl` and its own composition root. Composition locals do not cross
 * that boundary, so a dialog renders at the *system* density while everything
 * behind it renders zoomed.
 *
 * This was measured on device rather than assumed. With the zoom at 0.75 on a
 * 320dpi landscape phone:
 *
 *  - main tree: the Material3 `NavigationRail` measured **80dp** — exactly its
 *    documented default, confirming the zoom is applied;
 *  - dialog: the What's New card measured **1026px**, matching
 *    `560dp cap x 0.92 x 2.0 (unzoomed density)` rather than the
 *    `x 1.5` a zoomed dialog would produce.
 *
 * So the dialog was rendering at full system scale — the exact "comically
 * oversized" symptom, in the one surface most likely to be screenshotted.
 *
 * ## Why the fix is to re-provide, not to move the zoom
 *
 * The zoom has to be re-applied inside the dialog because the dialog has its own
 * density. Note that `LocalDensity.current` read here is the *window's* density
 * (2.0), not the already-zoomed one, so multiplying by [ZoomState.zoom] once is
 * correct — it is not a double application.
 *
 * Every [Dialog][androidx.compose.ui.window.Dialog] in the app must wrap its
 * content in this. `ModalBottomSheet` and `Popup` compose in-tree and are
 * already covered.
 */
@Composable
fun ProvideAppZoom(content: @Composable () -> Unit) {
    val baseDensity = LocalDensity.current
    val zoom = ZoomState.zoom
    val density = remember(baseDensity, zoom) {
        Density(
            density = baseDensity.density * zoom,
            fontScale = baseDensity.fontScale * zoom
        )
    }
    CompositionLocalProvider(LocalDensity provides density, content = content)
}
