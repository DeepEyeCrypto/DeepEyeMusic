package com.deepeye.musicpro.data.cache

import androidx.room.withTransaction
import com.deepeye.musicpro.data.cache.dao.CacheDao
import com.deepeye.musicpro.data.cache.entities.CachedAutoplayQueue
import com.deepeye.musicpro.data.cache.entities.CachedRecommendationRow
import com.deepeye.musicpro.data.cache.entities.CachedSearchResult
import com.deepeye.musicpro.data.cache.entities.CachedTrack
import com.deepeye.musicpro.data.db.AppDatabase
import com.deepeye.musicpro.domain.autoplay.QueueItem
import com.deepeye.musicpro.domain.recommendation.RecommendationResult
import com.deepeye.musicpro.domain.recommendation.RecommendationRow
import com.deepeye.musicpro.domain.recommendation.VideoItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager
@Inject
constructor(
    private val cacheDao: CacheDao,
    private val db: AppDatabase,
) {
    // ── Save Recommendations ─────────────────────────────────────
    suspend fun saveRecommendations(result: RecommendationResult) {
        db.withTransaction {
            // Delete old rows/tracks manually if needed, or let TTL handle it.
            // But we will upsert rows and their tracks.

            result.becauseYouListened.forEachIndexed { index, row ->
                saveRowAndTracks("byl_$index", "because_you_listened", row)
            }

            result.favoriteArtists.forEachIndexed { index, row ->
                saveRowAndTracks("fa_$index", "favorite_artists", row)
            }

            saveRowAndTracks("pfn", "perfect_for_now", result.perfectForNow)
            saveRowAndTracks("trending", "trending", result.trending)

            result.genreDive.forEachIndexed { index, row ->
                saveRowAndTracks("gd_$index", "genre_dive", row)
            }

            saveRowAndTracks("hg", "hidden_gems", result.hiddenGems)
        }
    }

    private suspend fun saveRowAndTracks(
        rowId: String,
        category: String,
        row: RecommendationRow,
    ) {
        val cachedRow =
            CachedRecommendationRow(
                rowId = rowId,
                title = row.title,
                subtitle = row.subtitle,
                category = category,
                accentColor = 0L, // Optional logic
            )
        cacheDao.upsertRow(cachedRow)

        val cachedTracks =
            row.items.mapIndexed { i, item ->
                CachedTrack(
                    rowId = rowId,
                    videoId = item.videoId,
                    title = item.title,
                    artist = item.artist,
                    channelId = item.channelId,
                    thumbnailUrl = "https://img.youtube.com/vi/${item.videoId}/maxresdefault.jpg",
                    duration = item.duration,
                    viewCount = "", // Assuming missing from VideoItem
                    rank = i,
                )
            }
        cacheDao.upsertTracks(cachedTracks)
    }

    // ── Load Recommendations ─────────────────────────────────────
    suspend fun loadCachedRecommendations(): RecommendationResult? {
        val rows = cacheDao.getFreshRows()
        if (rows.isEmpty()) return null

        val map = rows.groupBy { it.row.category }

        fun extractRows(category: String): List<RecommendationRow> {
            return map[category]?.map { cached ->
                RecommendationRow(
                    title = cached.row.title,
                    subtitle = cached.row.subtitle,
                    items =
                    cached.tracks.map {
                        VideoItem(
                            videoId = it.videoId,
                            title = it.title,
                            artist = it.artist,
                            channelId = it.channelId,
                            duration = it.duration,
                            genre = "",
                        )
                    },
                )
            } ?: emptyList()
        }

        val byl = extractRows("because_you_listened")
        val fa = extractRows("favorite_artists")
        val pfn = extractRows("perfect_for_now").firstOrNull() ?: RecommendationRow("", "", emptyList())
        val trending = extractRows("trending").firstOrNull() ?: RecommendationRow("", "", emptyList())
        val gd = extractRows("genre_dive")
        val hg = extractRows("hidden_gems").firstOrNull() ?: RecommendationRow("", "", emptyList())

        return RecommendationResult(
            becauseYouListened = byl,
            favoriteArtists = fa,
            perfectForNow = pfn,
            trending = trending,
            genreDive = gd,
            hiddenGems = hg,
        )
    }

    // ── Save / Load Autoplay Queue ─────────────────────────────────────
    suspend fun saveAutoplayQueue(
        seedId: String,
        queue: List<QueueItem>,
    ) {
        val cachedQueue =
            queue.mapIndexed { i, item ->
                CachedAutoplayQueue(
                    videoId = item.videoId,
                    title = item.title,
                    artist = item.artist,
                    channelId = item.channelId,
                    reason = item.reason,
                    score = item.score,
                    rank = i,
                    seedVideoId = seedId,
                )
            }
        cacheDao.upsertQueue(cachedQueue)
    }

    suspend fun loadAutoplayQueue(seedId: String): List<QueueItem>? {
        val cached = cacheDao.getQueueForSeed(seedId)
        if (cached.isEmpty()) return null
        return cached.map {
            QueueItem(
                videoId = it.videoId,
                title = it.title,
                artist = it.artist,
                channelId = it.channelId,
                reason = it.reason,
                rank = it.rank,
                score = it.score,
            )
        }
    }

    // ── Save / Load Search Results ─────────────────────────────────────
    suspend fun saveSearchResults(
        query: String,
        results: List<VideoItem>,
    ) {
        val cached =
            results.mapIndexed { i, item ->
                CachedSearchResult(
                    query = query,
                    videoId = item.videoId,
                    title = item.title,
                    artist = item.artist,
                    thumbnailUrl = "https://img.youtube.com/vi/${item.videoId}/maxresdefault.jpg",
                    duration = item.duration,
                    rank = i,
                )
            }
        cacheDao.upsertSearchResults(cached)
    }

    suspend fun loadSearchResults(query: String): List<VideoItem>? {
        val cached = cacheDao.getCachedSearch(query)
        if (cached.isEmpty()) return null
        return cached.map {
            VideoItem(
                videoId = it.videoId,
                title = it.title,
                artist = it.artist,
                channelId = "",
                duration = it.duration,
                genre = "",
            )
        }
    }

    // ── Cleanup ─────────────────────────────────────
    suspend fun cleanup() {
        val threeDaysAgo = System.currentTimeMillis() - 3 * 86400_000L
        cacheDao.deleteExpiredRows()
        cacheDao.deleteOldTracks(threeDaysAgo)
        cacheDao.deleteOldQueue(threeDaysAgo)
        cacheDao.deleteExpiredSearchResults()
    }
}
