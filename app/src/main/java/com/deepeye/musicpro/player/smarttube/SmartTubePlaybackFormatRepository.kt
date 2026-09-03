// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.smarttube

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartTubePlaybackFormatRepository @Inject constructor() {

    private val _snapshot = MutableStateFlow(SmartTubeFormatSnapshot())
    val snapshot: StateFlow<SmartTubeFormatSnapshot> = _snapshot.asStateFlow()

    fun updateMedia(mediaKey: String, title: String?) {
        _snapshot.update { cur ->
            if (cur.mediaKey == mediaKey) cur.copy(title = title ?: cur.title)
            else SmartTubeFormatSnapshot(mediaKey = mediaKey, title = title, isLoading = true)
        }
    }

    fun setFormats(
        mediaKey: String,
        videoFormats: List<DeepEyePlaybackFormat>,
        audioFormats: List<DeepEyePlaybackFormat>,
        activeVideoId: String? = null,
        activeAudioId: String? = null
    ) {
        _snapshot.update { cur ->
            val vid = activeVideoId ?: resolveBestVideo(videoFormats, cur)
            val aud = activeAudioId ?: resolveBestAudio(audioFormats, cur)
            android.util.Log.d("DeepEyeHQ", "event=repository_snapshot videoCount=${videoFormats.size} audioCount=${audioFormats.size} selectedVideoId=$vid selectedAudioId=$aud")
            cur.copy(
                mediaKey = mediaKey,
                videoFormats = videoFormats.map { it.copy(isSelected = (it.stableId == vid)) },
                audioFormats = audioFormats.map { it.copy(isSelected = (it.stableId == aud)) },
                currentVideoFormatId = vid,
                currentAudioFormatId = aud,
                isLoading = false,
                lastError = null
            )
        }
    }

    fun updateAvailableFormats(
        mediaKey: String,
        videoFormats: List<DeepEyePlaybackFormat>,
        audioFormats: List<DeepEyePlaybackFormat>,
        activeVideoId: String? = null,
        activeAudioId: String? = null
    ) = setFormats(mediaKey, videoFormats, audioFormats, activeVideoId, activeAudioId)

    fun selectVideoFormat(formatId: String) {
        _snapshot.update { cur ->
            cur.copy(
                videoFormats = cur.videoFormats.map { it.copy(isSelected = (it.stableId == formatId)) },
                currentVideoFormatId = formatId,
                selectionMode = SelectionMode.MANUAL,
                videoQualityPreset = VideoQualityPreset.CUSTOM
            )
        }
    }

    fun selectAudioFormat(formatId: String) {
        _snapshot.update { cur ->
            cur.copy(
                audioFormats = cur.audioFormats.map { it.copy(isSelected = (it.stableId == formatId)) },
                currentAudioFormatId = formatId,
                selectionMode = SelectionMode.MANUAL
            )
        }
    }
    fun setVideoPreset(preset: VideoQualityPreset) {
        _snapshot.update { cur ->
            val next = cur.copy(videoQualityPreset = preset, selectionMode = if (preset == VideoQualityPreset.AUTO) SelectionMode.AUTOMATIC else SelectionMode.SMARTTUBE_PRESET)
            val target = resolveBestVideo(next.videoFormats, next)
            next.copy(videoFormats = next.videoFormats.map { it.copy(isSelected = (it.stableId == target)) }, currentVideoFormatId = target)
        }
    }

    fun setVideoQualityPreset(preset: VideoQualityPreset) = setVideoPreset(preset)

    fun setVideoCodecPreference(pref: VideoCodecPreference) {
        _snapshot.update { cur ->
            val next = cur.copy(videoCodecPreference = pref)
            val target = resolveBestVideo(next.videoFormats, next)
            next.copy(videoFormats = next.videoFormats.map { it.copy(isSelected = (it.stableId == target)) }, currentVideoFormatId = target)
        }
    }

    fun setAudioLanguagePreference(pref: AudioLanguagePreference) {
        _snapshot.update { cur ->
            val next = cur.copy(audioLanguagePreference = pref)
            val target = resolveBestAudio(next.audioFormats, next)
            next.copy(audioFormats = next.audioFormats.map { it.copy(isSelected = (it.stableId == target)) }, currentAudioFormatId = target)
        }
    }

    fun setAudioQualityPreference(pref: AudioQualityPreference) {
        _snapshot.update { cur ->
            val next = cur.copy(audioQualityPreference = pref)
            val target = resolveBestAudio(next.audioFormats, next)
            next.copy(audioFormats = next.audioFormats.map { it.copy(isSelected = (it.stableId == target)) }, currentAudioFormatId = target)
        }
    }

    fun setAudioCodecPreference(pref: AudioCodecPreference) {
        _snapshot.update { cur ->
            val next = cur.copy(audioCodecPreference = pref)
            val target = resolveBestAudio(next.audioFormats, next)
            next.copy(audioFormats = next.audioFormats.map { it.copy(isSelected = (it.stableId == target)) }, currentAudioFormatId = target)
        }
    }

    fun setChannelPreference(pref: ChannelPreference) {
        _snapshot.update { cur ->
            val next = cur.copy(channelPreference = pref)
            val target = resolveBestAudio(next.audioFormats, next)
            next.copy(audioFormats = next.audioFormats.map { it.copy(isSelected = (it.stableId == target)) }, currentAudioFormatId = target)
        }
    }

    fun resetToAutomatic() {
        _snapshot.update { cur ->
            val reset = cur.copy(
                selectionMode = SelectionMode.AUTOMATIC,
                videoQualityPreset = VideoQualityPreset.AUTO,
                videoCodecPreference = VideoCodecPreference.AUTO,
                audioLanguagePreference = AudioLanguagePreference.ORIGINAL,
                audioQualityPreference = AudioQualityPreference.AUTO,
                audioCodecPreference = AudioCodecPreference.AUTO,
                channelPreference = ChannelPreference.AUTO
            )
            val vid = resolveBestVideo(reset.videoFormats, reset)
            val aud = resolveBestAudio(reset.audioFormats, reset)
            reset.copy(
                videoFormats = reset.videoFormats.map { it.copy(isSelected = (it.stableId == vid)) },
                audioFormats = reset.audioFormats.map { it.copy(isSelected = (it.stableId == aud)) },
                currentVideoFormatId = vid,
                currentAudioFormatId = aud
            )
        }
    }

    fun resetToDefaults() = resetToAutomatic()

    fun setError(error: String) {
        _snapshot.update { it.copy(isLoading = false, lastError = error) }
    }

    private fun resolveBestVideo(formats: List<DeepEyePlaybackFormat>, s: SmartTubeFormatSnapshot): String? {
        val pool = formats.filter { it.isDeviceCompatible }.ifEmpty { formats }
        if (pool.isEmpty()) return null
        val byCodec = when (s.videoCodecPreference) {
            VideoCodecPreference.PREFER_AV1 -> pool.filter { it.videoCodec == DeepEyeVideoCodec.AV1 }.ifEmpty { pool }
            VideoCodecPreference.PREFER_VP9 -> pool.filter { it.videoCodec == DeepEyeVideoCodec.VP9 }.ifEmpty { pool }
            VideoCodecPreference.PREFER_AVC -> pool.filter { it.videoCodec == DeepEyeVideoCodec.AVC }.ifEmpty { pool }
            VideoCodecPreference.PREFER_HEVC -> pool.filter { it.videoCodec == DeepEyeVideoCodec.HEVC }.ifEmpty { pool }
            VideoCodecPreference.AUTO -> pool
        }
        val maxH = when (s.videoQualityPreset) {
            VideoQualityPreset.DATA_SAVER -> 480
            VideoQualityPreset.BALANCED -> 1080
            VideoQualityPreset.HIGH_QUALITY -> 1440
            else -> Int.MAX_VALUE
        }
        val allowed = byCodec.filter { (it.height ?: 0) <= maxH }.ifEmpty { byCodec }
        return allowed.maxByOrNull { (it.height ?: 0) * 10000000L + (it.bitrate ?: 0L) }?.stableId
    }

    private fun resolveBestAudio(formats: List<DeepEyePlaybackFormat>, s: SmartTubeFormatSnapshot): String? {
        val pool = formats.filter { it.isDeviceCompatible }.ifEmpty { formats }
        if (pool.isEmpty()) return null
        val byLang = when (s.audioLanguagePreference) {
            AudioLanguagePreference.ORIGINAL -> pool.filter { it.isOriginalAudio == true }.ifEmpty { pool }
            AudioLanguagePreference.HINDI -> pool.filter { it.languageTag?.startsWith("hi", ignoreCase = true) == true }.ifEmpty { pool }
            AudioLanguagePreference.ENGLISH -> pool.filter { it.languageTag?.startsWith("en", ignoreCase = true) == true }.ifEmpty { pool }
            else -> pool
        }
        val byCodec = when (s.audioCodecPreference) {
            AudioCodecPreference.PREFER_OPUS -> byLang.filter { it.audioCodec == DeepEyeAudioCodec.OPUS }.ifEmpty { byLang }
            AudioCodecPreference.PREFER_AAC -> byLang.filter { it.audioCodec == DeepEyeAudioCodec.AAC }.ifEmpty { byLang }
            AudioCodecPreference.PREFER_MULTICHANNEL -> byLang.filter { (it.channelCount ?: 2) > 2 }.ifEmpty { byLang }
            else -> byLang
        }
        return if (s.audioQualityPreference == AudioQualityPreference.DATA_SAVER) {
            byCodec.minByOrNull { it.bitrate ?: Long.MAX_VALUE }?.stableId
        } else {
            byCodec.maxByOrNull { it.bitrate ?: 0L }?.stableId
        }
    }
}
