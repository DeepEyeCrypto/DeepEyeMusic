// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single Source of Truth for every card geometry in the app.
 *
 * ## Why this object exists
 *
 * Card dimensions used to be hardcoded independently at three places that had
 * to agree but could not be made to agree:
 *
 *  1. the real card composable (`aspectRatio`, `clip`, `width`),
 *  2. the skeleton/shimmer drawn while data loads,
 *  3. the `GridCells.Adaptive(minSize)` that sizes the columns holding them.
 *
 * Because the three were separate literals, changing a card's shape in one
 * place left the other two stale. The visible symptom was Cumulative Layout
 * Shift: the skeleton reserved one box, real content arrived in a differently
 * sized box, and every card below the fold jumped as data landed. Fixing that
 * by hand means editing the same number in several files and hoping they stay
 * in sync on the next change.
 *
 * They are now defined once here. A skeleton and the card it stands in for read
 * the *same* field, so they are geometrically identical by construction rather
 * than by discipline.
 *
 * ## Contract for consumers
 *
 * - Real cards: `aspectRatio`/`clip`/`width` must come from here, never a literal.
 * - Skeletons: must mirror the real card's fields exactly (same ratio, same
 *   radius, same metadata block height).
 * - Adaptive grids: `minSize` must be `<Type>.minWidth`, so a column can never
 *   be narrower than the card it is asked to hold.
 */
object CardGeometry {

    /**
     * Line height of the Continue Listening title, in reference sp.
     *
     * Mirrors the `lineHeight = 15.sp` that `ContinueListeningRow` applies to
     * its `bodySmall` title. It is routed through [scaledFontSize] so the
     * legibility floor applies to the *line box* as well as the glyphs — a
     * floored 12sp glyph inside an 8dp line box still clips its descenders.
     */
    const val TitleLineHeightSp = 15f

    /**
     * Line height of the Continue Listening artist/subtitle line, in sp.
     *
     * Mirrors `AppTypography.labelSmall`'s 16sp line height.
     */
    const val SubtitleLineHeightSp = 16f

    /**
     * Gap between cards within a carousel or grid.
     *
     * A `get()` accessor rather than a `val` so the value re-reads
     * [ViewportScaler.scaleFactor] on every access. A `val` would capture the
     * factor once, when this object is first class-loaded, and then keep
     * reporting that value after a rotation or a split-screen resize.
     */
    val CardSpacing: Dp get() = 8.dp

    /** Horizontal gutter at the screen edge. @see CardSpacing */
    val ScreenGutter: Dp get() = 8.dp

    /**
     * 16:9 video card — YouTube cinema, NetMirror, home video rails.
     *
     * [minWidth] is the adaptive-grid floor. It is the narrowest a 16:9 poster
     * stays legible at (~79dp tall) while still fitting ~5 columns on an 802dp
     * landscape phone, which is the whole point of the density pass.
     *
     * Rebuilt per access so the viewport factor is picked up; see
     * [CardSpacing].
     */
    val Video: VideoCard
        get() = VideoCard(
            aspectRatio = 16f / 9f,
            minWidth = 140.dp,
            cornerRadius = 10.dp,
            contentPadding = 6.dp,
            titleMaxLines = 2,
        )

    /**
     * 1:1 square music card — Spotify-style discovery rails.
     *
     * Square cover art tolerates a narrower column than a 16:9 poster (there is
     * no letterboxed frame to crop), hence a lower [Video.minWidth] floor.
     */
    val Music: MusicCard
        get() = MusicCard(
            aspectRatio = 1f,
            minWidth = 124.dp,
            cornerRadius = 12.dp,
            contentPadding = 8.dp,
            titleMaxLines = 2,
        )

    /**
     * Compact horizontal list row — "Continue Listening".
     *
     * Fixed [height] rather than a ratio: this card is artwork-beside-text, so
     * its height is set by the artwork square plus padding. [maxWidth] lets it
     * shrink inside a FlowRow-packed row without overflowing the viewport.
     *
     * Invariant relied upon by consumers: [height] ==
     * [artworkSize] + 2 * [contentPadding], so the text block never overflows
     * the row. Currently 52 + 12 = 64.
     *
     * Every term is scaled by the *same* factor, so the invariant survives
     * viewport scaling rather than drifting as the values are tuned
     * independently.
     *
     * ## [height] is a floor, not a ceiling
     *
     * [height] is derived purely from the artwork, so it cannot account for
     * the text beside it. That text is floored at [UiScale.MinReadableFontSize]
     * and is then multiplied by the user's accessibility font scale, which means
     * it stops shrinking while the geometry around it keeps shrinking. At the
     * aggressive viewport factor the two genuinely conflict: 2 floored title
     * lines plus the artist line need ~44dp against ~35dp of interior space.
     *
     * Consumers must therefore apply this as `heightIn(min = ...)` and never as
     * `height(...)`. [minHeight] is the value to pass, and
     * [ContinueListeningGeometry.minHeight] documents the arithmetic.
     */
    val ContinueListening: ContinueListeningGeometry
        get() = ContinueListeningGeometry(
            height = 64.dp,
            maxWidth = 220.dp,
            cornerRadius = 12.dp,
            artworkSize = 52.dp,
            contentPadding = 6.dp,
            titleMaxLines = 2,
            titleLineHeightSp = TitleLineHeightSp,
            subtitleLineHeightSp = SubtitleLineHeightSp,
            titleToSubtitleGap = 2.dp,
        )

