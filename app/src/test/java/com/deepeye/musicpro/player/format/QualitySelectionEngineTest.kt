// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QualitySelectionEngineTest {

    private val format4kAv1 = VideoTrackFormat(
        id = "video-4k-av1",
        label = "4K (2160p)",
        width = 3840,
        height = 2160,
        fps = 60.0f,
        bitrate = 20_000_000,
        sampleMimeType = "video/av01",
        codecs = "av01.0.08M.08",
        isHdr = true
    )

    private val format1080pVp9 = VideoTrackFormat(
        id = "video-1080p-vp9",
        label = "1080p (60fps)",
        width = 1920,
        height = 1080,
        fps = 60.0f,
        bitrate = 6_000_000,
        sampleMimeType = "video/x-vnd.on2.vp9",
        codecs = "vp09.00.41.08",
        isHdr = false
    )

    private val format1080pAvc = VideoTrackFormat(
        id = "video-1080p-avc",
        label = "1080p",
        width = 1920,
        height = 1080,
        fps = 30.0f,
        bitrate = 4_000_000,
        sampleMimeType = "video/avc",
        codecs = "avc1.640028",
        isHdr = false
    )

    private val format480p = VideoTrackFormat(
        id = "video-480p",
        label = "480p",
        width = 854,
        height = 480,
        fps = 30.0f,
        bitrate = 1_000_000,
        sampleMimeType = "video/avc",
        codecs = "avc1.4d401e",
        isHdr = false
    )

    private val audioOpusHq = AudioTrackFormat(
        id = "audio-opus-hq",
        label = "Opus High Quality",
        language = "en",
        bitrate = 160_000,
        sampleRate = 48_000,
        channelCount = 2,
        sampleMimeType = "audio/opus",
        codecs = "opus",
        isDefault = true
    )

    private val audioAacLq = AudioTrackFormat(
        id = "audio-aac-lq",
        label = "AAC Low",
        language = "en",
        bitrate = 64_000,
        sampleRate = 44_100,
        channelCount = 2,
        sampleMimeType = "audio/mp4a-latm",
        codecs = "mp4a.40.2",
        isDefault = false
    )

    @Test
    fun testSelectVideoFormat_highQuality_picksHighestResolution() {
        val available = listOf(format480p, format1080pVp9, format4kAv1)
        val capabilities = DeviceCodecCapabilities(
            supportsAv1 = true,
            supportsVp9 = true,
            supportsHevc = true,
            maxVideoWidth = 4096,
            maxVideoHeight = 2160,
            supportsHdr = true
        )

        val selected = QualitySelectionEngine.selectVideoFormat(
            available = available,
            preset = QualityPreset.HIGH_QUALITY,
            capabilities = capabilities
        )

        assertNotNull(selected)
        assertEquals("video-4k-av1", selected?.id)
    }

    @Test
    fun testSelectVideoFormat_hardwareConstraint_filtersUnsupportedAv1() {
        val available = listOf(format480p, format1080pVp9, format4kAv1)
        val capabilities = DeviceCodecCapabilities(
            supportsAv1 = false,
            supportsVp9 = true,
            supportsHevc = true,
            maxVideoWidth = 1920,
            maxVideoHeight = 1080,
            supportsHdr = false
        )

        val selected = QualitySelectionEngine.selectVideoFormat(
            available = available,
            preset = QualityPreset.HIGH_QUALITY,
            capabilities = capabilities
        )

        assertNotNull(selected)
        assertEquals("video-1080p-vp9", selected?.id)
    }

    @Test
    fun testSelectVideoFormat_dataSaver_picksLowestValidResolution() {
        val available = listOf(format480p, format1080pVp9, format4kAv1)
        val capabilities = DeviceCodecCapabilities(
            supportsAv1 = true,
            supportsVp9 = true,
            supportsHevc = true,
            maxVideoWidth = 3840,
            maxVideoHeight = 2160,
            supportsHdr = true
        )

        val selected = QualitySelectionEngine.selectVideoFormat(
            available = available,
            preset = QualityPreset.DATA_SAVER,
            capabilities = capabilities
        )

        assertNotNull(selected)
        assertEquals("video-480p", selected?.id)
    }

    @Test
    fun testSelectAudioFormat_highQuality_picksHighestBitrate() {
        val available = listOf(audioAacLq, audioOpusHq)
        val selected = QualitySelectionEngine.selectAudioFormat(available, QualityPreset.HIGH_QUALITY)

        assertNotNull(selected)
        assertEquals("audio-opus-hq", selected?.id)
    }

    @Test
    fun testSelectAudioFormat_dataSaver_picksLowestBitrate() {
        val available = listOf(audioAacLq, audioOpusHq)
        val selected = QualitySelectionEngine.selectAudioFormat(available, QualityPreset.DATA_SAVER)

        assertNotNull(selected)
        assertEquals("audio-aac-lq", selected?.id)
    }

    @Test
    fun testFilterSupportedVideoFormats_excludesExcessiveResolution() {
        val available = listOf(format480p, format1080pVp9, format4kAv1)
        val capabilities = DeviceCodecCapabilities(
            supportsAv1 = true,
            supportsVp9 = true,
            supportsHevc = true,
            maxVideoWidth = 1920,
            maxVideoHeight = 1080,
            supportsHdr = false
        )

        val filtered = QualitySelectionEngine.filterSupportedVideoFormats(available, capabilities)
        assertEquals(2, filtered.size)
        assertTrue(filtered.none { it.id == "video-4k-av1" })
    }

    @Test
    fun testQualityScore_favorsAv1AndVp9HigherThanAvc() {
        val av1Score = QualitySelectionEngine.estimateQualityScore(format4kAv1, isAv1Supported = true, isVp9Supported = true)
        val vp9Score = QualitySelectionEngine.estimateQualityScore(format1080pVp9, isAv1Supported = true, isVp9Supported = true)
        val avcScore = QualitySelectionEngine.estimateQualityScore(format1080pAvc, isAv1Supported = true, isVp9Supported = true)

        assertTrue(av1Score > vp9Score)
        assertTrue(vp9Score > avcScore)
    }

    @Test
    fun testSelectVideoFormat_balancedPreset_capsAt1080p() {
        val available = listOf(format480p, format1080pVp9, format4kAv1)
        val capabilities = DeviceCodecCapabilities(
            supportsAv1 = true,
            supportsVp9 = true,
            supportsHevc = true,
            maxVideoWidth = 3840,
            maxVideoHeight = 2160,
            supportsHdr = true
        )

        val selected = QualitySelectionEngine.selectVideoFormat(
            available = available,
            preset = QualityPreset.BALANCED,
            capabilities = capabilities
        )

        assertNotNull(selected)
        assertEquals("video-1080p-vp9", selected?.id)
    }

    @Test
    fun testBufferProfiles_validTimingConstraints() {
        BufferProfile.values().forEach { profile ->
            assertTrue("minBufferMs must be <= maxBufferMs", profile.minBufferMs <= profile.maxBufferMs)
            assertTrue("bufferForPlaybackMs must be > 0", profile.bufferForPlaybackMs > 0)
            assertTrue("bufferForPlaybackAfterRebufferMs >= bufferForPlaybackMs", profile.bufferForPlaybackAfterRebufferMs >= profile.bufferForPlaybackMs)
        }

        assertEquals(180000, BufferProfile.AGGRESSIVE_PRELOAD.maxBufferMs)
        assertEquals(200, BufferProfile.ULTRA_LOW_LATENCY.bufferForPlaybackMs)
    }

    @Test
    fun testDeviceCapabilities_summaryGeneration() {
        val capabilities = DeviceCodecCapabilities(
            supportsAv1 = true,
            supportsVp9 = false,
            supportsHevc = true,
            maxVideoWidth = 1920,
            maxVideoHeight = 1080,
            supportsHdr = false
        )

        val summary = capabilities.getSummary()
        assertTrue(summary.hasHwAv1)
        assertTrue(!summary.hasHwVp9)
        assertTrue(summary.hasHwHevc)
        assertEquals(1920, summary.maxVideoWidth)
        assertEquals(1080, summary.maxVideoHeight)
        assertTrue(!summary.supportsHdr)
    }
}
