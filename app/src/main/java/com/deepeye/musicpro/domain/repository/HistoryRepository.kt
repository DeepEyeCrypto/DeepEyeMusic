// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.data.db.DownloadHistoryEntity
import com.deepeye.musicpro.data.db.HistoryDao
import com.deepeye.musicpro.data.db.PlaybackHistoryEntity
import com.deepeye.musicpro.data.db.QueueSnapshotEntity
import com.deepeye.musicpro.data.db.SearchHistoryEntity
import com.deepeye.musicpro.data.db.VideoHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    // --- Search History ---
    suspend fun saveSearch(query: String, source: String, resultType: String) {
        val entity = SearchHistoryEntity(query = query, source = source, resultType = resultType)
        historyDao.insertSearch(entity)
    }

    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>> {
        return historyDao.getRecentSearches(limit)
    }

    suspend fun clearSearchHistory() {
        historyDao.clearSearchHistory()
    }

    // --- Playback History ---
    suspend fun recordPlayback(
        mediaId: String,
        title: String,
        artist: String,
        album: String,
        artworkUri: String?,
        playDurationMs: Long,
        totalDurationMs: Long,
        source: String
    ) {
        val completion = if (totalDurationMs > 0) playDurationMs.toFloat() / totalDurationMs.toFloat() else 0f
        val entity = PlaybackHistoryEntity(
            mediaId = mediaId,
            title = title,
            artist = artist,
            album = album,
            artworkUri = artworkUri,
            playDurationMs = playDurationMs,
            totalDurationMs = totalDurationMs,
            completionPercent = completion,
            source = source
        )
        historyDao.insertPlaybackEvent(entity)
    }

    fun getRecentPlaybacks(limit: Int = 50): Flow<List<PlaybackHistoryEntity>> {
        return historyDao.getRecentPlaybacks(limit)
    }

    suspend fun clearPlaybackHistory() {
        historyDao.clearPlaybackHistory()
    }

    // --- Video History ---
    suspend fun recordVideoProgress(
        videoId: String,
        title: String,
        thumbnailUri: String?,
        positionMs: Long,
        durationMs: Long
    ) {
        val completion = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
        val entity = VideoHistoryEntity(
            videoId = videoId,
            title = title,
            thumbnailUri = thumbnailUri,
            positionMs = positionMs,
            durationMs = durationMs,
            completionPercent = completion
        )
        historyDao.insertVideoHistory(entity)
    }

    fun getRecentVideos(limit: Int = 20): Flow<List<VideoHistoryEntity>> {
        return historyDao.getRecentVideos(limit)
    }

    suspend fun getVideoHistory(videoId: String): VideoHistoryEntity? {
        return historyDao.getVideoHistory(videoId)
    }

    suspend fun clearVideoHistory() {
        historyDao.clearVideoHistory()
    }

    // --- Download History ---
    suspend fun recordDownload(
        downloadId: Long,
        mediaId: String,
        title: String,
        status: String
    ) {
        val entity = DownloadHistoryEntity(
            downloadId = downloadId,
            mediaId = mediaId,
            title = title,
            status = status
        )
        historyDao.insertDownloadHistory(entity)
    }

    fun getDownloadHistory(): Flow<List<DownloadHistoryEntity>> {
        return historyDao.getDownloadHistory()
    }

    suspend fun clearDownloadHistory() {
        historyDao.clearDownloadHistory()
    }

    // --- Queue Snapshot ---
    suspend fun saveQueueSnapshot(queueJson: String, currentIndex: Int) {
        val snapshot = QueueSnapshotEntity(queueJson = queueJson, currentIndex = currentIndex)
        historyDao.saveQueueSnapshot(snapshot)
    }

    suspend fun updateQueueIndex(index: Int) {
        historyDao.updateQueueIndex(index)
    }

    suspend fun getQueueSnapshot(): QueueSnapshotEntity? {
        return historyDao.getQueueSnapshot()
    }

    // --- Privacy Controls ---
    suspend fun clearAllHistory() {
        clearSearchHistory()
        clearPlaybackHistory()
        clearVideoHistory()
        clearDownloadHistory()
    }

    // --- Backup & Restore ---
    suspend fun exportToJson(): String {
        val gson = com.google.gson.Gson()
        val searches = historyDao.getRecentSearches(1000).firstOrNull() ?: emptyList()
        val playbacks = historyDao.getRecentPlaybacks(1000).firstOrNull() ?: emptyList()
        val videos = historyDao.getRecentVideos(1000).firstOrNull() ?: emptyList()
        val downloads = historyDao.getDownloadHistory().firstOrNull() ?: emptyList()

        val backup = mapOf(
            "searches" to searches,
            "playbacks" to playbacks,
            "videos" to videos,
            "downloads" to downloads
        )
        return gson.toJson(backup)
    }

    suspend fun importFromJson(jsonString: String) {
        try {
            val gson = com.google.gson.Gson()
            val jsonObject = com.google.gson.JsonParser.parseString(jsonString).asJsonObject

            // 1. Searches Restore
            jsonObject.getAsJsonArray("searches")?.forEach { element ->
                try {
                    val entity = gson.fromJson(element, SearchHistoryEntity::class.java)
                    historyDao.insertSearch(entity)
                } catch (e: Exception) {
                    android.util.Log.e("HistoryRepository", "Failed to restore search entity", e)
                }
            }

            // 2. Playbacks Restore
            jsonObject.getAsJsonArray("playbacks")?.forEach { element ->
                try {
                    val entity = gson.fromJson(element, PlaybackHistoryEntity::class.java)
                    historyDao.insertPlaybackEvent(entity)
                } catch (e: Exception) {
                    android.util.Log.e("HistoryRepository", "Failed to restore playback entity", e)
                }
            }

            // 3. Videos Restore
            jsonObject.getAsJsonArray("videos")?.forEach { element ->
                try {
                    val entity = gson.fromJson(element, VideoHistoryEntity::class.java)
                    historyDao.insertVideoHistory(entity)
                } catch (e: Exception) {
                    android.util.Log.e("HistoryRepository", "Failed to restore video entity", e)
                }
            }

            // 4. Downloads Restore
            jsonObject.getAsJsonArray("downloads")?.forEach { element ->
                try {
                    val entity = gson.fromJson(element, DownloadHistoryEntity::class.java)
                    historyDao.insertDownloadHistory(entity)
                } catch (e: Exception) {
                    android.util.Log.e("HistoryRepository", "Failed to restore download entity", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HistoryRepository", "Failed to restore history", e)
        }
    }
}
