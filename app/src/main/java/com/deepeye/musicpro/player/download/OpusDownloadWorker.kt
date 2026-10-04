// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.download

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enterprise WorkManager background worker for caching ITAG 251 Opus audio streams
 * directly into ExoPlayer's SimpleCache for zero-latency, resilient offline playback.
 */
@HiltWorker
class OpusDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val simpleCache: SimpleCache,
    private val cacheDataSourceFactory: CacheDataSource.Factory
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()
        val cacheKey = inputData.getString(KEY_CACHE_KEY) ?: return@withContext Result.failure()

        try {
            android.util.Log.i(TAG, "Starting stealth background cache for key: $cacheKey")
            val dataSpec = DataSpec.Builder()
                .setUri(Uri.parse(streamUrl))
                .setKey(cacheKey)
                .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                .build()

            val dataSource = cacheDataSourceFactory.createDataSource()
            val cacheWriter = CacheWriter(
                dataSource,
                dataSpec,
                null,
                { requestLength, bytesCached, _ ->
                    if (requestLength > 0) {
                        val progressPercent = ((bytesCached * 100) / requestLength).toInt()
                        android.util.Log.d(TAG, "Caching $cacheKey progress: $progressPercent%")
                    }
                }
            )

            cacheWriter.cache()
            android.util.Log.i(TAG, "Successfully cached $cacheKey into SimpleCache")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Offline cache failed for $cacheKey: ${e.message}", e)
            if (isStopped) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "OpusDownloadWorker"
        const val KEY_STREAM_URL = "key_stream_url"
        const val KEY_CACHE_KEY = "key_cache_key"
    }
}
