// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a song stored in the local database.
 * Maps 1:1 from MediaStore scan results.
 */
@Entity(
    tableName = "songs",
    indices = [
        androidx.room.Index(value = ["title", "artist"])
    ]
)
data class SongEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "album")
    val album: String,
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    @ColumnInfo(name = "artist_id")
    val artistId: Long,
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "duration")
    val duration: Long,
    @ColumnInfo(name = "size")
    val size: Long,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "art_uri")
    val artUri: String?,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int = 0,
    @ColumnInfo(name = "year")
    val year: Int = 0,
    @ColumnInfo(name = "genre")
    val genre: String = "",
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = 0,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0,
)

/**
 * Room entity representing a user-created playlist.
 */
@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
)

/**
 * Cross-reference entity for the many-to-many Playlist ↔ Song relationship.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlist_id", "song_id"],
)
data class PlaylistSongCrossRef(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    @ColumnInfo(name = "song_id")
    val songId: Long,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity representing a song playback event.
 */
@Entity(
    tableName = "play_events",
    indices = [
        androidx.room.Index(value = ["song_id"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class PlayEvent(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "song_id")
    val songId: String,
    @ColumnInfo(name = "artist_id")
    val artistId: String,
    @ColumnInfo(name = "language")
    val language: String,
    @ColumnInfo(name = "played_ms")
    val playedMs: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "source")
    val source: String, // "local" or "youtube"
    @ColumnInfo(name = "completed_fully")
    val completedFully: Boolean = false,
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,
    @ColumnInfo(name = "title")
    val title: String = "",
)

/**
 * Room entity representing user feedback (like/dislike/skip) for a song.
 */
@Entity(tableName = "user_feedback")
data class UserFeedback(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: String,
    @ColumnInfo(name = "liked")
    val liked: Boolean = false,
    @ColumnInfo(name = "skipped_quickly")
    val skippedQuickly: Boolean = false,
    @ColumnInfo(name = "dont_play_again")
    val dontPlayAgain: Boolean = false,
)

/**
 * Room entity storing first-launch onboarding preferences.
 * Persists user's language, genre, and artist selections.
 */
@Entity(tableName = "onboarding_preferences")
data class OnboardingPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Singleton row
    @ColumnInfo(name = "languages")
    val languages: String = "", // JSON array of selected language codes
    @ColumnInfo(name = "genres")
    val genres: String = "", // JSON array of selected genres
    @ColumnInfo(name = "artists")
    val artists: String = "", // JSON array of selected artist names
    @ColumnInfo(name = "completed_at")
    val completedAt: Long = 0,
)
