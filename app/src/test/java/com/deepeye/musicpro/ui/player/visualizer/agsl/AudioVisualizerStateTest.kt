// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// AudioVisualizerStateTest — TDD tests for the audio uniform contract
// (Task 1). Covers band mapping, exponential smoothing, delta-time
// clamping, NaN/Infinity safety, and beat-pulse derivation.
//
// All tests are pure JVM — no Android framework needed.

package com.deepeye.musicpro.ui.player.visualizer.agsl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioVisualizerStateTest {

    private companion object {
        /** 1 frame at 60 Hz in nanoseconds. */
        const val FRAME_NANOS = 16_666_667L
    }

    // ── Band mapping ────────────────────────────────────────────────────

    @Test
    fun `first frame snaps to target instead of fading from zero`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 0.5f }
        val state = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 1_000_000_000L)
        assertEquals(0.5f, state.bass, 0.001f)
        assertEquals(0.5f, state.mids, 0.001f)
        assertEquals(0.5f, state.treble, 0.001f)
    }

    @Test
    fun `band mapping uses bands 0, 2 and 4 for bass mids treble`() {
        val smoother = AudioVisualizerStateSmoother()
        // Feed distinct values so mapping is unambiguous.
        val rawBands = floatArrayOf(0.1f, 0.9f, 0.3f, 0.9f, 0.7f, 0.5f)
        // First frame snaps.
        val state = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 1_000_000_000L)
        assertEquals(0.1f, state.bass, 0.001f)
        assertEquals(0.3f, state.mids, 0.001f)
        assertEquals(0.7f, state.treble, 0.001f)
    }

    @Test
    fun `short band array falls back to zero without crashing`() {
        val smoother = AudioVisualizerStateSmoother()
        val state = smoother.update(FloatArray(2) { 0.5f }, FloatArray(0), 1f, nowNanos = 1L)
        // Only bands[0] exists: bass is mapped, mids/treble default to 0.
        assertEquals(0.5f, state.bass, 0.001f)
        assertEquals(0f, state.mids, 0.001f)
        assertEquals(0f, state.treble, 0.001f)
    }

    // ── Exponential smoothing / continuity ─────────────────────────────

    @Test
    fun `a stepped input produces a monotonic ramp not a staircase`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6)

        // Prime with a high value.
        rawBands.fill(1.0f)
        smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 0L)

        // Now step to zero: the smoother must decay gradually, not snap.
        rawBands.fill(0f)
        val after1 = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = FRAME_NANOS)
        val after2 = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = FRAME_NANOS * 2)
        val after3 = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = FRAME_NANOS * 3)

        // Monotonically decreasing, strictly positive until near-zero.
        assertTrue("frame1 (${after1.bass}) must be < 1.0 (decaying)", after1.bass < 1.0f)
        assertTrue("frame2 (${after2.bass}) must be < frame1 (${after1.bass})", after2.bass < after1.bass)
        assertTrue("frame3 (${after3.bass}) must be < frame2 (${after2.bass})", after3.bass < after2.bass)
        assertTrue("frame3 (${after3.bass}) must still be > 0 (not a hard cut)", after3.bass > 0f)
    }

    @Test
    fun `attack is faster than release so transients stay punchy`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 0f }

        // Prime at zero.
        var t = 0L
        repeat(10) {
            t += FRAME_NANOS
            smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        }

        // Jump the target to max: attack (rising) must reach a high fraction
        // of the target within a single frame (τ_attack = 16 ms ≈ one frame).
        rawBands.fill(1.0f)
        t += FRAME_NANOS
        val spike = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        assertTrue(
            "attack frame (${spike.bass}) should be well up the curve (>0.5)",
            spike.bass > 0.5f
        )
    }

    // ── Delta-time clamping ─────────────────────────────────────────────

    @Test
    fun `a backgrounding stall clamps dt so values do not lunge`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 1f }

        // Prime at 1.0.
        smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 0L)

        // Simulate a 5-second backgrounding stall, then return with the target
        // at zero. Without clamping, dt=5.0s would let the value lunge to ~0
        // in one step (exp(-5/0.05) ≈ 0). With MAX_DT = 0.1s the decay is
        // bounded: alpha = 1 - exp(-0.1/0.05) ≈ 0.86, so value ≈ 0.14.
        rawBands.fill(0f)
        val afterStall = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 5_000_000_000L)
        assertTrue(
            "stall-clamped decay (${afterStall.bass}) must retain some level (>0.05)",
            afterStall.bass > 0.05f
        )
    }

    @Test
    fun `a backwards clock does not produce NaN`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 0.5f }
        smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 2_000_000_000L)
        // Now feed an EARLIER timestamp — dt would be negative.
        val state = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 1_000_000_000L)
        assertTrue("bass (${state.bass}) must be finite after a backwards clock", state.bass.isFinite())
        assertTrue("mids (${state.mids}) must be finite after a backwards clock", state.mids.isFinite())
        assertTrue("state.isValid() must hold", state.isValid())
    }

    // ── NaN / Infinity safety ──────────────────────────────────────────

    @Test
    fun `NaN band input is coerced to zero not propagated`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6)
        rawBands[0] = Float.NaN
        rawBands[2] = Float.POSITIVE_INFINITY
        rawBands[4] = Float.NaN
        val state = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = 1L)
        // First frame snaps — but the snap must not carry NaN/Infinity.
        assertTrue("bass from NaN (${state.bass}) must be finite", state.bass.isFinite())
        assertTrue("mids from +Inf (${state.mids}) must be finite and clamped", state.mids.isFinite() && state.mids <= AudioVisualizerState.MAX_BAND)
        assertTrue("treble from NaN (${state.treble}) must be finite", state.treble.isFinite())
    }

    @Test
    fun `NaN intensity falls back to one point zero`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 0.5f }
        val state = smoother.update(rawBands, FloatArray(0), intensity = Float.NaN, nowNanos = 1L)
        assertEquals(1f, state.intensity, 0f)
    }

    @Test
    fun `extreme intensity is clamped to the safe range`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 0.5f }
        val state = smoother.update(rawBands, FloatArray(0), intensity = 99f, nowNanos = 1L)
        assertEquals(AudioVisualizerState.MAX_INTENSITY, state.intensity, 0f)
    }

    // ── Beat pulse ──────────────────────────────────────────────────────

    @Test
    fun `a strong bass transient fires a beat pulse then decays`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 0f }

        // Prime at quiet.
        var t = 0L
        repeat(5) {
            t += FRAME_NANOS
            smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        }
        // Now fire a big transient: target bass jumps to 2.0.
        rawBands.fill(2f)
        t += FRAME_NANOS
        val hit = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        assertTrue(
            "beat pulse (${hit.beatPulse}) should fire on a strong transient (>0.1)",
            hit.beatPulse > 0.1f
        )

        // Then decay with quiet input: pulse must fall back toward zero.
        rawBands.fill(0f)
        val decayed = (1..30).fold(hit) { acc, i ->
            t += FRAME_NANOS
            smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        }
        assertTrue(
            "beat pulse (${decayed.beatPulse}) must decay below the hit (${hit.beatPulse})",
            decayed.beatPulse < hit.beatPulse
        )
    }

    @Test
    fun `idle state is valid and all-zero`() {
        val idle = AudioVisualizerState.IDLE
        assertTrue(idle.isValid())
        assertEquals(0f, idle.bass, 0f)
        assertEquals(0f, idle.energy, 0f)
        assertEquals(0f, idle.beatPulse, 0f)
        assertEquals(1f, idle.intensity, 0f)
    }

    // ── reset ───────────────────────────────────────────────────────────

    @Test
    fun `reset clears smoothing state so the next start is not a lerp from stale values`() {
        val smoother = AudioVisualizerStateSmoother()
        val rawBands = FloatArray(6) { 1f }
        var t = 0L
        repeat(20) {
            t += FRAME_NANOS
            smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        }
        smoother.reset()
        rawBands.fill(0f)
        t += FRAME_NANOS
        val state = smoother.update(rawBands, FloatArray(0), 1f, nowNanos = t)
        // After reset the first frame snaps to the new (zero) target.
        assertEquals(0f, state.bass, 0.001f)
    }
}
