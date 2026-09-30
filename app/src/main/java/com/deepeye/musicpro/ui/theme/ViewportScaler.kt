// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Viewport-aware scaling engine ("reference resolution" / Canvas Scaler).
 *
 * ## What problem this solves
 *
 * Landscape on a phone is an *aspect-ratio* problem, not a density problem.
 * A 6.1" handset rotated gives roughly 891 x 360dp: the long axis is wider
 * than a tablet's short axis, while the short axis -- the one that decides how
 * many rows fit -- is smaller than on portrait. Everything designed against
 * portrait proportions therefore reads as "zoomed in": too few items per
 * screen, oversized chrome, wasted horizontal runs.
 *
 * The blunt fix was to shrink `Density.density` (see
 * [UiScale.LandscapeDensityScale]). That works for layout but is a blunt
 * instrument: it is a constant, so it cannot distinguish a genuinely narrow
 * viewport from a wide one, and it silently rescales literals nobody intended
 * to touch.
 *
 * This engine is the fine-grained alternative. Author a dimension once in
 * "reference dp" and read it through `.sdp`; the value resolves against the
 * live viewport width instead of a constant.
 *
 * ## Contract
 *
 *  - At [ReferenceLandscapeWidth] the factor is exactly `1f`, so a
 *    viewport-neutral app is unchanged. Every existing `CardGeometryTest` and
 *    `TypographyScaleTest` assertion holds, because both resolve through the
 *    default factor.
 *  - The factor is clamped to [MinViewportScale]..[MaxViewportScale]. Split
 *    screen and small freeform windows must not shrink the UI to a fraction
 *    of its intended size, and a TV must not inflate it.
 *  - Scaling never applies to touch targets. See `Int.sdp` for why.
 *
 * ## Relationship to [UiScale.LandscapeDensityScale]
 *
 * These compose, and deliberately so. `Density.density` scales `dp -> px`
 * only; `sp` resolves through `Density.fontScale`, which this engine never
 * touches. So:
 *
 *  - `.sdp` on a layout dimension compounds with the landscape density trim.
 *  - `.ssp` on a text dimension is *independent* of it, which is why `.ssp`
 *    must clamp on its own to [UiScale.MinReadableFontSize].
 *
 * ## Ordering
 *
 * [ProvideViewportScale] must wrap the app root, above every consumer. The
 * geometry objects in this package are exposed as `get()` accessors precisely
 * so they re-read this value on every access instead of freezing a value
 * captured at class initialisation time.
 */
object ViewportScaler {

    /**
     * Reference canvas width in dp -- a standard desktop/tablet landscape.
     *
     * Dimensions authored as `N.sdp` mean "N dp as it should appear on a
     * viewport this wide". 840dp is the AFDS "Expanded" lower bound (see
     * [AFDSBreakpoint]), so it sits on an existing breakpoint rather than
     * inventing a new one.
     */
    const val ReferenceLandscapeWidth = 840f

    /**
     * Hard floor for the viewport factor.
     *
     * A 360dp-wide split-screen pane would compute `360/840 = 0.43` and
     * shrink the entire UI to under half size -- the "everything looks tiny"
     * failure mode recorded in [UiScale]. 0.90 keeps a narrow pane readable.
     */
    const val MinViewportScale = 0.90f

    /**
     * Hard ceiling for the viewport factor.
     *
     * Caps growth on very wide canvases (TV, desktop, unfolded foldables) so
     * switching to an external display does not rescale the whole app.
     */
    const val MaxViewportScale = 1.10f

    /**
     * Upper bound applied by `.ssp`.
     *
     * Stops a wide viewport from producing absurd type. Above roughly 96sp
     * text stops being a label and becomes a layout problem.
     */
    const val MaxScaledFontSize = 96f

    /**
     * Last committed viewport width in dp.
     *
     * Defaults to [ReferenceLandscapeWidth] so the factor is `1f` *before* any
     * composition runs. That default is load-bearing: it is what lets
     * `CardGeometryTest` and `TypographyScaleTest` resolve real values from a
     * plain JUnit thread with no composition and no mock.
     *
     * Read through [scaleFactor], never directly.
     */
    @Volatile
    var viewportWidthDp: Int = ReferenceLandscapeWidth.toInt()
        private set

    /** True viewport width in dp, defaulting to the reference width. */
    val currentWidthDp: Int get() = viewportWidthDp

