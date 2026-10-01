// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the scaling architecture: **one** authority, applied once, at the root.
 *
 * ## Why this file was rewritten rather than deleted
 *
 * It previously asserted that `.sdp` scaled proportionally to viewport width
 * (0.67 on a landscape phone). That engine is gone on purpose: it was a second
 * scaling axis competing with the root zoom, and it only ever reached *our*
 * components -- never the hardcoded `.dp` inside Material3. Keeping it meant our
 * cards rendered at 0.62 while Material's sliders and buttons sat at 0.92, a
 * 1.49x mismatch that is exactly the "disjointed" symptom.
 *
 * Deleting the file would have removed the only guard against that regression
 * returning. These tests assert the new invariants instead:
 *
 *  1. exactly one scaling axis (`.sdp` is identity, at any viewport width);
 *  2. the zoom is committed to `ZoomState` and honoured by typography;
 *  3. the compensation rule keeps rendered type at or above the floor;
 *  4. touch targets stay at 48dp *on screen* despite the zoom.
 *
 * Each test resets both engines in [tearDown]. They hold process-wide mutable
 * state by design (see [ViewportScaler] and [ZoomState]), so without that reset
 * these tests leak into each other and fail depending on execution order.
 */
class ViewportScalerTest {

    @After
    fun tearDown() {
        ViewportScaler.reset()
        ZoomState.reset()
    }

    private fun viewport(widthDp: Int) = ViewportScaler.updateViewportWidth(widthDp)

    private fun zoom(z: Float) = ZoomState.update(z)

    // -- Single-axis contract ------------------------------------------------

    /**
     * The core invariant of this pass: dp is scaled by the root zoom and nothing
     * else.
     *
     * If this ever becomes non-1f, the app has two scaling axes again and every
     * Material3 internal drifts out of proportion with our own components.
     */
    @Test
    fun `sdp is the identity transform at every viewport width`() {
        for (width in listOf(1, 320, 360, 411, 480, 600, 720, 802, 891, 1280, 2560)) {
            viewport(width)
            assertEquals(
                "dp must not be scaled per call site at ${width}dp -- the root " +
                    "zoom owns all dp scaling",
                1f, ViewportScaler.scaleFactor, 1e-6f
            )
        }
    }

    /** The literal must not drift either, independent of viewport width. */
    @Test
    fun `sdp returns its authored dp unchanged`() {
        viewport(802)
        assertEquals("140.sdp must be exactly 140dp", 140f, 140.sdp.value, 1e-6f)
        assertEquals("1.5f.sdp must be exactly 1.5dp", 1.5f, 1.5f.sdp.value, 1e-6f)
    }

    /** Before any composition runs, the engine must already be safe to read. */
    @Test
    fun `default state is uncompensated`() {
        assertEquals(
            "geometry objects are read from plain JUnit with no composition, so " +
                "the default zoom must be 1f",
            1f, ZoomState.zoom, 1e-6f
        )
        assertEquals(1f, ViewportScaler.scaleFactor, 1e-6f)
    }

    // -- The zoom itself -----------------------------------------------------

    /** Portrait must be untouched -- the zoom is scoped to landscape only. */
    @Test
    fun `portrait is not zoomed`() {
        assertEquals(
            "portrait layouts were already correct and must not be re-scaled",
            1f, UiScale.zoomFor(isLandscape = false), 1e-6f
        )
    }

    /**
     * The landscape value is pinned because it is the single number deciding how
     * large the whole app looks. A future edit that changes it silently changes
     * every screen, so it is asserted rather than left to code review.
     */
    @Test
    fun `landscape zoom is the pinned flat zoom-out`() {
        assertEquals(
            "the landscape zoom must stay at the flat 25% zoom-out",
            0.75f, UiScale.zoomFor(isLandscape = true), 1e-6f
        )
    }

    /** A zoom that shrank the UI to nothing would defeat the purpose. */
    @Test
    fun `zoom stays in a sane band`() {
        for (z in listOf(UiScale.LandscapeDensityScale, UiScale.PortraitDensityScale)) {
            assertTrue("a zoom of $z must keep the UI usable", z > 0.5f && z <= 2f)
        }
    }

    // -- Typography compensation --------------------------------------------

