package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Genre
import com.deepeye.musicpro.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for local music library operations.
 *
 * Data layer implements this interface — domain layer depends only on this contract.
 */
interface MusicRepository {

    // ── Songs ──
    fun getAllSongs(): Flow<List<Song>>
    fun getSongById(id: Long): Flow<Song?>
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    fun getSongsByArtist(artistId: Long): Flow<List<Song>>
    fun getSongsByGenre(genreId: Long): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    fun getRecentlyAdded(limit: Int = 20): Flow<List<Song>>

    // ── Albums ──
    fun getAllAlbums(): Flow<List<Album>>
    fun getAlbumById(id: Long): Flow<Album?>

    // ── Artists ──
    fun getAllArtists(): Flow<List<Artist>>
    fun getArtistById(id: Long): Flow<Artist?>

    // ── Genres ──
    fun getAllGenres(): Flow<List<Genre>>

    // ── Library Sync ──
    suspend fun syncFromMediaStore()
}
