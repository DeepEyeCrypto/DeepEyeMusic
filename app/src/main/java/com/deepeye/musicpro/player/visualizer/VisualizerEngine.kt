// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

@Singleton
class VisualizerEngine
@Inject
constructor() {
    companion object {
        private const val TAG = "VisualizerManager"
        private const val ZERO_FRAME_THRESHOLD = 60
        private const val RESET_BACKOFF_MS = 4000L
        private const val PEAK_HOLD_DECAY = 0.85f
        private const val DIAG_LOG_INTERVAL_FRAMES = 40
    }

    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0

    // Backward-compatible raw FFT bytes
    private val _fftData = MutableStateFlow(ByteArray(0))
    val fftData: StateFlow<ByteArray> = _fftData.asStateFlow()

    // 6-Band normalized acoustic energies: [0]=Bass, [1]=LowMid, [2]=Mid, [3]=HighMid, [4]=Treble, [5]=Peak
    private val _frequencyBands = MutableStateFlow(FloatArray(6) { 0f })
    val frequencyBands: StateFlow<FloatArray> = _frequencyBands.asStateFlow()

    // 32-Bin logarithmic spectrum for geometric vertex displacement
    private val _fftSpectrum = MutableStateFlow(FloatArray(32) { 0f })
    val fftSpectrum: StateFlow<FloatArray> = _fftSpectrum.asStateFlow()

    // Pre-allocated internal buffers to guarantee zero GC allocations in render loops
    private val smoothedSpectrum = FloatArray(32) { 0f }
    private val smoothedBands = FloatArray(6) { 0f }
    private val rawMagnitudes = FloatArray(512) { 0f }

    private var consecutiveZeroFrames = 0
    private var lastResetTime = 0L
    private var framesSinceDiagLog = 0

    // The real player session this engine must stay locked to. There is
    // deliberately NO session 0 (global output mix) fallback: that capture
    // path returns pure zeros on modern Android and permanently breaks the
    // visualizer (attached-to-dead-session symptom).
    private var targetSessionId: Int = 0

    @Volatile
    private var playbackActive: Boolean = false

    // Peak-hold envelope: guarantees kick-drum / transient peaks survive
    // StateFlow conflation between UI frames (fast attack, exponential release).
    private val peakHoldBands = FloatArray(6) { 0f }

    /**
     * Must be driven by ExoPlayer's isPlaying state (see AudioSessionManager).
     * Gates zero-data detection so silence or pause is never mistaken for a
     * dead capture session.
     */
    fun setPlaybackActive(active: Boolean) {
        if (playbackActive == active) return
        playbackActive = active
        if (!active) consecutiveZeroFrames = 0
        Log.d(TAG, "[VisualizerManager] playback_active=$active sessionId=$currentSessionId")
    }

    /**
     * True when a Visualizer instance exists, is enabled and is attached to the
     * live player session. Used by AudioSessionGuardian for self-healing re-attach.
     */
    @Synchronized
    fun isHealthy(): Boolean = visualizer != null && visualizer?.enabled == true && currentSessionId > 0

    /**
     * Starts or restarts the visualizer on the specified session ID.
     */
    @Synchronized
    fun start(sessionId: Int): Boolean {
        if (sessionId <= 0) {
            Log.w(TAG, "[VisualizerManager] attach_rejected sessionId=$sessionId reason=invalid_session current=$currentSessionId")
            return false
        }

        if (sessionId == currentSessionId && visualizer != null && visualizer?.enabled == true) {
            Log.d(TAG, "[VisualizerManager] already_active sessionId=$sessionId")
            return true
        }

        release()
        currentSessionId = sessionId
        targetSessionId = sessionId
        consecutiveZeroFrames = 0
        lastResetTime = System.currentTimeMillis()

        // Capture size range check
        val sizeRange = try {
            Visualizer.getCaptureSizeRange()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot get capture size range: ${e.message}")
            intArrayOf(128, 1024)
        }

        val candidateSizes = listOf(
            512,
            1024,
            sizeRange[1].coerceAtMost(1024),
            sizeRange[0].coerceAtLeast(128)
        ).distinct().filter { it in sizeRange[0]..sizeRange[1] }

        // Single-target capture: the live player session only. A session 0
        // (output mix) fallback is intentionally not attempted — it is not
        // capturable by apps on modern Android and yields pure zero frames.
        for (size in candidateSizes) {
            try {
                val viz = Visualizer(sessionId).apply {
                    captureSize = size
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                v: Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (waveform != null && waveform.isNotEmpty()) {
                                    processWaveformFallback(waveform)
                                }
                            }

                            override fun onFftDataCapture(
                                v: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (fft != null && fft.isNotEmpty()) {
                                    _fftData.value = fft
                                    processFft(fft)
                                }
                            }
                        },
                        Visualizer.getMaxCaptureRate(),
                        true,
                        true
                    )
                    enabled = true
                }
                visualizer = viz
                Log.i(TAG, "[VisualizerManager] attach_success sessionId=$sessionId captureSize=$size maxRate=${Visualizer.getMaxCaptureRate()}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "[VisualizerManager] attach_failed sessionId=$sessionId captureSize=$size reason=\"${e.message}\"")
            }
        }

        Log.e(TAG, "[VisualizerManager] attach_failed_all sessionId=$sessionId reason=no_compatible_capture_size")
        return false
    }

    private fun processFft(fft: ByteArray) {
        val n = (fft.size / 2).coerceAtMost(rawMagnitudes.size)
        if (n < 8) return

        var maxMagnitude = 0f
        rawMagnitudes[0] = abs(fft[0].toFloat()) // DC component
        maxMagnitude = max(maxMagnitude, rawMagnitudes[0])

        for (i in 1 until n) {
            val real = fft[2 * i].toFloat()
            val imag = fft[2 * i + 1].toFloat()
            val mag = hypot(real, imag)
            rawMagnitudes[i] = mag
            if (mag > maxMagnitude) {
                maxMagnitude = mag
            }
        }

        framesSinceDiagLog++
        if (framesSinceDiagLog >= DIAG_LOG_INTERVAL_FRAMES) {
            framesSinceDiagLog = 0
            Log.d(TAG, "[VisualizerManager] fft_frame sessionId=$currentSessionId playbackActive=$playbackActive peakMagnitude=$maxMagnitude bass=${smoothedBands[0]} mid=${smoothedBands[2]} treble=${smoothedBands[4]}")
        }

        // Zero-data diagnostic watchdog: while ExoPlayer is actually playing,
        // 60 consecutive silent FFT frames mean the capture is dead (stale or
        // unregistered audio session). Fully release and reconstruct the
        // Visualizer instance on the SAME live session with a backoff guard.
        if (maxMagnitude < 1.0f) {
            consecutiveZeroFrames++
            if (consecutiveZeroFrames >= ZERO_FRAME_THRESHOLD) {
                consecutiveZeroFrames = 0
                if (playbackActive) {
                    val now = System.currentTimeMillis()
                    if (now - lastResetTime > RESET_BACKOFF_MS) {
                        lastResetTime = now
                        Log.w(TAG, "[VisualizerManager] zero_fft_reset sessionId=$currentSessionId frames=$ZERO_FRAME_THRESHOLD playbackActive=true action=reconstruct_visualizer")
                        start(currentSessionId)
                    } else {
                        Log.w(TAG, "[VisualizerManager] zero_fft_reset_backoff sessionId=$currentSessionId backoffMs=$RESET_BACKOFF_MS")
                    }
                }
            }
            return
        } else {
            if (consecutiveZeroFrames > 0) {
                Log.d(TAG, "[VisualizerManager] fft_signal_locked sessionId=$currentSessionId peakMagnitude=$maxMagnitude")
                consecutiveZeroFrames = 0
            }
        }

        // Aggregate into 32 logarithmic bins
        val binStep = max(1, n / 32)
        for (bin in 0 until 32) {
            var sum = 0f
            val start = bin * binStep
            val end = (start + binStep).coerceAtMost(n)
            for (j in start until end) {
                sum += rawMagnitudes[j]
            }
            val avg = if (end > start) sum / (end - start) else 0f
            val normalized = (avg / 100f).coerceIn(0f, 2.0f)

            // EMA smoothing (0.35f fast rise, steady fall)
            val alpha = if (normalized > smoothedSpectrum[bin]) 0.50f else 0.22f
            smoothedSpectrum[bin] = smoothedSpectrum[bin] * (1f - alpha) + normalized * alpha
        }

        // 6 Frequency Bands:
        // [0] Bass: Sub-bass + Mid-bass (bins 0..3)
        var bassSum = 0f
        for (i in 0..3) bassSum += smoothedSpectrum[i]
        val bass = (bassSum / 4f).coerceIn(0f, 2.5f)

        // [1] Low-Mid: (bins 4..7)
        var lowMidSum = 0f
        for (i in 4..7) lowMidSum += smoothedSpectrum[i]
        val lowMid = (lowMidSum / 4f).coerceIn(0f, 2.0f)

        // [2] Mid: (bins 8..15)
        var midSum = 0f
        for (i in 8..15) midSum += smoothedSpectrum[i]
        val mid = (midSum / 8f).coerceIn(0f, 2.0f)

        // [3] High-Mid: (bins 16..23)
        var highMidSum = 0f
        for (i in 16..23) highMidSum += smoothedSpectrum[i]
        val highMid = (highMidSum / 8f).coerceIn(0f, 1.8f)

        // [4] Treble: (bins 24..31)
        var trebleSum = 0f
        for (i in 24..31) trebleSum += smoothedSpectrum[i]
        val treble = (trebleSum / 8f).coerceIn(0f, 1.5f)

        // [5] Peak / Global Energy
        val peak = (bass * 0.5f + mid * 0.3f + treble * 0.2f).coerceIn(0f, 2.5f)

        // Asymmetric EMA: fast attack so transient kick-drum hits are never
        // smoothed away, slower release for fluid decay.
        emaBand(0, bass, attack = 0.55f, release = 0.30f)
        emaBand(1, lowMid, attack = 0.55f, release = 0.30f)
        emaBand(2, mid, attack = 0.55f, release = 0.30f)
        emaBand(3, highMid, attack = 0.55f, release = 0.30f)
        emaBand(4, treble, attack = 0.55f, release = 0.30f)
        smoothedBands[5] = peak

        // Peak-hold envelope: the emitted value is the decaying max of recent
        // frames, so UI reads never conflate away a transient between
        // recompositions.
        for (i in 0..5) {
            peakHoldBands[i] = max(smoothedBands[i], peakHoldBands[i] * PEAK_HOLD_DECAY)
        }

        _fftSpectrum.value = smoothedSpectrum.clone()
        _frequencyBands.value = peakHoldBands.clone()
    }

    private fun emaBand(index: Int, target: Float, attack: Float, release: Float) {
        val alpha = if (target > smoothedBands[index]) attack else release
        smoothedBands[index] = smoothedBands[index] * (1f - alpha) + target * alpha
    }

    private fun processWaveformFallback(waveform: ByteArray) {
        if (!playbackActive) return // Never fake energy while paused/silent
        if (_frequencyBands.value[5] > 0.05f) return // FFT is already healthy

        var sumSquare = 0.0
        for (b in waveform) {
            val sample = (b.toInt() and 0xFF) - 128
            sumSquare += sample * sample
        }
        val rms = kotlin.math.sqrt(sumSquare / waveform.size).toFloat() / 128f
        if (rms > 0.02f) {
            smoothedBands[0] = rms * 1.5f
            smoothedBands[2] = rms * 1.0f
            smoothedBands[4] = rms * 0.8f
            smoothedBands[5] = rms * 1.2f
            _frequencyBands.value = smoothedBands.clone()
        }
    }

    @Synchronized
    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing visualizer", e)
        } finally {
            visualizer = null
            consecutiveZeroFrames = 0
            java.util.Arrays.fill(smoothedBands, 0f)
            java.util.Arrays.fill(peakHoldBands, 0f)
            _fftData.value = ByteArray(0)
            _frequencyBands.value = FloatArray(6) { 0f }
            _fftSpectrum.value = FloatArray(32) { 0f }
        }
    }
}