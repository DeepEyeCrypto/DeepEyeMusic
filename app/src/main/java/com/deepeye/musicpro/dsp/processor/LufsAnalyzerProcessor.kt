// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.C
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LufsAnalyzerProcessor @Inject constructor() : AudioProcessor {

    private val _currentLufs = MutableStateFlow(-14f)
    val currentLufs = _currentLufs.asStateFlow()

    private var active = true
    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var sampleRateHz: Int = 44100
    private var channelCount: Int = 2
    
    private var sumSquares = 0.0
    private var sampleCount = 0L
    private val WINDOW_SAMPLES: Long
        get() = (sampleRateHz * channelCount * 0.4).toLong()

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.sampleRateHz = inputAudioFormat.sampleRate
        this.channelCount = inputAudioFormat.channelCount
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
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
            buffer = ByteBuffer.allocateDirect(requiredBufferSize).order(java.nio.ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        
        while (inputBuffer.position() < limit) {
            val sampleValue = if (isFloat) {
                val f = inputBuffer.float
                buffer.putFloat(f)
                f.toDouble()
            } else {
                val s = inputBuffer.short
                buffer.putShort(s)
                s.toDouble() / Short.MAX_VALUE
            }
            
            sumSquares += sampleValue * sampleValue
            sampleCount++
            
            if (sampleCount >= WINDOW_SAMPLES) {
                val rms = sqrt(sumSquares / sampleCount)
                val dbFs = if (rms > 0) 20 * log10(rms) else -100.0
                val estimatedLufs = dbFs + 3.0
                _currentLufs.value = estimatedLufs.toFloat()
                
                sumSquares = 0.0
                sampleCount = 0
            }
        }
        
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
        sumSquares = 0.0
        sampleCount = 0
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    }
}
