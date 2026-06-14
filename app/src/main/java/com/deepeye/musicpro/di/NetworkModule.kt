// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing network and serialization dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): okhttp3.OkHttpClient {
        val cacheSize = (50 * 1024 * 1024).toLong() // 50 MB
        val cache = okhttp3.Cache(java.io.File(context.cacheDir, "http_cache"), cacheSize)

        return okhttp3.OkHttpClient.Builder()
            .cache(cache)
            .connectionPool(okhttp3.ConnectionPool(15, 5, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideNewPipeDownloader(
        client: okhttp3.OkHttpClient
    ): com.deepeye.musicpro.data.source.remote.youtube.NewPipeDownloader =
        com.deepeye.musicpro.data.source.remote.youtube.NewPipeDownloader(client)
}
