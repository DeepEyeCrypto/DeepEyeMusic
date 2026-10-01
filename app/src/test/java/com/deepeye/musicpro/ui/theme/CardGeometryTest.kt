package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the invariants of [CardGeometry], the single source of truth for card
 * dimensions.
 *
 * ## Why these assertions exist
 *
 * Loading skeletons used to declare their own `aspectRatio`, corner radius and
 * padding as independent literals. When a real card's geometry was retuned, the
 * skeleton kept the old numbers and the loading state stopped matching the
 * loaded state — Cumulative Layout Shift, with every card below the fold jumping
 * as data arrived.
 *
 * Consolidating the values into one object fixes that *only* while the numbers
 * stay sane. These tests are what stop a future edit from quietly reintroducing
 * it: they fail the build rather than shipping a janky screen.
 */
class CardGeometryTest {

    /**
     * The engine holds process-wide mutable state by design, so any test that
     * moves the viewport must restore it or it leaks into later tests and
     * produces order-dependent failures.
     */
    @After
    fun tearDown() {
        ViewportScaler.reset()
        ZoomState.reset()
    }

    private fun viewport(widthDp: Int) = ViewportScaler.updateViewportWidth(widthDp)

    /**
     * A skeleton must be able to reserve exactly the box the real card occupies.
     *
     * If these drift apart, `VideoCardSkeleton` and `SmartTubeVideoCard` render
     * different heights and the grid reflows on load.
     */
    @Test
    fun `video card geometry is internally consistent`() {
        val video = CardGeometry.Video

        // 16:9 must survive as an exact ratio — a float rounding artefact here
        // silently changes every video card's height.
        assertTrue(
            "Video.aspectRatio must stay 16:9, was ${video.aspectRatio}",
            kotlin.math.abs(video.aspectRatio - 16f / 9f) < 1e-6f
        )
        assertTrue(
            "Video.minWidth must be positive, was ${video.minWidth.value}",
            video.minWidth.value > 0f
        )
        assertTrue(
            "Video.cornerRadius must be positive, was ${video.cornerRadius.value}",
            video.cornerRadius.value > 0f
        )
        assertTrue(
            "Video.titleMaxLines must be >= 1 so the skeleton reserves a title line",
            video.titleMaxLines >= 1
        )
        assertTrue(
            "Video.contentPadding must be non-negative",
            video.contentPadding.value >= 0f
        )
    }

    @Test
    fun `music card geometry is internally consistent`() {
        val music = CardGeometry.Music

        // Square cover art: the Spotify-style card and its skeleton both rely on
        // this being exactly 1f.
        assertTrue(
            "Music.aspectRatio must stay 1:1, was ${music.aspectRatio}",
            kotlin.math.abs(music.aspectRatio - 1f) < 1e-6f
        )
        assertTrue(
            "Music.minWidth must be positive, was ${music.minWidth.value}",
            music.minWidth.value > 0f
        )
        assertTrue(
            "Music.titleMaxLines must be >= 1",
            music.titleMaxLines >= 1
        )
    }

    /**
     * The Continue Listening row is artwork-beside-text with a fixed height.
     *
     * Its artwork must fit inside the declared height, otherwise the artwork
     * overflows the row and the metadata is pushed out of view — the exact bug
     * this geometry was introduced to fix.
     */
    @Test
    fun `continue listening artwork fits inside its row height`() {
        val row = CardGeometry.ContinueListening

        assertTrue(
            "Artwork (${row.artworkSize}) + 2 * contentPadding " +
                "(${row.contentPadding}) must fit in row height (${row.height}); " +
                "otherwise the artwork overflows and clips the metadata",
            row.artworkSize + row.contentPadding * 2 <= row.height
        )
        assertTrue(
            "ContinueListening.maxWidth must be positive",
            row.maxWidth.value > 0f
        )
        assertTrue(
            "ContinueListening.cornerRadius must be positive",
            row.cornerRadius.value > 0f
        )
    }

