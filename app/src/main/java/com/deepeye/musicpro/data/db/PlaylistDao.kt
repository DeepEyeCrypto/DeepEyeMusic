// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for playlist-related database operations.
 */
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM local_playlists ORDER BY modified_at DESC")
    fun getAllPlaylists(): Flow<List<LocalPlaylistEntity>>

    @Query("SELECT * FROM local_playlists WHERE id = :id")
    fun getPlaylistById(id: Long): Flow<LocalPlaylistEntity?>

    @Query(
        """
        SELECT s.* FROM songs s 
        INNER JOIN playlist_song_cross_ref ps ON s.id = ps.song_id 
        WHERE ps.playlist_id = :playlistId 
        ORDER BY ps.sort_order ASC
    """,
    )
    fun getPlaylistSongs(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylistEntity): Long

    @Query("UPDATE local_playlists SET name = :name, modified_at = :modifiedAt WHERE id = :id")
    suspend fun renamePlaylist(
        id: Long,
        name: String,
        modifiedAt: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM local_playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(
        playlistId: Long,
        songId: Long,
    )

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    suspend fun getNextSortOrder(playlistId: Long): Int

    @Transaction
    suspend fun addSongToPlaylistEnd(
        playlistId: Long,
        songId: Long,
    ) {
        val sortOrder = getNextSortOrder(playlistId)
        addSongToPlaylist(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                sortOrder = sortOrder,
            ),
        )
    }

    @Query("SELECT * FROM playlist_song_cross_ref WHERE playlist_id = :playlistId ORDER BY sort_order ASC")
    suspend fun getPlaylistCrossRefs(playlistId: Long): List<PlaylistSongCrossRef>

    @Query(
        "UPDATE playlist_song_cross_ref SET sort_order = :sortOrder WHERE playlist_id = :playlistId AND song_id = :songId"
    )
    suspend fun updateSortOrder(
        playlistId: Long,
        songId: Long,
        sortOrder: Int
    )

    @Transaction
    suspend fun reorderPlaylistSongs(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int
    ) {
        val crossRefs = getPlaylistCrossRefs(playlistId).toMutableList()
        if (fromIndex !in crossRefs.indices || toIndex !in crossRefs.indices) return

        val item = crossRefs.removeAt(fromIndex)
        crossRefs.add(toIndex, item)

        // Update all sort orders
        crossRefs.forEachIndexed { index, crossRef ->
            if (crossRef.sortOrder != index) {
                updateSortOrder(playlistId, crossRef.songId, index)
            }
        }
    }

    // Delete all cross-refs when playlist is deleted
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: Long)
}
