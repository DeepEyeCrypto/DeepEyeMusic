package com.deepeye.musicpro.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deepeye.musicpro.data.cache.CacheManager
import com.deepeye.musicpro.domain.recommendation.RecommendationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RecommendationRefreshWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recEngine: RecommendationEngine,
    private val cacheManager: CacheManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val fresh = recEngine.buildRecommendations()
            cacheManager.saveRecommendations(fresh)
            cacheManager.cleanup()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