    /**
     * The multiplier every `.sdp` and `.ssp` read applies.
     *
     * Exactly `1f` at [ReferenceLandscapeWidth]; clamped to
     * [MinViewportScale]..[MaxViewportScale] otherwise.
     */
    val scaleFactor: Float
        get() = (viewportWidthDp / ReferenceLandscapeWidth)
            .coerceIn(MinViewportScale, MaxViewportScale)

    /**
     * Commits a new viewport width. Called only by [ProvideViewportScale].
     *
     * Internal rather than private so tests can drive the engine directly
     * without standing up a `LocalConfiguration`.
     */
    internal fun updateViewportWidth(widthDp: Int) {
        viewportWidthDp = widthDp
    }

    /**
     * Restores the reference width and a `1f` factor.
     *
     * Required between tests that move the viewport, otherwise state leaks
     * across tests in the same JVM and produces order-dependent failures.
     */
    fun reset() {
        viewportWidthDp = ReferenceLandscapeWidth.toInt()
    }
}
/**
 * Commits the live viewport width for the duration of [content].
 *
 * Must be mounted once, at the app root, above every consumer.
 *
 * The commit is a `remember` block rather than a `SideEffect` on purpose: a
 * `SideEffect` runs *after* the subtree composes, so children reading
 * geometry during their own composition would still see the previous width.
 * `remember` runs before its children, which is the ordering needed for
 * geometry to be correct on the frame the viewport changes.
 */
@Composable
fun ProvideViewportScale(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    remember(widthDp) { ViewportScaler.updateViewportWidth(widthDp) }
    content()
}

// ─── Scalable dp ─────────────────────────────────────────────────────────────

/**
 * Author a length in reference dp and resolve it against the live viewport.
 *
 * `140.sdp` means "140dp on an 840dp-wide viewport, proportionally scaled on
 * anything else". It resolves in a `@Composable` and in plain Kotlin alike,
 * because [ViewportScaler] holds the width as ordinary state rather than a
 * `CompositionLocal` -- that is what allows [CardGeometry] to stay
 * non-composable and remain testable from plain JUnit.
 *
 * **Do not use for touch targets.** A touch target is an accessibility floor
 * in dp, not a design dimension: shrinking one below 48dp is a regression
 * regardless of how much room the viewport has. Use [TouchTargets].
 */
val Int.sdp: Dp get() = (this * ViewportScaler.scaleFactor).dp

/** @see Int.sdp */
val Float.sdp: Dp get() = (this * ViewportScaler.scaleFactor).dp

/** @see Int.sdp */
val Double.sdp: Dp get() = (this * ViewportScaler.scaleFactor).dp

/** @see Int.sdp */
val Dp.sdp: Dp get() = this * ViewportScaler.scaleFactor

// ─── Scalable sp ─────────────────────────────────────────────────────────────

/**
 * The clamped result of scaling a raw sp value against the viewport.
 *
 * A named function so the clamping rule has exactly one implementation and can
 * be unit-tested directly, rather than being spread across every `.ssp` call
 * site.
 *
 * The floor at [UiScale.MinReadableFontSize] is the important part: this is the
 * single invariant that keeps the 12sp legibility floor true no matter what
 * the viewport does.
 */
fun scaledFontSize(rawSp: Float): Float {
    val viewportScaled = rawSp * ViewportScaler.scaleFactor
    return viewportScaled
        .coerceIn(UiScale.MinReadableFontSize, ViewportScaler.MaxScaledFontSize)
}

/**
 * Author a font size in reference sp and resolve it against the live viewport.
 *
 * Unlike `Dp`, `sp` is *not* affected by [UiScale.LandscapeDensityScale], so
 * this is the only way to make type respond to the viewport -- and therefore
 * the only place the legibility floor could be broken. Hence the clamp:
 *
 *  - never below [UiScale.MinReadableFontSize] (12sp), the invariant the
 *    typography guard test asserts across all 15 styles;
 *  - never above [ViewportScaler.MaxScaledFontSize].
 *
 * The result stays in `sp`, not px. Keeping the unit is deliberate: it lets
 * the user's Android accessibility font size still multiply on top, which is
 * what the [UiScale.MaxFontScale] guard exists to protect. Handing back pixels
 * here would quietly discard that setting.
 */
val Int.ssp: TextUnit
    get() = scaledFontSize(this.toFloat()).sp

/** @see Int.ssp */
val Float.ssp: TextUnit
    get() = scaledFontSize(this).sp

/** @see Int.ssp */
val Double.ssp: TextUnit
    get() = scaledFontSize(this.toFloat()).sp

/** Integer form of [scaledFontSize], rounded for whole-unit layout maths. */
fun scaledFontSizeInt(rawSp: Float): Int = scaledFontSize(rawSp).roundToInt()