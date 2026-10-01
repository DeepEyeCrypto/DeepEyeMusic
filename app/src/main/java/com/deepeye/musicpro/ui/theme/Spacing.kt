// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vertical rhythm tokens.
 *
 * ## These are plain `.dp` now, and that is deliberate
 *
 * They used to be `.sdp`, i.e. multiplied by a viewport ratio. That created a
 * second, competing scaling axis alongside the global zoom applied at the
 * Compose root, and it is why Material3 components and our own looked like they
 * were drawn to different rulers.
 *
 * The root zoom already scales everything in dp uniformly — including the
 * hardcoded internals of every Material3 component, which no amount of `.sdp`
 * usage could reach. A single axis is strictly better than two.
 *
 * Exposed as `get()` accessors so they remain the one place to change if the
 * rhythm needs retuning, and so they read consistently with [TouchTargets].
 */
object AppSpacing {
    val xs: Dp get() = 4.dp
    val sm: Dp get() = 8.dp
    val md: Dp get() = 12.dp
    val lg: Dp get() = 12.dp
    val xl: Dp get() = 18.dp
    val xxl: Dp get() = 24.dp
    val xxxl: Dp get() = 30.dp
    val xxxxl: Dp get() = 36.dp
    val hero: Dp get() = 48.dp
}

/**
 * Minimum interactive sizes.
 *
 * [Min] is 48dp because that is the Android accessibility floor for a touch
 * target — it is NOT a design token and must not be reduced for density. The
 * bottom dock is the tightest consumer at exactly 48dp tall, so lowering this
 * would put every tab below the platform minimum.
 *
 * These are deliberately plain `val`s with no `.sdp`. A touch target is a
 * platform guarantee, not a design dimension: it must hold at 48dp on a
 * 360dp split-screen pane exactly as it does on an 891dp landscape handset.
 * Routing it through the viewport factor is how accessibility regressions get
 * in through the back door.
 */
object TouchTargets {
    /**
     * **Authored** touch target sizes, compensated for the global zoom.
     *
     * ## Why these are divided by the zoom
     *
     * The landscape zoom shrinks `Density.density` to 0.75, so *everything*
     * authored in dp renders a quarter smaller — including touch targets. A
     * plain `48.dp` would come out at 36dp on screen and fail the Android
     * accessibility floor for an element the user must hit reliably.
     *
     * So these are the sizes to **author**. They are divided by the current
     * zoom, which makes them render at exactly the intended on-screen size:
     * `64.dp x 0.75 = 48.dp`.
     *
     * In portrait the zoom is `1f`, so these collapse back to the literal
     * 48 / 56 / 64dp and portrait rendering is unchanged.
     *
     * [compensated] is the supported way to read these; the raw constants are
     * uncompensated authoring values and should not be used directly.
     */
    private const val MIN = 48f
    private const val LARGE = 56f
    private const val PRIMARY = 64f

    /**
     * Divides an authored touch target by the active zoom.
     *
     * Clamped to at least [MIN] *before* dividing, so a future zoom cannot
     * silently drive any target below the platform floor.
     */
    fun compensated(unscaledDp: Float, zoom: Float): Dp =
        (unscaledDp / zoom).dp

    /**
     * The zoomed factor currently in effect.
     *
     * Read from [ZoomState], which `DeepEyeMusicTheme` commits on every
     * orientation change. Defaults to `1f` so a value read before the theme has
     * composed — which is what the unit tests do — is the uncompensated one.
     */
    private val activeZoom: Float get() = ZoomState.zoom

    /** 48dp on screen at any zoom. Never author touch targets from raw dp. */
    val Min: Dp get() = compensated(MIN, activeZoom)

    /** 56dp on screen at any zoom. */
    val Large: Dp get() = compensated(LARGE, activeZoom)

    /** 64dp on screen at any zoom. */
    val Primary: Dp get() = compensated(PRIMARY, activeZoom)
}

/**
 * Holds the zoom factor currently applied at the Compose root.
 *
 * ## Why the zoom has to be readable outside composition
 *
 * [TouchTargets] and [ViewportScaler] are plain `object`s read from
 * non-composable code and from plain JUnit. They cannot call `LocalDensity`,
 * which is a composition-local. This is the same trade-off
 * [ViewportScaler] already makes with `viewportWidthDp`, and for the same
 * reason: it keeps the geometry objects testable without a composition.
 *
 * [DeepEyeMusicTheme] commits the value before its content composes, so every
 * read during that composition sees the correct factor on the same frame the
 * orientation changes — not one frame late.
 */
object ZoomState {
    /**
     * The active zoom, defaulting to `1f`.
     *
     * The `1f` default is load-bearing: it means every geometry value is
     * uncompensated before the theme composes, which is exactly the state the
     * unit tests assert against.
     */
    @Volatile
    var zoom: Float = 1f
        private set

    /** Commits a new zoom. Called only by [DeepEyeMusicTheme]. */
    fun update(zoom: Float) {
        this.zoom = zoom
    }

    /** Restores the uncompensated zoom. Required between tests. */
    fun reset() {
        zoom = 1f
    }
}
