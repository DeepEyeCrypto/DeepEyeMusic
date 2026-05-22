// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for song-related database operations.
 * All queries return Flow for reactive UI updates.
 */
@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongById(id: Long): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist_id = :artistId ORDER BY album ASC, track_number ASC")
    fun getSongsByArtist(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY date_added DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int): Flow<List<SongEntity>>

    // ── Aggregation queries for Albums ──
    @Query("""
        SELECT DISTINCT album_id, album, artist, artist_id, art_uri, 
               COUNT(*) as song_count, SUM(duration) as total_duration,
               MAX(year) as year
        FROM songs 
        GROUP BY album_id 
        ORDER BY album ASC
    """)
    fun getAllAlbums(): Flow<List<AlbumProjection>>

    @Query("""
        SELECT DISTINCT album_id, album, artist, artist_id, art_uri,
               COUNT(*) as song_count, SUM(duration) as total_duration,
               MAX(year) as year
        FROM songs 
        WHERE album_id = :albumId 
        GROUP BY album_id
    """)
    fun getAlbumById(albumId: Long): Flow<AlbumProjection?>

    // ── Aggregation queries for Artists ──
    @Query("""
        SELECT DISTINCT artist_id, artist,
               COUNT(DISTINCT album_id) as album_count,
               COUNT(*) as song_count
        FROM songs 
        GROUP BY artist_id 
        ORDER BY artist ASC
    """)
    fun getAllArtists(): Flow<List<ArtistProjection>>

    @Query("""
        SELECT DISTINCT artist_id, artist,
               COUNT(DISTINCT album_id) as album_count,
               COUNT(*) as song_count
        FROM songs 
        WHERE artist_id = :artistId 
        GROUP BY artist_id
    """)
    fun getArtistById(artistId: Long): Flow<ArtistProjection?>

    // ── Aggregation queries for Genres ──
    @Query("""
        SELECT genre, COUNT(*) as song_count 
        FROM songs 
        WHERE genre != '' 
        GROUP BY genre 
        ORDER BY genre ASC
    """)
    fun getAllGenres(): Flow<List<GenreProjection>>

    // ── Write operations ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    @Update
    suspend fun update(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}

/**
 * Room projection for album aggregation queries.
 */
data class AlbumProjection(
    val album_id: Long,
    val album: String,
    val artist: String,
    val artist_id: Long,
    val art_uri: String?,
    val song_count: Int,
    val total_duration: Long,
    val year: Int
)

/**
 * Room projection for artist aggregation queries.
 */
data class ArtistProjection(
    val artist_id: Long,
    val artist: String,
    val album_count: Int,
    val song_count: Int
)

/**
 * Room projection for genre aggregation queries.
 */
data class GenreProjection(
    val genre: String,
    val song_count: Int
)
