// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.controller

import android.util.Log
import android.webkit.WebView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the WebView URL capture → ExoPlayer bridge for DSP/V4A support.
 *
 * Architecture:
 * 1. WebView loads YouTube page and plays video (handles all bot detection)
 * 2. shouldInterceptRequest captures googlevideo.com/videoplayback URLs
 * 3. This manager feeds the captured URL to ExoPlayer
 * 4. ExoPlayer creates an audio session → DSP/V4A attaches
 * 5. WebView audio is muted (video-only rendering)
 *
 * Failure handling:
 * - 403 from ExoPlayer → URL expired, request re-capture from WebView
 * - No URL captured in 15s → stay on WebView audio (no DSP)
 * - ExoPlayer error → fall back to WebView audio gracefully
 */
@Singleton
class StreamCaptureManager @Inject constructor(
    private val player: ExoPlayer,
) {
    companion object {
        private const val TAG = "StreamCapture"
        private const val CAPTURE_TIMEOUT_MS = 15_000L
        private const val URL_TTL_MS = 5 * 60 * 60 * 1000L // 5 hours
        private const val MAX_RETRY = 2
    }

    enum class DspState {
        INACTIVE,       // WebView playing, no DSP
        CAPTURING,      // Waiting for URL from WebView
        PREPARING,      // ExoPlayer preparing with captured URL
        ACTIVE,         // ExoPlayer playing, DSP attached
        FAILED,         // Capture/playback failed, WebView audio active
    }

    private val _dspState = MutableStateFlow(DspState.INACTIVE)
    val dspState: StateFlow<DspState> = _dspState.asStateFlow()

    private val _capturedUrl = MutableStateFlow<CapturedStream?>(null)
    val capturedUrl: StateFlow<CapturedStream?> = _capturedUrl.asStateFlow()

    private var currentVideoId: String? = null
    private var retryCount = 0
    private var captureJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Cache: videoId → (url, captureTime)
    private val cacheLock = Any()
    private val urlCache = LinkedHashMap<String, CapturedStream>(20, 0.75f, true)

    private val captureListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (_dspState.value == DspState.PREPARING || _dspState.value == DspState.ACTIVE) {
                handleExoPlayerError(error)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && _dspState.value == DspState.PREPARING) {
                _dspState.value = DspState.ACTIVE
                Log.i(TAG, "✅ DSP ACTIVE — ExoPlayer playing captured stream. V4A/DSP processing audio.")
            }
        }
    }

    init {
        player.addListener(captureListener)
    }

    data class CapturedStream(
        val url: String,
        val isAudioOnly: Boolean,
        val capturedAt: Long = System.currentTimeMillis(),
        val videoId: String = "",
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - capturedAt > URL_TTL_MS
    }

    /**
     * Called when a new video starts playing in WebView mode.
     * Begins the capture process.
     */
    fun onVideoStarted(videoId: String) {
        if (currentVideoId == videoId && _dspState.value == DspState.ACTIVE) {
            return // Already playing this video via ExoPlayer
        }

        currentVideoId = videoId
        retryCount = 0

        // Check cache first
        val cached = synchronized(cacheLock) { urlCache[videoId] }
        if (cached != null && !cached.isExpired) {
            Log.i(TAG, "Cache hit for $videoId (age=${(System.currentTimeMillis() - cached.capturedAt) / 1000}s)")
            attemptExoPlayerPlayback(cached)
            return
        }

        _dspState.value = DspState.CAPTURING
        Log.i(TAG, "Waiting for stream URL capture for $videoId...")

        // Timeout: if no URL captured in 15s, stay on WebView
        captureJob?.cancel()
        captureJob = scope.launch {
            delay(CAPTURE_TIMEOUT_MS)
            if (_dspState.value == DspState.CAPTURING) {
                Log.w(TAG, "Capture timeout for $videoId. Staying on WebView audio (no DSP).")
                _dspState.value = DspState.FAILED
            }
        }
    }

    /**
     * Called by shouldInterceptRequest when a googlevideo.com URL is captured.
     */
    fun onStreamUrlCaptured(url: String, isAudioOnly: Boolean) {
        val videoId = currentVideoId ?: return

        // Prefer audio-only URLs (smaller, faster, DSP-optimal)
        val existing = _capturedUrl.value
        if (existing != null && !existing.isAudioOnly && isAudioOnly) {
            Log.i(TAG, "Upgrading to audio-only stream for $videoId")
        } else if (existing != null && existing.isAudioOnly) {
            // Already have audio-only, skip video+audio
            return
        }

        val stream = CapturedStream(
            url = url,
            isAudioOnly = isAudioOnly,
            videoId = videoId,
        )
        _capturedUrl.value = stream
        synchronized(cacheLock) {
            urlCache[videoId] = stream
        }

        captureJob?.cancel() // Cancel timeout

        if (_dspState.value == DspState.CAPTURING || _dspState.value == DspState.INACTIVE) {
            attemptExoPlayerPlayback(stream)
        }
    }

    /**
     * Attempt to play the captured URL via ExoPlayer.
     */
    private fun attemptExoPlayerPlayback(stream: CapturedStream) {
        _dspState.value = DspState.PREPARING
        Log.i(TAG, "Attempting ExoPlayer playback (audio_only=${stream.isAudioOnly}, retry=$retryCount)")

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(stream.url)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "ExoPlayer setup failed: ${e.message}")
            _dspState.value = DspState.FAILED
        }
    }

    /**
     * Handle ExoPlayer errors — retry or fall back to WebView.
     */
    private fun handleExoPlayerError(error: PlaybackException) {
        val is403 = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        Log.e(TAG, "ExoPlayer error: code=${error.errorCode} is403=$is403 msg=${error.message}")

        if (is403 && retryCount < MAX_RETRY) {
            retryCount++
            Log.w(TAG, "URL expired (403). Requesting re-capture (retry $retryCount/$MAX_RETRY)")
            _capturedUrl.value = null // Clear old URL, wait for new capture
            _dspState.value = DspState.CAPTURING
            // WebView is still playing — it will naturally make new requests
            // that shouldInterceptRequest will capture
            captureJob?.cancel()
            captureJob = scope.launch {
                delay(CAPTURE_TIMEOUT_MS)
                if (_dspState.value == DspState.CAPTURING) {
                    _dspState.value = DspState.FAILED
                }
            }
        } else {
            Log.w(TAG, "ExoPlayer failed permanently. Falling back to WebView audio (no DSP).")
            _dspState.value = DspState.FAILED
            // Unmute WebView since ExoPlayer can't handle it
        }
    }

    /**
     * Called when the video changes or playback stops.
     */
    fun onVideoStopped() {
        captureJob?.cancel()
        currentVideoId = null
        _capturedUrl.value = null
        _dspState.value = DspState.INACTIVE
    }

    /**
     * Whether the WebView audio should be muted (ExoPlayer handling audio).
     */
    val shouldMuteWebView: Boolean
        get() = _dspState.value == DspState.ACTIVE || _dspState.value == DspState.PREPARING
}
