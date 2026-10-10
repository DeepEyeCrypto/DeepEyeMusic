// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.ui.player.visualizer.VisualizerSceneId
import com.deepeye.musicpro.ui.player.visualizer.agsl.AudioVisualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VisualizerStudioViewModel — Manages active scenes and shader uniform parameters.
 *
 * Bridges the zero-alloc [AudioVisualizerManager] FFT engine (attached to ExoPlayer's
 * audio session by PlayerController) into the two StateFlows that AGSL shaders consume:
 * a 32-bin normalized spectrum and a 6-band array indexed [0]=Bass, [2]=Mid,
 * [4]=Treble, [5]=Peak.
 *
 * The manager emits the *same* mutable FloatArray reference each frame, so we cannot
 * `stateIn()` it (StateFlow conflates by reference equality and would freeze). Instead
 * we collect in [viewModelScope] and copy into fresh arrays per frame — the same
 * copy-per-frame discipline [VisualizerEngine] already uses.
 */
@HiltViewModel
class VisualizerStudioViewModel
@Inject
constructor(
    private val audioVisualizerManager: AudioVisualizerManager,
) : ViewModel() {

    private val _activeScene = MutableStateFlow(VisualizerSceneId.NEON_TRIANGLE_GRID)
    val activeScene: StateFlow<VisualizerSceneId> = _activeScene.asStateFlow()

    // Uniform parameters for shader tuning
    private val _rotationSpeed = MutableStateFlow(0.12f)
    val rotationSpeed: StateFlow<Float> = _rotationSpeed.asStateFlow()

    private val _baseGlow = MutableStateFlow(0.45f)
    val baseGlow: StateFlow<Float> = _baseGlow.asStateFlow()

    // ── Live FFT data (real, from the attached native Visualizer) ───────────────
    private val _fftSpectrum = MutableStateFlow(FloatArray(SPECTRUM_BINS) { 0f })
    val fftSpectrum: StateFlow<FloatArray> = _fftSpectrum.asStateFlow()

    private val _frequencyBands = MutableStateFlow(FloatArray(BAND_COUNT) { 0f })
    val frequencyBands: StateFlow<FloatArray> = _frequencyBands.asStateFlow()

    // Scratch buffers reused each frame; never exposed to collectors directly.
    private val spectrumScratch = FloatArray(SPECTRUM_BINS) { 0f }

    init {
        viewModelScope.launch {
            audioVisualizerManager.fftData.collect { rawMagnitudes ->
                if (rawMagnitudes.isEmpty()) return@collect
                transform(rawMagnitudes)
                // Publish defensive copies so StateFlow reference-equality conflation
                // does not drop frames and downstream shaders never see a mutated array.
                _fftSpectrum.value = spectrumScratch.copyOf()
                _frequencyBands.value = bucketBands(spectrumScratch)
            }
        }
    }

    /**
     * Downsamples the raw 128-bin magnitudes (0..1) into a normalized 32-bin spectrum.
     */
    private fun transform(raw: FloatArray) {
        val bins = minOf(SPECTRUM_BINS, raw.size)
        val stride = maxOf(1, raw.size / bins)
        for (i in 0 until SPECTRUM_BINS) {
            val start = i * stride
            val end = minOf(raw.size, start + stride)
            var sum = 0f
            var count = 0
            for (j in start until end) {
                sum += raw[j]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            // Scale into the ~0..2 range AGSL uniform consumers expect.
            spectrumScratch[i] = (avg * BAND_GAIN).coerceIn(0f, 2f)
        }
    }

    /**
     * Buckets the 32-bin spectrum into 6 acoustic bands:
     * [0]=Bass, [1]=LowMid, [2]=Mid, [3]=HighMid, [4]=Treble, [5]=Peak.
     */
    private fun bucketBands(spectrum: FloatArray): FloatArray {
        fun avg(from: Int, to: Int): Float {
            var sum = 0f
            var count = 0
            for (i in from..to) {
                sum += spectrum[i]
                count++
            }
            return if (count > 0) sum / count else 0f
        }
        val bass = avg(0, 3).coerceIn(0f, 2.5f)
        val lowMid = avg(4, 7)
        val mid = avg(8, 15).coerceIn(0f, 2f)
        val highMid = avg(16, 23)
        val treble = avg(24, 31).coerceIn(0f, 2f)
        val peak = (bass * 0.5f + mid * 0.3f + treble * 0.2f).coerceIn(0f, 2.5f)
        return floatArrayOf(bass, lowMid, mid, highMid, treble, peak)
    }

    fun setActiveScene(sceneId: VisualizerSceneId) {
        _activeScene.value = sceneId
    }

    fun setRotationSpeed(speed: Float) {
        _rotationSpeed.value = speed
    }

    fun setBaseGlow(glow: Float) {
        _baseGlow.value = glow
    }

    companion object {
        private const val SPECTRUM_BINS = 32
        private const val BAND_COUNT = 6
        private const val BAND_GAIN = 2.0f
    }
}
