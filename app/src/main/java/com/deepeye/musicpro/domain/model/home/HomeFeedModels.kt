package com.deepeye.musicpro.domain.model.home

import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Song

data class HomeFeedState(
    val featuredVideo: HomeVideoItem? = null,
    val featuredMusic: HomeMusicItem? = null,
    val trending: List<HomeVideoItem> = emptyList(),
    val shorts: List<HomeVideoItem> = emptyList(),
    val quickPicks: List<HomeMusicItem> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
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
    val streamUrl: String? = null
)

data class HomeMusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: Long = 0,
    val playCount: Long = 0,
    val type: MusicItemType = MusicItemType.SONG,
    val streamUrl: String? = null
)

enum class RailType {
    CONTINUE_WATCHING,
    CONTINUE_LISTENING,
    SHORTS,
    QUICK_PICKS,
    TRENDING,
    NEW_RELEASES,
    LOCAL_RESUME
}

enum class MusicItemType {
    SONG, ALBUM, PLAYLIST
}
