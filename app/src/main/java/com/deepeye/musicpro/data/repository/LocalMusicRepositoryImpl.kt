// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.repository

import android.net.Uri
import com.deepeye.musicpro.data.db.SongDao
import com.deepeye.musicpro.data.source.local.MediaStoreScanner
import com.deepeye.musicpro.domain.model.Album
import com.deepeye.musicpro.domain.model.Artist
import com.deepeye.musicpro.domain.model.Genre
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.repository.MusicRepository
import com.deepeye.musicpro.data.db.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [MusicRepository] backed by Room + MediaStore.
 *
 * The Room database serves as the single source of truth.
 * [syncFromMediaStore] refreshes from the device's MediaStore.
 */
@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreScanner: MediaStoreScanner
) : MusicRepository {

    // ── Songs ──

    override fun getAllSongs(): Flow<List<Song>> =
        songDao.getAllSongs().map { entities -> entities.map { it.toDomain() } }

    override fun getSongById(id: Long): Flow<Song?> =
        songDao.getSongById(id).map { it?.toDomain() }

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumId).map { entities -> entities.map { it.toDomain() } }

    override fun getSongsByArtist(artistId: Long): Flow<List<Song>> =
        songDao.getSongsByArtist(artistId).map { entities -> entities.map { it.toDomain() } }

    override fun getSongsByGenre(genreId: Long): Flow<List<Song>> =
        songDao.getSongsByGenre(genreId.toString()).map { entities -> entities.map { it.toDomain() } }

    override fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query).map { entities -> entities.map { it.toDomain() } }

    override fun getRecentlyAdded(limit: Int): Flow<List<Song>> =
        songDao.getRecentlyAdded(limit).map { entities -> entities.map { it.toDomain() } }

    // ── Albums ──

    override fun getAllAlbums(): Flow<List<Album>> =
        songDao.getAllAlbums().map { projections ->
            projections.map { p ->
                Album(
                    id = p.album_id,
                    title = p.album,
                    artist = p.artist,
                    artistId = p.artist_id,
                    artUri = p.art_uri?.let { Uri.parse(it) },
                    songCount = p.song_count,
                    totalDuration = p.total_duration,
                    year = p.year
                )
            }
        }

    override fun getAlbumById(id: Long): Flow<Album?> =
        songDao.getAlbumById(id).map { p ->
            p?.let {
                Album(
                    id = it.album_id,
                    title = it.album,
                    artist = it.artist,
                    artistId = it.artist_id,
                    artUri = it.art_uri?.let { uri -> Uri.parse(uri) },
                    songCount = it.song_count,
                    totalDuration = it.total_duration,
                    year = it.year
                )
            }
        }

    // ── Artists ──

    override fun getAllArtists(): Flow<List<Artist>> =
        songDao.getAllArtists().map { projections ->
            projections.map { p ->
                Artist(
                    id = p.artist_id,
                    name = p.artist,
                    albumCount = p.album_count,
                    songCount = p.song_count
                )
            }
        }

    override fun getArtistById(id: Long): Flow<Artist?> =
        songDao.getArtistById(id).map { p ->
            p?.let {
                Artist(
                    id = it.artist_id,
                    name = it.artist,
                    albumCount = it.album_count,
                    songCount = it.song_count
                )
            }
        }

    // ── Genres ──

    override fun getAllGenres(): Flow<List<Genre>> =
        songDao.getAllGenres().map { projections ->
            projections.mapIndexed { index, p ->
                Genre(
                    id = index.toLong(),
                    name = p.genre,
                    songCount = p.song_count
                )
            }
        }

    // ── Library Sync ──

    override suspend fun syncFromMediaStore() {
        val scannedSongs = mediaStoreScanner.scanAllSongs()
        songDao.deleteAll()
        songDao.insertAll(scannedSongs)
    }

    // ── Mapper ──

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
