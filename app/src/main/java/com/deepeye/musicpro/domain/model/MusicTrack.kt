// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.model

import java.io.Serializable

enum class LikeStatus {
    LIKED,
    DISLIKED,
    NEUTRAL
}

data class MusicTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val likeStatus: LikeStatus = LikeStatus.NEUTRAL
) : Serializable
