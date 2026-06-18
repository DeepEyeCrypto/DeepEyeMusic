// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

data class PlaylistBackupData(
    val playlists: List<com.deepeye.musicpro.data.db.LocalPlaylistEntity>,
    val crossRefs: List<com.deepeye.musicpro.data.db.PlaylistSongCrossRef>
)
