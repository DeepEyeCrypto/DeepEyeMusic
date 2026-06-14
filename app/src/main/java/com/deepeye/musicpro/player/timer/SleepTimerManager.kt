// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.timer

import android.util.Log
import com.deepeye.musicpro.player.controller.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium sleep timer with linear volume fade in the final 30 seconds.
 *
 * Duration options: 15, 30, 45, 60, 90 minutes, or end of current track.
 * When the timer expires, volume fades linearly to zero over 30s, then pauses.
 * Cancel restores full volume immediately.
 */
@Singleton
class SleepTimerManager @Inject constructor(
    private val playerController: PlayerController,
) {
    companion object {
        private const val TAG = "SleepTimerManager"
        private const val FADE_DURATION_MS = 30_000L // 30 seconds
        private const val TICK_MS = 1000L

        /** Available timer durations in minutes. -1 means "end of current track". */
        val DURATION_OPTIONS = listOf(15, 30, 45, 60, 90, -1)
    }

    private val _timeRemainingMs = MutableStateFlow<Long?>(null)
    val timeRemainingMs: StateFlow<Long?> = _timeRemainingMs.asStateFlow()

    private val _isFading = MutableStateFlow(false)
    val isFading: StateFlow<Boolean> = _isFading.asStateFlow()

    private val _totalDurationMs = MutableStateFlow<Long?>(null)
    val totalDurationMs: StateFlow<Long?> = _totalDurationMs.asStateFlow()

    private var timerJob: Job? = null
    private var endOfTrackMode = false
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main)
    private var originalVolume = 1f

    /**
     * Starts the sleep timer.
     * @param minutes Duration in minutes. Use -1 for "end of current track".
     */
    fun startTimer(minutes: Int) {
        cancelTimer()

        if (minutes == -1) {
            // End of track mode — wait for current track to finish
            endOfTrackMode = true
            _timeRemainingMs.value = -1L // Sentinel for "end of track"
            _totalDurationMs.value = -1L
            Log.d(TAG, "Sleep timer started: end of current track")
            return
        }

        endOfTrackMode = false
        val totalMs = minutes * 60 * 1000L
        _timeRemainingMs.value = totalMs
        _totalDurationMs.value = totalMs
        originalVolume = playerController.player.volume

        Log.d(TAG, "Sleep timer started: ${minutes}min (${totalMs}ms)")

        timerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(TICK_MS)
                remaining -= TICK_MS
                _timeRemainingMs.value = remaining

                // Start volume fade in the final 30 seconds
                if (remaining <= FADE_DURATION_MS) {
                    _isFading.value = true
                    val fadeProgress = remaining.toFloat() / FADE_DURATION_MS
                    val volume = computeFadeVolume(remaining)
                    playerController.player.volume = volume
                }
            }

            // Timer expired — pause and release
            _timeRemainingMs.value = null
            _totalDurationMs.value = null
            _isFading.value = false
            playerController.player.volume = 0f
            playerController.player.pause()
            Log.d(TAG, "Sleep timer expired. Playback paused.")

            // Restore volume for next play
            playerController.player.volume = originalVolume
        }
    }

    /**
     * Called when a track ends. If in "end of track" mode, pauses playback.
     */
    fun onTrackEnded() {
        if (endOfTrackMode) {
            endOfTrackMode = false
            _timeRemainingMs.value = null
            _totalDurationMs.value = null
            playerController.player.pause()
            Log.d(TAG, "Sleep timer (end of track): Playback paused.")
        }
    }

    /**
     * Cancels the timer and restores full volume immediately.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        endOfTrackMode = false
        _timeRemainingMs.value = null
        _totalDurationMs.value = null
        _isFading.value = false
        playerController.player.volume = 1f
    }

    /**
     * Returns true if the sleep timer is active.
     */
    val isActive: Boolean
        get() = _timeRemainingMs.value != null || endOfTrackMode

    /**
     * Computes the volume level during the fade period.
     * Linear fade: volume = remainingMs / FADE_DURATION_MS
     *
     * @param remainingMs Time remaining in the fade window (0..30000)
     * @return Volume level (0.0..1.0)
     */
    fun computeFadeVolume(remainingMs: Long): Float {
        if (remainingMs >= FADE_DURATION_MS) return originalVolume
        if (remainingMs <= 0) return 0f
        return (remainingMs.toFloat() / FADE_DURATION_MS) * originalVolume
    }
}
