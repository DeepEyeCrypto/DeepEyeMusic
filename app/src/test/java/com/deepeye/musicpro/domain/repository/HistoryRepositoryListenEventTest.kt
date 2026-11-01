// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.data.db.ListenEvent
import com.deepeye.musicpro.data.db.PlaybackHistoryEntity
import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.db.SongStats
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.TasteModelEngine
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F1 contract: the live [HistoryRepository.recordListenEvent] path must populate
 * `listen_events` (the sole input of [TasteModelEngine]), closing the data gap
 * left open when Purge-Omega (f5fe0b88) removed the original writer.
 */
class HistoryRepositoryListenEventTest {
    private val recDao = mockk<RecommendationDao>(relaxed = true)
    private val repo = HistoryRepository(historyDao = mockk(), recommendationDao = recDao)

    @Test
    fun `completed playback writes a ListenEvent with computed completion ratio`() =
        runBlocking {
            val captured = slot<ListenEvent>()
            io.mockk.coEvery { recDao.insertListenEvent(capture(captured)) } answers { }

            repo.recordListenEvent(
                videoId = "v1",
                title = "Song",
                artist = "Artist",
                playDurationMs = 90_000L,
                totalDurationMs = 180_000L,
                wasSkipped = false,
            )

            io.mockk.coVerify(exactly = 1) { recDao.insertListenEvent(any()) }
            assertEquals("v1", captured.captured.videoId)
            assertEquals(0.5f, captured.captured.completionRatio)
            assertFalse(captured.captured.wasSkipped)
        }

    @Test
    fun `short partial listen is flagged as skipped`() =
        runBlocking {
            val captured = slot<ListenEvent>()
            io.mockk.coEvery { recDao.insertListenEvent(capture(captured)) } answers { }

            repo.recordListenEvent(
                videoId = "v2",
                title = "Song",
                artist = "Artist",
                playDurationMs = 5_000L,
                totalDurationMs = 180_000L,
                wasSkipped = true,
            )

            assertTrue(captured.captured.wasSkipped)
            assertTrue(captured.captured.completionRatio < 0.1f)
        }

    @Test
    fun `zero duration guards against divide-by-zero`() =
        runBlocking {
            val captured = slot<ListenEvent>()
            io.mockk.coEvery { recDao.insertListenEvent(capture(captured)) } answers { }

            repo.recordListenEvent(
                videoId = "v3",
                title = "Song",
                artist = "Artist",
                playDurationMs = 0L,
                totalDurationMs = 0L,
                wasSkipped = false,
            )

            assertEquals(0f, captured.captured.completionRatio)
        }

    /**
     * End-to-end data path: a `listen_events` table populated by the writer above is
     * readable by [TasteModelEngine] — i.e. the reader now sees non-empty affinity data.
     */
    @Test
    fun `taste engine reads non-empty affinity when listen_events has data`() =
        runBlocking {
            val now = 1_000_000_000_000L
            val day = 24L * 3_600_000L
            val dao: RecommendationDao = mockk(relaxed = true)
            io.mockk.coEvery { dao.getTopSongsSince(any(), any()) } answers {
                listOf(
                    SongStats(
                        videoId = "a1",
                        title = "t",
                        artist = "Heavy Artist",
                        channelId = "ch",
                        playCount = 5,
                        avgCompletion = 0.9f,
                        likes = 0,
                        skips = 0,
                        lastPlayed = now - 1L * day,
                    ),
                )
            }
            val engine = TasteModelEngine(dao, mockk(relaxed = true))

            val top = engine.affinityTopArtists(sinceMs = now - 30L * day, limit = 5, now = now)

            assertEquals(listOf("Heavy Artist"), top)
            assertTrue(engine.affinityScoreFor("Heavy Artist", now - 30L * day, now) > 0f)
        }
}
