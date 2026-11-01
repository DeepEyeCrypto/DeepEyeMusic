// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.recommendation

import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.db.SongStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD contract for [TasteModelEngine] (plan Task 2).
 *
 * The DAO returns [SongStats] rows carrying playCount + lastPlayed, so the
 * affinity model is:  score(artist) = SUM over songs of playCount * exp(-ageDays / 30).
 */
class TasteModelEngineTest {
    private val dao: RecommendationDao = mockk(relaxed = true)
    private val fetcher: ContentFetcher = mockk(relaxed = true)
    private val engine = TasteModelEngine(dao, fetcher)

    private fun song(
        videoId: String,
        artist: String,
        playCount: Int,
        ageMillis: Long,
        now: Long
    ) = SongStats(
        videoId = videoId,
        title = "t_$videoId",
        artist = artist,
        channelId = "ch_$artist",
        playCount = playCount,
        avgCompletion = 0.9f,
        likes = 0,
        skips = 0,
        lastPlayed = now - ageMillis,
    )

    @Test
    fun `recent heavy artist outranks older light artist`() =
        runBlocking {
            val now = 1_000_000_000_000L
            val day = 24L * 3600_000L
            coEvery { dao.getTopSongsSince(any(), any()) } answers {
                listOf(
                    // Artist A: 3 plays, 1 day old  -> 3 * exp(-1/30) ~= 2.89
                    song("v1", "ArtistA", 3, 1L * day, now),
                    // Artist B: 10 plays, 60 days old -> 10 * exp(-2) ~= 1.35
                    song("v2", "ArtistB", 10, 60L * day, now),
                )
            }

            val top = engine.affinityTopArtists(sinceMs = now - 30L * day, limit = 5, now = now)

            assertEquals(listOf("ArtistA", "ArtistB"), top)
        }

    @Test
    fun `empty history yields empty artist list`() =
        runBlocking {
            val now = 1_000_000_000_000L
            coEvery { dao.getTopSongsSince(any(), any()) } answers { emptyList() }

            val top = engine.affinityTopArtists(sinceMs = now, limit = 5, now = now)
            assertEquals(emptyList<String>(), top)
        }

    @Test
    fun `top artists feed the search seed in the exact order`() =
        runBlocking {
            val now = 1_000_000_000_000L
            val day = 24L * 3600_000L
            coEvery { dao.getTopSongsSince(any(), any()) } answers {
                listOf(
                    song("v1", "Arijit Singh", 5, 1L * day, now),
                    song("v2", "Shreya Ghoshal", 4, 1L * day, now),
                )
            }
            val querySlot = slot<String>()
            coEvery { fetcher.searchByQuery(capture(querySlot), any()) } answers { emptyList() }

            engine.recommend(querySeed = null, limit = 15, now = now)

            coVerify(exactly = 1) { fetcher.searchByQuery(any(), any()) }
            val seed = querySlot.captured
            assertTrue("seed must start with top artist, got: $seed", seed.startsWith("Arijit Singh"))
        }

    @Test
    fun `explicit seed skips the DAO entirely`() =
        runBlocking {
            val now = 1_000_000_000_000L

            engine.recommend(querySeed = "ravi shankar hits official songs", limit = 10, now = now)

            coVerify(exactly = 1) { fetcher.searchByQuery("ravi shankar hits official songs", 10) }
        }

    @Test
    fun `empty taste falls back to trending`() =
        runBlocking {
            val now = 1_000_000_000_000L
            coEvery { dao.getTopSongsSince(any(), any()) } answers { emptyList() }
            coEvery { fetcher.getTrendingMusic(any(), any()) } answers { emptyList() }

            val items = engine.recommend(querySeed = null, limit = 15, now = now)

            assertEquals(emptyList<VideoItem>(), items)
            coVerify(exactly = 1) { fetcher.getTrendingMusic(any(), any()) }
            coVerify(exactly = 0) { fetcher.searchByQuery(any(), any()) }
        }

    @Test
    fun `affinityScoreFor aggregates only the named artist`() =
        runBlocking {
            val now = 1_000_000_000_000L
            val day = 24L * 3600_000L
            coEvery { dao.getTopSongsSince(any(), any()) } answers {
                listOf(
                    song("v1", "Target", 4, 0L, now),
                    song("v2", "Other", 100, 0L, now),
                )
            }

            val score = engine.affinityScoreFor("Target", sinceMs = now - 30L * day, now = now)
            // 4 plays, 0 days old -> 4 * exp(0) = 4.0
            assertTrue("expected ~4.0 got $score", score > 3.9 && score < 4.1)
        }
}
