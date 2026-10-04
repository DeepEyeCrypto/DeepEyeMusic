# HERMES Offline-Omega Protocol (Caching Engine) Implementation Plan

## Goal
Implement an enterprise-grade offline caching and background pre-buffering engine for DeepEyeMusicPro using an application-wide Singleton `SimpleCache` with `CacheDataSource.Factory` in ExoPlayer and a Jetpack `WorkManager` `OpusDownloadWorker`.

## Current Context & Assumptions
- ExoPlayer is configured in `app/src/main/java/com/deepeye/musicpro/di/PlayerModule.kt` currently using standard `DefaultDataSource.Factory`.
- WorkManager dependencies (`androidx.work:work-runtime-ktx` & `androidx.hilt:hilt-work`) are available in `app/build.gradle.kts`.
- NowPlayingScreen has a download button wired to `PlayerViewModel.downloadCurrentTrack()`.

## Architecture & Proposed Approach
1. **Singleton Caching Layer (`PlayerModule.kt`)**:
   - Provide a Singleton `StandaloneDatabaseProvider`, `LeastRecentlyUsedCacheEvictor` (2 GB ceiling), and Singleton `SimpleCache` in `PlayerModule.kt`.
   - Build a `CacheDataSource.Factory` wrapping `DefaultHttpDataSource` with `FLAG_IGNORE_CACHE_ON_ERROR` and inject it into `DefaultMediaSourceFactory`.
2. **Resilient Background Worker (`OpusDownloadWorker.kt`)**:
   - Create `@HiltWorker` `OpusDownloadWorker` extending `CoroutineWorker` using ExoPlayer's `CacheWriter` to buffer ITAG 251 Opus streams directly into `SimpleCache` with deterministic `videoId` cache keys.
3. **UI Cache Querying & Offline State (`PlayerViewModel.kt` & `NowPlayingScreen.kt`)**:
   - Expose `isTrackCached(videoId)` by querying `SimpleCache.isCached(videoId, position, length)`.
   - Update download action button in `NowPlayingScreen.kt` to trigger `WorkManager` download and display cached state.

---

## Step-by-Step Implementation Tasks

### Task 1: Singleton `SimpleCache` & `CacheDataSource.Factory` in `PlayerModule.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/di/PlayerModule.kt`
- **Action**: Add providers for `SimpleCache` and `CacheDataSource.Factory`, and wire `CacheDataSource.Factory` into `ExoPlayer`'s `DefaultMediaSourceFactory`:
  ```kotlin
  @Provides
  @Singleton
  fun provideSimpleCache(
      @ApplicationContext context: Context
  ): androidx.media3.datasource.cache.SimpleCache {
      val cacheDir = java.io.File(context.cacheDir, "media_cache")
      val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(2L * 1024 * 1024 * 1024) // 2GB limit
      val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
      return androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider)
  }

  @Provides
  @Singleton
  fun provideCacheDataSourceFactory(
      @ApplicationContext context: Context,
      simpleCache: androidx.media3.datasource.cache.SimpleCache
  ): androidx.media3.datasource.cache.CacheDataSource.Factory {
      val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
          .setUserAgent("com.google.android.youtube/20.10.33 (Linux; U; Android 12)")
          .setConnectTimeoutMs(15000)
          .setReadTimeoutMs(20000)
          .setAllowCrossProtocolRedirects(true)
          .setDefaultRequestProperties(mapOf(
              "Accept" to "*/*",
              "Connection" to "keep-alive"
          ))
      val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
      return androidx.media3.datasource.cache.CacheDataSource.Factory()
          .setCache(simpleCache)
          .setUpstreamDataSourceFactory(upstreamFactory)
          .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
  }
  ```

### Task 2: Background `OpusDownloadWorker.kt` with `CacheWriter`
- **File**: `app/src/main/java/com/deepeye/musicpro/player/download/OpusDownloadWorker.kt`
- **Action**: Create `@HiltWorker` executing `CacheWriter` on `Dispatchers.IO`:
  ```kotlin
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
                  null
              )
              cacheWriter.cache()
              Result.success()
          } catch (e: Exception) {
              android.util.Log.e("OpusDownloadWorker", "Offline cache failed for $cacheKey", e)
              Result.retry()
          }
      }

      companion object {
          const val KEY_STREAM_URL = "key_stream_url"
          const val KEY_CACHE_KEY = "key_cache_key"
      }
  }
  ```

### Task 3: Cache Verification & WorkManager Enqueueing in `MusicDownloadManager.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/player/download/MusicDownloadManager.kt`
- **Action**: Inject `SimpleCache` and `WorkManager`. Add method `cacheTrackOffline(item: MediaItem)` to enqueue `OneTimeWorkRequestBuilder<OpusDownloadWorker>()` and `isCached(videoId: String): Boolean`.

### Task 4: ProGuard Rules & JNI Armor
- **File**: `app/proguard-rules.pro`
- **Action**: Add keep rules for `OpusDownloadWorker` and `androidx.media3.datasource.cache.**`.

### Task 5: Compilation & Physical Device Verification
- **Command**:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon assembleDebug
  ```
- **Verification**:
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  - Verify track caching during online playback and zero-latency offline start.

---

## Risks & Mitigations
- **SQLite DatabaseLockedException**: Avoided by strictly scoping `SimpleCache` as `@Singleton` in `PlayerModule.kt`.
- **Corrupted Cache Fragments**: Mitigated by `CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR` so broken cache blocks fall back directly to network without playback interruption.