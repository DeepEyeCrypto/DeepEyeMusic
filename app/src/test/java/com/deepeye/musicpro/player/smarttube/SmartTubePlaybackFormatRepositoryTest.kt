package com.deepeye.musicpro.player.smarttube

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmartTubePlaybackFormatRepositoryTest {

    private lateinit var repository: SmartTubePlaybackFormatRepository

    private val video1080pAvc = DeepEyePlaybackFormat(
        stableId = "v1080_avc",
        streamType = DeepEyeStreamType.VIDEO_ONLY,
        container = "mp4",
        mimeType = "video/mp4",
        videoCodec = DeepEyeVideoCodec.AVC,
        audioCodec = null,
        width = 1920,
        height = 1080,
        frameRate = 60f,
        bitrate = 4000000L,
        dynamicRange = DeepEyeDynamicRange.SDR,
        sampleRateHz = null,
        channelCount = null,
        languageTag = null,
        languageLabel = null,
        isOriginalAudio = null,
        isDefault = false,
        isSelected = false,
        isDeviceCompatible = true,
        incompatibilityReason = null,
        isVideoOnly = true,
        isAudioOnly = false,
        isProgressive = false,
        streamUrl = "https://example.com/v1080"
    )

    private val video720pVp9 = DeepEyePlaybackFormat(
        stableId = "v720_vp9",
        streamType = DeepEyeStreamType.VIDEO_ONLY,
        container = "webm",
        mimeType = "video/webm",
        videoCodec = DeepEyeVideoCodec.VP9,
        audioCodec = null,
        width = 1280,
        height = 720,
        frameRate = 30f,
        bitrate = 2000000L,
        dynamicRange = DeepEyeDynamicRange.SDR,
        sampleRateHz = null,
        channelCount = null,
        languageTag = null,
        languageLabel = null,
        isOriginalAudio = null,
        isDefault = false,
        isSelected = false,
        isDeviceCompatible = true,
        incompatibilityReason = null,
        isVideoOnly = true,
        isAudioOnly = false,
        isProgressive = false,
        streamUrl = "https://example.com/v720"
    )

    private val video4kAv1 = DeepEyePlaybackFormat(
        stableId = "v4k_av1",
        streamType = DeepEyeStreamType.VIDEO_ONLY,
        container = "mp4",
        mimeType = "video/mp4",
        videoCodec = DeepEyeVideoCodec.AV1,
        audioCodec = null,
        width = 3840,
        height = 2160,
        frameRate = 60f,
        bitrate = 15000000L,
        dynamicRange = DeepEyeDynamicRange.HDR10,
        sampleRateHz = null,
        channelCount = null,
        languageTag = null,
        languageLabel = null,
        isOriginalAudio = null,
        isDefault = false,
        isSelected = false,
        isDeviceCompatible = true,
        incompatibilityReason = null,
        isVideoOnly = true,
        isAudioOnly = false,
        isProgressive = false,
        streamUrl = "https://example.com/v4k"
    )

    private val audioAac128 = DeepEyePlaybackFormat(
        stableId = "a128_aac",
        streamType = DeepEyeStreamType.AUDIO_ONLY,
        container = "m4a",
        mimeType = "audio/mp4",
        videoCodec = null,
        audioCodec = DeepEyeAudioCodec.AAC,
        width = null,
        height = null,
        frameRate = null,
        bitrate = 128000L,
        dynamicRange = null,
        sampleRateHz = 44100,
        channelCount = 2,
        languageTag = "en",
        languageLabel = "English",
        isOriginalAudio = true,
        isDefault = true,
        isSelected = false,
        isDeviceCompatible = true,
        incompatibilityReason = null,
        isVideoOnly = false,
        isAudioOnly = true,
        isProgressive = false,
        streamUrl = "https://example.com/a128"
    )

    private val audioOpus160 = DeepEyePlaybackFormat(
        stableId = "a160_opus",
        streamType = DeepEyeStreamType.AUDIO_ONLY,
        container = "webm",
        mimeType = "audio/webm",
        videoCodec = null,
        audioCodec = DeepEyeAudioCodec.OPUS,
        width = null,
        height = null,
        frameRate = null,
        bitrate = 160000L,
        dynamicRange = null,
        sampleRateHz = 48000,
        channelCount = 2,
        languageTag = "en",
        languageLabel = "English",
        isOriginalAudio = true,
        isDefault = false,
        isSelected = false,
        isDeviceCompatible = true,
        incompatibilityReason = null,
        isVideoOnly = false,
        isAudioOnly = true,
        isProgressive = false,
        streamUrl = "https://example.com/a160"
    )

    @Before
    fun setUp() {
        repository = SmartTubePlaybackFormatRepository()
    }

    @Test
    fun `initial snapshot has default empty state`() {
        val snapshot = repository.snapshot.value
        assertEquals("", snapshot.mediaKey)
        assertNull(snapshot.title)
        assertTrue(snapshot.videoFormats.isEmpty())
        assertTrue(snapshot.audioFormats.isEmpty())
        assertEquals(SelectionMode.AUTOMATIC, snapshot.selectionMode)
    }

    @Test
    fun `setFormats populates video and audio formats and selects best automatic formats`() = runTest {
        val videoList = listOf(video720pVp9, video1080pAvc, video4kAv1)
        val audioList = listOf(audioAac128, audioOpus160)

        repository.setFormats(
            mediaKey = "media_123",
            videoFormats = videoList,
            audioFormats = audioList
        )

        val snapshot = repository.snapshot.value
        assertEquals("media_123", snapshot.mediaKey)
        assertEquals(3, snapshot.videoFormats.size)
        assertEquals(2, snapshot.audioFormats.size)
        assertFalse(snapshot.isLoading)
        assertNull(snapshot.lastError)
        assertEquals("v4k_av1", snapshot.currentVideoFormatId)
        assertEquals("a160_opus", snapshot.currentAudioFormatId)
    }

    @Test
    fun `selectVideoFormat switches to manual selection mode`() = runTest {
        val videoList = listOf(video720pVp9, video1080pAvc)
        repository.setFormats("media_123", videoList, listOf(audioAac128))

        repository.selectVideoFormat("v720_vp9")

        val snapshot = repository.snapshot.value
        assertEquals(SelectionMode.MANUAL, snapshot.selectionMode)
        assertEquals("v720_vp9", snapshot.currentVideoFormatId)
        assertTrue(snapshot.videoFormats.first { it.stableId == "v720_vp9" }.isSelected)
        assertFalse(snapshot.videoFormats.first { it.stableId == "v1080_avc" }.isSelected)
    }

    @Test
    fun `setVideoQualityPreset filters best resolution accordingly`() = runTest {
        val videoList = listOf(video720pVp9, video1080pAvc, video4kAv1)
        repository.setFormats("media_123", videoList, listOf(audioAac128))

        repository.setVideoQualityPreset(VideoQualityPreset.BALANCED)

        val snapshot = repository.snapshot.value
        assertEquals(VideoQualityPreset.BALANCED, snapshot.videoQualityPreset)
        assertEquals("v1080_avc", snapshot.currentVideoFormatId)
    }

    @Test
    fun `toDeepEyeFormat extension maps attributes correctly`() {
        val deepEyeFmt = video1080pAvc.toDeepEyeFormat(groupIndex = 0, trackIndex = 1)
        assertEquals("v1080_avc", deepEyeFmt.id)
        assertEquals(0, deepEyeFmt.groupIndex)
        assertEquals(1, deepEyeFmt.trackIndex)
        assertEquals(com.deepeye.musicpro.player.format.FormatType.VIDEO, deepEyeFmt.type)
        assertEquals(1920, deepEyeFmt.width)
        assertEquals(1080, deepEyeFmt.height)
        assertEquals("1080p", deepEyeFmt.qualityLabel)
        assertEquals(4000000, deepEyeFmt.bitrate)
    }
}
