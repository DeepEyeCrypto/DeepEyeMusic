// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.recommendation

import com.deepeye.musicpro.data.db.RecommendationDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * On-device taste model (plan Task 2).
 *
 * Replaces keyword-guessed "Based on your listening" with affinity scoring
 * grounded in real playback: each song's contribution to its artist is
 * `playCount * exp(-ageDays / DECAY_DAYS)` — recent heavy listening dominates,
 * stale history decays.
 *
 * Pure over [RecommendationDao] + [ContentFetcher]: no PII leaves the device.
 */
@Singleton
class TasteModelEngine
    @Inject
    constructor(
        private val recommendationDao: RecommendationDao,
        private val contentFetcher: ContentFetcher,
    ) {
        companion object {
            /** Half-life-ish decay window: a 30-day-old play scores ~37% of a fresh one. */
            const val DECAY_DAYS = 30.0
            const val SONG_SCAN_WINDOW_DAYS = 60L
            const val TOP_ARTIST_SCAN_SONGS = 60

            internal val DAY_MS = 24L * 3600_000L
        }

        /**
         * One-pass affinity scores for an arbitrary list of artists — used by the
         * autoplay queue-ranker so we query the DAO once per call instead of N times.
         *
         * Keys are lowercased so downstream matching is case-insensitive.
         */
        suspend fun affinityScoresForArtists(
            artists: List<String>,
            sinceMs: Long,
            now: Long = System.currentTimeMillis(),
        ): Map<String, Double> {
            if (artists.isEmpty()) return emptyMap()
            val byArtist = LinkedHashMap<String, Double>()
            val wantLower = artists.map { it.lowercase() }.toSet()
            for (song in recommendationDao.getTopSongsSince(sinceMs, TOP_ARTIST_SCAN_SONGS)) {
                val lower = song.artist.lowercase()
                if (lower !in wantLower) continue
                val ageDays = ((now - song.lastPlayed).coerceAtLeast(0L).toDouble()) / DAY_MS
                byArtist[lower] = (byArtist[lower] ?: 0.0) + song.playCount.toDouble() * exp(-ageDays / DECAY_DAYS)
            }
            return byArtist.filterValues { it > 0.0 }
        }

        /**
         * Top artists by decayed affinity within [sinceMs]..now.
         *
         * @param now injectable clock for deterministic tests.
         * @return artist names ordered by affinity descending.
         */
        suspend fun affinityTopArtists(
            sinceMs: Long,
            limit: Int = 5,
            now: Long = System.currentTimeMillis(),
        ): List<String> {
            val scores =
                affinityScoresForArtists(
                    artists =
                        listOf(
                            "*dummy* to force a scan when the list is empty",
                        ),
                    sinceMs = sinceMs,
                    now = now,
                )
            // When artists is a dummy list we actually want ALL scores to rank them —
            // so just delegate to the full-table path below for top-artists logic:
            val songs = recommendationDao.getTopSongsSince(sinceMs, TOP_ARTIST_SCAN_SONGS)
            if (songs.isEmpty()) return emptyList()
            val full = LinkedHashMap<String, Double>()
            for (song in songs) {
                val ageDays = ((now - song.lastPlayed).coerceAtLeast(0L).toDouble()) / DAY_MS
                full[song.artist] = (full[song.artist] ?: 0.0) + song.playCount.toDouble() * exp(-ageDays / DECAY_DAYS)
            }
            return full.entries
                .filter { it.value > 0.0 }
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }

        /**
         * Resolve the taste-grounded recommendation query seed, then fetch candidates.
         *
         * Seed order: explicit [querySeed] > top-2 affinity artists > public trending
         * rail (empty-taste fallback — the only honest source when no local history).
         */
        suspend fun recommend(
            querySeed: String? = null,
            limit: Int = 15,
            now: Long = System.currentTimeMillis(),
        ): List<VideoItem> {
            val sinceMs = now - SONG_SCAN_WINDOW_DAYS * DAY_MS
            val seed =
                querySeed
                    ?: affinityTopArtists(sinceMs = sinceMs, limit = 2, now = now)
                        .firstOrNull()
                        ?.let { "$it official songs" }

            return if (!seed.isNullOrBlank()) {
                contentFetcher.searchByQuery(seed, limit)
            } else {
                contentFetcher.getTrendingMusic("IN", limit)
            }
        }

        /**
         * Raw decayed affinity of a single artist, for queue-ranker scoring
         * (plan Task 5). 0.0 when the artist has no recent history.
         */
        suspend fun affinityScoreFor(
            artist: String,
            sinceMs: Long,
            now: Long = System.currentTimeMillis()
        ): Double {
            return affinityScoresForArtists(
                artists = listOf(artist),
                sinceMs = sinceMs,
                now = now,
            )[artist.lowercase()] ?: 0.0
        }
    }
