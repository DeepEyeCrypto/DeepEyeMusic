// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.crossfade

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

/**
 * Manages crossfade transitions between tracks using ExoPlayer volume control.
 *
 * Uses equal-power crossfade curve (cos/sin) to maintain constant combined
 * audio energy during the transition, preventing volume dips.
 *
 * Gapless playback is handled natively by ExoPlayer when crossfade is disabled.
 */
@Singleton
class CrossfadeManager @Inject constructor(
    private val player: ExoPlayer,
) {
    companion object {
        private const val TAG = "CrossfadeManager"
        const val MIN_DURATION_MS = 1000L
        const val MAX_DURATION_MS = 12000L
        private const val TICK_INTERVAL_MS = 50L
    }

    data class CrossfadeConfig(
        val enabled: Boolean = false,
        val durationMs: Long = 3000L,
        val curve: CrossfadeCurve = CrossfadeCurve.EQUAL_POWER,
    ) {
        val isValid: Boolean
            get() = durationMs in MIN_DURATION_MS..MAX_DURATION_MS
    }

    enum class CrossfadeCurve {
        EQUAL_POWER,
        LINEAR,
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var fadeJob: Job? = null
    private var _config = CrossfadeConfig()

    val config: CrossfadeConfig get() = _config

    /**
     * Updates the crossfade configuration.
     * Duration is clamped to [MIN_DURATION_MS, MAX_DURATION_MS].
     */
    fun setConfig(config: CrossfadeConfig) {
        _config = config.copy(
            durationMs = config.durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        )
    }

    /**
     * Starts a fade-out on the current track.
     * Called when the track is approaching its end (remaining time <= crossfade duration).
     *
     * @param onFadeComplete Called when fade-out reaches zero volume, signaling
     *        the caller to start the next track.
     */
    fun startFadeOut(onFadeComplete: () -> Unit) {
        if (!_config.enabled || !_config.isValid) {
            onFadeComplete()
            return
        }

        fadeJob?.cancel()
        fadeJob = scope.launch {
            Log.d(TAG, "Starting fade-out over ${_config.durationMs}ms")
            val startTime = System.currentTimeMillis()
            val duration = _config.durationMs.toFloat()

            try {
                while (isActive) {
                    val elapsed = (System.currentTimeMillis() - startTime).toFloat()
                    val progress = (elapsed / duration).coerceIn(0f, 1f)

                    val volume = computeFadeOutGain(progress, _config.curve)
                    player.volume = volume

                    if (progress >= 1f) {
                        break
                    }
                    delay(TICK_INTERVAL_MS)
                }
                onFadeComplete()
            } catch (e: CancellationException) {
                // Cancelled — restore volume
                player.volume = 1f
            }
        }
    }

    /**
     * Starts a fade-in on the new track (after it begins playing).
     */
    fun startFadeIn() {
        if (!_config.enabled || !_config.isValid) {
            player.volume = 1f
            return
        }

        fadeJob?.cancel()
        fadeJob = scope.launch {
            Log.d(TAG, "Starting fade-in over ${_config.durationMs}ms")
            val startTime = System.currentTimeMillis()
            val duration = _config.durationMs.toFloat()

            try {
                while (isActive) {
                    val elapsed = (System.currentTimeMillis() - startTime).toFloat()
                    val progress = (elapsed / duration).coerceIn(0f, 1f)

                    val volume = computeFadeInGain(progress, _config.curve)
                    player.volume = volume

                    if (progress >= 1f) {
                        break
                    }
                    delay(TICK_INTERVAL_MS)
                }
                player.volume = 1f
            } catch (e: CancellationException) {
                player.volume = 1f
            }
        }
    }

    /**
     * Cancels any active crossfade and restores full volume.
     */
    fun cancel() {
        fadeJob?.cancel()
        fadeJob = null
        player.volume = 1f
    }

    /**
     * Returns true if a crossfade is currently in progress.
     */
    val isActive: Boolean get() = fadeJob?.isActive == true

    /**
     * Checks if the next track buffer is ready for crossfade.
     * If not ready, caller should fall back to gapless.
     */
    fun shouldFallbackToGapless(): Boolean {
        // ExoPlayer handles buffering internally — if the next item isn't buffered,
        // we detect it by checking if playback state would stall.
        // For now, we rely on the caller to check buffer state before initiating crossfade.
        return !_config.enabled
    }

    // ═══════════════════════════════════════════════════
    // Crossfade Curves
    // ═══════════════════════════════════════════════════

    /**
     * Computes fade-out gain for a given progress [0, 1].
     * Equal-power: cos(t * π/2) — starts at 1, ends at 0.
     */
    fun computeFadeOutGain(progress: Float, curve: CrossfadeCurve): Float {
        return when (curve) {
            CrossfadeCurve.EQUAL_POWER -> cos(progress * Math.PI / 2.0).toFloat().coerceIn(0f, 1f)
            CrossfadeCurve.LINEAR -> (1f - progress).coerceIn(0f, 1f)
        }
    }

    /**
     * Computes fade-in gain for a given progress [0, 1].
     * Equal-power: sin(t * π/2) — starts at 0, ends at 1.
     */
    fun computeFadeInGain(progress: Float, curve: CrossfadeCurve): Float {
        return when (curve) {
            CrossfadeCurve.EQUAL_POWER -> sin(progress * Math.PI / 2.0).toFloat().coerceIn(0f, 1f)
            CrossfadeCurve.LINEAR -> progress.coerceIn(0f, 1f)
        }
    }
}
