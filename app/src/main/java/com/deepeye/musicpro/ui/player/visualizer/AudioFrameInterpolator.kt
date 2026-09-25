// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// AudioFrameInterpolator — UI-thread band interpolation
//
// Why this exists (see docs/deepeye_triangle_visualizer_runtime_report.md):
// the audio FFT is delivered at ~19 Hz, while the UI thread draws at 48–60 fps.
// Reading the engine's band arrays directly in the draw loop makes every value
// *step* roughly every 3rd frame, which reads as a stutter even though the
// frame budget is met.
//
// This class advances an exponentially-smoothed copy toward the newest sample
// using a delta-time-corrected coefficient, so the visual output is continuous
// at draw rate regardless of capture rate. Asymmetric attack/release keeps
// transients punchy while decays stay smooth.
//
// Allocation contract: update() and its helpers allocate nothing. All buffers
// are sized once at construction.

package com.deepeye.musicpro.ui.player.visualizer

import kotlin.math.exp

class AudioFrameInterpolator(
    bandCount: Int = DEFAULT_BAND_COUNT,
    spectrumSize: Int = DEFAULT_SPECTRUM_SIZE
) {
    /** Smoothed band energies, indexed [0]=Bass .. [5]=Peak. Safe to read in draw loops. */
    val bands = FloatArray(bandCount)

    /** Smoothed log-spectrum bins, used for geometric vertex displacement. */
    val spectrum = FloatArray(spectrumSize)

    private val targetBands = FloatArray(bandCount)
    private val targetSpectrum = FloatArray(spectrumSize)
    private var lastNanos = 0L
    private var primed = false

    /**
     * Advances the smoothed state toward the latest capture.
     *
     * @param intensity user-facing scale factor (0.5..1.5)
     * @param reducedMotion lengthens the time constants, damping fast motion
     * @param nowNanos monotonic clock, normally [System.nanoTime]
     */
    fun update(
        rawBands: FloatArray,
        rawSpectrum: FloatArray,
        intensity: Float,
        reducedMotion: Boolean,
        nowNanos: Long
    ) {
        // A negative delta means the clock went backwards (never expected, but a
        // NaN here would poison every band permanently). Guard it.
        var dt = if (lastNanos == 0L) DEFAULT_DT_SECONDS else (nowNanos - lastNanos) / 1e9f
        if (dt < 0f || dt.isNaN()) dt = 0f
        // Clamp so a long stall (GC, app backgrounded) does not produce a
        // single huge jump that reads as a glitch.
        if (dt > MAX_DT_SECONDS) dt = MAX_DT_SECONDS
        lastNanos = nowNanos

        val scale = if (intensity.isNaN()) 1f else intensity
        for (i in bands.indices) {
            targetBands[i] = (if (i < rawBands.size) rawBands[i] else 0f) * scale
        }
        for (i in spectrum.indices) {
            targetSpectrum[i] = (if (i < rawSpectrum.size) rawSpectrum[i] else 0f) * scale
        }

        if (!primed) {
            // First frame: snap to target instead of fading in from zero.
            for (i in bands.indices) bands[i] = targetBands[i]
            for (i in spectrum.indices) spectrum[i] = targetSpectrum[i]
            primed = true
            return
        }

        val tauBase = if (reducedMotion) TAU_REDUCED else TAU_NORMAL
        for (i in bands.indices) {
            bands[i] = approach(bands[i], targetBands[i], tauBase, dt)
        }
        for (i in spectrum.indices) {
            spectrum[i] = approach(spectrum[i], targetSpectrum[i], tauBase, dt)
        }
    }

    /** Clears smoothing state; call when playback stops so the next start is not a lerp from stale values. */
    fun reset() {
        primed = false
        lastNanos = 0L
        bands.fill(0f)
        spectrum.fill(0f)
    }

    companion object {
        const val DEFAULT_BAND_COUNT = 6
        const val DEFAULT_SPECTRUM_SIZE = 32

        /** Release time constant at normal motion. */
        const val TAU_NORMAL = 0.050f

        /** Release time constant under reduced-motion: ~3x slower, visibly calmer. */
        const val TAU_REDUCED = 0.150f

        /** Attack is faster than release so beats still read as sharp. */
        const val TAU_ATTACK = 0.016f

        private const val DEFAULT_DT_SECONDS = 1f / 60f
        private const val MAX_DT_SECONDS = 0.1f

        /**
         * One exponential smoothing step. Rising values use [TAU_ATTACK] (punchy),
         * falling values use the caller's release constant (smooth tail).
         */
        private fun approach(current: Float, target: Float, tauRelease: Float, dt: Float): Float {
            val tau = if (target > current) minOf(TAU_ATTACK, tauRelease) else tauRelease
            val alpha = 1f - exp(-dt / tau)
            return current + (target - current) * alpha
        }
    }
}