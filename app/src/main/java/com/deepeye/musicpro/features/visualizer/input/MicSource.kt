// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.features.visualizer.input

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MicSource — High-performance raw PCM microphone capture for visualizers.
 * Captures 44.1kHz mono PCM and streams into the Visualizer pipeline.
 */
class MicSource {
    private var audioRecord: AudioRecord? = null
    private val isCapturing = AtomicBoolean(false)
    
    private val _rawPcmData = MutableStateFlow(FloatArray(0))
    val rawPcmData: StateFlow<FloatArray> = _rawPcmData

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    suspend fun startCapture() = withContext(Dispatchers.IO) {
        if (isCapturing.get()) return@withContext

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("MicSource", "Failed to initialize AudioRecord")
            return@withContext
        }

        isCapturing.set(true)
        audioRecord?.startRecording()

        val buffer = ShortArray(bufferSize / 2)
        val pcm = FloatArray(bufferSize / 4)

        while (isCapturing.get()) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                for (i in 0 until read) {
                    pcm[i] = buffer[i].toFloat() / 32768f
                }
                _rawPcmData.value = pcm.copyOf(read)
            }
        }
    }

    fun stopCapture() {
        isCapturing.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
