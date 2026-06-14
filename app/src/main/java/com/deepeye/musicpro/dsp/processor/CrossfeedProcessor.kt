// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import javax.inject.Inject
import javax.inject.Singleton
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Custom ExoPlayer AudioProcessor that performs Binaural Crossfeed for headphone listening.
 * It blends a slightly delayed and filtered signal from the Left channel into the Right,
 * and vice versa, to simulate natural room acoustics and reduce listening fatigue.
 */
@Singleton
class CrossfeedProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // Simple delay line buffer (e.g., 300 microsecond delay ~ 13 samples at 44.1kHz)
    private val delayBufferL = FloatArray(32)
    private val delayBufferR = FloatArray(32)
    private var delayIndex = 0

    // Lowpass filter state for the crossfeed signal
    private var lastCrossL = 0f
    private var lastCrossR = 0f
    
    // Configurable parameters
    private var blendLevel = 0.25f // 25% blend
    private var cutoffAlpha = 0.4f // Simple 1-pole lowpass coefficient

    fun setEnabled(enabled: Boolean) {
        if (active != enabled) {
            active = enabled
            flush()
        }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        return if (active && inputAudioFormat.channelCount == 2) outputAudioFormat else AudioFormat.NOT_SET
    }

    override fun isActive(): Boolean {
        return active && inputAudioFormat.channelCount == 2
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive) {
            return
        }

        val limit = inputBuffer.limit()
        val position = inputBuffer.position()
        val frameCount = (limit - position) / inputAudioFormat.bytesPerFrame

        val requiredBufferSize = frameCount * outputAudioFormat.bytesPerFrame
        if (buffer.capacity() < requiredBufferSize) {
            buffer = ByteBuffer.allocateDirect(requiredBufferSize).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        // Processing 16-bit PCM: Crossfeed
        // delay length based on sample rate. 300us = 0.0003s. 0.0003 * 44100 = ~13 samples.
        val delaySamples = (inputAudioFormat.sampleRate * 0.0003f).toInt().coerceIn(1, 31)

        while (inputBuffer.position() < limit) {
            val inL = inputBuffer.short.toFloat()
            val inR = inputBuffer.short.toFloat()

            // Read delayed signals
            val readIndex = (delayIndex - delaySamples + 32) % 32
            val delayedL = delayBufferL[readIndex]
            val delayedR = delayBufferR[readIndex]

            // Apply simple 1-pole lowpass filter to the crossfeed signal
            val crossL = lastCrossL + cutoffAlpha * (delayedL - lastCrossL)
            val crossR = lastCrossR + cutoffAlpha * (delayedR - lastCrossR)
            lastCrossL = crossL
            lastCrossR = crossR

            // Blend
            var outL = inL + (crossR * blendLevel)
            var outR = inR + (crossL * blendLevel)

            // Normalize slightly to prevent clipping
            outL /= (1f + blendLevel)
            outR /= (1f + blendLevel)

            // Write current samples to delay buffer
            delayBufferL[delayIndex] = inL
            delayBufferR[delayIndex] = inR
            delayIndex = (delayIndex + 1) % 32

            buffer.putShort(outL.toInt().coerceIn(-32768, 32767).toShort())
            buffer.putShort(outR.toInt().coerceIn(-32768, 32767).toShort())
        }

        inputBuffer.position(limit)
        buffer.flip()
        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        delayBufferL.fill(0f)
        delayBufferR.fill(0f)
        delayIndex = 0
        lastCrossL = 0f
        lastCrossR = 0f
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
