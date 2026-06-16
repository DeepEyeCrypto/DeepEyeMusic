// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.deepeye.musicpro.dsp.model.TubeMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.tanh

/**
 * Custom ExoPlayer AudioProcessor that simulates Vacuum Tube harmonic distortion.
 * - Triode Mode: Asymmetric clipping (adds warm even-order harmonics).
 * - Pentode Mode: Symmetric clipping (adds punchy odd-order harmonics).
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

    fun setConfig(enabled: Boolean, mode: TubeMode, drivePercent: Int) {
        active = enabled
        tubeMode = mode
        // Map 0-100% to 1.0x - 4.0x drive
        drive = 1.0f + (drivePercent / 100f) * 3.0f
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        // ALWAYS return true so ExoPlayer keeps it in the pipeline
        // We will do a soft-bypass in queueInput if !active
        return inputAudioFormat.channelCount != androidx.media3.common.Format.NO_VALUE
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val position = inputBuffer.position()
        val frameCount = (limit - position) / inputAudioFormat.bytesPerFrame

        if (!inputBuffer.hasRemaining()) {
            return
        }

        val requiredBufferSize = frameCount * outputAudioFormat.bytesPerFrame
        if (buffer.capacity() < requiredBufferSize) {
            buffer = ByteBuffer.allocateDirect(requiredBufferSize).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        if (!active) {
            // Bypass mode: direct copy
            buffer.put(inputBuffer)
        } else {
            while (inputBuffer.position() < limit) {
                // Read 16-bit PCM sample
                var sample = inputBuffer.short.toFloat() / 32768f

                // Apply Tube distortion
                sample *= drive
                if (tubeMode == TubeMode.TRIODE) {
                    // Asymmetric clipping: affects positive peaks differently from negative peaks
                    if (sample > 0) {
                        sample = tanh(sample)
                    } else {
                        sample = -1f + 1f / (1f - sample) // softer negative clipping
                    }
                } else {
                    // Pentode: Symmetric clipping
                    sample = tanh(sample)
                }

                // Bring back to output level
                sample /= drive
                
                // Convert back to 16-bit
                val outSample = (sample * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                buffer.putShort(outSample)
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

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

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
