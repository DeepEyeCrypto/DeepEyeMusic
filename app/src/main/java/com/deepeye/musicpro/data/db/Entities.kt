package com.deepeye.musicpro.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a song stored in the local database.
 * Maps 1:1 from MediaStore scan results.
 */
@Entity(tableName = "songs")
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
    val dateModified: Long = 0
)

/**
 * Room entity representing a user-created playlist.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis()
)

/**
 * Cross-reference entity for the many-to-many Playlist ↔ Song relationship.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlist_id", "song_id"]
)
data class PlaylistSongCrossRef(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
