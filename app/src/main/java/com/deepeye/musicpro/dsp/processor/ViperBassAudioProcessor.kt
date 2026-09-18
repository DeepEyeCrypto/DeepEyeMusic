// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.deepeye.musicpro.dsp.model.ViperBassMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ViPER4Android-inspired Bass Enhancement AudioProcessor.
 *
 * Provides:
 * - Natural Bass: Resonant 2nd-order Peaking / Low-pass Biquad Filter.
 * - Pure Bass: Sub-bass psychoacoustic harmonic synthesizer (2nd/3rd harmonics).
 * - Subwoofer / Dynamic: 4th-order Butterworth low-shelf filter with soft saturation.
 */
@Singleton
class ViperBassAudioProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var bassMode = ViperBassMode.NATURAL
    private var bassFreq = 60
    private var bassGainDb = 0f
    private var linearGain = 1.0f

    // Biquad Filter State for Stereo Channels
    // Left
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var x1_L = 0.0; private var x2_L = 0.0
    private var y1_L = 0.0; private var y2_L = 0.0
    // Right
    private var x1_R = 0.0; private var x2_R = 0.0
    private var y1_R = 0.0; private var y2_R = 0.0

    // Harmonic synthesis state
    private var prevSampleL = 0.0
    private var prevSampleR = 0.0

    fun setConfig(enabled: Boolean, mode: ViperBassMode, freqHz: Int, gainDb: Float) {
        val wasActive = active
        active = enabled && gainDb > 0.01f
        bassMode = mode
        bassFreq = freqHz.coerceIn(30, 150)
        bassGainDb = gainDb.coerceIn(0f, 18f)
        linearGain = 10.0.pow((bassGainDb / 20.0)).toFloat()

        if (inputAudioFormat.sampleRate > 0) {
            updateCoefficients(inputAudioFormat.sampleRate)
        }
    }

    private fun updateCoefficients(sampleRate: Int) {
        val f0 = bassFreq.toDouble()
        val fs = sampleRate.toDouble()
        val q = if (bassMode == ViperBassMode.PURE) 1.2 else 0.707
        val a = 10.0.pow(bassGainDb / 40.0) // sqrt of gain
        val w0 = 2.0 * Math.PI * f0 / fs
        val alpha = sin(w0) / (2.0 * q)
        val cosw0 = cos(w0)

        when (bassMode) {
            ViperBassMode.NATURAL -> {
                // Peaking EQ at bassFreq
                val a0 = 1.0 + alpha / a
                b0 = (1.0 + alpha * a) / a0
                b1 = (-2.0 * cosw0) / a0
                b2 = (1.0 - alpha * a) / a0
                a1 = (-2.0 * cosw0) / a0
                a2 = (1.0 - alpha / a) / a0
            }
            ViperBassMode.PURE, ViperBassMode.DYNAMIC -> {
                // Low Shelf Filter
                val a0 = (a + 1.0) + (a - 1.0) * cosw0 + 2.0 * sqrt(a) * alpha
                b0 = (a * ((a + 1.0) - (a - 1.0) * cosw0 + 2.0 * sqrt(a) * alpha)) / a0
                b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cosw0)) / a0
                b2 = (a * ((a + 1.0) - (a - 1.0) * cosw0 - 2.0 * sqrt(a) * alpha)) / a0
                a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cosw0)) / a0
                a2 = ((a + 1.0) + (a - 1.0) * cosw0 - 2.0 * sqrt(a) * alpha) / a0
            }
        }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        updateCoefficients(inputAudioFormat.sampleRate)
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat.channelCount != Format.NO_VALUE
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val position = inputBuffer.position()
        val frameCount = (limit - position) / inputAudioFormat.bytesPerFrame

        if (!inputBuffer.hasRemaining()) return

        val requiredBufferSize = frameCount * outputAudioFormat.bytesPerFrame
        if (buffer.capacity() < requiredBufferSize) {
            buffer = ByteBuffer.allocateDirect(requiredBufferSize).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        if (!active || inputAudioFormat.channelCount != 2) {
            buffer.put(inputBuffer)
        } else {
            while (inputBuffer.position() < limit) {
                var sL = inputBuffer.short.toDouble() / 32768.0
                var sR = inputBuffer.short.toDouble() / 32768.0

                // 1. Apply Biquad Filter (Left)
                val outL = b0 * sL + b1 * x1_L + b2 * x2_L - a1 * y1_L - a2 * y2_L
                x2_L = x1_L; x1_L = sL; y2_L = y1_L; y1_L = outL

                // 1. Apply Biquad Filter (Right)
                val outR = b0 * sR + b1 * x1_R + b2 * x2_R - a1 * y1_R - a2 * y2_R
                x2_R = x1_R; x1_R = sR; y2_R = y1_R; y1_R = outR

                var finalL = outL
                var finalR = outR

                // 2. Pure Bass Harmonic Enhancement (Psychoacoustic Overtones)
                if (bassMode == ViperBassMode.PURE) {
                    val subL = outL - sL
                    val subR = outR - sR
                    // Generate 2nd and 3rd harmonics using non-linear waveshaping
                    val harmL = 0.35 * (subL * subL * if (subL > 0) 1.0 else -1.0)
                    val harmR = 0.35 * (subR * subR * if (subR > 0) 1.0 else -1.0)
                    finalL += harmL
                    finalR += harmR
                }

                // 3. Dynamic Auto-Headroom & Transparent Soft-Knee Saturation (Prevents "fata aawaj")
                val headroomScale = 1.0 / (1.0 + (linearGain - 1.0) * 0.18)
                finalL *= headroomScale
                finalR *= headroomScale

                finalL = softClip(finalL)
                finalR = softClip(finalR)

                val outIntL = (finalL * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val outIntR = (finalR * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                buffer.putShort(outIntL)
                buffer.putShort(outIntR)
            }
        }

        inputBuffer.position(limit)
        buffer.flip()
        outputBuffer = this.buffer
    }

    private fun softClip(x: Double): Double {
        val absX = abs(x)
        return if (absX <= 0.85) {
            x
        } else {
            val sign = if (x >= 0.0) 1.0 else -1.0
            val excess = absX - 0.85
            sign * (0.85 + 0.13 * tanh(excess / 0.13))
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        x1_L = 0.0; x2_L = 0.0; y1_L = 0.0; y2_L = 0.0
        x1_R = 0.0; x2_R = 0.0; y1_R = 0.0; y2_R = 0.0
        prevSampleL = 0.0; prevSampleR = 0.0
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}