package com.deepeye.musicpro.domain.model.library

data class LibraryItem(
    val id: String,
    val type: LibraryItemType,
    val title: String,
    val subtitle: String = "",
    val artworkUrl: String? = null,
    val videoId: String? = null,
    val artist: String? = null,
    val downloadState: DownloadState = DownloadState.NONE,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isOfflineAvailable: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
)
