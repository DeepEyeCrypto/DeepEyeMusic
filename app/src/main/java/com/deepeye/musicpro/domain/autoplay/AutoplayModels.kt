package com.deepeye.musicpro.domain.autoplay

import androidx.compose.runtime.Stable

enum class SeedSource {
    USER_PLAYED,
    ONBOARDING_PREF,
    FAVORITE_ARTIST,
    FAVORITE_LANGUAGE,
    CURRENT_CONTEXT,
    TRENDING,
    DISCOVERY,
}

data class AutoplaySeed(
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val language: String? = null,
    val genre: String? = null,
    val mood: String? = null,
    val source: SeedSource,
    val score: Float = 0f,
)

@Stable
data class QueueItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val reason: String,
    val rank: Int,
    val score: Float,
    val addedAt: Long = System.currentTimeMillis(),
    val artworkUri: String? = null,
    val isVideo: Boolean = false,
)

enum class AutoplayMode {
    FAMILIAR,
    DISCOVERY,
    BALANCED,
    FAMILIAR_PLUS,
}

@Stable
data class AutoplayState(
    val active: Boolean = true,
    val currentSeed: AutoplaySeed? = null,
    val queue: List<QueueItem> = emptyList(),
    val history: List<String> = emptyList(),
    val blacklist: Set<String> = emptySet(),
    val skipStreak: Int = 0,
    val likeStreak: Int = 0,
    val discoveryMode: Boolean = false,
    val familiarMode: Boolean = true,
    val lastGeneratedAt: Long = 0L,
)
