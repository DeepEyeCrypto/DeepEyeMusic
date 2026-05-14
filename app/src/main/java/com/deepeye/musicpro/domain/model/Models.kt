package com.deepeye.musicpro.domain.model

import android.net.Uri

/**
 * Domain model representing an album.
 */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val artUri: Uri?,
    val songCount: Int,
    val totalDuration: Long,   // milliseconds
    val year: Int = 0
)

/**
 * Domain model representing an artist.
 */
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val artUri: Uri? = null
)

/**
 * Domain model representing a user-created playlist.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val totalDuration: Long,   // milliseconds
    val artUri: Uri? = null,
    val createdAt: Long = 0,   // epoch millis
    val modifiedAt: Long = 0   // epoch millis
)

/**
 * Domain model representing a music genre.
 */
data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int
)
