// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.deepeye.musicpro.dsp.model.ViperClarityMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ViPER4Android-inspired Clarity & High-Frequency Exciter AudioProcessor.
 *
 * Provides:
 * - Natural Clarity: High-frequency transient enhancer (>3.5kHz).
 * - O-Zone+: Phase-aligned psychoacoustic vocal and air enhancer.
 * - X-HiFi: High-frequency harmonic reconstructor (synthesizes upper sparkle >10kHz).
 */
@Singleton
class ViperClarityProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var clarityMode = ViperClarityMode.NATURAL
    private var clarityGainDb = 0f

    // High Shelf Filter Coefficients
    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var x1_L = 0.0; private var x2_L = 0.0
    private var y1_L = 0.0; private var y2_L = 0.0
    private var x1_R = 0.0; private var x2_R = 0.0
    private var y1_R = 0.0; private var y2_R = 0.0

    // High-pass filter for harmonic exciter (X-HiFi)
    private var hp_x1_L = 0.0; private var hp_y1_L = 0.0
    private var hp_x1_R = 0.0; private var hp_y1_R = 0.0

    fun setConfig(enabled: Boolean, mode: ViperClarityMode, gainDb: Float) {
        active = enabled && gainDb > 0.01f
        clarityMode = mode
        clarityGainDb = gainDb.coerceIn(0f, 15f)

        if (inputAudioFormat.sampleRate > 0) {
            updateCoefficients(inputAudioFormat.sampleRate)
        }
    }

    private fun updateCoefficients(sampleRate: Int) {
        val f0 = if (clarityMode == ViperClarityMode.X_HIFI || clarityMode == ViperClarityMode.FEELING) 7000.0 else 4500.0
        val fs = sampleRate.toDouble()
        val a = 10.0.pow(clarityGainDb / 40.0) // Sqrt of gain
        val w0 = 2.0 * Math.PI * f0 / fs
        val alpha = sin(w0) / (2.0 * 0.707)
        val cosw0 = cos(w0)

        // High Shelf Filter
        val a0 = (a + 1.0) - (a - 1.0) * cosw0 + 2.0 * sqrt(a) * alpha
        b0 = (a * ((a + 1.0) + (a - 1.0) * cosw0 + 2.0 * sqrt(a) * alpha)) / a0
        b1 = (-2.0 * a * ((a - 1.0) + (a + 1.0) * cosw0)) / a0
        b2 = (a * ((a + 1.0) + (a - 1.0) * cosw0 - 2.0 * sqrt(a) * alpha)) / a0
        a1 = (2.0 * ((a - 1.0) - (a + 1.0) * cosw0)) / a0
        a2 = ((a + 1.0) - (a - 1.0) * cosw0 - 2.0 * sqrt(a) * alpha) / a0
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
            val exciterAmount = (clarityGainDb / 15.0).coerceIn(0.0, 1.0)

            while (inputBuffer.position() < limit) {
                val sL = inputBuffer.short.toDouble() / 32768.0
                val sR = inputBuffer.short.toDouble() / 32768.0

                // 1. Apply High-Shelf Biquad
                val outL = b0 * sL + b1 * x1_L + b2 * x2_L - a1 * y1_L - a2 * y2_L
                x2_L = x1_L; x1_L = sL; y2_L = y1_L; y1_L = outL

                val outR = b0 * sR + b1 * x1_R + b2 * x2_R - a1 * y1_R - a2 * y2_R
                x2_R = x1_R; x1_R = sR; y2_R = y1_R; y1_R = outR

                var finalL = outL
                var finalR = outR

                // 2. Mode Specific Processing
                when (clarityMode) {
                    ViperClarityMode.X_HIFI, ViperClarityMode.FEELING -> {
                        // High-pass 1-pole for exciter
                        val hpL = sL - hp_x1_L + 0.85 * hp_y1_L
                        hp_x1_L = sL; hp_y1_L = hpL

                        val hpR = sR - hp_x1_R + 0.85 * hp_y1_R
                        hp_x1_R = sR; hp_y1_R = hpR

                        // Generate odd+even high-frequency sparkle
                        val sparkleL = (hpL * abs(hpL) * 1.5) * exciterAmount
                        val sparkleR = (hpR * abs(hpR) * 1.5) * exciterAmount

                        finalL += sparkleL
                        finalR += sparkleR
                    }
                    ViperClarityMode.OZONE_PLUS -> {
                        // Stereo phase-aligned air expansion
                        val diff = (sL - sR) * 0.5 * exciterAmount
                        finalL += diff
                        finalR -= diff
                    }
                    ViperClarityMode.NATURAL -> {
                        // Pure transparent high-shelf recovery
                    }
                }

                // 3. Peak Safe Normalization
                finalL = finalL.coerceIn(-1.0, 1.0)
                finalR = finalR.coerceIn(-1.0, 1.0)

                val outIntL = (finalL * 32767.0).toInt().toShort()
                val outIntR = (finalR * 32767.0).toInt().toShort()

                buffer.putShort(outIntL)
                buffer.putShort(outIntR)
            }
        }

        inputBuffer.position(limit)
        buffer.flip()
        outputBuffer = this.buffer
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
        hp_x1_L = 0.0; hp_y1_L = 0.0; hp_x1_R = 0.0; hp_y1_R = 0.0
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}