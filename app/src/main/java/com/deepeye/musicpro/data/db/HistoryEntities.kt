// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "source")
    val source: String = "local", // local, youtube, etc.
    @ColumnInfo(name = "result_type")
    val resultType: String = "song",
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "album")
    val album: String = "",
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,
    @ColumnInfo(name = "played_at")
    val playedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "play_duration_ms")
    val playDurationMs: Long,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long,
    @ColumnInfo(name = "completion_percent")
    val completionPercent: Float,
    @ColumnInfo(name = "source")
    val source: String,
)

@Entity(tableName = "video_history")
data class VideoHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String? = null,
    @ColumnInfo(name = "position_ms")
    val positionMs: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "watched_at")
    val watchedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completion_percent")
    val completionPercent: Float,
)

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "download_id")
    val downloadId: Long,
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "status")
    val status: String, // COMPLETED, FAILED, DELETED
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "queue_history")
data class QueueSnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Singleton row for the most recent queue snapshot
    @ColumnInfo(name = "queue_json")
    val queueJson: String, // Serialized List<MediaItem>
    @ColumnInfo(name = "current_index")
    val currentIndex: Int,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)
