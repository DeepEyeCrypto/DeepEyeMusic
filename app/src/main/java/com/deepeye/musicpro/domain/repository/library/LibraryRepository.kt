package com.deepeye.musicpro.domain.repository.library

import com.deepeye.musicpro.data.library.dao.LibraryDao
import com.deepeye.musicpro.data.library.entities.DownloadEntity
import com.deepeye.musicpro.data.library.entities.LikedTrackEntity
import com.deepeye.musicpro.data.library.entities.PlaylistEntity
import com.deepeye.musicpro.data.library.entities.PlaylistTrackEntity
import com.deepeye.musicpro.data.library.entities.RecentPlayEntity
import com.deepeye.musicpro.data.library.entities.SavedTrackEntity
import com.deepeye.musicpro.domain.model.library.DownloadState
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.model.library.LibraryItemType
import com.deepeye.musicpro.ui.library.LibraryHomeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository for all library operations: likes, saves, playlists,
 * downloads, and recent plays. Backed entirely by Room for offline-first access.
 */
@Singleton
class LibraryRepository
@Inject
constructor(
    private val dao: LibraryDao,
) {
    // ── Aggregate Library Home ──

    fun observeLibraryHome(): Flow<LibraryHomeState> =
        combine(
            dao.observeLikedTracks(),
            dao.observePlaylists(),
            dao.observeCompletedDownloads(),
            dao.observeRecentPlays(),
        ) { liked, playlists, downloads, recent ->
            val downloadIds = downloads.map { it.videoId }.toSet()
            LibraryHomeState(
                likedCount = liked.size,
                playlistCount = playlists.size,
                downloadCount = downloads.size,
                recentCount = recent.size,
                likedTracks = liked.map {
                    it.toLibraryItem(downloadIds.contains(it.videoId))
                }.sortedWith(compareByDescending<LibraryItem> { it.addedAt }.thenBy { it.title }).distinctBy { it.id },
                playlists = playlists.map {
                    it.toLibraryItem()
                }.sortedWith(compareByDescending<LibraryItem> { it.addedAt }.thenBy { it.title }),
                downloads = downloads.map {
                    it.toLibraryItem()
                }.sortedWith(compareByDescending<LibraryItem> { it.addedAt }.thenBy { it.title }).distinctBy { it.id },
                recentPlays = recent.map {
                    it.toLibraryItem(downloadIds.contains(it.videoId))
                }.sortedWith(compareByDescending<LibraryItem> { it.addedAt }.thenBy { it.title }).distinctBy { it.id },
            )
        }

    // ── Likes ──

    suspend fun likeTrack(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
        artworkUrl: String? = null,
    ) {
        dao.likeTrack(
            LikedTrackEntity(
                videoId = videoId,
                title = title,
                artist = artist,
                channelId = channelId,
                artworkUrl = artworkUrl,
            ),
        )
    }

    suspend fun unlikeTrack(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
    ) {
        dao.unlikeTrack(
            LikedTrackEntity(videoId = videoId, title = title, artist = artist, channelId = channelId),
        )
    }

    fun isTrackLiked(videoId: String): Flow<Boolean> = dao.isTrackLiked(videoId)

    // ── Saves ──

    suspend fun saveTrack(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
    ) {
        dao.saveTrack(
            SavedTrackEntity(videoId = videoId, title = title, artist = artist, channelId = channelId),
        )
    }

    suspend fun removeSavedTrack(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
    ) {
        dao.removeSavedTrack(
            SavedTrackEntity(videoId = videoId, title = title, artist = artist, channelId = channelId),
        )
    }

    // ── Playlists ──

    suspend fun createPlaylist(
        name: String,
        description: String = "",
    ) {
        dao.upsertPlaylist(
            PlaylistEntity(
                playlistId = UUID.randomUUID().toString(),
                name = name,
                description = description,
            ),
        )
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.deletePlaylist(playlistId)
    }

    fun observePlaylists() = dao.observePlaylists()

    fun observePlaylistTracks(playlistId: String) = dao.observePlaylistTracks(playlistId)

    suspend fun addTrackToPlaylist(
        playlistId: String,
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String? = null,
    ) {
        val position = dao.getNextPlaylistTrackPosition(playlistId)
        dao.upsertPlaylistTracks(
            listOf(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    artworkUrl = artworkUrl,
                    position = position,
                ),
            ),
        )
    }

    // ── Downloads ──

    suspend fun markTrackDownloaded(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        localPath: String,
    ) {
        dao.upsertDownload(
            DownloadEntity(
                videoId = videoId,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                localPath = localPath,
                state = DownloadState.COMPLETED,
                progress = 100,
                downloadedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun observeDownloads() = dao.observeDownloads()

    suspend fun resyncDownloadedTracksFromStorage(context: android.content.Context) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val existingLocalPaths = dao.getCompletedDownloadsList().map { it.localPath }.toSet()
                val resolver = context.contentResolver

                // 1. Audio MediaStore scan
                val audioCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
                } else {
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val audioProjection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.TITLE,
                    android.provider.MediaStore.Audio.Media.ARTIST,
                    android.provider.MediaStore.Audio.Media.DATA
                )
                val audioSelection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    "${android.provider.MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                } else {
                    "${android.provider.MediaStore.Audio.Media.DATA} LIKE ?"
                }
                val selectionArgs = arrayOf("%DeepEyeMusic%")

                resolver.query(audioCollection, audioProjection, audioSelection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Song"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val path = cursor.getString(dataCol) ?: ""
                        val contentUri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                        if (path.isNotEmpty() && !existingLocalPaths.contains(contentUri) && !existingLocalPaths.contains(path)) {
                            val videoId = "restored_audio_$id"
                            dao.upsertDownload(
                                DownloadEntity(
                                    videoId = videoId,
                                    title = title,
                                    artist = artist,
                                    localPath = contentUri,
                                    state = DownloadState.COMPLETED,
                                    progress = 100,
                                    downloadedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                // 2. Video MediaStore scan
                val videoCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
                } else {
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                val videoProjection = arrayOf(
                    android.provider.MediaStore.Video.Media._ID,
                    android.provider.MediaStore.Video.Media.TITLE,
                    android.provider.MediaStore.Video.Media.ARTIST,
                    android.provider.MediaStore.Video.Media.DATA
                )
                val videoSelection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    "${android.provider.MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
                } else {
                    "${android.provider.MediaStore.Video.Media.DATA} LIKE ?"
                }

                resolver.query(videoCollection, videoProjection, videoSelection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.ARTIST)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Video"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val path = cursor.getString(dataCol) ?: ""
                        val contentUri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                        if (path.isNotEmpty() && !existingLocalPaths.contains(contentUri) && !existingLocalPaths.contains(path)) {
                            val videoId = "restored_video_$id"
                            dao.upsertDownload(
                                DownloadEntity(
                                    videoId = videoId,
                                    title = title,
                                    artist = artist,
                                    localPath = contentUri,
                                    state = DownloadState.COMPLETED,
                                    progress = 100,
                                    downloadedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryRepository", "Error resyncing downloads from storage", e)
            }
        }
    }

    // ── Recent Plays ──

    suspend fun recordRecentPlay(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String? = null,
    ) {
        dao.insertRecentPlay(
            RecentPlayEntity(videoId = videoId, title = title, artist = artist, artworkUrl = artworkUrl),
        )
        // Trim entries older than 30 days
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.trimRecentPlays(cutoff)
    }

    // ── Mappers ──

    private fun LikedTrackEntity.toLibraryItem(isOfflineAvailable: Boolean = false) =
        LibraryItem(
            id = videoId,
            type = LibraryItemType.SONG,
            title = title,
            subtitle = artist,
            artworkUrl = artworkUrl,
            videoId = videoId,
            artist = artist,
            isLiked = true,
            isOfflineAvailable = isOfflineAvailable,
            addedAt = likedAt,
        )

    private fun PlaylistEntity.toLibraryItem() =
        LibraryItem(
            id = playlistId,
            type = LibraryItemType.PLAYLIST,
            title = name,
            subtitle = description,
            artworkUrl = artworkUrl,
            addedAt = createdAt,
        )

    private fun DownloadEntity.toLibraryItem() =
        LibraryItem(
            id = videoId,
            type = LibraryItemType.SONG,
            title = title,
            subtitle = artist,
            artworkUrl = artworkUrl,
            videoId = videoId,
            artist = artist,
            downloadState = state,
            isOfflineAvailable = state == DownloadState.COMPLETED,
            addedAt = downloadedAt,
        )

    private fun RecentPlayEntity.toLibraryItem(isOfflineAvailable: Boolean = false) =
        LibraryItem(
            id = "$id",
            type = LibraryItemType.SONG,
            title = title,
            subtitle = artist,
            artworkUrl = artworkUrl,
            videoId = videoId,
            artist = artist,
            isOfflineAvailable = isOfflineAvailable,
            lastPlayedAt = playedAt,
            addedAt = playedAt,
        )

    // ── Subscribed Channels ──
    suspend fun subscribeChannel(channelId: String, channelName: String) {
        dao.subscribeChannel(
            com.deepeye.musicpro.data.library.entities.SubscribedChannelEntity(
                channelId = channelId,
                channelName = channelName
            )
        )
    }

    suspend fun unsubscribeChannel(channelId: String) {
        dao.unsubscribeChannel(channelId)
    }

    fun isChannelSubscribed(channelId: String): Flow<Boolean> = dao.isChannelSubscribed(channelId)

    suspend fun getAllSubscribedChannels() = dao.getAllSubscribedChannels()

    suspend fun updateLastSeenVideo(channelId: String, videoId: String) {
        dao.updateLastSeenVideo(channelId, videoId)
    }
}
