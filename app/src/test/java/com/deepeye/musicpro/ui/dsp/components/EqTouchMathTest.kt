// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pointer arithmetic of the 10-band EQ fader bank.
 *
 * Each test maps directly to a defect class observed in production:
 *  * stale-closure / px-vs-dp confusion in the old per-band fader Boxes,
 *  * fat-finger misses caused by 28dp targets with dead gaps between them,
 *  * degenerate/unlaid-out geometry producing NaN or out-of-range gains.
 */
class EqTouchMathTest {

    // ── band index resolution (ACTION_DOWN hit test) ──────────────────────────

    @Test
    fun `index divides surface width into ten contiguous slots`() {
        val width = 1000f
        for (i in 0 until 10) {
            val centerX = width * (i + 0.5f) / 10f
            assertEquals(
                "center of slot $i must resolve to band $i",
                i,
                EqTouchMath.bandIndexForTouchX(centerX, width, 10),
            )
        }
    }

    @Test
    fun `every x across the surface maps to exactly one band with no dead gap`() {
        val width = 1000f
        var x = 0f
        while (x < width) {
            val index = EqTouchMath.bandIndexForTouchX(x, width, 10)
            assertTrue("no dead gap at x=$x", index != null)
            assertTrue("index in range at x=$x", index!! in 0..9)
            x += 0.5f
        }
    }

    @Test
    fun `fat finger offset still lands on the intended band`() {
        // 1000px wide => 100px slots. A finger landing 40px late must still
        // resolve to the same band rather than hitting a neighbour.
        val width = 1000f
        val intended = 3
        val slotCenter = width * (intended + 0.5f) / 10f
        val sloppyTouch = slotCenter + 40f
        assertEquals(
            "a 40px fat-finger overshoot must not hand control to a neighbour",
            intended,
            EqTouchMath.bandIndexForTouchX(sloppyTouch, width, 10),
        )
    }

    @Test
    fun `out of range x is clamped to the outer bands instead of crashing`() {
        assertEquals(0, EqTouchMath.bandIndexForTouchX(-500f, 1000f, 10))
        assertEquals(9, EqTouchMath.bandIndexForTouchX(9999f, 1000f, 10))
    }

    @Test
    fun `degenerate geometry returns null so the gesture is ignored`() {
        assertNull(EqTouchMath.bandIndexForTouchX(100f, 0f, 10))
        assertNull(EqTouchMath.bandIndexForTouchX(100f, 1000f, 0))
        assertNull(EqTouchMath.bandIndexForTouchX(Float.NaN, 1000f, 10))
    }

    // ── Y to gain mapping (drag + tap-to-set) ─────────────────────────────────

    @Test
    fun `track top maps to max boost and bottom maps to max cut`() {
        val top = 500f
        val height = 400f
        assertEquals(
            EQ_MAX_DB,
            EqTouchMath.gainForTouchY(top, top, height)!!,
            0.001f,
        )
        assertEquals(
            EQ_MIN_DB,
            EqTouchMath.gainForTouchY(top + height, top, height)!!,
            0.001f,
        )
    }

    @Test
    fun `track midpoint maps to unity gain`() {
        assertEquals(
            0f,
            EqTouchMath.gainForTouchY(700f, 500f, 400f)!!,
            0.001f,
        )
    }

    @Test
    fun `absolute mapping is stable across repeated samples at the same y`() {
        // Guards the delta-accumulation defect: the same absolute Y must always
        // yield the same gain regardless of how the drag was sampled.
        val samples = (0..40).map { 600f + it }
        val gains = samples.map { EqTouchMath.gainForTouchY(it, 500f, 400f) }
        assertEquals("mapping must be deterministic", gains.distinct().size, 41)
        assertEquals(
            gains,
            samples.map { EqTouchMath.gainForTouchY(it, 500f, 400f) },
        )
    }

    @Test
    fun `dense pixel sweep stays strictly inside the dB range`() {
        val top = 500f
        val height = 400f
        var y = top - 200f // overshoot above the track
        while (y <= top + height + 200f) {
            val gain = EqTouchMath.gainForTouchY(y, top, height)!!
            assertTrue("gain $gain out of range at y=$y", gain >= EQ_MIN_DB)
            assertTrue("gain $gain out of range at y=$y", gain <= EQ_MAX_DB)
            y += 0.5f
        }
    }

