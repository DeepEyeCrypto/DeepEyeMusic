// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.controller

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.player.queue.QueueManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for the media player.
 * Orchestrates ExoPlayer, QueueManager, and DSP Engine.
 */
@Singleton
class PlayerController @Inject constructor(
    val player: ExoPlayer,
    private val queueManager: QueueManager,
    private val youtubeDataSource: YoutubeRemoteDataSource,
    private val audioSessionManager: com.deepeye.musicpro.dsp.session.AudioSessionManager,
    private val v4aEngine: com.deepeye.musicpro.dsp.engine.V4AEngine,
    private val tasteProfileRepository: com.deepeye.musicpro.domain.repository.TasteProfileRepository,
    private val musicRepository: com.deepeye.musicpro.domain.repository.MusicRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    val gainBudget = v4aEngine.gainBudget

    private var positionUpdateJob: Job? = null
    private var playJob: Job? = null
    private val playMutex = Mutex()

    // Taste profile analytics tracking variables
    private var currentTrackId: String? = null
    private var totalPlayTimeCurrentTrack: Long = 0L
    private var lastPlaybackStateTime: Long = 0L
    private val recentAutoplayTrackIds = mutableListOf<String>()

    init {
        // Wire DSP engine to the player session
        audioSessionManager.attachToPlayer(player)

        // VLC-style: Language-aware audio track selection
        scope.launch {
            tasteProfileRepository.getTasteProfile().collect { profile ->
                val languages = profile.preferredLanguages
                if (languages.isNotEmpty()) {
                    val lang = languages.first() // Pick the first preferred language
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setPreferredAudioLanguage(lang)
                        .setPreferredTextLanguage(lang)
                        .build()
                }
            }
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!playerState.value.isVideo) {
                    updateState { it.copy(isPlaying = isPlaying) }
                }
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
                if (playerState.value.isVideo) {
                    if (playbackState == Player.STATE_ENDED) onTrackEnded()
                    return
                }
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        updateState { it.copy(isLoading = true) }
                    }
                    Player.STATE_READY -> {
                        updateState { it.copy(isLoading = false) }
                    }
                    Player.STATE_ENDED -> {
                        onTrackEnded()
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (playerState.value.isVideo) return
                android.util.Log.e("PlayerController", "ExoPlayer Error: ${error.message}", error)
                updateState { it.copy(isLoading = false) }
            }
        })
    }

    val nowPlaying = playerState.map { it.currentItem }

    fun playMedia(item: MediaItem) {
        android.util.Log.e("PlayerController", "playMedia called with item: $item")
        
        playJob?.cancel()
        playJob = scope.launch {
            try {
                // Check if song is blocked in the database
                val feedback = tasteProfileRepository.getFeedback(item.id)
                if (feedback != null && feedback.dontPlayAgain) {
                    android.util.Log.e("PlayerController", "Track ${item.title} (${item.id}) is blocked. Auto-skipping!")
                    next()
                    return@launch
                }
 
                updateState { it.copy(isLoading = true) }
 
                val finalItem = when (item) {
                    is MediaItem.Local -> item
                    is MediaItem.Remote -> {
                        android.util.Log.e("PlayerController", "Remote item: ${item.title}, id: ${item.id}, streamUri: ${item.streamUri}")
                        if (item.streamUri == null || item.streamUri == Uri.EMPTY) {
                            android.util.Log.e("PlayerController", "streamUri is null or empty, fetching getStreamUrl...")
                            val result = youtubeDataSource.getStreamUrl(item.id, preferVideo = item.isVideo)
                            ensureActive()
                            android.util.Log.e("PlayerController", "getStreamUrl fetched: $result")
                            if (result != null) {
                                item.copy(
                                    streamUri = Uri.parse(result.url),
                                    isVideo = item.isVideo || result.isVideo
                                )
                            } else {
                                android.util.Log.e("PlayerController", "getStreamUrl returned null!")
                                if (item.isVideo) {
                                    // For video items: use the YouTube watch URL so the WebView iframe
                                    // can still render the video by videoId, preserving video mode
                                    android.util.Log.e("PlayerController", "Video item fallback: using YouTube watch URL for iframe playback")
                                    item.copy(
                                        streamUri = Uri.parse("https://www.youtube.com/watch?v=${item.id}"),
                                        isVideo = true
                                    )
                                } else {
                                    // For audio items: fall back to a placeholder audio stream
                                    android.util.Log.e("PlayerController", "Audio item fallback: using placeholder stream")
                                    item.copy(
                                        streamUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                                        isVideo = false
                                    )
                                }
                            }
                        } else {
                            item
                        }
                    }
                }

                ensureActive()

                // Fetch SponsorBlock segments
                var fetchedSegments: List<com.deepeye.musicpro.domain.model.SponsorSegment> = emptyList()
                if (finalItem is MediaItem.Remote && finalItem.isVideo) {
                    fetchedSegments = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                                list
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SponsorBlock", "Failed to fetch segments", e)
                            emptyList()
                        }
                    }
                }

                ensureActive()

                playMutex.withLock {
                    if (currentTrackId != null) {
                        recordCurrentTrackPlayStatsLocked(finishedSuccessfully = false)
                    }
                    currentTrackId = finalItem.id
                    totalPlayTimeCurrentTrack = 0L
                    lastPlaybackStateTime = System.currentTimeMillis()
                    lastSkippedSegment = null // Reset SponsorBlock state for new track

                    // ── LAYER 3: isVideo state → audio track + focus management ──
                    // When video plays: WebView handles audio, ExoPlayer audio track disabled + focus released
                    // When audio plays: ExoPlayer audio track restored + focus reclaimed
                    if (finalItem is MediaItem.Remote && finalItem.isVideo) {
                        mutePlayerForVideoMode()        // Disable audio renderer completely
                        abandonAudioFocusForWebViewMode() // Release audio focus for WebView
                    } else {
                        restorePlayerAudio()             // Enable audio renderer
                        restoreAudioFocusHandling()      // Reclaim audio focus
                    }

                    val media3Item = finalItem.toMedia3Item()
                    if (media3Item.localConfiguration?.uri == Uri.EMPTY) {
                        android.util.Log.e("PlayerController", "Cannot play: Stream URI is empty")
                        return@withLock
                    }

                    player.setMediaItem(media3Item)
                    player.prepare()
                    player.play()
                    
                    // Start the MediaSessionService (Media3 will handle foreground promotion)
                    val serviceIntent = android.content.Intent(context, com.deepeye.musicpro.player.service.MusicPlayerService::class.java)
                    context.startService(serviceIntent)
                    
                    if (finalItem is MediaItem.Remote && finalItem.isVideo) {
                        updateState { 
                            it.copy(
                                currentItem = finalItem, 
                                currentSong = null,
                                isVideo = true,
                                isLoading = false,
                                isPlaying = true,
                                sponsorSegments = fetchedSegments
                            ) 
                        }
                    } else {
                        updateState { 
                            it.copy(
                                currentItem = finalItem, 
                                currentSong = (finalItem as? MediaItem.Local)?.song,
                                isVideo = false,
                                isLoading = false,
                                sponsorSegments = fetchedSegments
                            ) 
                        }
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

    fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        queueManager.setQueue(items, startIndex)
        val firstItem = items.getOrNull(startIndex)
        if (firstItem != null) {
            playMedia(firstItem)
        }
    }

    fun togglePlayPause() {
        if (playerState.value.isVideo) {
            val currentlyPlaying = playerState.value.isPlaying
            updateState { it.copy(isPlaying = !currentlyPlaying) }
            if (currentlyPlaying) player.pause() else player.play()
        } else {
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        updateState { it.copy(playbackSpeed = speed) }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun next() {
        val nextTrack = queueManager.next()
        if (nextTrack != null) {
            playMedia(nextTrack)
        } else if (playerState.value.autoplayEnabled) {
            // Queue is empty, trigger Personalized Autoplay
            val current = playerState.value.currentItem
            android.util.Log.d("AutoplayEngine", "Queue empty. Triggering personalized autoplay. Last played: ${current?.title}")
            scope.launch {
                try {
                    val candidates: List<MediaItem> = withContext(Dispatchers.IO) {
                        if (current is MediaItem.Local) {
                            // Query local database for songs
                            val localSongs = musicRepository.getAllSongs().first()
                            localSongs.map { MediaItem.Local(it) }
                        } else if (current is MediaItem.Remote) {
                            // Fetch related remote songs
                            youtubeDataSource.getRelatedMusic(current.title, current.artist, isVideo = current.isVideo)
                        } else {
                            // Fallback: load some local songs
                            val localSongs = musicRepository.getAllSongs().first()
                            localSongs.map { MediaItem.Local(it) }
                        }
                    }

                    if (candidates.isEmpty()) {
                        android.util.Log.w("AutoplayEngine", "No autoplay candidates found. Stopping playback.")
                        return@launch
                    }

                    // Loop Guard: cap retry attempts to prevent infinite loop
                    var finalCandidate: MediaItem? = null
                    var attempts = 0
                    val maxAttempts = 5

                    while (finalCandidate == null && attempts < maxAttempts) {
                        attempts++
                        val candidate = tasteProfileRepository.getNextAutoPlayCandidate(
                            currentSongId = current?.id ?: "",
                            candidates = candidates
                        )

                        if (candidate != null) {
                            val id = candidate.id
                            // Exclude recently played autoplay tracks to ensure variety and avoid loops
                            if (!recentAutoplayTrackIds.contains(id)) {
                                finalCandidate = candidate
                            } else {
                                android.util.Log.d("AutoplayEngine", "Candidate $id found in recent history. Retrying...")
                            }
                        } else {
                            break
                        }
                    }

                    // Graceful fallback: pick a random item from candidate pool that is not recently played
                    if (finalCandidate == null) {
                        android.util.Log.d("AutoplayEngine", "Failed to resolve personalized candidate. Falling back to fresh random.")
                        finalCandidate = candidates
                            .filter { it.id != (current?.id ?: "") && !recentAutoplayTrackIds.contains(it.id) }
                            .randomOrNull() ?: candidates.randomOrNull()
                    }

                    if (finalCandidate != null) {
                        android.util.Log.i("AutoplayEngine", "Autoplay selected: ${finalCandidate.title} (${finalCandidate.id})")
                        // Add to recent list
                        recentAutoplayTrackIds.add(finalCandidate.id)
                        if (recentAutoplayTrackIds.size > 10) {
                            recentAutoplayTrackIds.removeAt(0)
                        }
                        playMedia(finalCandidate)
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
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (prevTrack != null) {
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

    private fun onTrackEnded() {
        recordCurrentTrackPlayStats(finishedSuccessfully = true)
        next()
    }

    private var lastSkippedSegment: com.deepeye.musicpro.domain.model.SponsorSegment? = null

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                val currentPos = player.currentPosition.coerceAtLeast(0)
                updateState {
                    it.copy(
                        position = currentPos,
                        duration = player.duration.coerceAtLeast(0)
                    )
                }
                
                // SponsorBlock Auto-Skip Logic
                val segments = playerState.value.sponsorSegments
                if (segments.isNotEmpty()) {
                    // Pre-skip 500ms before endMs to avoid jumping slightly back
                    val currentSegment = segments.find { currentPos >= it.startMs && currentPos < (it.endMs - 500) }
                    if (currentSegment != null && currentSegment != lastSkippedSegment) {
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

    // ── LAYER 1: ExoPlayer audio track disable/enable ──
    // Disables the audio renderer completely — more reliable than setVolume(0f)
    // because the renderer itself is removed, preventing any audio from playing.
    private fun mutePlayerForVideoMode() {
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        player.trackSelectionParameters = params
    }

    // Restores the audio renderer for normal audio-only playback
    private fun restorePlayerAudio() {
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
        player.trackSelectionParameters = params
    }

    // ── LAYER 2: Audio focus abandon/restore ──
    // Releases ExoPlayer's audio focus claim so WebView can handle audio naturally
    private fun abandonAudioFocusForWebViewMode() {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ false
        )
    }

    // Restores ExoPlayer's audio focus handling for audio-only mode
    private fun restoreAudioFocusHandling() {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
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
        val uri = when (this) {
            is MediaItem.Local -> song.uri
            is MediaItem.Remote -> streamUri ?: Uri.EMPTY
        }

        return Media3Item.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()
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
            val artistId = when (currentItem) {
                is MediaItem.Local -> currentItem.song.artistId.toString()
                is MediaItem.Remote -> currentItem.artist
            }

            val event = com.deepeye.musicpro.data.db.PlayEvent(
                songId = currentId,
                artistId = artistId,
                language = language,
                playedMs = played,
                durationMs = duration,
                source = source
            )

            scope.launch(Dispatchers.IO) {
                tasteProfileRepository.recordPlayEvent(event)
                
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
        val langs = listOf("hindi", "punjabi", "bhojpuri", "tamil", "telugu", "english", "haryanvi", "bengali", "korean")
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
}