    /**
     * The adaptive-grid floors must fit a sensible number of columns *after* the
     * global zoom.
     *
     * This is the density assertion for the whole pass. Column count is decided
     * by the width available at render time, and the zoom shrinks that by 25% in
     * landscape -- so a floor that fit 3 columns when unscaled may fit fewer
     * after zooming. Asserting the post-zoom count is what guarantees the UI
     * actually gets denser rather than merely smaller.
     */
    @Test
    fun `adaptive floors fit more columns on a zoomed landscape phone`() {
        val landscapePhoneWidth = 891.dp
        val zoom = UiScale.zoomFor(isLandscape = true)

        // Effective width the grid sees at render time.
        val effectiveWidth = landscapePhoneWidth * zoom

        val videoColumns =
            ((effectiveWidth + CardGeometry.CardSpacing) /
                (CardGeometry.Video.minWidth + CardGeometry.CardSpacing)).toInt()
        val musicColumns =
            ((effectiveWidth + CardGeometry.CardSpacing) /
                (CardGeometry.Music.minWidth + CardGeometry.CardSpacing)).toInt()

        assertTrue(
            "Video.minWidth (${CardGeometry.Video.minWidth}) must fit at least 4 " +
                "columns on a ${landscapePhoneWidth.value}dp landscape phone at " +
                "zoom $zoom, only $videoColumns fit",
            videoColumns >= 4
        )
        assertTrue(
            "Music.minWidth (${CardGeometry.Music.minWidth}) must fit at least 5 " +
                "columns on a ${landscapePhoneWidth.value}dp landscape phone at " +
                "zoom $zoom, only $musicColumns fit",
            musicColumns >= 5
        )
    }

    // ── The hybrid contract: geometry compacts, type does not ──────────────

    /**
     * The core hybrid assertion: geometry must compact while type holds its floor.
     *
     * This is the invariant the whole retune rests on. If the geometry stopped
     * compacting, the app would be "zoomed in" again; if the type followed it
     * down, every style would drop under 12sp and become unreadable. The two
     * must move in *opposite* directions, and neither may be allowed to drag
     * the other along.
     */
    @Test
    fun `geometry holds authored size while typography holds its floor`() {
        // Geometry no longer compacts per-viewport: `.sdp` is identity and the
        // single global zoom does the work at render time. So this now asserts
        // the two halves of that design are independent -- the authored geometry
        // is exactly what was declared, and the type floor is untouched by it.
        val zoom = UiScale.zoomFor(isLandscape = true)
        ZoomState.update(zoom)

        val authoredHeight = CardGeometry.ContinueListening.height
        val renderedHeight = authoredHeight.value * zoom
        val titleRendered = scaledFontSize(CardGeometry.TitleLineHeightSp) * zoom

        assertEquals(
            "authored geometry must be exactly what CardGeometry declares, " +
                "no per-viewport factor",
            64f, authoredHeight.value, 0.01f
        )
        assertTrue(
            "the global zoom must actually compact geometry in landscape, " +
                "was $renderedHeight",
            renderedHeight < 64f
        )
        assertTrue(
            "the title must still render at or above the floor after the zoom, " +
                "was ${titleRendered}sp",
            titleRendered >= UiScale.MinReadableFontSize - 1e-3f
        )

        ZoomState.reset()
    }

    /**
     * The artwork-derived [ContinueListeningGeometry.height] must never be
     * shorter than the text it contains, at any zoom.
     *
     * A consumer applying `height(...)` rather than `heightIn(min = ...)` would
     * clip the artist line off the bottom of the card. `minHeight` exists to
     * absorb exactly that, and this asserts the absorption still works now that
     * both terms move with the zoom instead of one term moving alone.
     */
    @Test
    fun `min height always covers the text block at every zoom`() {
        for (zoomValue in listOf(0.75f, 0.9f, 1.0f)) {
            ZoomState.update(zoomValue)
            val row = CardGeometry.ContinueListening

            val textBlock =
                scaledFontSize(row.titleLineHeightSp).dp * row.titleMaxLines +
                    row.titleToSubtitleGap +
                    scaledFontSize(row.subtitleLineHeightSp).dp +
                    row.contentPadding * 2

            assertTrue(
                "at zoom $zoomValue minHeight (${row.minHeight}) is shorter than " +
                    "the $textBlock text block and the card would clip",
                row.minHeight >= textBlock - 0.01.dp
            )
            assertTrue(
                "minHeight must never be shorter than the artwork height at " +
                    "zoom $zoomValue, was ${row.minHeight} vs ${row.height}",
                row.minHeight >= row.height
            )
        }
        ZoomState.reset()
    }

    /**
     * Spacing tokens must stay non-negative and smaller than a card.
     *
     * A negative gutter would push cards off-screen; a gutter larger than the card
     * itself means the geometry is nonsense.
     */
    @Test
    fun `spacing tokens are sane`() {
        assertTrue(
            "CardSpacing must be positive",
            CardGeometry.CardSpacing.value > 0f
        )
        assertTrue(
            "ScreenGutter must be non-negative",
            CardGeometry.ScreenGutter.value >= 0f
        )
        assertTrue(
            "ScreenGutter (${CardGeometry.ScreenGutter}) must be smaller than " +
                "Video.minWidth (${CardGeometry.Video.minWidth})",
            CardGeometry.ScreenGutter < CardGeometry.Video.minWidth
        )
    }
}
