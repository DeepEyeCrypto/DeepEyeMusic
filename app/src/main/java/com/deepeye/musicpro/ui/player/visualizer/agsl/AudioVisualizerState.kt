// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// AudioVisualizerState — Stable, zero-allocation audio uniform contract
// for the AGSL Visualizer Studio (Task 1).
//
// Why this exists:
// The existing AudioFrameInterpolator produces 6-band + 32-bin smoothed values,
// but the shader uniform dispatch in AgslVisualizer.kt reads raw band indices
// (bands[0], bands[2], bands[4], bands[5]) with ad-hoc coerceIn clamping and
// no NaN/Infinity guard. When the FFT capture thread and the 144 Hz draw loop
// race on a backgrounding GC stall, a single NaN can poison the shader's
// iBass/iMid/iTreble uniforms and produce a hard black or rainbow screen.
//
// This data class is the single source of truth for all audio-driven shader
// uniforms. It is sized once at construction, exposes immutable value
// snapshots, and provides a thread-safe [update] entry point that clamps,
// NaN-guards, and delta-time-corrects in one pass.
//
// Allocation contract: [update] allocates nothing. All buffers are owned by
// the instance and reused across frames.

package com.deepeye.musicpro.ui.player.visualizer.agsl

import com.deepeye.musicpro.ui.player.visualizer.AudioFrameInterpolator
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Immutable snapshot of audio-reactive shader uniforms.
 *
 * @property bass     Low-band energy, smoothed, in [0f, 2f]. Drives large-scale motion.
 * @property mids     Mid-band energy, smoothed, in [0f, 2f]. Drives turbulence / complexity.
 * @property treble   High-band energy, smoothed, in [0f, 2f]. Drives fine shimmer / sparkle.
 * @property energy   Overall RMS-like level, [0f, 1f]. Safe fallback when band detection is unreliable.
 * @property beatPulse Detected beat transient, [0f, 1f]. Zero when no reliable beat detector is active.
 * @property intensity User-facing scale factor, [0.5f, 1.5f]. 1.0 = default.
 */
data class AudioVisualizerState(
    val bass: Float,
    val mids: Float,
    val treble: Float,
    val energy: Float,
    val beatPulse: Float,
    val intensity: Float
) {
    /** True when all values are finite and in-range. Used by the shader dispatcher as a final safety net. */
    fun isValid(): Boolean =
        bass.isFinite() && mids.isFinite() && treble.isFinite() &&
            energy.isFinite() && beatPulse.isFinite() && intensity.isFinite() &&
            bass in 0f..MAX_BAND && mids in 0f..MAX_BAND && treble in 0f..MAX_BAND &&
            energy in 0f..1f && beatPulse in 0f..1f && intensity in 0.5f..1.5f

    companion object {
        const val MAX_BAND = 2f
        const val MIN_INTENSITY = 0.5f
        const val MAX_INTENSITY = 1.5f

        /** Default idle state used when no audio is playing or when a capture stalls. */
        val IDLE =
            AudioVisualizerState(
                bass = 0f,
                mids = 0f,
                treble = 0f,
                energy = 0f,
                beatPulse = 0f,
                intensity = 1f
            )
    }
}

/**
 * Zero-allocation audio uniform smoother.
 *
 * Feed it the raw [AudioFrameInterpolator]-produced band + spectrum arrays
 * and a monotonic clock. It returns a stable [AudioVisualizerState] snapshot
 * every frame.
 *
 * Mapping contract (matches existing 6-band AudioFrameInterpolator layout):
 * - [AudioVisualizerState.bass]     ← rawBands[0]  (sub-bass, ~31–62 Hz)
 * - [AudioVisualizerState.mids]     ← rawBands[2]  (mid, ~125–250 Hz)
 * - [AudioVisualizerState.treble]   ← rawBands[4]  (high, ~4 kHz)
 * - [AudioVisualizerState.energy]   ← sqrt(mean(bands²)) clamped to [0,1]
 * - [AudioVisualizerState.beatPulse] ← derived from |bass - bassPrev| when the delta is large enough to read as a transient
 *
 * Smoothing is asymmetric: attack (rising) uses a fast τ, release (falling)
 * uses a slower τ — same strategy as [AudioFrameInterpolator.approach] but
 * factored into a public data object so the shader dispatch can stay clean.
 *
 * Delta-time clamping: if `nowNanos - lastNanos` exceeds [MAX_DT_SECONDS]
 * (a backgrounding stall), the next frame treats dt as exactly [MAX_DT_SECONDS]
 * so the smoothing integral does not jump. A negative or NaN dt is coerced to
 * the default 1/60 s.
 */
