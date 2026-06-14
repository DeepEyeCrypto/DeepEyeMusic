package com.deepeye.musicpro.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepeye.musicpro.data.cache.CacheManager
import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.domain.autoplay.AutoplayRepository
import com.deepeye.musicpro.domain.autoplay.AutoplayState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutoplayPrefetchWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val autoplayRepo: AutoplayRepository,
    private val cacheManager: CacheManager,
    private val dao: RecommendationDao,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            // Get top 3 songs, prefetch queue for each
            val topSongs =
                dao.getTopSongsSince(
                    System.currentTimeMillis() - 7 * 86400_000L,
                    3,
                )
            for (song in topSongs) {
                val trackInfo =
                    com.deepeye.musicpro.domain.model.MediaItem.Remote(
                        id = song.videoId,
                        title = song.title,
                        artist = song.artist,
                        artworkUri = null,
                        isVideo = false,
                        duration = 0L,
                    )
                val queue =
                    autoplayRepo.generateNextQueue(
                        currentTrack = trackInfo,
                        autoplayState = AutoplayState(),
                    )
                cacheManager.saveAutoplayQueue(song.videoId, queue)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
