package com.deepeye.musicpro.domain.model.search

import java.io.Serializable

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: SearchFilter,
    val thumbnailUrl: String? = null,
    val artist: String? = null,
    val channelId: String? = null,
    val videoId: String? = null,
    val meta: Map<String, String> = emptyMap(),
    val score: Float = 0f,
) : Serializable
