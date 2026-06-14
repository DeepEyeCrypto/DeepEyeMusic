package com.deepeye.musicpro.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listen_events")
data class ListenEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val genre: String = "",
    // Engagement signals
    val listenDurationMs: Long,
    val totalDurationMs: Long,
    val completionRatio: Float,
    val wasSkipped: Boolean,
    val wasLiked: Boolean,
    val wasDisliked: Boolean,
    val wasAddedToPlaylist: Boolean,
    val wasReplayed: Boolean,
    val seekCount: Int,
    val shareCount: Int,
    // Context signals
    val timeOfDay: Int,
    val dayOfWeek: Int,
    val isHeadphonesOn: Boolean,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "artist_scores")
data class ArtistScore(
    @PrimaryKey val channelId: String,
    val artistName: String,
    val totalScore: Float,
    val playCount: Int,
    val avgCompletion: Float,
    val likeCount: Int,
    val skipCount: Int,
    val lastPlayedAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "song_scores")
data class SongScore(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val preferenceScore: Float,
    val playCount: Int,
    val totalListenMs: Long,
    val lastPlayedAt: Long,
    val isBlacklisted: Boolean = false,
)

// Data classes for DAO projections
data class SongStats(
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val playCount: Int,
    val avgCompletion: Float,
    val likes: Int,
    val skips: Int,
    val lastPlayed: Long,
)

data class ArtistStats(
    val channelId: String,
    val artistName: String,
    val playCount: Int,
    val avgCompletion: Float,
    val likeCount: Int,
    val skipCount: Int,
)

data class GenreCount(
    val genre: String,
    val count: Int,
)