    @Test
    fun `gain mapping handles real device pixel scales identically to dp`() {
        // Regression for the px/dp defect: 120dp at density 3 is 360px. A tap at
        // the exact top of the track must read +12dB, not a clamped minimum.
        val dpTrackHeight = 120f
        listOf(1f, 2f, 2.75f, 3f, 4f).forEach { density ->
            val trackHeightPx = dpTrackHeight * density
            val topPx = 1000f
            assertEquals(
                "density $density: top of track must be +12dB",
                EQ_MAX_DB,
                EqTouchMath.gainForTouchY(topPx, topPx, trackHeightPx)!!,
                0.001f,
            )
            assertEquals(
                "density $density: bottom of track must be -12dB",
                EQ_MIN_DB,
                EqTouchMath.gainForTouchY(topPx + trackHeightPx, topPx, trackHeightPx)!!,
                0.001f,
            )
            assertEquals(
                "density $density: midpoint must be 0dB",
                0f,
                EqTouchMath.gainForTouchY(
                    topPx + trackHeightPx / 2f,
                    topPx,
                    trackHeightPx,
                )!!,
                0.001f,
            )
        }
    }

    @Test
    fun `zero height track returns null instead of dividing by zero`() {
        assertNull(EqTouchMath.gainForTouchY(100f, 100f, 0f))
        assertNull(EqTouchMath.gainForTouchY(Float.NaN, 100f, 400f))
    }

    // ── touch zone ownership (parent scroll interception) ─────────────────────

    @Test
    fun `touch zone covers the track plus the fat finger overshoot`() {
        val top = 500f
        val height = 400f
        val overshoot = 48f
        assertTrue(EqTouchMath.isWithinTouchZone(top, top, height, overshoot))
        assertTrue(EqTouchMath.isWithinTouchZone(top + height, top, height, overshoot))
        assertTrue(
            EqTouchMath.isWithinTouchZone(top - overshoot, top, height, overshoot)
        )
        assertTrue(
            EqTouchMath.isWithinTouchZone(top + height + overshoot, top, height, overshoot)
        )
    }

    @Test
    fun `touch in the readout or label gutter is left to the parent scroller`() {
        val top = 500f
        val height = 400f
        val overshoot = 48f
        assertFalse(
            EqTouchMath.isWithinTouchZone(top - overshoot - 1f, top, height, overshoot)
        )
        assertFalse(
            EqTouchMath.isWithinTouchZone(
                top + height + overshoot + 1f,
                top,
                height,
                overshoot,
            )
        )
    }

    @Test
    fun `unlaid out track never claims ownership of a touch`() {
        assertFalse(EqTouchMath.isWithinTouchZone(100f, 100f, 0f, 48f))
    }

    // ── magnetic detent (snap to unity) ────────────────────────────────────────

    @Test
    fun `gains inside the detent collapse to exact zero`() {
        // The 1dB round-trip defect: a drag released just off unity used to rest at
        // a fractional gain that read out as a stray -1 / +1.
        listOf(0f, 0.4f, -0.4f, 0.999f, -0.999f, 1.0f, -1.0f).forEach { gain ->
            assertEquals(
                "gain $gain must snap to exact zero",
                0f,
                EqTouchMath.applyMagneticDetent(gain),
                0.0001f,
            )
        }
    }

    @Test
    fun `gains outside the detent are left untouched`() {
        listOf(1.0001f, -1.0001f, 4f, -7.5f, EQ_MAX_DB, EQ_MIN_DB).forEach { gain ->
            assertEquals(
                "gain $gain is outside the detent and must pass through",
                gain,
                EqTouchMath.applyMagneticDetent(gain),
                0.0001f,
            )
        }
    }

    @Test
    fun `detent is symmetric about zero`() {
        var g = 0f
        while (g <= EQ_MAX_DB) {
            assertEquals(
                EqTouchMath.applyMagneticDetent(g),
                -EqTouchMath.applyMagneticDetent(-g),
                0.0001f,
            )
            g += 0.05f
        }
    }

