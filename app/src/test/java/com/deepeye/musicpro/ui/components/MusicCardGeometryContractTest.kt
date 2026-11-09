package com.deepeye.musicpro.ui.components

import com.deepeye.musicpro.ui.music.musicLandscapeCardWidth
import com.deepeye.musicpro.ui.theme.CardGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD contract for the Music section card geometry.
 *
 * Locked reference: ALL 5 content sections (Home, YouTube, Video Hub, Music,
 * Library) must render the SAME card — 210px × 219px, 16:9 thumbnail,
 * same NeonMediaCard VIDEO geometry, same metadata layout.
 *
 * Music previously deviated: 105.dp width (158px) + 1:1 square thumbnail.
 * These tests assert against the REAL production helpers in
 * [NeonMediaCardGeometry] / [musicLandscapeCardWidth]. The red phase is
 * guaranteed by the current production values (105.dp + 1:1), NOT by the
 * test's own local logic — the test must not contain a copy of the fix.
 */
class MusicCardGeometryContractTest {

    // ── Width ────────────────────────────────────────────────────────────

    @Test
    fun `music landscape card width equals video minWidth (140dp = 210px SSOT)`() {
        assertEquals(CardGeometry.Video.minWidth, musicLandscapeCardWidth(true))
    }

    @Test
    fun `music portrait card width stays music minWidth`() {
        assertEquals(CardGeometry.Music.minWidth, musicLandscapeCardWidth(false))
    }

    // ── Aspect ratio (16:9 mandatory, no square) ─────────────────────────

    @Test
    fun `music card thumbnail aspect ratio is 16 to 9`() {
        assertEquals(16f / 9f, NeonMediaCardGeometry(NeonCardType.MUSIC).aspectRatio)
        assertEquals(16f / 9f, NeonMediaCardGeometry(NeonCardType.VIDEO).aspectRatio)
        assertEquals(16f / 9f, NeonMediaCardGeometry(NeonCardType.COMPACT).aspectRatio)
    }

    @Test
    fun `music card thumbnail is NOT square`() {
        val ratio = NeonMediaCardGeometry(NeonCardType.MUSIC).aspectRatio
        assertTrue("expected 16:9 (1.777), got square ratio $ratio", ratio > 1.2f)
    }

    // ── Geometry selection ───────────────────────────────────────────────

    @Test
    fun `music cardType resolves to video geometry`() {
        val g = NeonMediaCardGeometry(NeonCardType.MUSIC)
        assertEquals(CardGeometry.Video.aspectRatio, g.aspectRatio)
        assertEquals(CardGeometry.Video.titleMaxLines, g.titleMaxLines)
    }
}