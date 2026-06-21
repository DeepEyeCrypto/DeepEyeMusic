// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.download

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.deepeye.musicpro.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicDownloadManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: com.deepeye.musicpro.domain.repository.HistoryRepository,
    private val libraryRepository: com.deepeye.musicpro.domain.repository.library.LibraryRepository,
    private val sourceResolverManager: com.deepeye.musicpro.domain.resolver.SourceResolverManager,
    okHttpClient: okhttp3.OkHttpClient,
) {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    // Clone the injected OkHttpClient to set an appropriate read timeout for large file downloads
    private val downloadHttpClient = okHttpClient.newBuilder()
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _activeDownloads =
        MutableStateFlow<Map<Long, MediaItem>>(emptyMap())
    val activeDownloads: kotlinx.coroutines.flow.StateFlow<Map<Long, MediaItem>> = _activeDownloads.asStateFlow()

    fun downloadTrack(item: MediaItem) { android.util.Log.e("TEST_DOWNLOAD", "downloadTrack called!");
        if (item is MediaItem.Local) {
            Toast.makeText(context, "Track already in local library", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val downloadId = System.currentTimeMillis()
            try {
                val sanitizedTitle = item.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _activeDownloads.value = _activeDownloads.value.toMutableMap().apply { put(downloadId, item) }
                    Toast.makeText(context, "Download started: ${item.title}", Toast.LENGTH_SHORT).show()
                }
                historyRepository.recordDownload(downloadId, item.id, item.title, "STARTED")

                val resolvedUrl = (item as? MediaItem.Remote)?.streamUri?.toString() 
                    ?: sourceResolverManager.resolve(item.id, (item as? MediaItem.Remote)?.isVideo == true)
                    ?: throw Exception("Could not resolve stream URL")

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$sanitizedTitle.mp3")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MUSIC + "/DeepEyeMusic")
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore entry")
                
                var success = false
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        DownloaderHelper.downloadWithResume(
                            client = downloadHttpClient,
                            url = resolvedUrl,
                            out = out
                        )
                    }
                    success = true
                } finally {
                    if (!success) {
                        resolver.delete(uri, null, null)
                    }
                }
                
                if (success) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val updateValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(uri, updateValues, null, null)
                    }
                    historyRepository.recordDownload(downloadId, item.id, item.title, "COMPLETED")
                    libraryRepository.markTrackDownloaded(
                        videoId = item.id,
                        title = item.title,
                        artist = item.artist,
                        artworkUrl = item.artworkUri?.toString(),
                        localPath = uri.toString()
                    )
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _activeDownloads.value = _activeDownloads.value.toMutableMap().apply { remove(downloadId) }
                        Toast.makeText(context, "Download complete: ${item.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicDownloadManager", "Download failed", e); historyRepository.recordDownload(downloadId, item.id, item.title, "FAILED")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _activeDownloads.value = _activeDownloads.value.toMutableMap().apply { remove(downloadId) }
                    Toast.makeText(context, "Unable to download: ${item.title}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        val item = _activeDownloads.value[downloadId]
        if (item != null) {
            scope.launch {
                historyRepository.recordDownload(downloadId, item.id, item.title, "CANCELLED")
            }
        }
        _activeDownloads.value = _activeDownloads.value.toMutableMap().apply { remove(downloadId) }
    }
}
