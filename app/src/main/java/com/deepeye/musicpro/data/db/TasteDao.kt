// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TasteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvent(event: PlayEvent)

    @Query("SELECT * FROM play_events ORDER BY timestamp DESC")
    fun getAllPlayEvents(): Flow<List<PlayEvent>>

    @Query("SELECT * FROM play_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PlayEvent>>

    @Query("SELECT * FROM play_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getHistorySince(sinceTimestamp: Long): Flow<List<PlayEvent>>

    @Query(
        """
        SELECT song_id AS trackId, title, artist_id AS artist, artwork_uri AS artworkUri,
               SUM(played_ms) AS totalPlayTimeMs, COUNT(*) AS playCount
        FROM play_events
        WHERE timestamp >= :sinceTimestamp
        GROUP BY song_id
        ORDER BY totalPlayTimeMs DESC
        LIMIT :limit
        """
    )
    fun getTopTracks(sinceTimestamp: Long, limit: Int): Flow<List<TopTrackResult>>

    @Query(
        """
        SELECT song_id AS trackId, title, artist_id AS artist, artwork_uri AS artworkUri,
               SUM(played_ms) AS totalPlayTimeMs, COUNT(*) AS playCount
        FROM play_events
        GROUP BY song_id
        ORDER BY totalPlayTimeMs DESC
        LIMIT :limit
        """
    )
    fun getTopTracksAllTime(limit: Int): Flow<List<TopTrackResult>>

    @Query("SELECT * FROM play_events WHERE song_id = :songId ORDER BY timestamp DESC")
    suspend fun getPlayEventsForSong(songId: String): List<PlayEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: UserFeedback)

    @Query("SELECT * FROM user_feedback WHERE song_id = :songId")
    suspend fun getFeedback(songId: String): UserFeedback?

    @Query("SELECT * FROM user_feedback WHERE song_id = :songId")
    fun getFeedbackFlow(songId: String): Flow<UserFeedback?>

    @Query("SELECT * FROM user_feedback")
    fun getAllFeedback(): Flow<List<UserFeedback>>
}

/**
 * Projection for top tracks aggregation query.
 */
data class TopTrackResult(
    val trackId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val totalPlayTimeMs: Long,
    val playCount: Int,
)
