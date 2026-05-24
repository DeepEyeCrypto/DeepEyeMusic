// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import android.content.Context
import androidx.room.Room
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "deepeye_music.db"
        )
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
    fun provideRecommendationDao(database: AppDatabase): com.deepeye.musicpro.data.db.RecommendationDao = database.recommendationDao()

    @Provides
    @Singleton
    fun provideDspDatabase(@ApplicationContext context: Context): com.deepeye.musicpro.dsp.data.DspDatabase {
        return Room.databaseBuilder(
            context,
            com.deepeye.musicpro.dsp.data.DspDatabase::class.java,
            "deepeye_dsp.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDspPresetDao(database: com.deepeye.musicpro.dsp.data.DspDatabase): com.deepeye.musicpro.dsp.data.DspPresetDao =
        database.dspPresetDao()
}