    /**
     * The floor must hold in **rendered** terms at the shipping zoom.
     *
     * `scaledFontSize` divides authored sizes by the zoom so they land back on
     * their intended rendered size. Without that division a 12sp style would
     * render at `12 x 0.75 = 9sp` in landscape and the floor would be violated
     * with nothing failing at build time -- the original "tiny text" regression.
     */
    @Test
    fun `rendered font size never falls below the legibility floor`() {
        val zooms = listOf(0.6f, 0.75f, 0.8f, 0.9f, 1.0f, 1.25f)
        for (z in zooms) {
            zoom(z)
            for (authoredSp in listOf(4f, 8f, 12f, 14f, 16f, 30f, 96f, 200f)) {
                val rendered = scaledFontSize(authoredSp) * z
                assertTrue(
                    "${authoredSp}sp authored at zoom $z renders at ${rendered}sp, " +
                        "below the ${UiScale.MinReadableFontSize}sp floor",
                    rendered >= UiScale.MinReadableFontSize - 1e-3f
                )
            }
        }
    }

    /**
     * The compensation must be exact, not approximate.
     *
     * Asserting the round-trip catches an off-by-a-clamp that would otherwise
     * look like slightly wrong type rather than an obvious failure.
     */
    @Test
    fun `font size compensation round-trips exactly`() {
        zoom(UiScale.LandscapeDensityScale)
        for (authoredSp in listOf(12f, 14f, 16f, 23f, 30f)) {
            val rendered = scaledFontSize(authoredSp) * UiScale.LandscapeDensityScale
            assertEquals(
                "${authoredSp}sp must render back at exactly itself after " +
                    "compensation, got $rendered",
                authoredSp, rendered, 0.01f
            )
        }
    }

    /** A degenerate zoom must not produce infinity or NaN. */
    @Test
    fun `zero zoom does not divide by zero`() {
        zoom(0f)
        val scaled = scaledFontSize(14f)
        assertTrue(
            "a zero zoom must degrade gracefully, produced $scaled",
            scaled.isFinite() && scaled > 0f
        )
    }

    /** The authored floor is what the compensation clamps to; it must track zoom. */
    @Test
    fun `authored floor compensates for the zoom`() {
        assertEquals(
            "at unity the authored floor is the rendered floor",
            UiScale.MinReadableFontSize,
            UiScale.authoredFloorFor(1f), 1e-4f
        )
        assertTrue(
            "a shrinking zoom must raise the authored floor above the rendered " +
                "one, was ${UiScale.authoredFloorFor(0.75f)}",
            UiScale.authoredFloorFor(0.75f) > UiScale.MinReadableFontSize
        )
    }

    // -- Touch targets -------------------------------------------------------

    /**
     * The 48dp accessibility floor must survive the zoom **on screen**.
     *
     * The single most important assertion in this file. The zoom shrinks every
     * dp in the app by 25%, so an uncompensated `48.dp` target would silently
     * become 36dp -- below the Android floor for an element the user has to hit
     * reliably, and impossible to spot by eye on a device.
     */
    @Test
    fun `touch targets stay at 48dp on screen under the zoom`() {
        for (z in listOf(UiScale.LandscapeDensityScale, UiScale.PortraitDensityScale)) {
            zoom(z)
            assertEquals(
                "TouchTargets.Min must render at exactly 48dp at zoom $z, " +
                    "authored ${TouchTargets.Min.value}dp",
                48f, TouchTargets.Min.value * z, 0.01f
            )
            assertTrue(
                "TouchTargets.Large must stay above Min at zoom $z",
                TouchTargets.Large.value * z > TouchTargets.Min.value * z
            )
            assertTrue(
                "TouchTargets.Primary must stay above Large at zoom $z",
                TouchTargets.Primary.value * z > TouchTargets.Large.value * z
            )
        }
    }

    /** In portrait the zoom is 1f, so touch targets collapse to their literals. */
    @Test
    fun `touch targets are uncompensated in portrait`() {
        ZoomState.reset()
        assertEquals("portrait Min", 48f, TouchTargets.Min.value, 0.01f)
        assertEquals("portrait Large", 56f, TouchTargets.Large.value, 0.01f)
        assertEquals("portrait Primary", 64f, TouchTargets.Primary.value, 0.01f)
    }
}
