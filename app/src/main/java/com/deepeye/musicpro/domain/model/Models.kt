// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

import android.net.Uri

import androidx.compose.runtime.Immutable

/**
 * Domain model representing an album.
 */
@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val artUri: Uri?,
    val songCount: Int,
    val totalDuration: Long, // milliseconds
    val year: Int = 0,
)

/**
 * Domain model representing an artist.
 */
@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val artUri: Uri? = null,
)

/**
 * Domain model representing a user-created playlist.
 */
@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val totalDuration: Long, // milliseconds
    val artUri: Uri? = null,
    val createdAt: Long = 0, // epoch millis
    val modifiedAt: Long = 0, // epoch millis
)

/**
 * Domain model representing a music genre.
 */
@Immutable
data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int,
)
