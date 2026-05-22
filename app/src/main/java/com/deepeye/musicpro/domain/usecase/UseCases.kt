// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.usecase

import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Genre
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ═══════════════════════════════════════════════════
// Song Use Cases
// ═══════════════════════════════════════════════════

class GetAllSongsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> = repository.getAllSongs()
}

class GetSongByIdUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(id: Long): Flow<Song?> = repository.getSongById(id)
}

class GetSongsByAlbumUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(albumId: Long): Flow<List<Song>> = repository.getSongsByAlbum(albumId)
}

class GetSongsByArtistUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(artistId: Long): Flow<List<Song>> = repository.getSongsByArtist(artistId)
}

class SearchSongsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> = repository.searchSongs(query)
}

class GetRecentlyAddedUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<Song>> = repository.getRecentlyAdded(limit)
}

// ═══════════════════════════════════════════════════
// Album Use Cases
// ═══════════════════════════════════════════════════

class GetAllAlbumsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Album>> = repository.getAllAlbums()
}

class GetAlbumByIdUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(id: Long): Flow<Album?> = repository.getAlbumById(id)
}

// ═══════════════════════════════════════════════════
// Artist Use Cases
// ═══════════════════════════════════════════════════

class GetAllArtistsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Artist>> = repository.getAllArtists()
}

class GetArtistByIdUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(id: Long): Flow<Artist?> = repository.getArtistById(id)
}

// ═══════════════════════════════════════════════════
// Genre Use Cases
// ═══════════════════════════════════════════════════

class GetAllGenresUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Genre>> = repository.getAllGenres()
}

// ═══════════════════════════════════════════════════
// Playlist Use Cases
// ═══════════════════════════════════════════════════

class CreatePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(name: String): Long = repository.createPlaylist(name)
}

class DeletePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long) = repository.deletePlaylist(playlistId)
}

class AddSongToPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long) =
        repository.addSongToPlaylist(playlistId, songId)
}

class RemoveSongFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long) =
        repository.removeSongFromPlaylist(playlistId, songId)
}

// ═══════════════════════════════════════════════════
// Library Sync Use Case
// ═══════════════════════════════════════════════════

class SyncLibraryUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke() = repository.syncFromMediaStore()
}
