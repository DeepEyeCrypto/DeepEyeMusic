package com.deepeye.musicpro.player.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the Android Visualizer API to capture FFT and waveform data
 * from the active audio session.
 *
 * Requires RECORD_AUDIO permission.
 */
@Singleton
class VisualizerEngine @Inject constructor() {

    companion object {
        private const val TAG = "VisualizerEngine"
        private const val CAPTURE_SIZE = 256 // Number of FFT data points
    }

    private val _fftData = MutableStateFlow(FloatArray(CAPTURE_SIZE / 2))
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val _waveformData = MutableStateFlow(ByteArray(CAPTURE_SIZE))
    val waveformData: StateFlow<ByteArray> = _waveformData.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var visualizer: Visualizer? = null

    /**
     * Starts capturing audio data from the given audio session.
     */
    fun start(audioSessionId: Int) {
        release()

        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(CAPTURE_SIZE * 2)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer,
                            waveform: ByteArray,
                            samplingRate: Int
                        ) {
                            _waveformData.value = waveform.copyOf()
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer,
                            fft: ByteArray,
                            samplingRate: Int
                        ) {
                            _fftData.value = normalizeFFT(fft)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2, // 10 FPS capture rate
                    true,  // waveform
                    true   // fft
                )
                enabled = true
            }
            _isActive.value = true
            Log.i(TAG, "Visualizer started for session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer", e)
            _isActive.value = false
        }
    }

    /**
     * Releases the visualizer.
     */
    fun release() {
        try {
            visualizer?.apply {
                enabled = false
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing visualizer", e)
        } finally {
            visualizer = null
            _isActive.value = false
        }
    }

    /**
     * Converts raw FFT byte data to normalized float magnitudes (0..1).
     */
    private fun normalizeFFT(fft: ByteArray): FloatArray {
        val magnitudes = FloatArray(fft.size / 2)
        for (i in magnitudes.indices) {
            val real = fft[2 * i].toFloat()
            val imaginary = fft[2 * i + 1].toFloat()
            val magnitude = kotlin.math.sqrt(real * real + imaginary * imaginary)
            // Normalize to 0..1 range (128 is theoretical max for byte-based FFT)
            magnitudes[i] = (magnitude / 128f).coerceIn(0f, 1f)
        }
        return magnitudes
    }
}
