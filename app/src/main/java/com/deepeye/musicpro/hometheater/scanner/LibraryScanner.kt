// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.hometheater.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.deepeye.musicpro.hometheater.model.MediaType
import com.deepeye.musicpro.hometheater.model.MediaMetadata
import com.deepeye.musicpro.hometheater.model.NetworkShareSource
import com.deepeye.musicpro.hometheater.model.NetworkSourceType
import com.deepeye.musicpro.hometheater.model.PhotoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans local filesystem and network shares for media content.
 * Extracts metadata, computes stable identifiers for deduplication,
 * and emits progress updates during the scan.
 */
@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress(0, 0, ""))
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _discoveredItems = MutableStateFlow<List<MediaMetadata>>(emptyList())
    val discoveredItems: StateFlow<List<MediaMetadata>> = _discoveredItems.asStateFlow()

    private val _discoveredPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val discoveredPhotos: StateFlow<List<PhotoItem>> = _discoveredPhotos.asStateFlow()

    private val _discoveredNetworkSources = MutableStateFlow<List<NetworkShareSource>>(emptyList())
    val discoveredNetworkSources: StateFlow<List<NetworkShareSource>> = _discoveredNetworkSources.asStateFlow()

    companion object {
        private const val TAG = "LibraryScanner"

        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
            "ts", "m2ts", "vob", "mpg", "mpeg", "3gp", "ogv", "divx"
        )

        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "ogg", "opus", "wma", "m4a",
            "ape", "alac", "aiff", "dsd", "dsf", "dff"
        )

        private val PHOTO_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff",
            "heic", "heif", "raw", "cr2", "nef", "arw", "dng"
        )
    }

    /**
     * Full library scan across all registered local paths and network sources.
     */
    suspend fun startFullScan(
        localPaths: List<String> = getDefaultScanPaths(),
        networkSources: List<NetworkShareSource> = emptyList()
    ) {
        if (_scanState.value == ScanState.SCANNING) {
            Log.w(TAG, "Scan already in progress, ignoring request")
            return
        }

        _scanState.value = ScanState.SCANNING
        _discoveredItems.value = emptyList()
        _discoveredPhotos.value = emptyList()

        val allMedia = mutableListOf<MediaMetadata>()
        val allPhotos = mutableListOf<PhotoItem>()
        var filesScanned = 0

        try {
            // Phase 1: Local filesystem scan
            _scanProgress.value = ScanProgress(0, 0, "Scanning local media...")
            for (path in localPaths) {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    val items = scanDirectory(dir)
                    filesScanned += items.size
                    allMedia.addAll(items)
                    _discoveredItems.value = allMedia.toList()
                    _scanProgress.value = ScanProgress(
                        filesScanned, filesScanned, "Scanned: ${dir.name}"
                    )
                }
            }

            // Phase 2: Photo scan
            _scanProgress.value = ScanProgress(filesScanned, 0, "Scanning photos...")
            val photos = scanMediaStorePhotos()
            allPhotos.addAll(photos)
            _discoveredPhotos.value = allPhotos.toList()

            // Phase 3: Network source discovery
            if (networkSources.isNotEmpty()) {
                _scanProgress.value = ScanProgress(
                    filesScanned, 0, "Probing network sources..."
                )
                val networkItems = scanNetworkSources(networkSources)
                allMedia.addAll(networkItems)
                _discoveredItems.value = allMedia.toList()
            }

            _scanState.value = ScanState.COMPLETED
            _scanProgress.value = ScanProgress(
                filesScanned + allPhotos.size,
                filesScanned + allPhotos.size,
                "Scan complete: ${allMedia.size} media, ${allPhotos.size} photos"
            )
            Log.i(TAG, "Scan completed: ${allMedia.size} media items, ${allPhotos.size} photos")

        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
            _scanState.value = ScanState.ERROR
            _scanProgress.value = ScanProgress(0, 0, "Scan failed: ${e.message}")
        }
    }

    /**
     * Recursively scans a directory for media files.
     */
    private suspend fun scanDirectory(directory: File): List<MediaMetadata> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaMetadata>()
        try {
            val files = directory.listFiles() ?: return@withContext emptyList()
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".")) {
                    items.addAll(scanDirectory(file))
                } else if (file.isFile) {
                    val ext = file.extension.lowercase()
                    when {
                        ext in VIDEO_EXTENSIONS -> items.add(parseVideoMetadata(file))
                        ext in AUDIO_EXTENSIONS -> items.add(parseAudioMetadata(file))
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied scanning: ${directory.absolutePath}")
        }
        return@withContext items
    }
