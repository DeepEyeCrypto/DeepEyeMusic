// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.deepeye.musicpro.data.db.AppDatabase
import com.deepeye.musicpro.data.db.PlaylistDao
import com.deepeye.musicpro.data.db.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscribed_channels` (
                        `channelId` TEXT NOT NULL,
                        `channelName` TEXT NOT NULL,
                        `lastSeenVideoId` TEXT NOT NULL,
                        `subscribedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`channelId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_personalized_sections` (
                        `accountKey` TEXT NOT NULL,
                        `sectionType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT,
                        `sourceLabel` TEXT NOT NULL,
                        `explanation` TEXT,
                        `cachedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        PRIMARY KEY(`accountKey`, `sectionType`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_personalized_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountKey` TEXT NOT NULL,
                        `sectionType` TEXT NOT NULL,
                        `itemId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `channelId` TEXT,
                        `artworkUrl` TEXT,
                        `durationMs` INTEGER NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `sourceBadge` TEXT,
                        `explanation` TEXT,
                        `rank` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_cached_personalized_items_accountKey_sectionType`
                    ON `cached_personalized_items` (`accountKey`, `sectionType`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_cached_personalized_items_itemId`
                    ON `cached_personalized_items` (`itemId`)
                    """.trimIndent()
                )
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "deepeye_music.db",
        )
            .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideTasteDao(database: AppDatabase): com.deepeye.musicpro.data.db.TasteDao = database.tasteDao()

    @Provides
    fun provideRecommendationDao(
        database: AppDatabase
    ): com.deepeye.musicpro.data.db.RecommendationDao = database.recommendationDao()

    @Provides
    fun provideCacheDao(database: AppDatabase): com.deepeye.musicpro.data.cache.dao.CacheDao = database.cacheDao()

    @Provides
    @Singleton
    fun providePersonalizedSectionDao(
        database: AppDatabase
    ): com.deepeye.musicpro.data.cache.dao.PersonalizedSectionDao = database.personalizedSectionDao()

    @Provides
    @Singleton
    fun provideLibraryDao(
        database: AppDatabase,
    ): com.deepeye.musicpro.data.library.dao.LibraryDao = database.libraryDao()

    @Provides
    @Singleton
    fun provideHistoryDao(
        database: AppDatabase,
    ): com.deepeye.musicpro.data.db.HistoryDao = database.historyDao()

    @Provides
    @Singleton
    fun provideDspDatabase(
        @ApplicationContext context: Context,
    ): com.deepeye.musicpro.dsp.data.DspDatabase {
        return Room.databaseBuilder(
            context,
            com.deepeye.musicpro.dsp.data.DspDatabase::class.java,
            "deepeye_dsp.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDspPresetDao(
        database: com.deepeye.musicpro.dsp.data.DspDatabase
    ): com.deepeye.musicpro.dsp.data.DspPresetDao =
        database.dspPresetDao()

    @Provides
    fun provideDspProfileDao(
        database: com.deepeye.musicpro.dsp.data.DspDatabase
    ): com.deepeye.musicpro.dsp.data.DspProfileDao =
        database.dspProfileDao()
}
