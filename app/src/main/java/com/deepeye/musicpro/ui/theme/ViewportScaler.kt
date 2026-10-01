// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
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
     * Reference canvas width in dp -- a desktop-class landscape viewport.
     *
     * Dimensions authored as `N.sdp` mean "N dp as it should appear on a
     * viewport this wide", and the same is true of `.ssp`.
     *
     * This was 840dp (the AFDS "Expanded" lower bound), which produced a
     * negligible 0.955x factor on an 802dp landscape phone -- visually
     * indistinguishable from unscaled, leaving the layout reading "zoomed in".
     * 1200dp is a desktop-class canvas and yields ~0.67x on that same phone,
     * which is the density the design actually wants.
     *
     * Note that [MinViewportScale] had to move with this value: at a 0.90 floor
     * the factor pinned to 0.90 and the new reference bought nothing at all.
     */
    const val ReferenceLandscapeWidth = 1200f

    /**
     * Hard floor for the viewport factor.
     *
     * Sized so a phone in landscape (~802dp) resolves *proportionally* at
     * `802/1200 = 0.67` instead of being pinned to a clamp. Raising this back
     * toward 1.0 is what previously neutralised the reference width.
     *
     * A 360dp split-screen pane computes `360/1200 = 0.30` unclamped and is
     * pinned here, so the "everything looks tiny" failure mode recorded in
     * [UiScale] still cannot occur -- the floor, not the ratio, governs narrow
     * panes.
     */
    const val MinViewportScale = 0.66f

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
     * The multiplier every `.sdp` read applies.
     *
     * ## This is always `1f`, and that is the point
     *
     * The global zoom applied at the Compose root (see
     * `UiScale.LandscapeDensityScale` and `DeepEyeMusicTheme`) is now the
     * *single* scaling authority for the app. A second, independent viewport
     * ratio would re-create the exact bug this pass exists to fix: our
     * components on one ruler, Material3 internals on another, rendering at a
     * 1.49x mismatch.
     *
     * The constant is retained rather than deleted because `.sdp` appears in
     * hundreds of call sites, and because it documents the new contract — a test
     * asserts it stays exactly `1f`, so any future reintroduction of a second
     * scaling axis fails the build rather than shipping silently.
     */
    const val ScaleFactor: Float = 1f

    /**
     * Last committed viewport width in dp.
     *
     * Defaults to [ReferenceLandscapeWidth] so [scaleFactor] is `1f` *before* any
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
     * Always [ScaleFactor].
     *
     * @see ScaleFactor for why this is a constant now.
     */
    val scaleFactor: Float get() = ScaleFactor

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
 * Author a length in dp and return it unchanged.
 *
 * ## This is now the identity function, and that is intentional
 *
 * `.sdp` previously multiplied by [ViewportScaler.scaleFactor], a viewport
 * ratio. That axis is retired: the global zoom applied at the Compose root
 * scales *every* dp value uniformly, including the hardcoded internals of
 * Material3 components, which `.sdp` could never reach.
 *
 * Keeping the two meant our own components rendered at
 * `viewportRatio x zoom` while Material's rendered at `zoom` alone — a 1.49x
 * mismatch at the old settings. That mismatch is precisely the "disjointed"
 * symptom this pass was raised to fix.
 *
 * The extension is **not** removed because it appears in several hundred call
 * sites; deleting it would be a huge, risky diff for no behavioural gain. It
 * now reads as an explicit no-op, so a reader immediately sees that dp is
 * unscaled at authoring time and scaled once, globally, at render time.
 *
 * **Do not reintroduce a factor here.** The single-axis contract is asserted by
 * `ViewportScalerTest`.
 */
val Int.sdp: Dp get() = this.dp

/** @see Int.sdp */
val Float.sdp: Dp get() = this.dp

/** @see Int.sdp */
val Double.sdp: Dp get() = this.dp

/** @see Int.sdp */
val Dp.sdp: Dp get() = this

// ─── Scalable sp ─────────────────────────────────────────────────────────────

/**
 * The result of compensating an authored font size for the global zoom.
 *
 * ## What this does and why it is not optional
 *
 * The root zoom multiplies `Density.fontScale` by the same factor it applies to
 * `density`. That is deliberate: a zoom that shrinks boxes but leaves type at
 * full size makes text overflow the containers it is supposed to label, which
 * is not what "zoom out" means.
 *
 * But it has a hard consequence. A style authored at `12.sp` with a zoom of
 * `0.75` renders at `12 x 0.75 = 9sp` — below [UiScale.MinReadableFontSize]
 * and unreadable on a 6" panel. So an authored size must be **divided** by the
 * zoom to land on the intended *rendered* size: `16.sp x 0.75 = 12sp`.
 *
 * That division is this function. It is the only place the compensation is
 * implemented, so the rule has exactly one definition and can be tested
 * directly.
 *
 * The clamp bounds are likewise compensated:
 *  - the floor is [UiScale.authoredFloorFor], so the *rendered* size never
 *    drops below [UiScale.MinReadableFontSize];
 *  - the ceiling is divided too, so it stays a true rendered-size bound rather
 *    than an authored one.
 *
 * **The result stays in `sp`, not px.** Keeping the unit means the user's
 * Android accessibility font size still multiplies on top, which is what
 * [UiScale.MaxFontScale] exists to protect. Returning pixels would silently
 * discard that setting.
 */
