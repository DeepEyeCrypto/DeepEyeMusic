package com.deepeye.musicpro.player.visualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisualizerEngine @Inject constructor() {
    private var visualizer: Visualizer? = null
    
    private val _fftData = MutableStateFlow(ByteArray(0))
    val fftData: StateFlow<ByteArray> = _fftData.asStateFlow()

    /**
     * Returns true if the visualizer was successfully started.
     */
    fun start(sessionId: Int): Boolean {
        release() // Symmetric cleanup first
        if (sessionId == 0) return false

        // Try progressively smaller capture sizes if max fails
        val sizeRange = Visualizer.getCaptureSizeRange() // [min, max]
        val candidateSizes = listOf(
            sizeRange[1],       // max (usually 8192 or 4096)
            2048,
            1024,
            sizeRange[0]        // min (usually 128)
        ).distinct().filter { it <= sizeRange[1] && it >= sizeRange[0] }

        for (size in candidateSizes) {
            try {
                val viz = Visualizer(sessionId)
                viz.captureSize = size
                viz.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {
                            // Not used for now
                        }
                        override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {
                            _fftData.value = fft
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                viz.enabled = true
                visualizer = viz
                Log.i("VisualizerEngine", "✅ Visualizer attached to session $sessionId (captureSize=$size)")
                return true
            } catch (e: Exception) {
                Log.w("VisualizerEngine", "⚠️ Failed with captureSize=$size for session $sessionId: ${e.message}")
            }
        }

        Log.e("VisualizerEngine", "❌ All capture sizes failed for session $sessionId")
        return false
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.e("VisualizerEngine", "Error releasing visualizer", e)
        } finally {
            visualizer = null
            _fftData.value = ByteArray(0)
        }
    }
}
