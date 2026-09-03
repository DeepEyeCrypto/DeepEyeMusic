// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent Room entity for user-hidden songs and artists.
 *
 * Account-scoped: [accountKey] is the primary isolation dimension.
 * Survives process death and app updates via Room database.
 *
 * Never includes tokens, cookies, or identifiable user data — only hide decisions.
 */
@Entity(
    tableName = "hidden_content",
    indices = [
        Index("accountKey"),
        Index("itemId"),
        Index("accountKey", "itemType"),
    ]
)
data class HiddenContentEntity(
    /** Opaque account identifier (e.g., token hash). Null = logged-out/local device hides. */
    val accountKey: String?,
    /** Unique item identifier (song ID, artist name, etc.). */
    val itemId: String,
    /** Title of the item (song title or artist name). */
    val title: String,
    /** Artist name (for songs); optional. */
    val artist: String? = null,
    /** Type of hidden content: "SONG", "ARTIST", etc. */
    val itemType: String = "SONG",
    /** Epoch millis when this hide decision was recorded. */
    val hiddenAt: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)
