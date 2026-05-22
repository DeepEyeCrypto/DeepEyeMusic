// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

import android.net.Uri

/**
 * Domain model representing a single audio track.
 *
 * This is the core data type flowing through the entire app —
 * from data layer (MediaStore/Room) through domain (use cases) to UI (ViewModels).
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val uri: Uri,
    val duration: Long,        // milliseconds
    val size: Long,            // bytes
    val path: String,
    val artUri: Uri?,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val dateAdded: Long = 0,   // epoch seconds
    val dateModified: Long = 0 // epoch seconds
)
