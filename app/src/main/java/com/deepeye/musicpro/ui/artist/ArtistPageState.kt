package com.deepeye.musicpro.ui.artist

import com.deepeye.musicpro.domain.model.search.SearchResultItem

import java.io.Serializable

data class ArtistPageState(
    val artistId: String = "",
    val artistName: String = "",
    val heroImageUrl: String? = null,
    val bio: String = "",
    val subscribersText: String = "",
    val isFollowing: Boolean = false,
    val topSongs: List<SearchResultItem> = emptyList(),
    val albums: List<SearchResultItem> = emptyList(),
    val videos: List<SearchResultItem> = emptyList(),
    val similarArtists: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
) : Serializable
