// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.dsp.processor

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom ExoPlayer AudioProcessor that performs real-time Vocal Isolation/Removal.
 * It uses the "OOPS" (Out Of Phase Stereo) method by subtracting the right channel
 * from the left channel to cancel out center-panned audio (typically lead vocals).
 *
 * - **Video Playback Fixes:** Fixed a critical bug where `ExoPlayer` would crash with an `IllegalArgumentException` and `IllegalStateException` due to custom `AudioProcessor`s returning `AudioFormat.NOT_SET` while maintaining an `isActive() == true` state. Implemented proper buffer empty checks and soft-bypasses across `TubeSimulatorProcessor`, `CrossfeedProcessor`, and `VocalRemoverProcessor` to allow seamless toggling of effects without causing audio dropouts or breaking video playback.
 * - **DSPEngine Module Connectivity Fix:** Re-injected `TubeSimulatorProcessor`, `CrossfeedProcessor`, and `VocalRemoverProcessor` into `DSPEngine` and properly bound them to parameter updates (`DspParams`), resolving the issue where these processors would not activate or update when toggled from the UI.
- **Viper Bass UI Modernization:** Replaced hardware-style rotary knobs with sleek, linear sliders (`DspSliderRow`) for Sub Cutoff and Sub-Bass parameters to ensure visual consistency across the DSP panel and improve usability.
 */
@Singleton
class VocalRemoverProcessor
    @Inject
    constructor() : AudioProcessor {
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
                buffer = ByteBuffer.allocateDirect(requiredBufferSize).order(ByteOrder.nativeOrder())
            } else {
                buffer.clear()
            }

            if (!active || inputAudioFormat.channelCount != 2) {
                // Bypass mode: direct copy
                buffer.put(inputBuffer)
            } else {
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
