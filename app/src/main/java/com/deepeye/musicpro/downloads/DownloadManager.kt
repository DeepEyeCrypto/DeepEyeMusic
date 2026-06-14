package com.deepeye.musicpro.downloads

import com.deepeye.musicpro.data.library.dao.LibraryDao
import com.deepeye.musicpro.data.library.entities.DownloadEntity
import com.deepeye.musicpro.domain.model.library.DownloadState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages download lifecycle: enqueue → progress → complete/fail.
 * Actual file download logic (WorkManager integration) can be added later.
 * This class handles the database state tracking.
 */
@Singleton
class DownloadManager
@Inject
constructor(
    private val dao: LibraryDao,
) {
    suspend fun enqueueDownload(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String? = null,
    ) {
        dao.upsertDownload(
            DownloadEntity(
                videoId = videoId,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                state = DownloadState.QUEUED,
                progress = 0,
            ),
        )
    }

    suspend fun updateProgress(
        videoId: String,
        progress: Int,
    ) {
        val current = dao.getDownload(videoId) ?: return
        dao.upsertDownload(
            current.copy(
                state = DownloadState.DOWNLOADING,
                progress = progress,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun pauseDownload(videoId: String) {
        val current = dao.getDownload(videoId) ?: return
        dao.upsertDownload(
            current.copy(
                state = DownloadState.PAUSED,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun completeDownload(
        videoId: String,
        localPath: String,
        sizeBytes: Long = 0L,
    ) {
        val current = dao.getDownload(videoId) ?: return
        dao.upsertDownload(
            current.copy(
                state = DownloadState.COMPLETED,
                progress = 100,
                localPath = localPath,
                sizeBytes = sizeBytes,
                downloadedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun failDownload(videoId: String) {
        val current = dao.getDownload(videoId) ?: return
        dao.upsertDownload(
            current.copy(
                state = DownloadState.FAILED,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeDownload(videoId: String) {
        dao.deleteDownload(videoId)
    }

    fun observeDownloads() = dao.observeDownloads()

    fun observeCompletedDownloads() = dao.observeCompletedDownloads()
}
