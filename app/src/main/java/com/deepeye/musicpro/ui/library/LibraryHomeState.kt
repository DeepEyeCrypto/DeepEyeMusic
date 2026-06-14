package com.deepeye.musicpro.ui.library

import com.deepeye.musicpro.domain.model.library.LibraryItem

/**
 * Aggregated state for the Library home screen.
 * Counts are used for section headers; lists for content rows.
 */
data class LibraryHomeState(
    val likedCount: Int = 0,
    val playlistCount: Int = 0,
    val downloadCount: Int = 0,
    val recentCount: Int = 0,
    val likedTracks: List<LibraryItem> = emptyList(),
    val playlists: List<LibraryItem> = emptyList(),
    val downloads: List<LibraryItem> = emptyList(),
    val recentPlays: List<LibraryItem> = emptyList(),
    val isLoading: Boolean = false,
)
