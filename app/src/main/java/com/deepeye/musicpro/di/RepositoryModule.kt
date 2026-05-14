package com.deepeye.musicpro.di

import com.deepeye.musicpro.data.repository.LocalMusicRepositoryImpl
import com.deepeye.musicpro.data.repository.PlaylistRepositoryImpl
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their implementations.
 * This is the bridge between domain and data layers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: LocalMusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
}
