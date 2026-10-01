// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

/**
 * Global UI scale policy.
 *
 * The app previously shrank `LocalDensity.density` by a hardcoded 0.85 at the
 * root, and then by a further 0.58 in landscape. Those two multipliers compose:
 * a landscape render reached `0.85 * 0.58 = 0.49`, so every `dp` and every `sp`
 * in the app came out at roughly half size. That is why text and controls
 * looked uniformly undersized on *every* screen rather than on a few.
 *
 * `Density.density` is not a "make it slimmer" knob — it is the dp-to-pixel
 * scale. Shrinking it to fit more content on screen shrinks the type too, which
 * is the wrong lever. Fit is a layout concern and belongs in layout.
 *
 * The rules this object encodes:
 *  - density is never scaled below [LandscapeDensityScale] on the short axis;
 *  - the user's Android accessibility font size is honoured up to
 *    [MaxFontScale] (never silently discarded);
 *  - the [AppTypography] scale never drops below [MinReadableFontSize],
 *    except for the three narrow categories documented on that constant.
 */
object UiScale {

    /**
     * Multiplier applied to the system `fontScale`.
     *
     * A hard `coerceAtMost(1.0f)` used to sit here, which threw away the
     * user's accessibility font setting outright: someone on 1.30x rendered at
     * 1.00x. The cap is now 1.3 so that real accessibility sizes apply, while
     * still bounding the worst case — past ~1.3x fixed-height rows start
     * clipping rather than growing, so an uncapped pass-through trades one
     * defect for another.
     */
    const val MaxFontScale = 1.3f

    /**
     * Density multiplier for landscape, where the short axis is scarce.
     *
     * ## What this is now
     *
     * This is the app's **single, global zoom factor**. It is applied once, at
     * the Compose root, by wrapping the tree in
     * `CompositionLocalProvider(LocalDensity provides …)` inside
     * `DeepEyeMusicTheme`.
     *
     * It must be the *only* place density is changed. It used to be applied in
     * `DeepEyeMusicApp` while `MainActivity` independently re-provided
     * `LocalDensity`, so the two compounded to `0.92 x 0.92 = 0.85` on top of a
     * `.sdp` engine running at 0.67 -- three axes fighting, with Material3
     * internals landing on a different number from our own components.
     *
     * ## Why a global zoom and not per-widget `.sdp`
     *
     * Material3 components size themselves with hardcoded internal `.dp`
     * values: `Button` content padding, `Slider` track and thumb, dialog
     * insets, `ListItem` gutters. None of them consult a custom `.sdp`
     * extension, so a `.sdp`-only approach scales our cards to 0.62 while
     * every Material control stays at 0.92 -- a 1.49x mismatch that reads as
     * "disjointed". Scaling `Density.density` is the only lever that reaches
     * *both*, which is what makes the result uniform.
     *
     * See `ContentBounds` for why the "empty" half of the report is a separate
     * problem that density cannot solve.
     */
    const val LandscapeDensityScale = 0.75f

    /**
     * Density multiplier for portrait.
     *
     * Exactly `1f`. Portrait phone layouts were already correct and correctly
     * dense, and the zoom is scoped to landscape only. A non-unity portrait
     * value would re-break the portrait tuning for no benefit.
     */
    const val PortraitDensityScale = 1.0f

    /**
     * Smallest font size, in sp, that body or label text in the app may use.
     *
     * Below 12sp text stops being reliably legible on a 6.1" 1080p panel, and
     * Android's own accessibility guidance treats 12sp as the practical floor
     * for secondary text. 14sp remains the floor for anything that carries
     * meaning; 12sp is for metadata and decoration only.
     *
     * ## Why this survived the aggressive viewport retune
     *
     * `ViewportScaler` now references a 1200dp canvas, so a landscape phone
     * resolves to roughly 0.67x. Unclamped, that turns the 12sp floor value into
     * 8.0sp and every style below ~18sp into unreadable text. This floor is
     * therefore load-bearing at the new density, not decorative: it is what
     * stops the compaction pass from reproducing the original "everything looks
     * tiny" regression that this constant was written to prevent.
     *
     * The accepted tradeoff is that the lower half of the scale compresses —
     * several styles converge on 12sp and lose some of their relative
     * distinction. Geometry compacts aggressively; type compacts only down to
     * the legibility limit.
     *
     * This floor applies to the [AppTypography] scale. It is deliberately NOT
     * applied to two categories that must opt out of it, and which are tracked
     * here so the exception is visible rather than accidental:
     *
     *  1. **Corner chips** — `4K`, `HDR`, `ATMOS`, `HR`, `PLAYLIST`. These are
     *     wrapped in a padded pill sized around their own text, and are
     *     repeated up to 3x on a single thumbnail. At 12sp they overflow the
     *     pill and stop a video rail from holding its row height. They sit at
     *     9–10sp.
     *  2. **Thumbnail metadata overlays** — duration, bitrate, source tag.
     *     These render *on top of* cover art, so their contrast comes from a
     *     scrim rather than the page background. They sit at 10–11sp.
     *  3. **Diagnostics readouts** — the "STATS FOR NERDS" panel and the
     *     NowPlaying codec/decoder dump. These are monospace key:value dumps
     *     that grow with the number of fields, and the user opened them
     *     deliberately. They sit at 10–11sp.
     *
     * All three are visual labels or opt-in technical readouts, not reading
     * text, and none carries meaning that is unavailable elsewhere. Anything
     * a user must read in order to operate the app must respect the floor.
     */
    const val MinReadableFontSize = 12f

    // ─── Global zoom policy ───────────────────────────────────────────────────

    /**
     * Resolves the zoom factor for a given orientation.
     *
     * @param isLandscape whether the current configuration is landscape.
     * @return the multiplier to apply to `Density.density`.
     */
    fun zoomFor(isLandscape: Boolean): Float =
        if (isLandscape) LandscapeDensityScale else PortraitDensityScale

    /**
     * Smallest font size that may be **authored**, such that it still renders at
     * or above [MinReadableFontSize] after the global zoom is applied.
     *
     * ## Why this is not simply `MinReadableFontSize`
     *
     * The zoom multiplies `Density.fontScale` as well as `density`, because a
     * "zoom out" that shrinks boxes but not type looks like a rendering bug
     * rather than a density change — text stops fitting its own containers.
     *
     * The consequence is that a 12sp style authored at `fontScale = 1` renders
     * at `12 x 0.75 = 9sp` in landscape. That is below the floor and unreadable.
     * So the authored value has to be *divided* by the zoom to land on 12sp
     * after it is applied: `16sp x 0.75 = 12sp`.
     *
     * This is the whole reason the type scale is authored through
     * `scaledFontSize` rather than as raw `.sp` literals — see `ViewportScaler`.
     *
     * The floor is evaluated against the *portrait* zoom deliberately. Zooming
     * in (a factor above 1, which the app does not currently use) is allowed to
     * render below the floor, because the user asked for bigger type in that
     * case; only shrinking needs the compensation.
     */
    fun authoredFloorFor(zoom: Float): Float =
        if (zoom >= 1f) MinReadableFontSize else MinReadableFontSize / zoom
}