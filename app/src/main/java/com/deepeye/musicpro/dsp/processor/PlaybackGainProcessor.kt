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
 * ViPER4Android-inspired Playback Gain Control (AGC) AudioProcessor.
 *
 * Automatically normalizes audio volume levels across dynamic tracks
 * using smooth envelope tracking and anti-pumping release curves.
 */
@Singleton
class PlaybackGainProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var maxGain = 2.0f // 1x .. 8x
    private var targetThreshold = 0.85f // ~ -1.4 dBFS
    private var envelope = 0.0f
    private var currentGain = 1.0f

    private var attackCoeff = 0.05f
    private var releaseCoeff = 0.0005f

    fun setConfig(enabled: Boolean, maxGainFactor: Float, thresholdDb: Float) {
        active = enabled
        maxGain = maxGainFactor.coerceIn(1.0f, 6.0f)
        targetThreshold = 10.0f.pow(thresholdDb.coerceIn(-12f, -0.1f) / 20f)
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat

        // Attack: 10ms, Release: 300ms
        val sr = inputAudioFormat.sampleRate.toFloat()
        attackCoeff = 1.0f - exp(-1.0f / (0.010f * sr))
        releaseCoeff = 1.0f - exp(-1.0f / (0.300f * sr))

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
                val inL = inputBuffer.short.toFloat() / 32768f
                val inR = inputBuffer.short.toFloat() / 32768f

                val peak = max(abs(inL), abs(inR))

                // Fast attack, slow smooth release envelope
                if (peak > envelope) {
                    envelope += attackCoeff * (peak - envelope)
                } else {
                    envelope += releaseCoeff * (peak - envelope)
                }

                // Compute required target gain
                val desiredGain = if (envelope > 0.001f) {
                    (targetThreshold / envelope).coerceIn(0.5f, maxGain)
                } else {
                    1.0f
                }

                // Smooth gain transition
                currentGain += 0.001f * (desiredGain - currentGain)

                var outL = inL * currentGain
                var outR = inR * currentGain

                // Soft saturation clamp
                if (abs(outL) > 0.95f) outL = tanh(outL)
                if (abs(outR) > 0.95f) outR = tanh(outR)

                val outShortL = (outL * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val outShortR = (outR * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

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
        envelope = 0.0f
        currentGain = 1.0f
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}