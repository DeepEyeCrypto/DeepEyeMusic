// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.visualizer.agsl

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import kotlin.math.min

/**
 * VisualizerDataBridge — High-Performance Zero-Allocation FFT-to-Bitmap Audio Texture Bridge.
 *
 * Transcodes raw FFT spectrum and waveform audio data into a 256x1 ARGB_8888 texture
 * bound to AGSL RuntimeShader's `uniform shader iChannel0` via BitmapShader.
 *
 * Pixel Encoding Contract (256x1):
 * - Red (Channel 0): FFT Frequency Spectrum Magnitude (0..255)
 * - Green (Channel 1): Waveform Oscilloscope Amplitude (0..255)
 * - Blue (Channel 2): Transient Energy / Band Flux (0..255)
 * - Alpha: 255 (Opaque)
 */
class VisualizerDataBridge(
    private val textureWidth: Int = DEFAULT_TEXTURE_WIDTH
) {
    private val pixelBuffer = IntArray(textureWidth)
    private val audioBitmap: Bitmap = Bitmap.createBitmap(textureWidth, 1, Bitmap.Config.ARGB_8888)
    private val audioShader: BitmapShader = BitmapShader(
        audioBitmap,
        Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP
    )

    /**
     * Updates the internal 256x1 audio bitmap with the latest FFT spectrum and band energies.
     * Zero-allocation execution suitable for 60/120 FPS frame clocks.
     */
    fun updateAudioTexture(
        fftSpectrum: FloatArray,
        frequencyBands: FloatArray,
        intensity: Float = 1f
    ): BitmapShader {
        val specLen = fftSpectrum.size
        val bandsLen = frequencyBands.size
        val scale = if (intensity.isNaN()) 1f else intensity

        val bass = if (bandsLen > 0) frequencyBands[0].coerceIn(0f, 2f) else 0f
        val mids = if (bandsLen > 2) frequencyBands[2].coerceIn(0f, 2f) else 0f
        val treble = if (bandsLen > 4) frequencyBands[4].coerceIn(0f, 2f) else 0f
        val peak = if (bandsLen > 5) frequencyBands[5].coerceIn(0f, 2f) else 0f

        val blueValue = ((peak * 0.4f + bass * 0.3f + mids * 0.3f) * scale * 255f)
            .toInt()
            .coerceIn(0, 255)

        for (i in 0 until textureWidth) {
            // Map 256 pixels across the available FFT spectrum
            val specIndex = if (specLen > 0) {
                val ratio = i.toFloat() / (textureWidth - 1)
                val mapped = (ratio * (specLen - 1)).toInt()
                min(mapped, specLen - 1)
            } else {
                0
            }

            val rawMag = if (specLen > specIndex) fftSpectrum[specIndex] else 0f
            val redValue = (rawMag * scale * 255f).toInt().coerceIn(0, 255)

            // Waveform synthetic / phase modulation for green channel
            val greenValue = ((rawMag * 0.7f + (if (i % 2 == 0) treble else bass) * 0.3f) * scale * 255f)
                .toInt()
                .coerceIn(0, 255)

            // Pack ARGB_8888 (0xFF_RR_GG_BB)
            pixelBuffer[i] = (0xFF shl 24) or (redValue shl 16) or (greenValue shl 8) or blueValue
        }

        audioBitmap.setPixels(pixelBuffer, 0, textureWidth, 0, 0, textureWidth, 1)
        return audioShader
    }

    /** Returns the underlying cached [BitmapShader] */
    fun getAudioShader(): BitmapShader = audioShader

    companion object {
        const val DEFAULT_TEXTURE_WIDTH = 256
    }
}
