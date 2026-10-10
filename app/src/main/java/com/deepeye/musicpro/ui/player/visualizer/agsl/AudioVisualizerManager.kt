// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// AudioVisualizerManager — AudioFx Visualizer FFT Capture Engine
//
// Bridges ExoPlayer's audioSessionId into android.media.audiofx.Visualizer
// to capture real-time FFT magnitude data for AGSL shader iChannel0 texture.
//
package com.deepeye.musicpro.ui.player.visualizer.agsl

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioVisualizerManager — Zero-alloc FFT capture from ExoPlayer audio session.
 *
 * Pre-allocates a single ByteArray for FFT magnitude to avoid per-frame
 * allocations in the audio callback. Thread-safe via synchronized attach/detach.
 */
@Singleton
class AudioVisualizerManager
@Inject
constructor() {
    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    private val fftSize = 128
    // Pre-allocated ByteArray reused across all FFT captures — zero allocation in callback
    private val preAllocatedMagnitude = ByteArray(fftSize)
    // Nier-Engine FFT Decay/Smoothing Buffer
    private val smoothedMagnitude = FloatArray(fftSize)
    
    // SharedFlow is used to avoid StateFlow's equality check and permit true zero-alloc event pushing
    private val _fftData = kotlinx.coroutines.flow.MutableSharedFlow<FloatArray>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    private val _isCapturing = MutableStateFlow(false)
    private val lock = Any()

    val fftData: kotlinx.coroutines.flow.Flow<FloatArray> = _fftData
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    /**
     * Attaches Visualizer to ExoPlayer's audio session.
     * Must be called AFTER audioTrackInitialized in AnalyticsListener.
     */
    fun attachToAudioSession(sessionId: Int): Boolean = synchronized(lock) {
        if (sessionId == audioSessionId && visualizer != null) return true

        try {
            detachInternal()

            audioSessionId = sessionId

            val maxRate = Visualizer.getMaxCaptureRate()
            val captureSize = Visualizer.getCaptureSizeRange()[1]

            visualizer = Visualizer(sessionId).apply {
                setCaptureSize(captureSize)
                enabled = true
            }

            visualizer?.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        // Waveform data unused
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        val numMag = minOf(smoothedMagnitude.size, fft.size / 2)
                        for (i in 0 until numMag) {
                            val newMag = (fft[i * 2].toInt() and 0xFF).toFloat() / 255f
                            // Nier-Engine Decay: smoothed[i] = prev[i]*0.7 + new[i]*0.3
                            smoothedMagnitude[i] = smoothedMagnitude[i] * 0.7f + newMag * 0.3f
                        }
                        // Emit in-place 
                        _fftData.tryEmit(smoothedMagnitude)
                    }
                },
                maxRate / 2,
                false,
                true
            )

            _isCapturing.value = true
            Log.d(TAG, "[AudioVisualizerManager] Attached to session $sessionId, FFT size: $captureSize, rate: $maxRate")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "[AudioVisualizerManager] Failed to attach: ${e.message}", e)
            return false
        }
    }

    /** Detaches and cleans up the Visualizer. Thread-safe. */
    fun detach(): Unit = synchronized(lock) {
        detachInternal()
    }

    private fun detachInternal() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            _isCapturing.value = false
        } catch (e: Exception) {
            Log.w(TAG, "[AudioVisualizerManager] Error during detach: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AudioVisualizerMgr"
    }
}