    /**
     * Dense two-line widget — Home quick-play tiles and generic compact rows.
     */
    val Compact: CompactCard
        get() = CompactCard(
            minWidth = 124.dp,
            cornerRadius = 8.dp,
            height = 44.dp,
            contentPadding = 6.dp,
        )
}

/**
 * Geometry contract for a 16:9 video card.
 *
 * @param aspectRatio width / height. Must be identical on the card and its skeleton.
 * @param minWidth adaptive-grid column floor; also the narrowest legal card width.
 * @param cornerRadius outer corner radius of the card.
 * @param contentPadding padding around the text block beneath the poster.
 * @param titleMaxLines title clamp. The skeleton must reserve exactly this many
 *   lines or the title area changes height when data lands.
 */
data class VideoCard(
    val aspectRatio: Float,
    val minWidth: Dp,
    val cornerRadius: Dp,
    val contentPadding: Dp,
    val titleMaxLines: Int,
) {
    /** Shape matching [cornerRadius] — avoids repeating `RoundedCornerShape(n)` literals. */
    val shape: RoundedCornerShape get() = RoundedCornerShape(cornerRadius)
}

/**
 * Geometry contract for a 1:1 music card.
 *
 * @see VideoCard for the meaning of each field.
 */
data class MusicCard(
    val aspectRatio: Float,
    val minWidth: Dp,
    val cornerRadius: Dp,
    val contentPadding: Dp,
    val titleMaxLines: Int,
) {
    val shape: RoundedCornerShape get() = RoundedCornerShape(cornerRadius)
}

/**
 * Geometry contract for a fixed-height horizontal list row.
 *
 * @param height artwork-driven row height. A **floor, not a ceiling** — apply it
 *   with `heightIn(min = ...)`, never `height(...)`. See [minHeight].
 * @param maxWidth widest the row may grow; it shrinks below this when packed.
 * @param artworkSize square artwork edge, which drives [height].
 * @param titleMaxLines lines the title may occupy before ellipsizing.
 * @param titleLineHeightSp title line height in reference sp.
 * @param subtitleLineHeightSp subtitle line height in reference sp.
 * @param titleToSubtitleGap space between the title and subtitle blocks.
 */
data class ContinueListeningGeometry(
    val height: Dp,
    val maxWidth: Dp,
    val cornerRadius: Dp,
    val artworkSize: Dp,
    val contentPadding: Dp,
    val titleMaxLines: Int = 2,
    val titleLineHeightSp: Float = 15f,
    val subtitleLineHeightSp: Float = 16f,
    val titleToSubtitleGap: Dp = 0.dp,
) {
    val shape: RoundedCornerShape get() = RoundedCornerShape(cornerRadius)

    /**
     * The smallest height that fits both the artwork and the text block.
     *
     * ## Why the artwork height alone is not enough
     *
     * [height] is derived from the artwork square, but the metadata column sits
     * *beside* that artwork and is governed by the type scale, not by geometry.
     * Those two run on different rails once the viewport factor drops below the
     * legibility threshold:
     *
     *  - geometry scales linearly with the viewport (0.668x on an 802dp phone);
     *  - text stops at [UiScale.MinReadableFontSize] and is then multiplied by
     *    the user's accessibility font scale.
     *
     * So the text stops shrinking at exactly the moment the box around it keeps
     * shrinking. At 0.668x the artwork needs 34.7dp of the 42.7dp row while two
     * floored title lines plus the artist line need 44dp — the fixed box is
     * already ~9dp short at 1.0x font scale, and ~22dp short at the 1.3x cap.
     *
     * `maxOf` therefore resolves to the text requirement at aggressive factors
     * and to the artwork requirement at gentle ones, so the row is never
     * smaller than the larger of the two demands.
     *
     * Line heights go through [scaledFontSize] rather than a plain multiply, so
     * the floor lifts the line box as well as the glyphs — otherwise a floored
     * 12sp glyph would sit in an 8dp line box and clip its own descenders.
     *
     * This deliberately ignores the accessibility font multiplier. It cannot be
     * read here: `Density.fontScale` is a composition-local, and this object is
     * non-composable so that `CardGeometryTest` can assert it from plain JUnit.
     * A fixed box would be wrong at 1.3x for that same reason, which is the
     * whole point of consumers passing this as a `min`.
     */
    val minHeight: Dp
        get() = maxOf(
            height,
            scaledFontSize(titleLineHeightSp).dp * titleMaxLines +
                titleToSubtitleGap +
                scaledFontSize(subtitleLineHeightSp).dp +
                contentPadding * 2,
        )
}

/**
 * Geometry contract for a dense two-line widget.
 *
 * @param height exact widget height.
 */
data class CompactCard(
    val minWidth: Dp,
    val cornerRadius: Dp,
    val height: Dp,
    val contentPadding: Dp,
) {
    val shape: RoundedCornerShape get() = RoundedCornerShape(cornerRadius)
}
