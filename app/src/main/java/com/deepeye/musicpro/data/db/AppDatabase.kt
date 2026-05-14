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
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}