fun scaledFontSize(rawSp: Float): Float {
    val zoom = ZoomState.zoom
    // A zoom of 0 would divide to infinity. It is not a legal value, but guard
    // anyway so a future edit cannot produce a silent divide-by-zero.
    if (zoom <= 0f) return rawSp
    val authored = rawSp / zoom
    return authored.coerceIn(
        UiScale.authoredFloorFor(zoom),
        ViewportScaler.MaxScaledFontSize / zoom
    )
}

/**
 * Author a font size in dp-equivalent sp and compensate it for the global zoom.
 *
 * @see scaledFontSize for the compensation rule and the clamp bounds.
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

// ─── Dynamic typography ──────────────────────────────────────────────────────

/**
 * Compensates one [TextStyle] for the active global zoom.
 *
 * `lineHeight` goes through the **same** compensation as `fontSize`, which
 * preserves the leading ratio the design was tuned with. Compensating the two
 * independently would flatten the vertical rhythm at one zoom or clip descenders
 * at the other.
 *
 * `lineHeight` is then re-floored at `fontSize`: a line box under the glyph size
 * clips the descender of a "g", which is exactly the squeezed-type symptom this
 * exists to avoid.
 *
 * `letterSpacing` is deliberately left untouched. It is a designer-tuned
 * tracking adjustment in em-like units, not a dimension; compensating it would
 * make display type lose its optical tightening precisely when it is already
 * being compressed.
 */
private fun TextStyle.scaleToViewport(): TextStyle {
    val fontSize = scaledFontSize(this.fontSize.value)
    // Compensate the line box through the same rule, then lift it to at least
    // the glyph size. Both values are *authored*; the root zoom multiplies
    // fontScale afterwards, so the ratio is preserved on screen.
    val lineHeight = maxOf(scaledFontSize(this.lineHeight.value), fontSize)
    return copy(
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
    )
}

/**
 * The type scale compensated for the active global zoom.
 *
 * ## Why this cannot be a `val`
 *
 * `AppTypography` is a top-level `val`, evaluated once at class-load time.
 * Reading [ZoomState.zoom] from inside it would capture whatever zoom happened
 * to be committed at that moment and then serve that stale value for the life of
 * the process -- type that never re-flows on rotation.
 *
 * A `@Composable` fixes this: it re-evaluates on each composition, keyed on the
 * zoom, so type and geometry are guaranteed to agree on every frame.
 *
 * ## Ordering with [ZoomState]
 *
 * This **reads** `ZoomState.zoom`; it does not write it. `DeepEyeMusicTheme`
 * commits the zoom *before* it calls this, so the value observed here is already
 * correct for the current orientation. That ordering is what stops the app
 * rendering one frame at the wrong type scale after a rotation.
 *
 * ## Floor behaviour
 *
 * [UiScale.MinReadableFontSize] holds in **rendered** terms. At the landscape
 * zoom of 0.75 an authored 16sp becomes 16 / 0.75 = 21.3sp, which renders back
 * at exactly 12sp — the floor, not below it. The authored numbers therefore look
 * large in source and are not; see `scaledFontSize`.
 */
@Composable
fun rememberDynamicTypography(): Typography {
    val widthDp = LocalConfiguration.current.screenWidthDp
    // Keyed on the zoom as well as the width: the zoom is now what actually moves
    // the type scale, and keying on width alone would return a stale Typography
    // on rotation while geometry had already re-resolved.
    val zoom = ZoomState.zoom
    return remember(widthDp, zoom) {
        ViewportScaler.updateViewportWidth(widthDp)
        AppTypography.scaleToViewport()
    }
}

/** Applies [scaleToViewport] across all fifteen Material3 styles. */
private fun Typography.scaleToViewport(): Typography = Typography(
    displayLarge = displayLarge.scaleToViewport(),
    displayMedium = displayMedium.scaleToViewport(),
    displaySmall = displaySmall.scaleToViewport(),
    headlineLarge = headlineLarge.scaleToViewport(),
    headlineMedium = headlineMedium.scaleToViewport(),
    headlineSmall = headlineSmall.scaleToViewport(),
    titleLarge = titleLarge.scaleToViewport(),
    titleMedium = titleMedium.scaleToViewport(),
    titleSmall = titleSmall.scaleToViewport(),
    bodyLarge = bodyLarge.scaleToViewport(),
    bodyMedium = bodyMedium.scaleToViewport(),
    bodySmall = bodySmall.scaleToViewport(),
    labelLarge = labelLarge.scaleToViewport(),
    labelMedium = labelMedium.scaleToViewport(),
    labelSmall = labelSmall.scaleToViewport(),
)
