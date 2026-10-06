// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.autoplay

import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoplayRepository @Inject constructor(
    private val dao: RecommendationDao,
    private val contentFetcher: ContentFetcher,
    private val smartTubeEngine: SmartTubeEngine,
) {
    suspend fun generateNextQueue(
        currentTrack: MediaItem?,
        autoplayState: AutoplayState,
        activeQueueArtists: List<String> = emptyList(),
    ): List<QueueItem> = withContext(Dispatchers.IO) {
        if (currentTrack == null) return@withContext emptyList()

        val blacklist = dao.getBlacklistedVideoIds().toSet() + autoplayState.blacklist
        val recentHistory = autoplayState.sessionHistory.toSet()

        val candidates = coroutineScope {
            val smartTubeDeferred = async {
                if (currentTrack !is MediaItem.Local) {
                    try {
                        val auto = smartTubeEngine.getAlgorithmicNext(currentTrack.id)
                        if (auto != null && auto.videoId.isNotBlank()) {
                            listOf(
                                VideoItem(
                                    videoId = auto.videoId,
                                    title = auto.title,
                                    artist = auto.artist,
                                    channelId = "",
                                    duration = auto.durationSeconds.toString(),
                                    genre = "SmartTube-Algorithmic"
                                )
                            )
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else emptyList()
            }

            val relatedDeferred = async {
                try {
                    if (currentTrack is MediaItem.Local) {
                        val query = "${currentTrack.title} ${currentTrack.artist} song audio"
                        contentFetcher.searchByQuery(query, 20)
                    } else {
                        val isVideo = (currentTrack as? MediaItem.Remote)?.isVideo ?: false
                        contentFetcher.getRelatedVideos(currentTrack.id, 20, isVideo)
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val smartList = smartTubeDeferred.await()
            val relatedList = relatedDeferred.await()

            (smartList + relatedList)
                .distinctBy { it.videoId }
                .filterNot { it.videoId in blacklist || it.videoId in recentHistory }
        }

        candidates.take(20).mapIndexed { index, video ->
            QueueItem(
                videoId = video.videoId,
                title = video.title,
                artist = video.artist,
                channelId = video.channelId,
                reason = if (video.genre == "SmartTube-Algorithmic") "Up Next on YouTube" else "Similar Track",
                rank = index + 1,
                score = 1.0f - (index * 0.04f),
            )
        }
    }
}
