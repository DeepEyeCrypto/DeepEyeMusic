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

    fun start(sessionId: Int) {
        release() // Symmetric cleanup first
        if (sessionId == 0) return

        try {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // Max capture size
                
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {
                        // Not used for now
                    }

                    override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {
                        _fftData.value = fft
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                
                enabled = true
            }
            Log.i("VisualizerEngine", "✅ Visualizer attached to session $sessionId")
        } catch (e: Exception) {
            Log.e("VisualizerEngine", "❌ Failed to attach visualizer: ${e.message}")
        }
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
