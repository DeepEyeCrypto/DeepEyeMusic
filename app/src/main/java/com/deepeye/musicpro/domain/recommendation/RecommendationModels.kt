package com.deepeye.musicpro.domain.recommendation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
data class VideoItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val duration: String,
    val genre: String = "",
)

data class ScoredVideo(
    val video: VideoItem,
    val score: Float,
)

@Stable
data class RecommendationRow(
    val title: String,
    val subtitle: String = "",
    val items: List<VideoItem>,
)

@Immutable
data class RecommendationResult(
    val becauseYouListened: List<RecommendationRow>,
    val favoriteArtists: List<RecommendationRow>,
    val perfectForNow: RecommendationRow,
    val trending: RecommendationRow,
    val genreDive: List<RecommendationRow>,
    val hiddenGems: RecommendationRow,
)
