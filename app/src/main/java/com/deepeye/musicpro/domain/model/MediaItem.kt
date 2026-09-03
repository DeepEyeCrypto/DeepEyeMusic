// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaItem as Media3Item

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

fun MediaItem.toMedia3Item(): Media3Item {
    val uri = when (this) {
        is MediaItem.Local -> song.uri
        is MediaItem.Remote -> streamUri ?: Uri.EMPTY
    }

    val builder = Media3Item.Builder()
        .setUri(uri)
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUri)
                .build(),
        )

    val uriStr = uri.toString()
    if (uriStr.startsWith("data:application/dash+xml") || uriStr.contains("manifest/dash") || uriStr.contains(".mpd") || uriStr.contains("dash")) {
        builder.setMimeType(MimeTypes.APPLICATION_MPD)
    } else if (uriStr.contains("manifest/hls") || uriStr.contains(".m3u8") || uriStr.contains("m3u8")) {
        builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    }

    return builder.build()
}

