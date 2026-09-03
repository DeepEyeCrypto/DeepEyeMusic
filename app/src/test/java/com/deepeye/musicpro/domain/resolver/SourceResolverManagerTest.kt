package com.deepeye.musicpro.domain.resolver

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SourceResolverManagerTest {

    private lateinit var resolverOne: SourceResolver
    private lateinit var resolverTwo: SourceResolver
    private lateinit var resolverThree: SourceResolver
    private lateinit var manager: SourceResolverManager

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        resolverOne = mockk()
        every { resolverOne.name } returns "ResolverOne"
        every { resolverOne.priority } returns 1
        coEvery { resolverOne.resolveSource(any(), any()) } answers {
            val vid = firstArg<String>()
            val prefer = secondArg<Boolean>()
            val url = kotlinx.coroutines.runBlocking { resolverOne.resolveStreamUrl(vid, prefer) }
            url?.let { ResolvedSource(url = it, isVideo = prefer) }
        }

        resolverTwo = mockk()
        every { resolverTwo.name } returns "ResolverTwo"
        every { resolverTwo.priority } returns 2
        coEvery { resolverTwo.resolveSource(any(), any()) } answers {
            val vid = firstArg<String>()
            val prefer = secondArg<Boolean>()
            val url = kotlinx.coroutines.runBlocking { resolverTwo.resolveStreamUrl(vid, prefer) }
            url?.let { ResolvedSource(url = it, isVideo = prefer) }
        }

        resolverThree = mockk()
        every { resolverThree.name } returns "ResolverThree"
        every { resolverThree.priority } returns 3
        coEvery { resolverThree.resolveSource(any(), any()) } answers {
            val vid = firstArg<String>()
            val prefer = secondArg<Boolean>()
            val url = kotlinx.coroutines.runBlocking { resolverThree.resolveStreamUrl(vid, prefer) }
            url?.let { ResolvedSource(url = it, isVideo = prefer) }
        }

        val resolvers = setOf(resolverTwo, resolverThree, resolverOne)
        manager = SourceResolverManager(resolvers)
    }

    @Test
    fun `resolve uses highest priority resolver first`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid1", false) } returns "http://url1"

        val result = manager.resolve("vid1", false)

        assertEquals("http://url1", result)
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl("vid1", false) }
        coVerify(exactly = 0) { resolverTwo.resolveStreamUrl(any(), any()) }
    }

    @Test
    fun `resolve falls back to lower priority resolver on null return`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid2", true) } returns null
        coEvery { resolverTwo.resolveStreamUrl("vid2", true) } returns "http://url2"

        val result = manager.resolve("vid2", true)

        assertEquals("http://url2", result)
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl("vid2", true) }
        coVerify(exactly = 1) { resolverTwo.resolveStreamUrl("vid2", true) }
    }

    @Test
    fun `resolve falls back on exception thrown by primary resolver`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid3", false) } throws RuntimeException("Network Error")
        coEvery { resolverTwo.resolveStreamUrl("vid3", false) } returns "http://url-fallback"

        val result = manager.resolve("vid3", false)

        assertEquals("http://url-fallback", result)
    }

    @Test
    fun `resolve caches successful extraction`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid4", true) } returns "http://cached-url"

        val firstResult = manager.resolve("vid4", true)
        val secondResult = manager.resolve("vid4", true)

        assertEquals("http://cached-url", firstResult)
        assertEquals("http://cached-url", secondResult)
        
        // Ensure resolver was only called once due to caching
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl("vid4", true) }
    }

    @Test
    fun `forceRefresh bypasses cache and resolves again`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid5", false) } returns "http://url-first" andThen "http://url-refreshed"

        val firstResult = manager.resolve("vid5", false)
        val secondResult = manager.resolve("vid5", false, forceRefresh = true)

        assertEquals("http://url-first", firstResult)
        assertEquals("http://url-refreshed", secondResult)
        
        coVerify(exactly = 2) { resolverOne.resolveStreamUrl("vid5", false) }
    }

    @Test
    fun `resolve returns null if all resolvers fail`() = runTest {
        coEvery { resolverOne.resolveStreamUrl("vid6", false) } returns null
        coEvery { resolverTwo.resolveStreamUrl("vid6", false) } throws Exception("Failed")
        coEvery { resolverThree.resolveStreamUrl("vid6", false) } returns null

        val result = manager.resolve("vid6", false)

        assertNull(result)
        coVerify(exactly = 1) { resolverOne.resolveStreamUrl(any(), any()) }
        coVerify(exactly = 1) { resolverTwo.resolveStreamUrl(any(), any()) }
        coVerify(exactly = 1) { resolverThree.resolveStreamUrl(any(), any()) }
    }

    @Test
    fun `resolveSource returns rich ResolvedSource with formats and manifest URLs`() = runTest {
        val dummyVideoFormat = com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat(
            stableId = "v1080",
            streamType = com.deepeye.musicpro.player.smarttube.DeepEyeStreamType.VIDEO_ONLY,
            container = "mp4",
            mimeType = "video/mp4",
            videoCodec = com.deepeye.musicpro.player.smarttube.DeepEyeVideoCodec.AVC,
            audioCodec = null,
            width = 1920,
            height = 1080,
            frameRate = 60f,
            bitrate = 5000000L,
            dynamicRange = com.deepeye.musicpro.player.smarttube.DeepEyeDynamicRange.SDR,
            sampleRateHz = null,
            channelCount = null,
            languageTag = null,
            languageLabel = null,
            isOriginalAudio = null,
            isDefault = true,
            isSelected = false,
            isDeviceCompatible = true,
            incompatibilityReason = null,
            isVideoOnly = true,
            isAudioOnly = false,
            isProgressive = false,
            streamUrl = "http://video.mp4"
        )
        val dummyAudioFormat = com.deepeye.musicpro.player.smarttube.DeepEyePlaybackFormat(
            stableId = "a128",
            streamType = com.deepeye.musicpro.player.smarttube.DeepEyeStreamType.AUDIO_ONLY,
            container = "m4a",
            mimeType = "audio/mp4",
            videoCodec = null,
            audioCodec = com.deepeye.musicpro.player.smarttube.DeepEyeAudioCodec.AAC,
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
            isSelected = true,
            isDeviceCompatible = true,
            incompatibilityReason = null,
            isVideoOnly = false,
            isAudioOnly = true,
            isProgressive = false,
            streamUrl = "http://audio.m4a"
        )

        val resolved = ResolvedSource(
            url = "http://manifest.mpd",
            isVideo = true,
            videoFormats = listOf(dummyVideoFormat),
            audioFormats = listOf(dummyAudioFormat),
            dashManifestUrl = "http://manifest.mpd",
            hlsManifestUrl = "http://playlist.m3u8"
        )

        coEvery { resolverOne.resolveSource("vid7", true) } returns resolved

        val result = manager.resolveSource("vid7", true)

        assertNotNull(result)
        assertEquals("http://manifest.mpd", result?.url)
        assertEquals(1, result?.videoFormats?.size)
        assertEquals("v1080", result?.videoFormats?.first()?.stableId)
        assertEquals(1, result?.audioFormats?.size)
        assertEquals("a128", result?.audioFormats?.first()?.stableId)
        assertEquals("http://manifest.mpd", result?.dashManifestUrl)
        assertEquals("http://playlist.m3u8", result?.hlsManifestUrl)
    }
}