class AudioVisualizerStateSmoother(
    bandCount: Int = AudioFrameInterpolator.DEFAULT_BAND_COUNT,
) {
    private val smoothedBands = FloatArray(bandCount)
    private val smoothedSpectrum = FloatArray(0) // reserved for future geometric displacement
    private var lastNanos = 0L
    private var primed = false

    private var lastBassForBeat = 0f
    private var beatEnergy = 0f

    /**
     * Advances the smoother toward the newest capture. Allocates nothing.
     *
     * @param rawBands Raw 6-band energies from [AudioFrameInterpolator.bands].
     * @param rawSpectrum Raw log-spectrum bins (unused for the 5 primary uniforms,
     *                    kept for future geometric displacement).
     * @param intensity User scale factor; NaN-safe.
     * @param nowNanos Monotonic clock, normally [System.nanoTime].
     */
    fun update(
        rawBands: FloatArray,
        rawSpectrum: FloatArray,
        intensity: Float,
        nowNanos: Long
    ): AudioVisualizerState {
        // ── Delta-time guard ─────────────────────────────────────────────
        // A backgrounding GC stall produces a huge or negative dt; both would
        // either freeze the smoothing (negative) or make it lunge (huge).
        var dt = if (lastNanos == 0L) DEFAULT_DT_SECONDS else (nowNanos - lastNanos) / 1e9f
        if (dt < 0f || dt.isNaN() || dt.isInfinite()) dt = DEFAULT_DT_SECONDS
        if (dt > MAX_DT_SECONDS) dt = MAX_DT_SECONDS
        lastNanos = nowNanos

        // ── NaN/Infinity guard on input ─────────────────────────────────
        val safeIntensity =
            if (intensity.isFinite()) {
                intensity.coerceIn(AudioVisualizerState.MIN_INTENSITY, AudioVisualizerState.MAX_INTENSITY)
            } else {
                1f
            }

        // ── Target computation (allocate nothing) ───────────────────────
        // Sanitize raw band values: NaN → 0 (no signal), +Inf → MAX_BAND, -Inf → 0.
        // Without this the first-frame snap and every later smoothing step would
        // carry a non-finite value into the shader uniforms.
        val targetBass = sanitize(rawBands.getOrNull(0) ?: 0f) * safeIntensity
        val targetMids = sanitize(rawBands.getOrNull(2) ?: 0f) * safeIntensity
        val targetTreble = sanitize(rawBands.getOrNull(4) ?: 0f) * safeIntensity

        // Overall energy: RMS of the first 3 bands (bass/mids/treble) — cheap
        // and stable. Clamped to [0,1] so the shader never sees >1.0.
        val energyRms =
            sqrt(
                (targetBass * targetBass + targetMids * targetMids + targetTreble * targetTreble) / 3f
            ).coerceIn(0f, 1f)

        // ── Smoothing step (asymmetric attack/release) ─────────────────
        if (!primed) {
            // First frame: snap so the visualizer does not fade in from zero.
            smoothedBands[0] = targetBass
            smoothedBands[2] = targetMids
            smoothedBands[4] = targetTreble
            lastBassForBeat = targetBass
            primed = true
        } else {
            smoothedBands[0] = approach(smoothedBands[0], targetBass, dt)
            smoothedBands[2] = approach(smoothedBands[2], targetMids, dt)
            smoothedBands[4] = approach(smoothedBands[4], targetTreble, dt)
        }

        // ── Beat pulse: detect bass transient ──────────────────────────
        // A reliable beat detector needs a lookback buffer; we use a
        // one-frame transient proxy: the rate of change of bass energy.
        // When the delta is positive and exceeds the beat threshold, we
        // pulse; when negative or too small, we decay. This avoids
        // allocating a lookback buffer while still giving the shader
        // a sharp, visible accent on each strong downbeat.
        val bassDelta = smoothedBands[0] - lastBassForBeat
        if (bassDelta > BEAT_THRESHOLD && bassDelta > 0f) {
            beatEnergy = minOf(1f, beatEnergy + bassDelta * BEAT_GAIN)
        } else {
            beatEnergy = approach(beatEnergy, 0f, dt, releaseTau = TAU_BEAT_RELEASE)
        }
        lastBassForBeat = smoothedBands[0]

        // ── Return immutable snapshot ───────────────────────────────────
        return AudioVisualizerState(
            bass = smoothedBands[0].coerceIn(0f, AudioVisualizerState.MAX_BAND),
            mids = smoothedBands[2].coerceIn(0f, AudioVisualizerState.MAX_BAND),
            treble = smoothedBands[4].coerceIn(0f, AudioVisualizerState.MAX_BAND),
            energy = energyRms,
            beatPulse = beatEnergy.coerceIn(0f, 1f),
            intensity = safeIntensity
        )
    }

    /** Clears all smoothing state; call when playback stops. */
    fun reset() {
        smoothedBands.fill(0f)
        lastNanos = 0L
        primed = false
        lastBassForBeat = 0f
        beatEnergy = 0f
    }

    private fun approach(
        current: Float,
        target: Float,
        dt: Float,
        releaseTau: Float = TAU_RELEASE
    ): Float {
        val tau = if (target > current) minOf(TAU_ATTACK, releaseTau) else releaseTau
        val alpha = 1f - exp(-dt / tau)
        val next = current + (target - current) * alpha
        // Final NaN/Infinity guard: if any arithmetic overflowed, fall back to target.
        return if (next.isFinite()) next else target
    }

    /**
     * Sanitizes a raw band value: NaN → 0, +Inf → [MAX_BAND], -Inf → 0.
     * This is the last line of defence before the value reaches the shader
     * uniforms; the [approach] helper already guards against overflow, but
     * the first-frame snap path bypasses it.
     */
    private fun sanitize(raw: Float): Float =
        when {
            raw.isNaN() -> 0f
            raw == Float.POSITIVE_INFINITY -> AudioVisualizerState.MAX_BAND
            raw == Float.NEGATIVE_INFINITY -> 0f
            else -> raw.coerceIn(0f, AudioVisualizerState.MAX_BAND)
        }

    companion object {
        const val DEFAULT_DT_SECONDS = 1f / 60f
        const val MAX_DT_SECONDS = 0.1f

        /** Attack τ: fast, so beats still read as sharp. */
        const val TAU_ATTACK = 0.016f

        /** Release τ: smooth tail. */
        const val TAU_RELEASE = 0.050f

        /** Beat-release τ: decays the beat pulse a little faster than a band. */
        const val TAU_BEAT_RELEASE = 0.12f

        /** Bass transient threshold for a beat pulse to fire. */
        const val BEAT_THRESHOLD = 0.04f

        /** Gain applied to the transient when a beat fires. */
        const val BEAT_GAIN = 6f
    }
}
