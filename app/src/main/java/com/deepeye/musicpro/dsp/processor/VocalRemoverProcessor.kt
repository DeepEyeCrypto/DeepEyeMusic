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
 * Custom ExoPlayer AudioProcessor that performs real-time Vocal Isolation/Removal.
 * It uses the "OOPS" (Out Of Phase Stereo) method by subtracting the right channel 
 * from the left channel to cancel out center-panned audio (typically lead vocals).
 */
@Singleton
class VocalRemoverProcessor @Inject constructor() : AudioProcessor {

    private var active = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

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
        
        // We output the same format. If mono, we can't do OOPS, so we just act as a pass-through.
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

        // Processing 16-bit PCM: Out Of Phase Stereo (L - R)
        while (inputBuffer.position() < limit) {
            val left = inputBuffer.short.toInt()
            val right = inputBuffer.short.toInt()

            // OOPS: (L - R) / 2 to prevent clipping
            val diff = (left - right) / 2

            // Output the diff to both channels (creating a mono track centered)
            buffer.putShort(diff.toShort())
            buffer.putShort(diff.toShort())
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
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
