// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp

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
     * Gap between cards within a carousel or grid.
     *
     * A `get()` accessor rather than a `val` so the value re-reads
     * [ViewportScaler.scaleFactor] on every access. A `val` would capture the
     * factor once, when this object is first class-loaded, and then keep
     * reporting that value after a rotation or a split-screen resize.
     */
    val CardSpacing: Dp get() = 8.sdp

    /** Horizontal gutter at the screen edge. @see CardSpacing */
    val ScreenGutter: Dp get() = 8.sdp

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
            minWidth = 140.sdp,
            cornerRadius = 10.sdp,
            contentPadding = 6.sdp,
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
            minWidth = 104.sdp,
            cornerRadius = 10.sdp,
            contentPadding = 6.sdp,
            titleMaxLines = 1,
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
     */
    val ContinueListening: ContinueListeningGeometry
        get() = ContinueListeningGeometry(
            height = 64.sdp,
            maxWidth = 220.sdp,
            cornerRadius = 12.sdp,
            artworkSize = 52.sdp,
            contentPadding = 6.sdp,
        )

    /**
     * Dense two-line widget — Home quick-play tiles and generic compact rows.
     */
    val Compact: CompactCard
        get() = CompactCard(
            minWidth = 124.sdp,
            cornerRadius = 8.sdp,
            height = 44.sdp,
            contentPadding = 6.sdp,
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
 * @param height exact row height. The skeleton uses this directly.
 * @param maxWidth widest the row may grow; it shrinks below this when packed.
 * @param artworkSize square artwork edge, which drives [height].
 */
data class ContinueListeningGeometry(
    val height: Dp,
    val maxWidth: Dp,
    val cornerRadius: Dp,
    val artworkSize: Dp,
    val contentPadding: Dp,
) {
    val shape: RoundedCornerShape get() = RoundedCornerShape(cornerRadius)
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
