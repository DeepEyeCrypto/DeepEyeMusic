package com.deepeye.musicpro.data.library.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deepeye.musicpro.data.library.entities.DownloadEntity
import com.deepeye.musicpro.data.library.entities.LikedTrackEntity
import com.deepeye.musicpro.data.library.entities.PlaylistEntity
import com.deepeye.musicpro.data.library.entities.PlaylistTrackEntity
import com.deepeye.musicpro.data.library.entities.RecentPlayEntity
import com.deepeye.musicpro.data.library.entities.SavedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    // ── Liked Tracks ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun likeTrack(track: LikedTrackEntity)

    @Delete
    suspend fun unlikeTrack(track: LikedTrackEntity)

    @Query("SELECT * FROM liked_tracks ORDER BY likedAt DESC")
    fun observeLikedTracks(): Flow<List<LikedTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_tracks WHERE videoId = :videoId)")
    fun isTrackLiked(videoId: String): Flow<Boolean>

    // ── Saved Tracks ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrack(track: SavedTrackEntity)

    @Delete
    suspend fun removeSavedTrack(track: SavedTrackEntity)

    @Query("SELECT * FROM saved_tracks ORDER BY addedAt DESC")
    fun observeSavedTracks(): Flow<List<SavedTrackEntity>>

    // ── Playlists ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun observePlaylist(playlistId: String): Flow<PlaylistEntity?>

    // ── Playlist Tracks ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistTracks(playlistId: String): Flow<List<PlaylistTrackEntity>>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeTrackFromPlaylist(
        playlistId: String,
        videoId: String,
    )

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getNextPlaylistTrackPosition(playlistId: String): Int

    // ── Downloads ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE state = 'COMPLETED'")
    fun observeCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownload(videoId: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteDownload(videoId: String)

    // ── Recent Plays ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(play: RecentPlayEntity)

    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecentPlays(limit: Int = 30): Flow<List<RecentPlayEntity>>

    @Query("DELETE FROM recent_plays WHERE playedAt < :cutoff")
    suspend fun trimRecentPlays(cutoff: Long)

    // ── Subscribed Channels ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun subscribeChannel(channel: com.deepeye.musicpro.data.library.entities.SubscribedChannelEntity)

    @Query("DELETE FROM subscribed_channels WHERE channelId = :channelId")
    suspend fun unsubscribeChannel(channelId: String)

    @Query("SELECT * FROM subscribed_channels ORDER BY subscribedAt DESC")
    fun observeSubscribedChannels(): Flow<List<com.deepeye.musicpro.data.library.entities.SubscribedChannelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM subscribed_channels WHERE channelId = :channelId)")
    fun isChannelSubscribed(channelId: String): Flow<Boolean>

    @Query("SELECT * FROM subscribed_channels")
    suspend fun getAllSubscribedChannels(): List<com.deepeye.musicpro.data.library.entities.SubscribedChannelEntity>

    @Query("UPDATE subscribed_channels SET lastSeenVideoId = :videoId WHERE channelId = :channelId")
    suspend fun updateLastSeenVideo(channelId: String, videoId: String)
}
