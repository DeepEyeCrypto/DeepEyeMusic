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
}
