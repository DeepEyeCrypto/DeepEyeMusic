// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model.home

import androidx.compose.ui.graphics.Color

data class VideoRailItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val duration: String,
    val viewCount: String,
    val publishedAt: String,
    val isLive: Boolean = false,
    val isTrending: Boolean = false,
    val category: VideoRailCategory,
)

enum class VideoRailCategory {
    TRENDING_IN,
    BECAUSE_YOU_WATCHED,
    NEW_FROM_ARTISTS,
    TOP_CHARTS,
    SHORTS_VIBES,
}

data class VideoRailState(
    val sections: List<VideoRailSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentlyPlayingId: String? = null,
    val activeSection: VideoRailCategory = VideoRailCategory.TRENDING_IN,
)

data class VideoRailSection(
    val category: VideoRailCategory,
    val title: String,
    val subtitle: String,
    val items: List<VideoRailItem>,
    val accentColor: Color = Color(0xFF00E5FF),
)
