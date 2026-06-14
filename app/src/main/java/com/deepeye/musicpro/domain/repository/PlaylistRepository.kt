// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.domain.model.Playlist
import com.deepeye.musicpro.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for playlist operations.
 */
interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>

    fun getPlaylistById(id: Long): Flow<Playlist?>

    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>>

    suspend fun createPlaylist(name: String): Long

    suspend fun renamePlaylist(
        playlistId: Long,
        newName: String,
    )

    suspend fun deletePlaylist(playlistId: Long)

    suspend fun addSongToPlaylist(
        playlistId: Long,
        songId: Long,
    )

    suspend fun removeSongFromPlaylist(
        playlistId: Long,
        songId: Long,
    )

    suspend fun reorderPlaylistSong(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int,
    )
}
