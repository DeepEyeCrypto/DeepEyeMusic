// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main Room database for DeepEyeMusicPro.
 *
 * Contains the songs table (synced from MediaStore) and
 * playlist tables (user-created playlists with song cross-references).
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlayEvent::class,
        UserFeedback::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun tasteDao(): TasteDao
}
