// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

import android.net.Uri

import androidx.compose.runtime.Immutable

@Immutable
sealed class MediaItem {
    abstract val id: String
    abstract val title: String
    abstract val artist: String
    abstract val artworkUri: Uri?
    abstract val duration: Long

    @Immutable
    data class Local(
        val song: Song,
    ) : MediaItem() {
        override val id: String = song.id.toString()
        override val title: String = song.title
        override val artist: String = song.artist
        override val artworkUri: Uri? = song.artUri
        override val duration: Long = song.duration
    }

    @Immutable
    data class Remote(
        override val id: String,
        override val title: String,
        override val artist: String,
        override val artworkUri: Uri?,
        override val duration: Long,
        val streamUri: Uri? = null,
        val isVideo: Boolean = false,
    ) : MediaItem()
}
