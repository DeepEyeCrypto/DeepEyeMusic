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
     * Enough to recover a little vertical breathing room, gentle enough that
     * the type stays comfortably readable. Was 0.58.
     */
    const val LandscapeDensityScale = 0.92f

    /**
     * Smallest font size, in sp, that body or label text in the app may use.
     *
     * Below 12sp text stops being reliably legible on a 6.1" 1080p panel, and
     * Android's own accessibility guidance treats 12sp as the practical floor
     * for secondary text. 14sp remains the floor for anything that carries
     * meaning; 12sp is for metadata and decoration only.
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
}