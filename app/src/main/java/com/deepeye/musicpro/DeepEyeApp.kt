// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.deepeye.musicpro.dsp.data.PresetRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

/**
 * DeepEyeMusicPro Application class.
 * Entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class DeepEyeApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var presetRepository: PresetRepository

    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            presetRepository.seedBuiltinPresets()
        }
        scheduleBackgroundWorkers()
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = androidx.work.WorkManager.getInstance(this)

        val recRefresh =
            androidx.work.PeriodicWorkRequestBuilder<com.deepeye.musicpro.workers.RecommendationRefreshWorker>(
                6,
                java.util.concurrent.TimeUnit.HOURS,
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.MINUTES)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "rec_refresh",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            recRefresh,
        )

        val queuePrefetch =
            androidx.work.PeriodicWorkRequestBuilder<com.deepeye.musicpro.workers.AutoplayPrefetchWorker>(
                3,
                java.util.concurrent.TimeUnit.HOURS,
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        workManager.enqueueUniquePeriodicWork(
            "queue_prefetch",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            queuePrefetch,
        )

        val channelSync =
            androidx.work.PeriodicWorkRequestBuilder<com.deepeye.musicpro.workers.ChannelSyncWorker>(
                2,
                java.util.concurrent.TimeUnit.HOURS,
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        workManager.enqueueUniquePeriodicWork(
            "channel_sync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            channelSync,
        )
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(coil3.video.VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
