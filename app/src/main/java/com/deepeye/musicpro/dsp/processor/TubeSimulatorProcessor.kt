// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.deepeye.musicpro.dsp.model.TubeMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ViPER4Android-inspired 6J1 Vacuum Tube & AnalogX Harmonic AudioProcessor.
 *
 * Simulates analog tube warmth:
 * - Triode Mode: Asymmetric clipping curve producing warm 2nd/4th even-order harmonics.
 * - Pentode Mode: Symmetric clipping curve adding punchy odd-order dynamic harmonics.
 * - DC Blocking Filter: Prevents DC bias accumulation from asymmetric saturation.
 */
@Singleton
class TubeSimulatorProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var tubeMode = TubeMode.TRIODE
    private var drive = 1.0f

    // DC Blocker State
    private var dc_x1_L = 0f; private var dc_y1_L = 0f
    private var dc_x1_R = 0f; private var dc_y1_R = 0f

    fun setConfig(enabled: Boolean, mode: TubeMode, drivePercent: Int) {
        active = enabled && drivePercent > 0
        tubeMode = mode
        // Map 0-100% to 1.0x - 3.5x analog drive
        drive = 1.0f + (drivePercent.coerceIn(0, 100) / 100f) * 2.5f
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
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
            val comp = 1.0f / tanh(drive)

            while (inputBuffer.position() < limit) {
                var sL = (inputBuffer.short.toFloat() / 32768f) * drive
                var sR = (inputBuffer.short.toFloat() / 32768f) * drive

                // Tube Non-Linear Transfer Curve
                if (tubeMode == TubeMode.TRIODE) {
                    // Asymmetric transfer function generating even harmonics
                    sL = if (sL > 0f) tanh(sL) else tanh(sL * 0.82f) + 0.05f * (sL * sL)
                    sR = if (sR > 0f) tanh(sR) else tanh(sR * 0.82f) + 0.05f * (sR * sR)
                } else {
                    // Symmetric Pentode Saturation
                    sL = tanh(sL)
                    sR = tanh(sR)
                }

                sL *= comp
                sR *= comp

                // DC-Blocking Filter: y[n] = x[n] - x[n-1] + 0.995 * y[n-1]
                val dcOutL = sL - dc_x1_L + 0.995f * dc_y1_L
                dc_x1_L = sL; dc_y1_L = dcOutL

                val dcOutR = sR - dc_x1_R + 0.995f * dc_y1_R
                dc_x1_R = sR; dc_y1_R = dcOutR

                // Soft knee clamp to eliminate harsh inter-sample overshoot
                val cleanL = softClip(dcOutL)
                val cleanR = softClip(dcOutR)

                val outShortL = (cleanL * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val outShortR = (cleanR * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                buffer.putShort(outShortL)
                buffer.putShort(outShortR)
            }
        }

        inputBuffer.position(limit)
        buffer.flip()
        outputBuffer = this.buffer
    }

    private fun softClip(x: Float): Float {
        val absX = kotlin.math.abs(x)
        return if (absX <= 0.88f) {
            x
        } else {
            val sign = if (x >= 0.0f) 1.0f else -1.0f
            val excess = absX - 0.88f
            sign * (0.88f + 0.10f * kotlin.math.tanh((excess / 0.10f).toDouble()).toFloat())
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
        dc_x1_L = 0f; dc_y1_L = 0f
        dc_x1_R = 0f; dc_y1_R = 0f
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}