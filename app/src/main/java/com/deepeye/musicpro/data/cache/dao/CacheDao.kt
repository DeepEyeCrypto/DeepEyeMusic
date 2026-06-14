package com.deepeye.musicpro.data.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deepeye.musicpro.data.cache.entities.CachedAutoplayQueue
import com.deepeye.musicpro.data.cache.entities.CachedRecommendationRow
import com.deepeye.musicpro.data.cache.entities.CachedRowWithTracks
import com.deepeye.musicpro.data.cache.entities.CachedSearchResult
import com.deepeye.musicpro.data.cache.entities.CachedTrack

@Dao
interface CacheDao {
    // ── Recommendation rows ──────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRow(row: CachedRecommendationRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<CachedTrack>)

    @Transaction
    @Query(
        """
        SELECT * FROM cached_recommendation_rows
        WHERE expiresAt > :now
        ORDER BY cachedAt DESC
    """,
    )
    suspend fun getFreshRows(now: Long = System.currentTimeMillis()): List<CachedRowWithTracks>

    @Query("SELECT * FROM cached_tracks WHERE rowId = :rowId ORDER BY rank ASC")
    suspend fun getTracksForRow(rowId: String): List<CachedTrack>

    // ── Autoplay queue ────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueue(queue: List<CachedAutoplayQueue>)

    @Query(
        """
        SELECT * FROM cached_autoplay_queue
        WHERE seedVideoId = :seedId
        AND cachedAt > :since
        ORDER BY rank ASC
    """,
    )
    suspend fun getQueueForSeed(
        seedId: String,
        since: Long = System.currentTimeMillis() - 3600_000L,
    ): List<CachedAutoplayQueue>

    // ── Search cache ──────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchResults(results: List<CachedSearchResult>)

    @Query(
        """
        SELECT * FROM cached_search_results
        WHERE query LIKE '%' || :query || '%'
        AND expiresAt > :now
        ORDER BY rank ASC
        LIMIT 20
    """,
    )
    suspend fun getCachedSearch(
        query: String,
        now: Long = System.currentTimeMillis(),
    ): List<CachedSearchResult>

    // ── Cleanup ───────────────────────────────
    @Query("DELETE FROM cached_recommendation_rows WHERE expiresAt < :now")
    suspend fun deleteExpiredRows(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_tracks WHERE cachedAt < :cutoff")
    suspend fun deleteOldTracks(cutoff: Long)

    @Query("DELETE FROM cached_autoplay_queue WHERE cachedAt < :cutoff")
    suspend fun deleteOldQueue(cutoff: Long)

    @Query("DELETE FROM cached_search_results WHERE expiresAt < :now")
    suspend fun deleteExpiredSearchResults(now: Long = System.currentTimeMillis())
}
