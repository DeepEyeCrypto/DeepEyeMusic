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

/**
 * ViPER4Android-inspired Field Surround & DiffSurround AudioProcessor.
 *
 * Expands stereo soundstage width using Mid/Side spatial matrix decomposition,
 * decorrelated delay lines, and center-image vocal preservation.
 */
@Singleton
class FieldSurroundProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var surroundStrength = 500 // 0..1000
    private var midImageStrength = 500 // 0..1000
    private var stereoWidth = 1.2f

    // Circular delay buffer for Side channel (up to 30ms at 192kHz ~ 6000 samples)
    private val delayBuffer = FloatArray(8192)
    private var writeIndex = 0
    private var delaySamples = 441 // ~10ms at 44.1kHz

    // Low-pass filter for delayed side signal
    private var lastDelayedSide = 0f

    fun setConfig(enabled: Boolean, strength: Int, midStrength: Int) {
        active = enabled && strength > 0
        surroundStrength = strength.coerceIn(0, 1000)
        midImageStrength = midStrength.coerceIn(0, 1000)
        // Map strength to 1.0x - 2.2x width
        stereoWidth = 1.0f + (surroundStrength / 1000f) * 1.2f

        if (inputAudioFormat.sampleRate > 0) {
            updateDelay(inputAudioFormat.sampleRate)
        }
    }

    private fun updateDelay(sampleRate: Int) {
        // Delay between 6ms (subtle) to 18ms (immersive) based on strength
        val delayMs = 6.0f + (surroundStrength / 1000f) * 12.0f
        delaySamples = ((delayMs / 1000f) * sampleRate).toInt().coerceIn(64, delayBuffer.size - 1)
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        updateDelay(inputAudioFormat.sampleRate)
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
            val midGain = 0.8f + (midImageStrength / 1000f) * 0.4f
            val wetFactor = (surroundStrength / 1000f) * 0.45f
            val bufSize = delayBuffer.size

            while (inputBuffer.position() < limit) {
                val inL = inputBuffer.short.toFloat() / 32768f
                val inR = inputBuffer.short.toFloat() / 32768f

                // Mid/Side Decomposition
                val mid = (inL + inR) * 0.5f * midGain
                val side = (inL - inR) * 0.5f

                // Push side to circular delay line
                delayBuffer[writeIndex] = side
                val readIndex = (writeIndex - delaySamples + bufSize) % bufSize
                val delayedRaw = delayBuffer[readIndex]
                writeIndex = (writeIndex + 1) % bufSize

                // 1-pole low-pass on delayed side (damps harsh reflections above 4kHz)
                lastDelayedSide += 0.35f * (delayedRaw - lastDelayedSide)

                // Synthesize enhanced stereo field
                val expandedSide = side * stereoWidth + lastDelayedSide * wetFactor

                val outL = (mid + expandedSide).coerceIn(-1.0f, 1.0f)
                val outR = (mid - expandedSide).coerceIn(-1.0f, 1.0f)

                val outShortL = (outL * 32767f).toInt().toShort()
                val outShortR = (outR * 32767f).toInt().toShort()

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
        delayBuffer.fill(0f)
        writeIndex = 0
        lastDelayedSide = 0f
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}