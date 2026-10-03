// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ViPER4Android-inspired Master Limiter & Anti-Clipping AudioProcessor.
 *
 * Employs a soft-knee hyperbolic curve with configurable ceiling
 * to prevent DAC inter-sample clipping and digital harshness.
 */
@Singleton
class MasterLimiterProcessor @Inject constructor() : AudioProcessor {

    private var active = true
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var threshold = 0.84f // -1.5 dBFS safe threshold
    private var ceiling = 0.944f  // -0.5 dBFS safety margin against DAC inter-sample clipping

    fun setConfig(enabled: Boolean, thresholdDb: Float, ceilingDb: Float = -0.5f) {
        active = enabled
        // Strictly clamp ceiling between -3.0 dBFS and -0.5 dBFS
        ceiling = 10.0f.pow(ceilingDb.coerceIn(-3.0f, -0.5f) / 20f)
        val rawThreshold = 10.0f.pow(thresholdDb.coerceIn(-12.0f, -0.5f) / 20f)
        // Ensure a guaranteed minimum headroom of 0.04f to prevent division by zero / NaN in hyperbolic tangent
        threshold = rawThreshold.coerceAtMost(ceiling - 0.04f)
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
            val headroom = (ceiling - threshold).coerceAtLeast(0.04f)

            while (inputBuffer.position() < limit) {
                var sL = inputBuffer.short.toFloat() / 32768f
                var sR = inputBuffer.short.toFloat() / 32768f

                // Left channel soft knee limit
                val absL = abs(sL)
                if (absL > threshold) {
                    val excess = absL - threshold
                    val compressed = threshold + headroom * tanh(excess / headroom)
                    sL = if (sL > 0) compressed else -compressed
                }

                // Right channel soft knee limit
                val absR = abs(sR)
                if (absR > threshold) {
                    val excess = absR - threshold
                    val compressed = threshold + headroom * tanh(excess / headroom)
                    sR = if (sR > 0) compressed else -compressed
                }

                val outShortL = (sL * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val outShortR = (sR * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                buffer.putShort(outShortL)
                buffer.putShort(outShortR)
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
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}