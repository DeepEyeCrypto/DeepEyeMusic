// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository.video

import androidx.compose.ui.graphics.Color
import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.domain.model.home.VideoRailCategory
import com.deepeye.musicpro.domain.model.home.VideoRailItem
import com.deepeye.musicpro.domain.model.home.VideoRailSection
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient

@Singleton
class VideoRailRepository
@Inject
constructor(
    private val contentFetcher: ContentFetcher,
    private val recommendationDao: RecommendationDao,
    private val settingsDataStore: SettingsDataStore,
    private val authClient: AuthenticatedYouTubeClient,
) {
    suspend fun loadAllSections(): List<VideoRailSection> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val authSettings = try { settingsDataStore.settings.first() } catch (e: Exception) { null }
                val hasAuth = authSettings?.youtubeAccessToken != null

                if (hasAuth) {
                    val authHome = try { authClient.getHomeFeed() } catch (e: Exception) { emptyList() }
                    
                    return@coroutineScope buildList {
                        if (authHome.isNotEmpty()) {
                            add(
                                VideoRailSection(
                                    category = VideoRailCategory.TRENDING_IN,
                                    title = "🌟 For You",
                                    subtitle = "Personalized Recommendations",
                                    items = authHome.take(20).map {
                                        com.deepeye.musicpro.domain.model.home.VideoRailItem(
                                            videoId = it.id,
                                            title = it.title,
                                            channelName = it.channelName,
                                            thumbnailUrl = it.thumbnailUrl,
                                            duration = "${it.duration / 60}:${(it.duration % 60).toString().padStart(2, '0')}",
                                            viewCount = "",
                                            publishedAt = "",
                                            isLive = it.isLive,
                                            isTrending = false,
                                            category = VideoRailCategory.TRENDING_IN
                                        )
                                    },
                                    accentColor = Color(0xFFFF6B35),
                                )
                            )
                            if (authHome.size > 20) {
                                add(
                                    VideoRailSection(
                                        category = VideoRailCategory.TOP_CHARTS,
                                        title = "🎯 Discover More",
                                        subtitle = "Fresh content",
                                        items = authHome.drop(20).take(20).map {
                                            com.deepeye.musicpro.domain.model.home.VideoRailItem(
                                                videoId = it.id,
                                                title = it.title,
                                                channelName = it.channelName,
                                                thumbnailUrl = it.thumbnailUrl,
                                                duration = "${it.duration / 60}:${(it.duration % 60).toString().padStart(2, '0')}",
                                                viewCount = "",
                                                publishedAt = "",
                                                isLive = it.isLive,
                                                isTrending = false,
                                                category = VideoRailCategory.TOP_CHARTS
                                            )
                                        },
                                        accentColor = Color(0xFFE91E63),
                                    )
                                )
                            }
                        }
                    }
                }

                // Get user's top songs for seeding
                val topSongs =
                    recommendationDao
                        .getTopSongsSince(
                            System.currentTimeMillis() - 30L * 86400_000,
                            limit = 3,
                        )

                // Parallel fetches
                val trendingDeferred =
                    async {
                        contentFetcher.getTrendingMusic("IN", 15)
                    }
                val relatedDeferred =
                    async {
                        topSongs.firstOrNull()?.let { song ->
                            contentFetcher.getRelatedVideos(song.videoId, 12)
                        } ?: emptyList()
                    }
                val topChartsDeferred =
                    async {
                        contentFetcher.searchByQuery(
                            "top music videos india 2026",
                            10,
                        )
                    }

                val trending = trendingDeferred.await()
                val related = relatedDeferred.await()
                val topCharts = topChartsDeferred.await()

                // Build sections
                buildList {
                    if (trending.isNotEmpty()) {
                        add(
                            VideoRailSection(
                                category = VideoRailCategory.TRENDING_IN,
                                title = "🔥 Trending in India",
                                subtitle = "Updated today",
                                items = trending.map {
                                    it.toRailItem(
                                        VideoRailCategory.TRENDING_IN,
                                        isTrending = true
                                    )
                                },
                                accentColor = Color(0xFFFF6B35),
                            ),
                        )
                    }
                    if (related.isNotEmpty()) {
                        add(
                            VideoRailSection(
                                category = VideoRailCategory.BECAUSE_YOU_WATCHED,
                                title = "▶️ Because You Watched",
                                subtitle =
                                topSongs.firstOrNull()?.title
                                    ?: "Your recent plays",
                                items = related.map { it.toRailItem(VideoRailCategory.BECAUSE_YOU_WATCHED) },
                                accentColor = Color(0xFF00E5FF),
                            ),
                        )
                    }
                    if (topCharts.isNotEmpty()) {
                        add(
                            VideoRailSection(
                                category = VideoRailCategory.TOP_CHARTS,
                                title = "🏆 Top Music Videos",
                                subtitle = "Most watched this week",
                                items = topCharts.map { it.toRailItem(VideoRailCategory.TOP_CHARTS) },
                                accentColor = Color(0xFFFFCC00),
                            ),
                        )
                    }
                }
            }
        }

    private fun VideoItem.toRailItem(
        category: VideoRailCategory,
        isTrending: Boolean = false,
    ): VideoRailItem {
        return VideoRailItem(
            videoId = this.videoId,
            title = this.title,
            channelName = this.artist,
            thumbnailUrl = "https://i.ytimg.com/vi/${this.videoId}/maxresdefault.jpg",
            duration = "3:45", // Mock since VideoItem doesn't have it
            viewCount = "1M views", // Mock
            publishedAt = "Recently", // Mock
            isLive = false,
            isTrending = isTrending,
            category = category,
        )
    }
}
