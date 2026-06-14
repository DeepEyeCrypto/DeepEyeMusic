package com.deepeye.musicpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecommendationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListenEvent(event: ListenEvent)

    @Query(
        """
        SELECT videoId, title, artist, channelId,
               COUNT(*) as playCount,
               AVG(completionRatio) as avgCompletion,
               SUM(CASE WHEN wasLiked = 1 THEN 1 ELSE 0 END) as likes,
               SUM(CASE WHEN wasSkipped = 1 THEN 1 ELSE 0 END) as skips,
               MAX(timestamp) as lastPlayed
        FROM listen_events
        WHERE timestamp > :since
        GROUP BY videoId
        ORDER BY playCount DESC, avgCompletion DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopSongsSince(
        since: Long,
        limit: Int = 20,
    ): List<SongStats>

    @Query(
        """
        SELECT channelId, artist as artistName,
               COUNT(*) as playCount,
               AVG(completionRatio) as avgCompletion,
               SUM(CASE WHEN wasLiked = 1 THEN 1 ELSE 0 END) as likeCount,
               SUM(CASE WHEN wasSkipped = 1 THEN 1 ELSE 0 END) as skipCount
        FROM listen_events
        WHERE timestamp > :since
        GROUP BY channelId
        ORDER BY playCount DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopArtistsSince(
        since: Long,
        limit: Int = 10,
    ): List<ArtistStats>

    @Query(
        """
        SELECT videoId, title, artist, channelId,
               COUNT(*) as playCount,
               AVG(completionRatio) as avgCompletion,
               SUM(CASE WHEN wasLiked = 1 THEN 1 ELSE 0 END) as likes,
               SUM(CASE WHEN wasSkipped = 1 THEN 1 ELSE 0 END) as skips,
               MAX(timestamp) as lastPlayed
        FROM listen_events
        WHERE wasLiked = 1
        GROUP BY videoId
        ORDER BY lastPlayed DESC
        LIMIT :limit
    """,
    )
    suspend fun getRecentlyLikedSongs(limit: Int = 10): List<SongStats>

    @Query(
        """
        SELECT channelId, artist as artistName,
               COUNT(*) as playCount,
               AVG(completionRatio) as avgCompletion,
               SUM(CASE WHEN wasLiked = 1 THEN 1 ELSE 0 END) as likeCount,
               SUM(CASE WHEN wasSkipped = 1 THEN 1 ELSE 0 END) as skipCount
        FROM listen_events
        WHERE wasLiked = 1
        GROUP BY channelId
        ORDER BY likeCount DESC, playCount DESC
        LIMIT :limit
    """,
    )
    suspend fun getFavoriteArtistsByLikes(limit: Int = 5): List<ArtistStats>

    @Query(
        """
        SELECT * FROM listen_events
        WHERE timeOfDay BETWEEN :hourMin AND :hourMax
        AND completionRatio > 0.7
        ORDER BY timestamp DESC LIMIT 50
    """,
    )
    suspend fun getSongsForTimeContext(
        hourMin: Int,
        hourMax: Int,
    ): List<ListenEvent>

    @Query(
        """
        SELECT genre, COUNT(*) as count
        FROM listen_events
        WHERE timestamp > :since AND genre != ''
        GROUP BY genre
        ORDER BY count DESC LIMIT 5
    """,
    )
    suspend fun getTopGenres(since: Long): List<GenreCount>

    @Query(
        """
        SELECT videoId FROM listen_events
        WHERE wasDisliked = 1
        GROUP BY videoId
        HAVING COUNT(*) >= 2
    """,
    )
    suspend fun getBlacklistedVideoIds(): List<String>

    @Query("DELETE FROM listen_events WHERE timestamp < :cutoff")
    suspend fun deleteOldListenEvents(cutoff: Long)
}
