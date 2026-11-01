package com.deepeye.musicpro.diagnostics.research

import android.content.Context
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InnerTubeResearchProbeTest {
    private val context = mockk<Context>(relaxed = true)
    private val authClient = mockk<AuthenticatedYouTubeClient>(relaxed = true)
    private val smartTubeEngine = mockk<SmartTubeEngine>(relaxed = true)
    private val innerTubeClient = mockk<InnerTubeRemoteClient>(relaxed = true)
    private val youtubeDs = mockk<YoutubeRemoteDataSource>(relaxed = true)

    private val probe =
        InnerTubeResearchProbe(
            context = context,
            authClient = authClient,
            smartTubeEngine = smartTubeEngine,
            innerTubeClient = innerTubeClient,
            youtubeDs = youtubeDs,
        )

    @Test
    fun `probeMusicHome returns at least one capture`() =
        runBlocking {
            val result = probe.probeMusicHome()
            assertTrue("expected >=1 capture, got ${result.size}", result.isNotEmpty())
        }

    @Test
    fun `probeAutoplayGraph returns both provider captures`() =
        runBlocking {
            val result = probe.probeAutoplayGraph("test_video_id")
            assertTrue("expected >=2 captures, got ${result.size}", result.size >= 2)
            assertTrue(result.any { it.endpoint == "up_next_smarttube" })
            assertTrue(result.any { it.endpoint == "up_next_innertube_music" })
        }

    @Test
    fun `probeRelated returns capture with endpoint and videoId`() =
        runBlocking {
            val result = probe.probeRelated("test_video_id")
            assertEquals("related_videos", result.endpoint)
            assertEquals("test_video_id", result.videoId)
            assertTrue(result.ts > 0)
        }

    @Test
    fun `captureRelatedVideos returns a short status string`() =
        runBlocking {
            val result = probe.captureRelatedVideos("test_video_id")
            assertTrue(result.isNotEmpty())
            assertTrue(
                "status '$result' must be a short NO_RELATED/n_RELATED string",
                result.startsWith("NO_RELATED") || result.contains("_RELATED")
            )
        }
}
