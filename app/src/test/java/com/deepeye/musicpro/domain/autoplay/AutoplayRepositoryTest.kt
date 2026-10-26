// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.autoplay

import com.deepeye.musicpro.data.cache.CacheManager
import com.deepeye.musicpro.data.prefs.AppSettings
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import com.deepeye.musicpro.util.NetworkConditionChecker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * AUTOPLAY-DEBUG-OMEGA test harness.
 * Safe telemetry only: IDs, sizes, thread names. No payloads/tokens (MASVS).
 */
class AutoplayRepositoryTest {

    private lateinit var contentFetcher: ContentFetcher
    private lateinit var smartTubeEngine: SmartTubeEngine
    private lateinit var innerTubeClient: InnerTubeRemoteClient
    private lateinit var cacheManager: CacheManager
    private lateinit var networkConditionChecker: NetworkConditionChecker
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: AutoplayRepository

    @Before
    fun setup() {
        contentFetcher = mockk(relaxed = true)
        smartTubeEngine = mockk(relaxed = true)
        innerTubeClient = mockk(relaxed = true)
        cacheManager = mockk(relaxed = true)
        networkConditionChecker = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        every { networkConditionChecker.isNetworkMetered() } returns false
        every { settingsDataStore.settings } returns flowOf(AppSettings(autoplayOnCellular = true))
        coEvery { cacheManager.loadAutoplayQueue(any()) } returns null
        repository = AutoplayRepository(
            contentFetcher, smartTubeEngine, innerTubeClient,
            cacheManager, networkConditionChecker, settingsDataStore,
        )
    }

    private fun remoteTrack(id: String) = MediaItem.Remote(
        id = id, title = "Title $id", artist = "Artist",
        artworkUri = null, duration = 180_000L, streamUri = null, isVideo = false,
    )

    @Test
    fun `generateNextQueue sends bare videoId contract without fabricated playlistId`() = runTest {
        val currentId = "dQw4w9WgXcQ"
        coEvery { smartTubeEngine.fetchUpNext(any()) } returns null
        coEvery { innerTubeClient.fetchNextAutoplay(any()) } returns null
        coEvery { contentFetcher.getRelatedVideos(any(), any(), any()) } returns emptyList()
        repository.generateNextQueue(remoteTrack(currentId), AutoplayState())
        // FEL-400: fabricated playlistId (e.g. RDAMVM<videoId>) is rejected with
        // HTTP 400. The provider contract is bare videoId only.
        coVerify(exactly = 1) { smartTubeEngine.fetchUpNext(currentId) }
        coVerify(exactly = 1) { innerTubeClient.fetchNextAutoplay(currentId) }
    }

    @Test
    fun `generateNextQueue deduplicates current track to avoid Loop Trap`() = runTest {
        val currentId = "CURRENT12345"
        coEvery { smartTubeEngine.fetchUpNext(any()) } returns null
        coEvery { innerTubeClient.fetchNextAutoplay(any()) } returns null
        coEvery { contentFetcher.getRelatedVideos(currentId, 20, false) } returns listOf(
            VideoItem(videoId = currentId, title = "Same", artist = "A", channelId = "", duration = "3:00"),
            VideoItem(videoId = "NEXT0000001", title = "Next", artist = "B", channelId = "", duration = "3:00"),
        )
        val queue = repository.generateNextQueue(remoteTrack(currentId), AutoplayState())
        assertTrue("expected candidates size=" + queue.size, queue.isNotEmpty())
        assertFalse("Loop Trap queue=" + queue.map { it.videoId }, queue.any { it.videoId == currentId })
    }

    @Test
    fun `generateNextQueue excludes recent history window without halting stream`() = runTest {
        val currentId = "CURRENT12345"
        val history = listOf("HIST0000001", "HIST0000002")
        coEvery { smartTubeEngine.fetchUpNext(any()) } returns null
        coEvery { innerTubeClient.fetchNextAutoplay(any()) } returns null
        coEvery { contentFetcher.getRelatedVideos(currentId, 20, false) } returns listOf(
            VideoItem(videoId = "HIST0000001", title = "H1", artist = "A", channelId = "", duration = "3:00"),
            VideoItem(videoId = "FRESH000001", title = "Fresh", artist = "B", channelId = "", duration = "3:00"),
        )
        val queue = repository.generateNextQueue(remoteTrack(currentId), AutoplayState(history = history))
        assertTrue(queue.isNotEmpty())
        assertFalse(queue.any { it.videoId in history })
    }

    @Test
    fun `SmartTube fetchUpNext parser filters currently-playing track from mocked next JSON`() {
        val currentId = "CURRENT12345"
        val nextId = "NEXT0000001"
        val payload = JSONObject(
            """
            {
              "contents": {
                "singleColumnWatchNextResults": {
                  "autoplay": {
                    "autoplay": {
                      "sets": [
                        {
                          "autoplayVideoRenderer": {
                            "autoplayEndpointRenderer": {
                              "endpoint": { "watchEndpoint": { "videoId": "$currentId", "replayIfSameVideo": true } }
                            }
                          }
                        },
                        {
                          "autoplayVideoRenderer": {
                            "autonavEndpointRenderer": {
                              "endpoint": { "watchEndpoint": { "videoId": "$nextId" } }
                            }
                          },
                          "nextVideoRenderer": {
                            "maybeHistoryEndpointRenderer": {
                              "endpoint": { "watchEndpoint": { "videoId": "$nextId" } },
                              "item": {
                                "previewButtonRenderer": {
                                  "title": { "runs": [{ "text": "Mocked Next" }] },
                                  "byline": { "runs": [{ "text": "Mocked Artist" }] }
                                }
                              }
                            }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent(),
        )
        val auth: com.deepeye.musicpro.domain.auth.InnerTubeAuthManager = mockk(relaxed = true)
        val ctx: android.content.Context = mockk(relaxed = true)
        val http: okhttp3.OkHttpClient = mockk(relaxed = true)
        val realEngine = SmartTubeEngine(ctx, auth, http)
        val method = SmartTubeEngine::class.java.getDeclaredMethod("extractUpNextTrack", JSONObject::class.java, String::class.java)
        method.isAccessible = true
        val track = method.invoke(realEngine, payload, currentId) as com.deepeye.musicpro.data.source.remote.youtube.AutoplayTrack?
        assertTrue(track != null)
        assertTrue("Loop Trap id=" + track?.videoId, track?.videoId == nextId)
    }

    @Test
    fun `generateNextQueue rethrows CancellationException from fetchUpNext, never swallows`() = runTest {
        val currentId = "CURRENT12345"
        coEvery { smartTubeEngine.fetchUpNext(currentId) } throws CancellationException("synthetic cancellation")
        coEvery { innerTubeClient.fetchNextAutoplay(any()) } returns null
        coEvery { contentFetcher.getRelatedVideos(any(), any(), any()) } returns emptyList()
        try {
            repository.generateNextQueue(remoteTrack(currentId), AutoplayState())
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected: cancellation propagates, never swallowed
        }
    }
}
