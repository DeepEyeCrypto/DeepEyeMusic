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
        LocalPlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlayEvent::class,
        UserFeedback::class,
        OnboardingPreferencesEntity::class,
        ListenEvent::class,
        ArtistScore::class,
        SongScore::class,
        com.deepeye.musicpro.data.cache.entities.CachedRecommendationRow::class,
        com.deepeye.musicpro.data.cache.entities.CachedTrack::class,
        com.deepeye.musicpro.data.cache.entities.CachedAutoplayQueue::class,
        com.deepeye.musicpro.data.cache.entities.CachedSearchResult::class,
        com.deepeye.musicpro.data.library.entities.LikedTrackEntity::class,
        com.deepeye.musicpro.data.library.entities.SavedTrackEntity::class,
        com.deepeye.musicpro.data.library.entities.PlaylistEntity::class,
        com.deepeye.musicpro.data.library.entities.PlaylistTrackEntity::class,
        com.deepeye.musicpro.data.library.entities.DownloadEntity::class,
        com.deepeye.musicpro.data.library.entities.RecentPlayEntity::class,
        SearchHistoryEntity::class,
        PlaybackHistoryEntity::class,
        VideoHistoryEntity::class,
        DownloadHistoryEntity::class,
        QueueSnapshotEntity::class,
        com.deepeye.musicpro.data.library.entities.SubscribedChannelEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun tasteDao(): TasteDao

    abstract fun recommendationDao(): RecommendationDao

    abstract fun cacheDao(): com.deepeye.musicpro.data.cache.dao.CacheDao

    abstract fun libraryDao(): com.deepeye.musicpro.data.library.dao.LibraryDao

    abstract fun historyDao(): HistoryDao
}