    @Test
    fun `detent never pushes a gain outside the dB range`() {
        var g = EQ_MIN_DB
        while (g <= EQ_MAX_DB) {
            val snapped = EqTouchMath.applyMagneticDetent(g)
            assertTrue("snapped $snapped out of range at $g", snapped >= EQ_MIN_DB)
            assertTrue("snapped $snapped out of range at $g", snapped <= EQ_MAX_DB)
            g += 0.05f
        }
    }

    // ── pannable strip: minimum slot width ───────────────────────────────────
    // Defect class: in the landscape DSP grid the card is only ~145dp wide, so
    // `viewportWidth / bandCount` collapsed the ten slots to ~10.5dp each — a
    // 4.5x violation of the 48dp fat-finger floor.

    @Test
    fun `ten bands always reserve at least the 48dp minimum slot width`() {
        // 48dp at the measured device density of 2.0 => 96px.
        val minSlotPx = 96f
        val content = EqTouchMath.requiredContentWidth(10, minSlotPx)
        assertEquals("ten 48dp slots need 960px of content", 960f, content, 0.001f)
        assertTrue("every slot must be at least 48dp", content / 10f >= minSlotPx)
    }

    @Test
    fun `minimum slot width is honoured at every real device density`() {
        listOf(1f, 2f, 2.75f, 3f, 4f).forEach { density ->
            val minSlotPx = 48f * density
            val content = EqTouchMath.requiredContentWidth(10, minSlotPx)
            assertEquals(
                "density $density: every slot must be >= 48dp",
                minSlotPx,
                content / 10f,
                0.001f,
            )
        }
    }

    @Test
    fun `degenerate slot or band counts reserve no content width`() {
        assertEquals(0f, EqTouchMath.requiredContentWidth(0, 96f), 0.001f)
        assertEquals(0f, EqTouchMath.requiredContentWidth(-3, 96f), 0.001f)
        assertEquals(0f, EqTouchMath.requiredContentWidth(10, 0f), 0.001f)
        assertEquals(0f, EqTouchMath.requiredContentWidth(10, -5f), 0.001f)
        assertEquals(0f, EqTouchMath.requiredContentWidth(10, Float.NaN), 0.001f)
    }

    // ── pannable strip: pan bounds ───────────────────────────────────────────

    @Test
    fun `a narrow card yields exactly the overflow as legal travel`() {
        // 210px usable viewport, 960px of content => 750px of pan.
        assertEquals(750f, EqTouchMath.maxScrollOffset(960f, 210f), 0.001f)
    }

    @Test
    fun `a wide card has no travel so panning stays inert`() {
        assertEquals(0f, EqTouchMath.maxScrollOffset(960f, 1604f), 0.001f)
        assertEquals(
            "an exactly-fitting strip must not pan at all",
            0f,
            EqTouchMath.maxScrollOffset(960f, 960f),
            0.001f,
        )
    }

    @Test
    fun `pan is clamped to the legal range at both ends`() {
        assertEquals(
            "must not pan before the start of the strip",
            0f,
            EqTouchMath.clampScrollOffset(-400f, 960f, 210f),
            0.001f,
        )
        assertEquals(
            "must not pan past the end of the strip",
            750f,
            EqTouchMath.clampScrollOffset(9999f, 960f, 210f),
            0.001f,
        )
        assertEquals(300f, EqTouchMath.clampScrollOffset(300f, 960f, 210f), 0.001f)
    }

    @Test
    fun `degenerate pan geometry is inert rather than crashing`() {
        assertEquals(0f, EqTouchMath.maxScrollOffset(Float.NaN, 210f), 0.001f)
        assertEquals(0f, EqTouchMath.maxScrollOffset(960f, Float.NaN), 0.001f)
        assertEquals(0f, EqTouchMath.clampScrollOffset(Float.NaN, 960f, 210f), 0.001f)
    }

    // ── pannable strip: drawing and hit testing agree ─────────────────────────

    @Test
    fun `panned slot centres shift left by exactly the offset`() {
        val content = 960f
        for (i in 0 until 10) {
            val unpanned = EqTouchMath.slotCenterX(i, content, 10)!!
            val panned = EqTouchMath.slotCenterX(i, content, 10, 300f)!!
            assertEquals(
                "slot $i must shift by exactly the pan offset",
                unpanned - 300f,
                panned,
                0.001f,
            )
        }
    }

