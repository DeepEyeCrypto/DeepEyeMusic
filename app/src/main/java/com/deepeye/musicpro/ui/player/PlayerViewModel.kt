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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
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
    private val authClient: com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient,
    private val youtubeRemoteDataSource: com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
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

    private val _videoDetails = MutableStateFlow<com.deepeye.musicpro.domain.model.VideoDetails?>(null)
    val videoDetails: StateFlow<com.deepeye.musicpro.domain.model.VideoDetails?> = _videoDetails.asStateFlow()

    private val _extractedColors = MutableStateFlow<com.deepeye.musicpro.util.ExtractedColors?>(null)
    val extractedColors: StateFlow<com.deepeye.musicpro.util.ExtractedColors?> = _extractedColors.asStateFlow()

    private val _currentLyrics = MutableStateFlow<com.deepeye.musicpro.domain.model.Lyrics?>(null)
    val currentLyrics: StateFlow<com.deepeye.musicpro.domain.model.Lyrics?> = _currentLyrics.asStateFlow()

    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerManager.timeRemainingMs

    private val _isBackgroundPlaybackEnabled = MutableStateFlow(false)
    val isBackgroundPlaybackEnabled: StateFlow<Boolean> = _isBackgroundPlaybackEnabled.asStateFlow()

    private val _audioBoostLevel = MutableStateFlow(0)
    val audioBoostLevel: StateFlow<Int> = _audioBoostLevel.asStateFlow()

    fun setAudioBoostLevel(level: Int) {
        _audioBoostLevel.value = level
        try {
            val volumeMultiplier = when (level) {
                3 -> 1.25f
                6 -> 1.5f
                12 -> 2.0f
                else -> 1.0f
            }
            playerController.player.volume = volumeMultiplier.coerceIn(0f, 2.0f)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private val _subtitlesEnabled = MutableStateFlow(false)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    fun toggleSubtitles() {
        _subtitlesEnabled.value = !_subtitlesEnabled.value
    }

    private val _showStatsForNerds = MutableStateFlow(false)
    val showStatsForNerds: StateFlow<Boolean> = _showStatsForNerds.asStateFlow()

    fun toggleStatsForNerds() {
        _showStatsForNerds.value = !_showStatsForNerds.value
    }

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
                    _videoDetails.value = null
                    if (mediaItem is com.deepeye.musicpro.domain.model.MediaItem.Remote) {
                        try {
                                                        _videoDetails.value = youtubeRemoteDataSource.getVideoDetails(mediaItem.id)
                            
                            // Check YouTube Interaction Status (Likes/Dislikes/Subscriptions)
                            try {
                                val intStatus = authClient.getVideoInteractionStatus(mediaItem.id)
                                if (intStatus != null) {
                                    playerController.updateLikeState(isLiked = intStatus.isLiked, isDisliked = intStatus.isDisliked)
                                    // Make sure local DB agrees with remote truth on Likes
                                    if (intStatus.isLiked) {
                                        libraryRepository.likeTrack(mediaItem.id, mediaItem.title, mediaItem.artist, "")
                                    } else if (!intStatus.isLiked) {
                                        libraryRepository.unlikeTrack(mediaItem.id, mediaItem.title, mediaItem.artist, "")
                                    }
                                } else {
                                    // if Not Auth'd / no response, check local DB
                                    val isLikedOffline = libraryRepository.isTrackLiked(mediaItem.id).first()
                                    playerController.updateLikeState(isLiked = isLikedOffline, isDisliked = false)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("PlayerViewModel", "Failed to sync YouTube interaction status", e)
                            }

                        } catch (e: Exception) {
                            // ignore
                        }
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

    fun toggleVideoMode() {
        val current = playerState.value.currentItem ?: return
        if (current is com.deepeye.musicpro.domain.model.MediaItem.Remote) {
            val targetIsVideo = !playerState.value.isVideo
            val currentPos = playerController.player.currentPosition
            playerController.playMedia(current.copy(isVideo = targetIsVideo), seekPosition = currentPos)
        }
    }

    fun playMedia(item: com.deepeye.musicpro.domain.model.MediaItem) {
        playerController.playMedia(item)
    }

    fun setQueue(
        items: List<com.deepeye.musicpro.domain.model.MediaItem>,
        startIndex: Int = 0,
    ) {
        playerController.setQueue(items, startIndex)
    }

    val queue = playerController.currentQueue
    val currentQueueIndex = playerController.currentQueueIndex

    fun clearQueue() {
        playerController.clearQueue()
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
        viewModelScope.launch(Dispatchers.IO) {
            // Update YouTube remote first!
            try {
                if (liked) authClient.likeVideo(currentId) else authClient.removeLike(currentId)
            } catch(e: Exception) {}
            
            // Sync local state
            playerController.updateLikeState(isLiked = liked, isDisliked = false)

            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                tasteProfileRepository.recordFeedback(currentId, liked = liked, dontPlayAgain = false)
                if (liked) {
                    libraryRepository.likeTrack(currentId, currentItem.title, currentItem.artist, "")
                    recommendationEngine.trackListenEvent(currentId, currentItem.title, currentItem.artist, "", 1000L, 1000L, false, true, false, false, false)
                } else {
                    libraryRepository.unlikeTrack(currentId, currentItem.title, currentItem.artist, "")
                }
            }
        }
    }


        fun dislikeTrack() {
        val currentItem = playerState.value.currentItem ?: return
        val currentId = currentItem.id
        val currentlyDisliked = playerState.value.isDisliked
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentlyDisliked) authClient.removeLike(currentId) else authClient.dislikeVideo(currentId)
            } catch(e: Exception) {}
            
            playerController.updateLikeState(isLiked = false, isDisliked = !currentlyDisliked)

            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                tasteProfileRepository.recordFeedback(currentId, liked = false, dontPlayAgain = true)
                if (!currentlyDisliked) {
                    recommendationEngine.trackListenEvent(currentId, currentItem.title, currentItem.artist, "", 1000L, 1000L, true, false, true, false, false)
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

    val diagnostics: StateFlow<com.deepeye.musicpro.player.format.PlaybackDiagnostics> =
        playerController.diagnostics

    val smartTubeFormatSnapshot: StateFlow<com.deepeye.musicpro.player.smarttube.SmartTubeFormatSnapshot> =
        playerController.smartTubePlaybackFormatRepository.snapshot

    fun setQualityPreset(preset: com.deepeye.musicpro.player.format.QualityPreset) {
        playerController.setQualityPreset(preset)
    }

    fun setVideoFormat(format: com.deepeye.musicpro.player.format.DeepEyeFormat) {
        playerController.setVideoFormat(format)
    }

    fun setAudioFormat(format: com.deepeye.musicpro.player.format.DeepEyeFormat) {
        playerController.setAudioFormat(format)
    }

    fun setBufferProfile(profile: com.deepeye.musicpro.player.format.BufferProfile) {
        playerController.setBufferProfile(profile)
    }

    fun getPlaybackDiagnostics(): com.deepeye.musicpro.player.format.PlaybackDiagnostics {
        return playerController.getPlaybackDiagnostics()
    }
}
