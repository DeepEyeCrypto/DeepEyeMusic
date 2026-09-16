// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.hometheater.scanner

import com.deepeye.musicpro.hometheater.model.MediaType
import com.deepeye.musicpro.hometheater.model.MediaMetadata
import com.deepeye.musicpro.hometheater.model.PhotoItem
import com.deepeye.musicpro.hometheater.model.TvShowSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the unified Kodi-style media catalog: Movies, TV Shows, Episodes,
 * Music Tracks, Photos, smart mixes, and playback resume states.
 */
@Singleton
class MediaLibraryRepository @Inject constructor(
    private val scanner: LibraryScanner
) {
    private val _movies = MutableStateFlow<List<MediaMetadata>>(emptyList())
    val movies: StateFlow<List<MediaMetadata>> = _movies.asStateFlow()

    private val _tvShows = MutableStateFlow<List<TvShowSeries>>(emptyList())
    val tvShows: StateFlow<List<TvShowSeries>> = _tvShows.asStateFlow()

    private val _musicTracks = MutableStateFlow<List<MediaMetadata>>(emptyList())
    val musicTracks: StateFlow<List<MediaMetadata>> = _musicTracks.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _recentlyAdded = MutableStateFlow<List<MediaMetadata>>(emptyList())
    val recentlyAdded: StateFlow<List<MediaMetadata>> = _recentlyAdded.asStateFlow()

    private val _inProgress = MutableStateFlow<List<MediaMetadata>>(emptyList())
    val inProgress: StateFlow<List<MediaMetadata>> = _inProgress.asStateFlow()

    suspend fun refreshLibrary() {
        scanner.startFullScan()
        val allItems = scanner.discoveredItems.value
        val allPhotos = scanner.discoveredPhotos.value

        _movies.value = allItems.filter { it.mediaType == MediaType.MOVIE }
        _musicTracks.value = allItems.filter { it.mediaType == MediaType.MUSIC_TRACK }
        _photos.value = allPhotos

        // Aggregate episodes into TV Shows
        val episodes = allItems.filter { it.mediaType == MediaType.EPISODE }
        val showMap = episodes.groupBy { it.title.substringBefore(" - S").substringBefore(" S0") }
        _tvShows.value = showMap.map { (title, eps) ->
            TvShowSeries(
                id = "show_${title.hashCode()}",
                title = title,
                posterUrl = eps.firstOrNull()?.posterUrl,
                fanartUrl = eps.firstOrNull()?.fanartUrl,
                overview = eps.firstOrNull()?.overview,
                seasonCount = eps.mapNotNull { it.seasonNumber }.distinct().size.coerceAtLeast(1),
                episodeCount = eps.size,
                unwatchedCount = eps.count { !it.isWatched },
                rating = eps.map { it.rating }.average().toFloat().takeIf { !it.isNaN() } ?: 0f,
                genres = eps.flatMap { it.genres }.distinct()
            )
        }

        _recentlyAdded.value = allItems.sortedByDescending { it.dateAdded }.take(20)
        _inProgress.value = allItems.filter { it.watchProgressFraction in 0.05f..0.92f }
    }

    fun updateProgress(mediaId: String, fraction: Float) {
        val watched = fraction >= 0.92f
        _movies.value = _movies.value.map {
            if (it.id == mediaId) it.copy(watchProgressFraction = fraction, isWatched = watched) else it
        }
    }
}