/**
     * Extracts video metadata using MediaMetadataRetriever.
     * Infers TV show structure from path conventions (Show/Season/Episode).
     */
    private fun parseVideoMetadata(file: File): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)

            val title = file.nameWithoutExtension
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val dateAdded = file.lastModified()

            val resolution = when {
                height >= 2160 -> "4K UHD"
                height >= 1440 -> "1440p QHD"
                height >= 1080 -> "1080p FHD"
                height >= 720 -> "720p HD"
                height >= 480 -> "480p SD"
                else -> "${width}x${height}"
            }

            // Infer TV show structure from path
            // Convention: ShowName/Season XX/Episode.mkv
            val pathParts = file.parentFile?.path?.split("/") ?: emptyList()
            val seasonNumber = inferSeasonNumber(pathParts, file.name)
            val episodeNumber = inferEpisodeNumber(file.nameWithoutExtension)

            val mediaType = if (seasonNumber != null && episodeNumber != null) {
                MediaType.EPISODE
            } else {
                MediaType.MOVIE
            }

            MediaMetadata(
                id = computeFileHash(file),
                title = cleanTitle(title),
                mediaType = mediaType,
                uri = file.absolutePath,
                year = inferYear(file.name),
                durationMs = duration,
                posterUrl = null, // Will be populated by scraper addon
                fanartUrl = null,
                resolution = resolution,
                watchProgressFraction = 0f,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                dateAdded = dateAdded
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse metadata for: ${file.name}", e)
            // Fallback: basic metadata from file system
            MediaMetadata(
                id = computeFileHash(file),
                title = file.nameWithoutExtension,
                mediaType = MediaType.MOVIE,
                uri = file.absolutePath,
                dateAdded = file.lastModified()
            )
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
/**
     * Extracts audio metadata.
     */
    private fun parseAudioMetadata(file: File): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            val dateAdded = file.lastModified()

            MediaMetadata(
                id = computeFileHash(file),
                title = title,
                originalTitle = file.nameWithoutExtension,
                mediaType = MediaType.MUSIC_TRACK,
                uri = file.absolutePath,
                year = year,
                durationMs = duration,
                genres = genre?.let { listOf(it) } ?: emptyList(),
                overview = buildString {
                    artist?.let { append("Artist: $it\n") }
                    album?.let { append("Album: $it\n") }
                    bitrate?.let { append("Bitrate: ${it / 1000}kbps") }
                },
                dateAdded = dateAdded
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse audio metadata for: ${file.name}", e)
            MediaMetadata(
                id = computeFileHash(file),
                title = file.nameWithoutExtension,
                mediaType = MediaType.MUSIC_TRACK,
                uri = file.absolutePath,
                dateAdded = file.lastModified()
            )
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Scans MediaStore for photos (the system-level indexed photo library).
     */
    private suspend fun scanMediaStorePhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()
        val resolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION
        )

        val selection = "${MediaStore.Images.Media.IS_TRASHED} = 0"
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val orientColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Untitled"
                    val dateTaken = cursor.getLong(dateColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val orientation = cursor.getInt(orientColumn)

                    photos.add(
                        PhotoItem(
                            id = "photo_$id",
                            title = name,
                            uri = path,
                            dateTaken = dateTaken,
                            resolution = "${width}x${height}",
                            orientationDegrees = orientation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore photo scan failed", e)
        }

        photos
    }

    /**
     * Probes network share sources for later on-demand browsing.
     */
    private suspend fun scanNetworkSources(sources: List<NetworkShareSource>): List<MediaMetadata> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<MediaMetadata>()

            for (source in sources) {
                try {
                    when (source.type) {
                        NetworkSourceType.LOCAL_STORAGE -> {
                            val dir = File(source.sharePath)
                            if (dir.exists() && dir.isDirectory) {
                                items.addAll(scanDirectory(dir))
                            }
                        }
                        else -> {
                            // For network sources (SMB, WebDAV, DLNA, NFS, HTTP),
                            // streaming is handled by the NetworkBrowser.
                            // Register the source for later browsing.
                            Log.i(TAG, "Registered network source: ${source.name} (${source.type})")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to scan network source: ${source.name}", e)
                }
            }

            items
        }

    // ── Utility Functions ──────────────────────────────────────────

    private fun getDefaultScanPaths(): List<String> {
        val paths = mutableListOf<String>()

        // Common media directories
        val externalStorage = Environment.getExternalStorageDirectory()
        listOf("Movies", "Videos", "Download", "Music", "DCIM").forEach { dir ->
            val path = File(externalStorage, dir)
            if (path.exists()) paths.add(path.absolutePath)
        }

        // Internal app-visible storage
        context.getExternalFilesDir(null)?.let { paths.add(it.absolutePath) }

        return paths
    }

    private fun computeFileHash(file: File): String {
        // Use file path + size + last modified as a fast, stable identifier
        return "media_${file.absolutePath.hashCode()}_${file.length()}_${file.lastModified()}"
    }

    private fun inferSeasonNumber(pathParts: List<String>, fileName: String): Int? {
        // Look for "Season XX" or "S01" patterns in path
        for (part in pathParts) {
            val seasonMatch = Regex("""(?i)season\s*(\d+)""").find(part)
            if (seasonMatch != null) return seasonMatch.groupValues[1].toIntOrNull()

            val sPattern = Regex("""(?i)^s(\d+)$""").find(part)
            if (sPattern != null) return sPattern.groupValues[1].toIntOrNull()
        }
        return null
    }

    private fun inferEpisodeNumber(title: String): Int? {
        // Match patterns like "S01E05", "s01e05", "E05", "EP05"
        val patterns = listOf(
            Regex("""(?i)[sS]\d+[eE](\d+)"""),
            Regex("""(?i)^[eE][pP]?(\d+)"""),
            Regex("""(?i)\bep(?:isode)?\s*(\d+)""")
        )
        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null) return match.groupValues[1].toIntOrNull()
        }
        return null
    }

    private fun inferYear(fileName: String): Int? {
        // Match 4-digit year in parentheses or separated by dots/dashes
        val yearMatch = Regex("""[(\[\.]?(19\d{2}|20\d{2})[)\]\.\s-]""").find(fileName)
        return yearMatch?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""\[.*?]"""), "")       // Remove brackets: [1080p], [x265]
            .replace(Regex("""\(.*?\)"""), "")       // Remove parens: (2024), (BluRay)
            .replace(Regex("""\.(mkv|mp4|avi|mov)$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[._-]"""), " ")        // Replace dots/underscores/dashes
            .replace(Regex("""\s{2,}"""), " ")       // Collapse multiple spaces
            .trim()
    }
}

enum class ScanState {
    IDLE,
    SCANNING,
    COMPLETED,
    ERROR
}

data class ScanProgress(
    val filesScanned: Int,
    val totalExpected: Int,
    val currentPath: String
)