// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import android.net.Uri
import com.deepeye.musicpro.data.db.PlaylistDao
import com.deepeye.musicpro.data.db.PlaylistEntity
import com.deepeye.musicpro.data.db.SongEntity
import com.deepeye.musicpro.domain.model.Playlist
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PlaylistRepository] backed by Room.
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getPlaylistById(id: Long): Flow<Playlist?> =
        playlistDao.getPlaylistById(id).map { it?.toDomain() }

    override fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> =
        playlistDao.getPlaylistSongs(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    override suspend fun renamePlaylist(playlistId: Long, newName: String) =
        playlistDao.renamePlaylist(playlistId, newName)

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) =
        playlistDao.addSongToPlaylistEnd(playlistId, songId)

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)

    override suspend fun reorderPlaylistSong(playlistId: Long, fromIndex: Int, toIndex: Int) {
        // TODO: Implement reorder via batch update of sort_order values
    }

    // ── Mappers ──

    private fun PlaylistEntity.toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        songCount = 0,  // Populated reactively via separate query
        totalDuration = 0,
        createdAt = createdAt,
        modifiedAt = modifiedAt
    )

    private fun SongEntity.toDomain(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        artistId = artistId,
        uri = Uri.parse(uri),
        duration = duration,
        size = size,
        path = path,
        artUri = artUri?.let { Uri.parse(it) },
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        dateAdded = dateAdded,
        dateModified = dateModified
    )
}
