// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.visualizer.VisualizerEngine
import com.deepeye.musicpro.util.ColorExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    private val playerController: PlayerController,
    private val visualizerEngine: VisualizerEngine,
    private val colorExtractor: ColorExtractor,
    private val downloadManager: com.deepeye.musicpro.player.download.MusicDownloadManager,
    private val tasteProfileRepository: com.deepeye.musicpro.domain.repository.TasteProfileRepository,
    private val lyricsRepository: com.deepeye.musicpro.domain.repository.LyricsRepository,
    private val sleepTimerManager: com.deepeye.musicpro.player.timer.SleepTimerManager,
    private val recommendationEngine: com.deepeye.musicpro.domain.recommendation.RecommendationEngine,
    private val libraryRepository: com.deepeye.musicpro.domain.repository.library.LibraryRepository,
) : ViewModel() {
    val playerState: StateFlow<PlayerState> = playerController.playerState
    val autoplayState: StateFlow<com.deepeye.musicpro.domain.autoplay.AutoplayState> = playerController.autoplayState
    val player = playerController.player

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentSongFeedback: StateFlow<com.deepeye.musicpro.data.db.UserFeedback?> =
        playerController.playerState
            .map { it.currentItem?.id }
            .flatMapLatest { songId ->
                if (songId != null) {
                    tasteProfileRepository.getFeedbackFlow(songId)
                } else {
                    flowOf(null)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _dominantColor = MutableStateFlow(Color(0xFF7B3FE4))
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

    private val _extractedColors = MutableStateFlow<com.deepeye.musicpro.util.ExtractedColors?>(null)
    val extractedColors: StateFlow<com.deepeye.musicpro.util.ExtractedColors?> = _extractedColors.asStateFlow()

    private val _currentLyrics = MutableStateFlow<com.deepeye.musicpro.domain.model.Lyrics?>(null)
    val currentLyrics: StateFlow<com.deepeye.musicpro.domain.model.Lyrics?> = _currentLyrics.asStateFlow()

    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerManager.timeRemainingMs

    private val _isBackgroundPlaybackEnabled = MutableStateFlow(false)
    val isBackgroundPlaybackEnabled: StateFlow<Boolean> = _isBackgroundPlaybackEnabled.asStateFlow()

    fun enableBackgroundPlayback() {
        _isBackgroundPlaybackEnabled.value = true
        playerController.enableBackgroundPlayback(true)
    }

    val fftData =
        visualizerEngine.fftData.map { bytes ->
            if (bytes.isEmpty()) {
                FloatArray(0)
            } else {
                val magnitudes = FloatArray(bytes.size / 2)
                for (i in magnitudes.indices) {
                    val r = bytes[i * 2].toInt()
                    val im = bytes[i * 2 + 1].toInt()
                    magnitudes[i] = (Math.sqrt((r * r + im * im).toDouble()) / 128f).toFloat().coerceIn(0f, 1f)
                }
                magnitudes
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FloatArray(0))

    init {
        // Observe artwork changes for color extraction
        viewModelScope.launch {
            playerController.playerState
                .map { it.currentItem }
                .distinctUntilChanged()
                .collectLatest { mediaItem ->
                    _currentLyrics.value = null
                    mediaItem?.artworkUri?.let { uri ->
                        val colors = colorExtractor.extractColors(uri)
                        _extractedColors.value = colors
                        _dominantColor.value = colors?.primary ?: Color(0xFF7B3FE4)
                    } ?: run {
                        _extractedColors.value = null
                        _dominantColor.value = Color(0xFF7B3FE4)
                    }
                    if (mediaItem != null) {
                        // Duration from playerState might be delayed, we use current duration or 0
                        val lyrics = lyricsRepository.getLyrics(mediaItem.title, mediaItem.artist, 0L)
                        _currentLyrics.value = lyrics
                    }
                }
        }
    }

    private var lastActionTime = 0L
    private val debounceDelay = 350L // Prevents race conditions on fast clicks

    fun togglePlayPause() {
        if (System.currentTimeMillis() - lastActionTime > debounceDelay) {
            lastActionTime = System.currentTimeMillis()
            playerController.togglePlayPause()
        }
    }

    fun next() {
        if (System.currentTimeMillis() - lastActionTime > debounceDelay) {
            lastActionTime = System.currentTimeMillis()
            playerController.next()
        }
    }

    fun previous() {
        if (System.currentTimeMillis() - lastActionTime > debounceDelay) {
            lastActionTime = System.currentTimeMillis()
            playerController.previous()
        }
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun toggleRepeat() = playerController.toggleRepeat()

    fun toggleShuffle() = playerController.toggleShuffle()

    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)

    fun setVideoQuality(quality: String) = playerController.setVideoQuality(quality)

    fun startSleepTimer(minutes: Int) = sleepTimerManager.startTimer(minutes)

    fun cancelSleepTimer() = sleepTimerManager.cancelTimer()

    fun toggleAutoplay() = playerController.toggleAutoplay()

    fun setAutoplayMode(mode: com.deepeye.musicpro.domain.autoplay.AutoplayMode) = playerController.setAutoplayMode(
        mode
    )

    fun removeAutoplayQueueItem(videoId: String) = playerController.removeAutoplayQueueItem(videoId)

    fun playMedia(item: com.deepeye.musicpro.domain.model.MediaItem) {
        playerController.playMedia(item)
    }

    fun setQueue(
        items: List<com.deepeye.musicpro.domain.model.MediaItem>,
        startIndex: Int = 0,
    ) {
        playerController.setQueue(items, startIndex)
    }

    fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        playerController.moveQueueItem(fromIndex, toIndex)
    }

    fun removeMediaItem(index: Int) {
        playerController.removeQueueItem(index)
    }

    fun seekToMediaItem(index: Int) {
        playerController.playQueueItem(index)
    }

    val activeDownloads = downloadManager.activeDownloads


    fun downloadCurrentTrack() { android.util.Log.e("PlayerViewModel", "downloadCurrentTrack called! currentItem=" + playerState.value.currentItem?.title);
        playerState.value.currentItem?.let {
            downloadManager.downloadTrack(it)
        }
    }

    fun likeTrack(liked: Boolean) {
        val currentItem = playerState.value.currentItem ?: return
        val currentId = currentItem.id
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                tasteProfileRepository.recordFeedback(currentId, liked = liked, dontPlayAgain = false)
                
                if (liked) {
                    // Sync with Library UI
                    libraryRepository.likeTrack(
                        videoId = currentId,
                        title = currentItem.title,
                        artist = currentItem.artist,
                        channelId = "",
                        artworkUrl = currentItem.artworkUri?.toString()
                    )
                    
                    // Fire an immediate listen event so the Recommendation Engine picks it up as a Liked Song right away
                    recommendationEngine.trackListenEvent(
                        videoId = currentId,
                        title = currentItem.title,
                        artist = currentItem.artist,
                        channelId = "", // Not critically needed for pure likes
                        listenDurationMs = 1000L,
                        totalDurationMs = 1000L,
                        wasSkipped = false,
                        wasLiked = true,
                        wasDisliked = false,
                        wasAddedToPlaylist = false,
                        wasReplayed = false
                    )
                } else {
                    // Remove from Library UI
                    libraryRepository.unlikeTrack(
                        videoId = currentId,
                        title = currentItem.title,
                        artist = currentItem.artist,
                        channelId = ""
                    )
                }
            }
        }
    }

    fun blockTrack() {
        val currentId = playerState.value.currentItem?.id ?: return
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                // Block is dislike + dontPlayAgain
                tasteProfileRepository.recordFeedback(currentId, liked = false, dontPlayAgain = true)
            }
            // Automatically advance to the next track!
            next()
        }
    }
}
