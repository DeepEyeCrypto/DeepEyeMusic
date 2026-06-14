// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model.home

data class HomeFeedState(
    val featuredVideo: HomeVideoItem? = null,
    val featuredMusic: HomeMusicItem? = null,
    val trending: List<HomeVideoItem> = emptyList(),
    val shorts: List<HomeVideoItem> = emptyList(),
    val quickPicks: List<HomeMusicItem> = emptyList(),
    val continueWatching: List<HomeVideoItem> = emptyList(),
    val continueListening: List<HomeMusicItem> = emptyList(),
    val localResume: List<HomeMusicItem> = emptyList(),
    val moodMixes: List<MoodMix> = emptyList(),
    val activeDspPreset: String? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
)

data class HomeVideoItem(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String = "",
    val thumbnailUrl: String,
    val duration: Long = 0,
    val viewCount: Long = 0,
    val uploadDate: String = "",
    val isShort: Boolean = false,
    val isLive: Boolean = false,
    val streamUrl: String? = null,
    val lastWatchedPosition: Long = 0, // millis — for continue watching
    val progressPercent: Float = 0f,
)

data class HomeMusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: Long = 0,
    val playCount: Long = 0,
    val type: MusicItemType = MusicItemType.SONG,
    val streamUrl: String? = null,
    val lastPlayedAt: Long = 0,
)

data class MoodMix(
    val mood: MoodCategory,
    val label: String,
    val query: String,
    val emoji: String,
    val accentColor: Long = 0xFF7B3FE4,
)

enum class MoodCategory {
    CHILL,
    ENERGETIC,
    ROMANTIC,
    FOCUS,
    SAD,
    PARTY,
    WORKOUT,
    SLEEP,
}

enum class RailType {
    CONTINUE_WATCHING,
    CONTINUE_LISTENING,
    SHORTS,
    QUICK_PICKS,
    TRENDING,
    NEW_RELEASES,
    LOCAL_RESUME,
    MOOD_MIX,
}

enum class MusicItemType {
    SONG,
    ALBUM,
    PLAYLIST,
}