    @Test
    fun `the curve and the fader bank cannot drift apart when panned`() {
        // Both draw through the same function, so for every band the drawn centre
        // must equal the centre the hit test resolves to.
        val content = 960f
        for (offset in listOf(0f, 137f, 500f, 750f)) {
            for (i in 0 until 10) {
                val drawn = EqTouchMath.slotCenterX(i, content, 10, offset)!!
                val hit = EqTouchMath.bandIndexForTouchX(drawn, offset, content, 10)
                assertEquals(
                    "offset $offset: the drawn centre of band $i must hit band $i",
                    i,
                    hit,
                )
            }
        }
    }

    @Test
    fun `hit testing follows the pan so the band under the finger changes`() {
        val content = 960f
        // At offset 0 the left edge of the viewport shows band 0.
        assertEquals(0, EqTouchMath.bandIndexForTouchX(10f, 0f, content, 10))
        // After panning one slot left, that same screen X now shows band 1.
        assertEquals(1, EqTouchMath.bandIndexForTouchX(10f, 96f, content, 10))
        // And the last band is reachable at full pan.
        assertEquals(9, EqTouchMath.bandIndexForTouchX(205f, 750f, content, 10))
    }

    @Test
    fun `a panned strip never leaves a dead gap in the viewport`() {
        val content = 960f
        val viewport = 210f
        val maxOffset = EqTouchMath.maxScrollOffset(content, viewport)
        var offset = 0f
        while (offset <= maxOffset) {
            var x = 0f
            while (x < viewport) {
                assertTrue(
                    "dead gap at x=$x offset=$offset",
                    EqTouchMath.bandIndexForTouchX(x, offset, content, 10) != null,
                )
                x += 0.5f
            }
            offset += 7f
        }
    }

    @Test
    fun `panned slot centre is null for degenerate geometry instead of NaN`() {
        assertNull(EqTouchMath.slotCenterX(0, 0f, 10, 0f))
        assertNull(EqTouchMath.slotCenterX(0, 960f, 0, 0f))
        assertNull(EqTouchMath.slotCenterX(0, 960f, 10, Float.NaN))
    }

    @Test
    fun `offset zero is identical to the unpanned helper`() {
        for (i in 0 until 10) {
            assertEquals(
                EqTouchMath.slotCenterX(i, 960f, 10)!!,
                EqTouchMath.slotCenterX(i, 960f, 10, 0f)!!,
                0.001f,
            )
        }
    }

    // ── slot centre geometry (curve grid ⇄ fader alignment) ─────────────────────

    @Test
    fun `slot centre is the midpoint of its contiguous slot`() {
        val width = 1000f
        val slotWidth = width / 10f
        for (i in 0 until 10) {
            assertEquals(
                "grid line for band $i must hit the centre of its slot",
                slotWidth * i + slotWidth / 2f,
                EqTouchMath.slotCenterX(i, width, 10)!!,
                0.001f,
            )
        }
    }

    @Test
    fun `each slot centre resolves back to its own band on touch`() {
        // This is the alignment guarantee: whatever X the grid draws at, a touch
        // there must land on the same band, otherwise the label and the fader lie.
        val width = 1000f
        for (i in 0 until 10) {
            val centerX = EqTouchMath.slotCenterX(i, width, 10)!!
            assertEquals(
                "grid line at $centerX must be touchable as band $i",
                i,
                EqTouchMath.bandIndexForTouchX(centerX, width, 10),
            )
        }
    }

    @Test
    fun `slot centres are strictly increasing and inside the surface`() {
        val width = 987f
        var previous = Float.NEGATIVE_INFINITY
        for (i in 0 until 10) {
            val centerX = EqTouchMath.slotCenterX(i, width, 10)!!
            assertTrue("centres must increase at $i", centerX > previous)
            assertTrue("centre $centerX escaped the surface", centerX in 0f..width)
            previous = centerX
        }
    }

    @Test
    fun `slot centre rejects degenerate geometry`() {
        assertNull(EqTouchMath.slotCenterX(0, 0f, 10))
        assertNull(EqTouchMath.slotCenterX(0, 1000f, 0))
        assertNull(EqTouchMath.slotCenterX(0, Float.NaN, 10))
    }
}
