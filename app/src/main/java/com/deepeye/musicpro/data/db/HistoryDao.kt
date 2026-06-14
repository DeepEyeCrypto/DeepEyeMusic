// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // --- Search History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // --- Playback History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackEvent(event: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY played_at DESC LIMIT :limit")
    fun getRecentPlaybacks(limit: Int = 50): Flow<List<PlaybackHistoryEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearPlaybackHistory()

    // --- Video History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoHistory(event: VideoHistoryEntity)

    @Query("SELECT * FROM video_history ORDER BY watched_at DESC LIMIT :limit")
    fun getRecentVideos(limit: Int = 20): Flow<List<VideoHistoryEntity>>

    @Query("SELECT * FROM video_history WHERE video_id = :videoId LIMIT 1")
    suspend fun getVideoHistory(videoId: String): VideoHistoryEntity?

    @Query("DELETE FROM video_history")
    suspend fun clearVideoHistory()

    // --- Download History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadHistory(event: DownloadHistoryEntity)

    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun getDownloadHistory(): Flow<List<DownloadHistoryEntity>>

    @Query("DELETE FROM download_history")
    suspend fun clearDownloadHistory()

    // --- Queue Snapshot ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueueSnapshot(snapshot: QueueSnapshotEntity)

    @Query("SELECT * FROM queue_history WHERE id = 1 LIMIT 1")
    suspend fun getQueueSnapshot(): QueueSnapshotEntity?

    @Query("UPDATE queue_history SET current_index = :index, timestamp = :timestamp WHERE id = 1")
    suspend fun updateQueueIndex(index: Int, timestamp: Long = System.currentTimeMillis())
}
