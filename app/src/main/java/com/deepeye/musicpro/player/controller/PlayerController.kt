// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.controller

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.data.db.AppDatabase
import com.deepeye.musicpro.data.prefs.dspDataStore
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.domain.resolver.SourceResolverManager
import com.deepeye.musicpro.player.queue.QueueManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.common.MediaItem as Media3Item

/**
 * Controller for the media player.
 * Orchestrates ExoPlayer, QueueManager, and DSP Engine.
 */
@Singleton
@OptIn(kotlinx.coroutines.FlowPreview::class)
class PlayerController
@Inject
constructor(
    val player: ExoPlayer,
    private val queueManager: QueueManager,
    private val sourceResolverManager: SourceResolverManager,
    private val audioSessionManager: com.deepeye.musicpro.dsp.session.AudioSessionManager,
    private val dspEngine: com.deepeye.musicpro.dsp.engine.DSPEngine,
    private val tasteProfileRepository: com.deepeye.musicpro.domain.repository.TasteProfileRepository,
    private val historyRepository: com.deepeye.musicpro.domain.repository.HistoryRepository,
    private val libraryRepository: com.deepeye.musicpro.domain.repository.library.LibraryRepository,
    private val musicRepository: com.deepeye.musicpro.domain.repository.MusicRepository,
    private val recommendationEngine: com.deepeye.musicpro.domain.recommendation.RecommendationEngine,
    private val autoplayRepository: com.deepeye.musicpro.domain.autoplay.AutoplayRepository,
    private val sleepTimerManager: dagger.Lazy<com.deepeye.musicpro.player.timer.SleepTimerManager>,
    private val playbackPathEnforcer: com.deepeye.musicpro.diagnostics.PlaybackPathEnforcer,
    private val audioSessionGuardian: com.deepeye.musicpro.diagnostics.AudioSessionGuardian,
    private val forensics: com.deepeye.musicpro.diagnostics.ExoPlayerForensics,
    private val dspProfileManager: com.deepeye.musicpro.dsp.profile.DspProfileManager,
    private val gamificationEngine: com.deepeye.musicpro.domain.gamification.GamificationEngine,
    private val tubeSimulatorProcessor: com.deepeye.musicpro.dsp.processor.TubeSimulatorProcessor,
    private val dspController: com.deepeye.musicpro.dsp.controller.DSPController,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _autoplayState = MutableStateFlow(com.deepeye.musicpro.domain.autoplay.AutoplayState())
    val autoplayState: StateFlow<com.deepeye.musicpro.domain.autoplay.AutoplayState> = _autoplayState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var playJob: Job? = null
    private val playMutex = Mutex()
    private var lastSkippedSegment: com.deepeye.musicpro.domain.model.SponsorSegment? = null

    // Taste profile analytics tracking variables
    private var currentTrackId: String? = null
    private var totalPlayTimeCurrentTrack: Long = 0L
    private var lastPlaybackStateTime: Long = 0L
    private val recentAutoplayTrackIds = mutableListOf<String>()
    private var playRetryCount = 0

    var isBackgroundPlaybackEnabled = false

    fun enableBackgroundPlayback(enable: Boolean) {
        isBackgroundPlaybackEnabled = enable
    }

    init {
        // Register diagnostics
        playbackPathEnforcer.registerPlayer(player)
        audioSessionGuardian.startMonitoring(player)
        audioSessionManager.attachToPlayer(player)
        player.addAnalyticsListener(forensics)

        // Initial load of global DSP profile so DSP works before opening DSP screen
        scope.launch {
            try {
                // Try resolving global profile
                val profile = dspProfileManager.resolveProfile("*", "*")
                if (profile == null) {
                    // Try applying empty profile so default params are set
                    dspProfileManager.loadAndApplyProfile("*")
                } else {
                    dspProfileManager.loadAndApplyProfile("*")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("PlayerController", "Failed to init DSP profile", e)
            }
        }

        // Configure software processors from DSPEngine params
        scope.launch {
            dspEngine.currentParams.collectLatest { params ->
                tubeSimulatorProcessor.setConfig(
                    enabled = params.enabled && params.tubeEnabled,
                    mode = params.tubeMode,
                    drivePercent = params.tubeDrive
                )
            }
        }

        // VLC-style: Language-aware audio track selection
        scope.launch {
            tasteProfileRepository.getTasteProfile().collect { profile ->
                val languages = profile.preferredLanguages
                if (languages.isNotEmpty()) {
                    val lang = languages.first() // Pick the first preferred language
                    player.trackSelectionParameters =
                        player.trackSelectionParameters
                            .buildUpon()
                            .setPreferredAudioLanguage(lang)
                            .setPreferredTextLanguage(lang)
                            .setForceHighestSupportedBitrate(true)
                            .build()
                }
            }
        }

        // --- Queue Snapshotting (decoupled for performance) ---
        // 1. Queue CONTENT changes → full Gson serialization (debounced 1s to survive rapid reordering)
        scope.launch {
            queueManager.queue
                .debounce(1000L)
                .collectLatest { queue ->
                    if (queue.isNotEmpty()) {
                        try {
                            val gson = com.google.gson.Gson()
                            val json = gson.toJson(queue)
                            val index = queueManager.currentIndex.value
                            historyRepository.saveQueueSnapshot(json, index)
                        } catch (e: Exception) {
                            android.util.Log.e("PlayerController", "Failed to snapshot queue", e)
                        }
                    }
                }
        }
        // 2. Index-only changes → lightweight SQL UPDATE (no JSON serialization)
        scope.launch {
            queueManager.currentIndex
                .collectLatest { index ->
                    if (index >= 0 && queueManager.queue.value.isNotEmpty()) {
                        try {
                            historyRepository.updateQueueIndex(index)
                        } catch (e: Exception) {
                            android.util.Log.e("PlayerController", "Failed to update queue index", e)
                        }
                    }
                }
        }

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        startPositionUpdates()
                        lastPlaybackStateTime = System.currentTimeMillis()
                    } else {
                        stopPositionUpdates()
                        if (currentTrackId != null) {
                            totalPlayTimeCurrentTrack += System.currentTimeMillis() - lastPlaybackStateTime
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            updateState { it.copy(isLoading = true) }
                        }
                        Player.STATE_READY -> {
                            updateState { it.copy(isLoading = false) }
                            // Force DSP re-attach since the AudioTrack has been created
                            audioSessionManager.forceReattach()
                        }
                        Player.STATE_ENDED -> {
                            onTrackEnded()
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("PlayerController", "ExoPlayer Error: ${error.message}", error)
                    updateState { it.copy(isLoading = false) }

                    val currentPos = player.currentPosition.coerceAtLeast(0)
                    val currentItem = playerState.value.currentItem

                    // Show non-technical error message and auto-skip to next track
                    scope.launch {
                        if (currentItem is MediaItem.Remote && playRetryCount < 3) {
                            playRetryCount++
                            android.util.Log.w("PlayerController", "Retrying playMedia for remote track: ${currentItem.title} (Attempt $playRetryCount/3) at position $currentPos due to error: ${error.message}")
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Playback error. Retrying stream... (Attempt $playRetryCount/3)",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                            kotlinx.coroutines.delay(500)
                            playMedia(currentItem, isRetry = true, seekPosition = currentPos)
                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Unable to play this media right now.",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                            kotlinx.coroutines.delay(500)
                            next()
                        }
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        val currentPos = player.currentPosition.coerceAtLeast(0)
                        updateState {
                            it.copy(
                                position = currentPos,
                                duration = player.duration.coerceAtLeast(0),
                            )
                        }
                    }
                }
            }
        )
        // --- URL prefetch cache (prefetch next 3 queue tracks) ---
        scope.launch {
            kotlinx.coroutines.flow.combine(queueManager.queue, queueManager.currentIndex) { q, idx -> Pair(q, idx) }
                .collectLatest { (q, idx) ->
                    if (q.isNotEmpty() && idx >= 0) {
                        // Launch a single IO coroutine to sequentially prefetch to avoid mutex contention
                        scope.launch(Dispatchers.IO) {
                            for (i in 1..3) {
                                val nextIdx = idx + i
                                if (nextIdx in q.indices) {
                                    val item = q[nextIdx]
                                    if (item is MediaItem.Remote && (item.streamUri == null || item.streamUri == Uri.EMPTY)) {
                                        try {
                                            android.util.Log.d("PlayerController", "Queue prefetcher: Resolving stream URL for track: ${item.title} (${item.id})")
                                            sourceResolverManager.resolve(item.id, item.isVideo)
                                        } catch (e: Exception) {
                                            android.util.Log.w("PlayerController", "Queue prefetcher failed for ${item.id}: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    val nowPlaying = playerState.map { it.currentItem }

    fun playMedia(item: MediaItem, isRetry: Boolean = false, seekPosition: Long = 0L) {
        android.util.Log.d("PlayerController", "playMedia called with item: $item, isRetry: $isRetry, seekPosition: $seekPosition")

        playJob?.cancel()
        playJob =
            scope.launch {
                try {
                    if (!isRetry) {
                        playRetryCount = 0
                    }
                    // Check if song is blocked in the database
                    val feedback = tasteProfileRepository.getFeedback(item.id)
                    if (feedback != null && feedback.dontPlayAgain) {
                        android.util.Log.i("PlayerController", "Track ${item.title} (${item.id}) is blocked. Auto-skipping!")
                        next()
                        return@launch
                    }

                    val oldCurrentItem = _playerState.value.currentItem
                    // Immediately pause old track and show new track info with loading spinner
                    player.pause()
                    updateState { it.copy(
                        isLoading = true, 
                        currentItem = item, 
                        isPlaying = false,
                        isVideo = item is MediaItem.Remote && item.isVideo
                    ) }

                    val finalItem =
                        when (item) {
                            is MediaItem.Local -> item
                            is MediaItem.Remote -> {
                                android.util.Log.d("PlayerController", "Remote item: ${item.title}, id: ${item.id}, streamUri: ${item.streamUri}")
                                if (item.streamUri == null || item.streamUri == Uri.EMPTY || isRetry) {
                                    android.util.Log.d("PlayerController", "streamUri needs resolution, fetching getStreamUrl (forceRefresh=$isRetry)...")
                                    
                                    val url = sourceResolverManager.resolve(item.id, item.isVideo, forceRefresh = isRetry)
                                    ensureActive()
                                    android.util.Log.d("PlayerController", "First extraction fetched: $url")

                                    // Audio extraction fallback: if requested audio-only but it failed, retry with video DASH/HLS
                                    var finalUrl = url
                                    if (finalUrl == null && !item.isVideo) {
                                        android.util.Log.d("PlayerController", "Audio stream extraction failed. Retrying with video DASH/HLS stream fallback...")
                                        finalUrl = sourceResolverManager.resolve(item.id, true, forceRefresh = isRetry)
                                        ensureActive()
                                        android.util.Log.d("PlayerController", "Fallback video extraction fetched: $finalUrl")
                                    }

                                    if (finalUrl != null) {
                                        // If the original requested item was audio-only (item.isVideo == false),
                                        // keep it as isVideo = false so ExoPlayer plays it as audio-only,
                                        // even if the fallback returned result has isVideo = true (meaning DASH/HLS stream).
                                        item.copy(
                                            streamUri = Uri.parse(finalUrl),
                                            isVideo = item.isVideo, // Keep original isVideo setting for proper focus/WebView mute
                                        )
                                    } else {
                                        if (item.isVideo) {
                                            android.util.Log.w("PlayerController", "Video stream extraction failed for ${item.title}! Using YouTube fallback player...")
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Trying another source...",
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                            item.copy(
                                                streamUri = Uri.parse("https://www.youtube.com/watch?v=${item.id}"),
                                                isVideo = true,
                                                duration = item.duration.takeIf { it > 0 } ?: 180000L,
                                            )
                                        } else {
                                            android.util.Log.e("PlayerController", "All stream extraction attempts failed for ${item.title}! Audio extraction failed.")
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Failed to extract audio stream for ${item.title}",
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                            throw Exception("Failed to extract stream for ${item.id}")
                                        }
                                    }
                                } else {
                                    item
                                }
                            }
                        }

                    ensureActive()

                    // Fetch SponsorBlock segments asynchronously so we don't block playback startup
                    val isVideoItem = finalItem is MediaItem.Remote && finalItem.isVideo
                    
                    if (isVideoItem) {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val url = java.net.URL("https://sponsor.ajay.app/api/skipSegments?videoID=${finalItem.id}&categories=[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"preview\"]")
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.requestMethod = "GET"
                                connection.connectTimeout = 3000
                                connection.readTimeout = 3000
                                if (connection.responseCode == 200) {
                                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                                    val jsonArray = org.json.JSONArray(jsonStr)
                                    val list = mutableListOf<com.deepeye.musicpro.domain.model.SponsorSegment>()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        val segmentArray = obj.getJSONArray("segment")
                                        val startMs = (segmentArray.getDouble(0) * 1000).toLong()
                                        val endMs = (segmentArray.getDouble(1) * 1000).toLong()
                                        val category = obj.getString("category")
                                        list.add(com.deepeye.musicpro.domain.model.SponsorSegment(startMs, endMs, category))
                                    }
                                    android.util.Log.d("SponsorBlock", "Found ${list.size} segments for ${finalItem.id}")
                                    // Update state with fetched segments once they arrive
                                    updateState { it.copy(sponsorSegments = list.toImmutableList()) }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SponsorBlock", "Failed to fetch segments asynchronously", e)
                            }
                        }
                    }

                    playMutex.withLock {
                        if (currentTrackId != null) {
                            recordCurrentTrackPlayStatsLocked(finishedSuccessfully = false)
                        }
                        currentTrackId = finalItem.id
                        totalPlayTimeCurrentTrack = 0L
                        lastPlaybackStateTime = System.currentTimeMillis()
                        lastSkippedSegment = null // Reset SponsorBlock state for new track

                        val media3Item = finalItem.toMedia3Item()
                        if (media3Item.localConfiguration?.uri == Uri.EMPTY || media3Item.localConfiguration?.uri == null) {
                            android.util.Log.e("PlayerController", "Cannot play: Stream URI is empty")
                            scope.launch { delay(1000); next() }
                            return@withLock
                        }

                        player.setMediaItem(media3Item)
                        if (seekPosition > 0L) {
                            android.util.Log.d("PlayerController", "Seeking player to position: $seekPosition")
                            player.seekTo(seekPosition)
                        }
                        player.prepare()
                        player.play()

                        // Load per-track DSP Profile for the new track
                        scope.launch {
                            try {
                                dspProfileManager.loadAndApplyProfile(finalItem.id)
                            } catch (e: Exception) {
                                android.util.Log.e("PlayerController", "Failed to load DSP profile for track ${finalItem.id}", e)
                            }
                        }

                        // Start the MediaSessionService (Media3 will handle foreground promotion)
                        try {
                            val serviceIntent = android.content.Intent(context, com.deepeye.musicpro.player.service.MusicPlayerService::class.java)
                            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("PlayerController", "Could not start MediaSessionService from background", e)
                        }

                        updateState {
                            it.copy(
                                currentItem = finalItem,
                                currentSong = (finalItem as? MediaItem.Local)?.song,
                                isVideo = finalItem is MediaItem.Remote && finalItem.isVideo,
                                isLoading = false,
                                sponsorSegments = if (finalItem.id != oldCurrentItem?.id) {
                                    persistentListOf()
                                } else {
                                    _playerState.value.sponsorSegments
                                },
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    android.util.Log.d("PlayerController", "playMedia cancelled for item: ${item.title}")
                } catch (e: Exception) {
                    android.util.Log.e("PlayerController", "Exception in playMedia: ${e.message}", e)
                    updateState { it.copy(isLoading = false) }
                }
            }
    }

    fun setQueue(
        items: List<MediaItem>,
        startIndex: Int = 0,
    ) {
        queueManager.setQueue(items, startIndex)
        val firstItem = items.getOrNull(startIndex)
        if (firstItem != null) {
            playMedia(firstItem)
        }
    }

    fun playQueueItem(index: Int) {
        val item = queueManager.jumpTo(index)
        if (item != null) {
            playMedia(item)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        queueManager.moveItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        val wasPlaying = index == queueManager.currentIndex.value
        queueManager.removeItem(index)
        if (wasPlaying) {
            val nextItem = queueManager.currentItem
            if (nextItem != null) {
                playMedia(nextItem)
            } else {
                player.stop()
                updateState { it.copy(currentItem = null, isPlaying = false, isLoading = false) }
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        updateState { it.copy(playbackSpeed = speed) }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun setVideoQuality(quality: String) {
        val maxHeight = when (quality.lowercase().substringBefore(" ")) {
            "1080p" -> 1080
            "720p" -> 720
            "480p" -> 480
            else -> Integer.MAX_VALUE
        }
        val parameters = player.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(
                if (maxHeight < Integer.MAX_VALUE) maxHeight * 16 / 9 else Integer.MAX_VALUE,
                maxHeight
            )
            .build()
        player.trackSelectionParameters = parameters
        android.util.Log.d("PlayerController", "Natively set video quality constraint: Max height = $maxHeight")
    }

    fun next() {
        val trackId = currentTrackId
        var isTrackSkipped = false
        if (trackId != null && playerState.value.isPlaying) {
            val listenMs = totalPlayTimeCurrentTrack + (System.currentTimeMillis() - lastPlaybackStateTime)
            val item = playerState.value.currentItem
            val totalMs = playerState.value.duration
            isTrackSkipped = listenMs < totalMs * 0.9f // Consider it a skip if less than 90% played

            scope.launch {
                recommendationEngine.trackListenEvent(
                    videoId = item?.id ?: "",
                    title = item?.title ?: "",
                    artist = item?.artist ?: "",
                    channelId = "",
                    listenDurationMs = listenMs,
                    totalDurationMs = totalMs,
                    wasSkipped = isTrackSkipped,
                    wasLiked = false,
                    wasDisliked = false,
                    wasAddedToPlaylist = false,
                    wasReplayed = false,
                )
            }
        }

        val nextTrack = queueManager.next()
        if (nextTrack != null) {
            playMedia(nextTrack)
        } else if (playerState.value.autoplayEnabled) {
            // Queue is empty, trigger Personalized Autoplay
            val current = playerState.value.currentItem
            android.util.Log.d(
                "AutoplayEngine",
                "Queue empty. Triggering personalized autoplay. Last played: ${current?.title}"
            )
            scope.launch {
                try {
                    val candidates =
                        withContext(Dispatchers.IO) {
                            autoplayRepository.generateNextQueue(current, _autoplayState.value)
                        }

                    if (candidates.isEmpty()) {
                        android.util.Log.w("AutoplayEngine", "No autoplay candidates found. Stopping playback.")
                        return@launch
                    }

                    // Exclude recently played autoplay tracks
                    val validCandidates = candidates.filter {
                        !recentAutoplayTrackIds.contains(it.videoId) && it.videoId != (current?.id ?: "")
                    }
                    val finalCandidate = validCandidates.firstOrNull() ?: candidates.firstOrNull()

                    if (finalCandidate != null) {
                        android.util.Log.i(
                            "AutoplayEngine",
                            "Autoplay selected: ${finalCandidate.title} (${finalCandidate.videoId})"
                        )
                        // Add to recent list
                        recentAutoplayTrackIds.add(finalCandidate.videoId)
                        if (recentAutoplayTrackIds.size > 10) {
                            recentAutoplayTrackIds.removeAt(0)
                        }

                        // Update AutoplayState
                        _autoplayState.update { state ->
                            state.copy(
                                queue = validCandidates.drop(1),
                                history = (state.history + (current?.id ?: "")).takeLast(50),
                                skipStreak = if (isTrackSkipped) state.skipStreak + 1 else 0,
                                lastGeneratedAt = System.currentTimeMillis(),
                                discoveryMode = finalCandidate.score < 0.4f,
                                familiarMode = finalCandidate.score >= 0.4f,
                            )
                        }

                        val mediaItem =
                            MediaItem.Remote(
                                id = finalCandidate.videoId,
                                title = finalCandidate.title,
                                artist = finalCandidate.artist,
                                artworkUri =
                                android.net.Uri.parse(
                                    "https://img.youtube.com/vi/${finalCandidate.videoId}/hqdefault.jpg",
                                ),
                                isVideo = (current as? MediaItem.Remote)?.isVideo ?: false,
                                duration = 0L,
                            )
                        playMedia(mediaItem)
                    } else {
                        android.util.Log.w("AutoplayEngine", "Autoplay pool completely exhausted. Halting.")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AutoplayEngine", "Error during autoplay resolution", e)
                }
            }
        }
    }

    fun previous() {
        val prevTrack = queueManager.previous()
        if (player.currentPosition > 3000 || prevTrack == null) {
            player.seekTo(0)
        } else {
            playMedia(prevTrack)
        }
    }

    fun toggleRepeat() {
        queueManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        queueManager.toggleShuffleMode()
    }

    fun toggleAutoplay() {
        updateState { it.copy(autoplayEnabled = !it.autoplayEnabled) }
    }

    fun setAutoplayMode(mode: com.deepeye.musicpro.domain.autoplay.AutoplayMode) {
        _autoplayState.update { state ->
            state.copy(
                familiarMode = mode == com.deepeye.musicpro.domain.autoplay.AutoplayMode.FAMILIAR,
                discoveryMode = mode == com.deepeye.musicpro.domain.autoplay.AutoplayMode.DISCOVERY,
                // Clear pre-fetched queue so next autoplay trigger regenerates with new mode
                queue = emptyList(),
            )
        }
    }

    fun removeAutoplayQueueItem(videoId: String) {
        _autoplayState.update { state ->
            state.copy(queue = state.queue.filter { it.videoId != videoId })
        }
    }

    private fun onTrackEnded() {
        recordCurrentTrackPlayStats(finishedSuccessfully = true)
        // Honor the "end of current track" sleep-timer option before advancing.
        // If the timer pauses playback here, the queue does not auto-advance.
        val timer = sleepTimerManager.get()
        if (timer.isActive && timer.timeRemainingMs.value == -1L) {
            timer.onTrackEnded()
            return
        }
        next()
    }


    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob =
            scope.launch {
                while (isActive) {
                    val currentPos = player.currentPosition.coerceAtLeast(0)
                    updateState {
                        it.copy(
                            position = currentPos,
                            duration = player.duration.coerceAtLeast(0),
                        )
                    }

                    // SponsorBlock Auto-Skip Logic
                    val segments = playerState.value.sponsorSegments
                    if (segments.isNotEmpty()) {
                        val currentSegment = segments.find { seg ->
                            currentPos >= seg.startMs && currentPos < seg.endMs && seg != lastSkippedSegment
                        }
                        if (currentSegment != null) {
                            lastSkippedSegment = currentSegment
                            android.util.Log.d("SponsorBlock", "Auto-skipping ${currentSegment.category} to ${currentSegment.endMs}ms")
                            player.seekTo(currentSegment.endMs)
                        }
                    }

                    delay(250)
                }
            }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }



    private var isAppInForeground = true

    fun setAppInForeground(foreground: Boolean) {
        isAppInForeground = foreground
        updateState { it.copy(isAppInForeground = foreground) }
    }

    private fun updateState(transform: (PlayerState) -> PlayerState) {
        _playerState.update(transform)
    }

    private fun MediaItem.toMedia3Item(): Media3Item {
        val uri =
            when (this) {
                is MediaItem.Local -> song.uri
                is MediaItem.Remote -> streamUri ?: Uri.EMPTY
            }

        val builder = Media3Item.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .build(),
            )
            
        // Help ExoPlayer correctly identify adaptive streams when URLs lack standard extensions
        val uriStr = uri.toString()
        if (uriStr.contains("manifest/dash") || uriStr.contains(".mpd")) {
            builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
        } else if (uriStr.contains("manifest/hls") || uriStr.contains(".m3u8") || uriStr.contains("m3u8")) {
            builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        }

        return builder.build()
    }

    private fun recordCurrentTrackPlayStats(finishedSuccessfully: Boolean) {
        scope.launch {
            playMutex.withLock {
                recordCurrentTrackPlayStatsLocked(finishedSuccessfully)
            }
        }
    }

    private fun recordCurrentTrackPlayStatsLocked(finishedSuccessfully: Boolean) {
        val currentItem = playerState.value.currentItem ?: return
        val currentId = currentTrackId ?: return

        // Accumulate remaining time since last state change if playing
        if (player.isPlaying) {
            totalPlayTimeCurrentTrack += System.currentTimeMillis() - lastPlaybackStateTime
        }

        val duration = player.duration.coerceAtLeast(0)
        val played = totalPlayTimeCurrentTrack.coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)

        // Make sure it wasn't a zero-duration glitch
        if (played > 500) {
            val source = if (currentItem is MediaItem.Local) "local" else "youtube"
            val language = getLanguageForMedia(currentItem)
            val artistId =
                when (currentItem) {
                    is MediaItem.Local -> currentItem.song.artistId.toString()
                    is MediaItem.Remote -> currentItem.artist
                }

            val event =
                com.deepeye.musicpro.data.db.PlayEvent(
                    songId = currentId,
                    artistId = artistId,
                    language = language,
                    playedMs = played,
                    durationMs = duration,
                    source = source,
                )

            scope.launch(Dispatchers.IO) {
                // Gamification update
                gamificationEngine.checkAndUpdateStreak()
                if (duration > 0) {
                    val ratio = played.toFloat() / duration.toFloat()
                    gamificationEngine.updateSongCompletion(duration, ratio)
                    val minutes = (played / 60000).toInt()
                    gamificationEngine.updateDailyListeningMinutes(minutes)
                }

                tasteProfileRepository.recordPlayEvent(event)

                val isVideo = (currentItem as? MediaItem.Remote)?.isVideo == true
                if (isVideo) {
                    historyRepository.recordVideoProgress(
                        videoId = currentId,
                        title = currentItem.title,
                        thumbnailUri = currentItem.artworkUri?.toString(),
                        positionMs = played,
                        durationMs = duration
                    )
                } else {
                    historyRepository.recordPlayback(
                        mediaId = currentId,
                        title = currentItem.title,
                        artist = currentItem.artist,
                        album = (currentItem as? MediaItem.Local)?.song?.album ?: "",
                        artworkUri = currentItem.artworkUri?.toString(),
                        playDurationMs = played,
                        totalDurationMs = duration,
                        source = source
                    )
                }
                
                libraryRepository.recordRecentPlay(
                    videoId = currentId,
                    title = currentItem.title,
                    artist = currentItem.artist,
                    artworkUrl = currentItem.artworkUri?.toString()
                )

                // If skipped quickly (<10s) and not a full finish
                if (played < 10000 && !finishedSuccessfully && duration > 15000) {
                    tasteProfileRepository.recordQuickSkip(currentId)
                }
            }
        }

        // Reset variables
        currentTrackId = null
        totalPlayTimeCurrentTrack = 0L
    }

    private fun getLanguageForMedia(item: MediaItem): String {
        val titleLower = item.title.lowercase()
        val langs =
            listOf("hindi", "punjabi", "bhojpuri", "tamil", "telugu", "english", "haryanvi", "bengali", "korean")
        for (lang in langs) {
            if (titleLower.contains(lang)) return lang.replaceFirstChar { it.uppercase() }
        }
        if (item is MediaItem.Local) {
            val genreLower = item.song.genre.lowercase()
            for (lang in langs) {
                if (genreLower.contains(lang)) return lang.replaceFirstChar { it.uppercase() }
            }
            return if (item.song.genre.isNotEmpty()) item.song.genre else "Local"
        }
        return "English"
    }

    fun applyDSPPreset(preset: com.deepeye.musicpro.dsp.model.DSPPreset) {
        dspController.applyPreset(preset)
    }
}
