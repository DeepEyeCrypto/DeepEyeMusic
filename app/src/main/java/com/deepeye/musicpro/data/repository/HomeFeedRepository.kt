package com.deepeye.musicpro.data.repository

import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.home.HomeFeedState
import com.deepeye.musicpro.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeFeedRepository @Inject constructor(
    private val youtubeDs: YoutubeRemoteDataSource,
    private val localRepo: MusicRepository
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun getHomeFeed(): HomeFeedState =
        withContext(ioDispatcher) {
            // Run all in parallel
            val trendingDeferred  = async { youtubeDs.getTrending() }
            val shortsDeferred    = async { youtubeDs.getShorts() }
            val musicDeferred     = async { youtubeDs.searchMusic("top hits 2025") }
            val localDeferred     = async { localRepo.getRecentlyAdded(limit = 10).first() }

            val trending = trendingDeferred.await()
            val shorts   = shortsDeferred.await()
            val music    = musicDeferred.await()
            val local    = localDeferred.await()

            android.util.Log.d("HomeFeed", "Trending: ${trending.size}, Shorts: ${shorts.size}, Music: ${music.size}, Local: ${local.size}")

            HomeFeedState(
                featuredVideo     = trending.firstOrNull(),
                featuredMusic     = music.firstOrNull(),
                trending          = trending.drop(1).take(12),
                shorts            = shorts,
                quickPicks        = music.drop(1).take(10),
                isLoading         = false,
                isOffline         = trending.isEmpty() && music.isEmpty()
            )
        }
}
