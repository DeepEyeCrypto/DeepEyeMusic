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
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                if (url.contains("/youtubei/v1")) {
                    val newRequestBuilder = request.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)")
                        .header("X-YouTube-Client-Name", "85")
                        .header("X-YouTube-Client-Version", "7.20210614.03.00")
                        .header("Content-Type", "application/json")
                    chain.proceed(newRequestBuilder.build())
                } else {
                    chain.proceed(request)
                }
            }
            .build()
    }
}
