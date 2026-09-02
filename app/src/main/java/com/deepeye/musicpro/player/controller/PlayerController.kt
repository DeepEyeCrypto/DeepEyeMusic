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
import com.deepeye.musicpro.domain.model.toMedia3Item
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.domain.resolver.SourceResolverManager
import com.deepeye.musicpro.player.queue.QueueManager
import com.deepeye.musicpro.player.smarttube.SmartTubePlaybackFormatRepository
import com.deepeye.musicpro.player.smarttube.toDeepEyeFormat
import com.deepeye.musicpro.player.recovery.PlaybackRecoveryCoordinator
import com.deepeye.musicpro.player.recovery.PlaybackRecoveryErrorMapper
import com.deepeye.musicpro.player.recovery.PlaybackRecoveryError
import com.deepeye.musicpro.player.recovery.PlaybackRecoverySnapshot
import com.deepeye.musicpro.player.recovery.StreamRecoveryPolicy
import com.deepeye.musicpro.player.recovery.SmartTubeSourceRefreshUseCase
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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
    private val lyricsRepository: com.deepeye.musicpro.domain.lyrics.LyricsRepository,
    private val dspProfileManager: com.deepeye.musicpro.dsp.profile.DspProfileManager,
    private val gamificationEngine: com.deepeye.musicpro.domain.gamification.GamificationEngine,
    private val tubeSimulatorProcessor: com.deepeye.musicpro.dsp.processor.TubeSimulatorProcessor,
    private val dspController: com.deepeye.musicpro.dsp.controller.DSPController,
    private val cloudSyncManager: com.deepeye.musicpro.domain.sync.CloudSyncManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    val qualitySelectionEngine: com.deepeye.musicpro.player.format.QualitySelectionEngine = com.deepeye.musicpro.player.format.QualitySelectionEngine(com.deepeye.musicpro.player.format.DeviceCodecCapabilities()),
    val deviceCodecCapabilities: com.deepeye.musicpro.player.format.DeviceCodecCapabilities = com.deepeye.musicpro.player.format.DeviceCodecCapabilities(),
    val smartTubePlaybackFormatRepository: SmartTubePlaybackFormatRepository = SmartTubePlaybackFormatRepository(),
    val streamRecoveryPolicy: StreamRecoveryPolicy = StreamRecoveryPolicy(),
    val playbackRecoveryErrorMapper: PlaybackRecoveryErrorMapper = PlaybackRecoveryErrorMapper(),
    val playbackRecoveryCoordinator: PlaybackRecoveryCoordinator = PlaybackRecoveryCoordinator(
        refreshUseCase = SmartTubeSourceRefreshUseCase(sourceResolverManager),
        policy = streamRecoveryPolicy,
        smartTubePlaybackFormatRepository = smartTubePlaybackFormatRepository,
        dspProfileManager = dspProfileManager,
        dspEngine = dspEngine,
        context = context
    ),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val formatInventoryMergePolicy = FormatInventoryMergePolicy()
    private val formatInventoryMediaGate = FormatInventoryMediaGate()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _autoplayState = MutableStateFlow(com.deepeye.musicpro.domain.autoplay.AutoplayState())
    val autoplayState: StateFlow<com.deepeye.musicpro.domain.autoplay.AutoplayState> = _autoplayState.asStateFlow()

    val diagnostics: StateFlow<com.deepeye.musicpro.player.format.PlaybackDiagnostics> = _playerState
        .map { getPlaybackDiagnostics() }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = getPlaybackDiagnostics()
        )

    private var positionUpdateJob: Job? = null
    private var playJob: Job? = null
    private var stablePlaybackResetJob: Job? = null
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
        player.addAnalyticsListener(androidx.media3.exoplayer.util.EventLogger(null, "EventLogger"))

        // Sync recovery coordinator state to PlayerState
        scope.launch {
            playbackRecoveryCoordinator.isRecovering.collectLatest { recovering ->
                updateState { it.copy(isRecovering = recovering) }
            }
        }
        scope.launch {
            playbackRecoveryCoordinator.recoveryMessage.collectLatest { msg ->
                updateState { it.copy(recoveryMessage = msg) }
            }
        }

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
        
        // 3. Sync to PlayerState (so UI can observe the actual queue)
        scope.launch {
            kotlinx.coroutines.flow.combine(queueManager.queue, queueManager.currentIndex) { q, idx -> Pair(q, idx) }
                .collectLatest { (q, idx) ->
                    updateState { it.copy(queue = q.toImmutableList(), currentIndex = idx) }
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
                            syncCurrentProgressToDatabaseAndCloud()
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    stablePlaybackResetJob?.cancel()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            updateState { it.copy(isLoading = true) }
                        }
                        Player.STATE_READY -> {
                            updateState { it.copy(isLoading = false) }
                            playRetryCount = 0
                            val activeId = currentTrackId ?: player.currentMediaItem?.mediaId
                            if (activeId != null) {
                                stablePlaybackResetJob?.cancel()
                                stablePlaybackResetJob = scope.launch {
                                    delay(StreamRecoveryPolicy.STABLE_PLAYBACK_RESET_MS)
                                    if (player.playbackState == Player.STATE_READY && (currentTrackId == activeId || player.currentMediaItem?.mediaId == activeId)) {
                                        playbackRecoveryCoordinator.recordSuccessfulPlayback(activeId)
                                        android.util.Log.d("DeepEyeRecovery", "event=budget_reset mediaKeyHash=${activeId.hashCode()} reason=stable_playback_reached")
                                    }
                                }
                            }
                            // Force DSP re-attach since the AudioTrack has been created
                            audioSessionManager.forceReattach()
                        }
                        Player.STATE_ENDED -> {
                            stablePlaybackResetJob?.cancel()
                            onTrackEnded()
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    stablePlaybackResetJob?.cancel()
                    android.util.Log.e("PlayerController", "ExoPlayer Error: code=${error.errorCode} (${error.errorCodeName}), message=${error.message}", error)
                    updateState { it.copy(isLoading = false) }

                    val currentItem = playerState.value.currentItem
                    val snapshot = captureRecoverySnapshot()

                    if (snapshot != null && currentItem is MediaItem.Remote) {
                        val recoveryError = playbackRecoveryErrorMapper.map(error)
                        val attemptCount = if (playbackRecoveryCoordinator.canRecover(snapshot.mediaId)) 0 else 1
                        if (streamRecoveryPolicy.shouldRecover(recoveryError, attemptCount)) {
                            scope.launch {
                                val result = playbackRecoveryCoordinator.recover(
                                    error = recoveryError,
                                    snapshot = snapshot,
                                    player = player,
                                    onApplyState = { refreshedItem, restoredPos, wasPlaying, fallbackReason ->
                                        updateState { cur ->
                                            val vFormats = smartTubePlaybackFormatRepository.snapshot.value.videoFormats.map { it.toDeepEyeFormat() }
                                            val aFormats = smartTubePlaybackFormatRepository.snapshot.value.audioFormats.map { it.toDeepEyeFormat() }
                                            cur.copy(
                                                currentItem = refreshedItem,
                                                isPlaying = wasPlaying,
                                                isLoading = false,
                                                position = restoredPos,
                                                availableVideoFormats = if (vFormats.isNotEmpty()) vFormats.toImmutableList() else cur.availableVideoFormats,
                                                availableAudioFormats = if (aFormats.isNotEmpty()) aFormats.toImmutableList() else cur.availableAudioFormats,
                                                selectedVideoFormat = vFormats.firstOrNull { it.id == smartTubePlaybackFormatRepository.snapshot.value.currentVideoFormatId } ?: cur.selectedVideoFormat,
                                                selectedAudioFormat = aFormats.firstOrNull { it.id == smartTubePlaybackFormatRepository.snapshot.value.currentAudioFormatId } ?: cur.selectedAudioFormat
                                            )
                                        }
                                    }
                                )
                                if (!result.recovered) {
                                    android.util.Log.w("PlayerController", "Recovery failed: ${result.safeReason}. Advancing to next track...")
                                    kotlinx.coroutines.delay(1000)
                                    next()
                                }
                            }
                            return
                        }
                    }

                    scope.launch {
                        if (currentItem is MediaItem.Remote && playRetryCount < 2) {
                            playRetryCount++
                            android.util.Log.w("PlayerController", "Retrying playMedia for remote track: ${currentItem.title} (Attempt $playRetryCount/2)")
                            kotlinx.coroutines.delay(500L * playRetryCount)
                            playMedia(currentItem, isRetry = true, seekPosition = player.currentPosition.coerceAtLeast(0))
                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Unable to play this media right now.",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                            kotlinx.coroutines.delay(500)
                            playRetryCount = 0
                            next()
                        }
                    }
                }

                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    // Extract runtime inventory from Media3 tracks. Auto rows (isAuto=true) are
                    // prepended by QualitySelectionEngine and are never counted as real formats.
                    val vFormats = qualitySelectionEngine.extractVideoFormats(tracks).toImmutableList()
                    val aFormats = qualitySelectionEngine.extractAudioFormats(tracks).toImmutableList()
                    val selV = vFormats.firstOrNull { it.isSelected && !it.isAuto }
                    val selA = aFormats.firstOrNull { it.isSelected && !it.isAuto }

                    val vFormat = player.videoFormat
                    val aFormat = player.audioFormat

                    updateState { cur ->
                        // --- Stale-inventory reset gate (media identity check) ---
                        // SmartTube resolver inventory is authoritative ONLY for the media item it
                        // was resolved for. If Media3 now reports tracks that belong to a different
                        // item (e.g. a sparse muxed fallback replaced the expected stream), reset the
                        // stale catalog to loading/empty BEFORE merging, so item B never shows item A's
                        // format list while B is still resolving. Fail-safe: null keys never reset.
                        val incomingMediaId = player.currentMediaItem?.mediaId
                        val gate = formatInventoryMediaGate
                        val existingKey = gate.materializeMediaKey(cur.currentItem?.id, cur.position)
                        val incomingKey = gate.materializeMediaKey(incomingMediaId, player.currentPosition.coerceAtLeast(0))
                        val inventoryReset = if (gate.shouldReset(existingKey, incomingKey)) {
                            android.util.Log.i(
                                "DeepEyeHQ",
                                "event=format_inventory_reset mediaOp=stale_existing vs incoming reason=media_transition"
                            )
                            true
                        } else {
                            false
                        }
                        val mergeBaseV = if (inventoryReset) persistentListOf() else cur.availableVideoFormats
                        val mergeBaseA = if (inventoryReset) persistentListOf() else cur.availableAudioFormats

                        // --- Merge SmartTube inventory with Media3 runtime inventory ---
                        val result = formatInventoryMergePolicy.merge(
                            existingVideo = mergeBaseV,
                            existingAudio = mergeBaseA,
                            incomingVideo = vFormats,
                            incomingAudio = aFormats
                        )
                        android.util.Log.d(
                            "DeepEyeHQ",
                            "event=format_inventory_merged existingV=${result.existingRealVideo} incomingV=${result.incomingRealVideo} " +
                                "existingA=${result.existingRealAudio} incomingA=${result.incomingRealAudio} preserveV=${result.preserveExistingVideoInventory} " +
                                "preserveA=${result.preserveExistingAudioInventory} reason=${result.decisionReason}"
                        )

                        cur.copy(
                            availableVideoFormats = result.videoFormats,
                            availableAudioFormats = result.audioFormats,
                            selectedVideoFormat = selV ?: cur.selectedVideoFormat,
                            selectedAudioFormat = selA ?: cur.selectedAudioFormat,
                            activeVideoDecoderName = vFormat?.sampleMimeType?.let { mime -> deviceCodecCapabilities.getCodecDisplayName(mime, vFormat.codecs ?: "") } ?: cur.activeVideoDecoderName,
                            activeAudioDecoderName = aFormat?.sampleMimeType?.let { mime -> deviceCodecCapabilities.getCodecDisplayName(mime, aFormat.codecs ?: "") } ?: cur.activeAudioDecoderName,
                        )
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
        
        // --- Infinite Autoplay Prefetch ---
        scope.launch {
            kotlinx.coroutines.flow.combine(queueManager.queue, queueManager.currentIndex) { q, idx -> Pair(q, idx) }
                .collectLatest { (q, idx) ->
                    if (q.isNotEmpty() && idx >= q.size - 2 && playerState.value.autoplayEnabled) {
                        val current = q.getOrNull(idx)
                        if (current != null && !_autoplayState.value.isGenerating) {
                            _autoplayState.update { it.copy(isGenerating = true) }
                            android.util.Log.d("AutoplayEngine", "Infinite Prefetch Triggered. Current Queue Size: ${q.size}, Index: $idx. Seed: ${current.title}")
                            try {
                                val activeArtists = q.map { it.artist }
                                val candidates = withContext(Dispatchers.IO) {
                                    autoplayRepository.generateNextQueue(current, _autoplayState.value, activeArtists)
                                }
                                if (candidates.isNotEmpty()) {
                                    val mediaItems = candidates.map { c ->
                                        MediaItem.Remote(
                                            id = c.videoId,
                                            title = c.title,
                                            artist = c.artist,
                                            artworkUri = android.net.Uri.parse("https://img.youtube.com/vi/${c.videoId}/hqdefault.jpg"),
                                            isVideo = (current as? MediaItem.Remote)?.isVideo ?: false,
                                            duration = 0L,
                                        )
                                    }
                                    queueManager.addItems(mediaItems)
                                    val newIds = candidates.map { it.videoId }
                                    recentAutoplayTrackIds.addAll(newIds)
                                    
                                    // Ensure it doesn't grow indefinitely to prevent OOM
                                    if (recentAutoplayTrackIds.size > 1000) {
                                        recentAutoplayTrackIds.subList(0, recentAutoplayTrackIds.size - 500).clear()
                                    }

                                    _autoplayState.update { state ->
                                        state.copy(
                                            history = (state.history + (current.id)).takeLast(50),
                                            sessionHistory = state.sessionHistory + newIds,
                                            lastGeneratedAt = System.currentTimeMillis()
                                        )
                                    }
                                } else {
                                    android.util.Log.w("AutoplayEngine", "Prefetch returned empty candidates! Queue might halt if not handled.")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AutoplayEngine", "Failed to generate next tracks", e)
                            } finally {
                                _autoplayState.update { it.copy(isGenerating = false) }
                            }
                        }
                    }
                }
        }
    }

    val nowPlaying = playerState.map { it.currentItem }

    fun playMedia(item: MediaItem, isRetry: Boolean = false, seekPosition: Long = 0L) {
        android.util.Log.d("PlayerController", "playMedia called with item: $item, isRetry: $isRetry, seekPosition: $seekPosition")

        stablePlaybackResetJob?.cancel()
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
                                    
                                    val resolvedSource = sourceResolverManager.resolveSource(item.id, item.isVideo, forceRefresh = isRetry)
                                    ensureActive()
                                    android.util.Log.d("PlayerController", "First extraction fetched: ${resolvedSource?.url}")

                                    // Reciprocal stream fallbacks:
                                    //  - Audio-only item failed with video request (preferVideo=true returns
                                    //    DASH/HLS even for audio-only items) -> retry with preferVideo=false.
                                    //  - Video item failed with preferVideo=true -> retry with preferVideo=false
                                    //    so ExoPlayer at least gets a playable audio stream; the shared WebView
                                    //    player renders the video track in the hybrid architecture.
                                    var finalResolvedSource = resolvedSource
                                    if (finalResolvedSource == null && !item.isVideo) {
                                        android.util.Log.d("PlayerController", "Audio stream extraction failed. Retrying with video DASH/HLS stream fallback...")
                                        finalResolvedSource = sourceResolverManager.resolveSource(item.id, true, forceRefresh = isRetry)
                                        ensureActive()
                                        android.util.Log.d("PlayerController", "Fallback video extraction fetched: ${finalResolvedSource?.url}")
                                    } else if (finalResolvedSource == null && item.isVideo) {
                                        android.util.Log.w("PlayerController", "Video stream extraction failed for ${item.title}. Retrying with audio-only stream fallback...")
                                        finalResolvedSource = sourceResolverManager.resolveSource(item.id, false, forceRefresh = isRetry)
                                        ensureActive()
                                        android.util.Log.d("PlayerController", "Fallback audio-only extraction fetched: ${finalResolvedSource?.url}")
                                    }

                                    val finalUrl = finalResolvedSource?.url
                                    if (finalResolvedSource != null) {
                                        android.util.Log.d("DeepEyeHQ", "event=controller_received_formats videoCount=${finalResolvedSource.videoFormats.size} audioCount=${finalResolvedSource.audioFormats.size} queueSize=${queueManager.queue.value.size}")
                                        if (finalResolvedSource.videoFormats.isNotEmpty() || finalResolvedSource.audioFormats.isNotEmpty()) {
                                            smartTubePlaybackFormatRepository.setFormats(
                                                mediaKey = item.id,
                                                videoFormats = finalResolvedSource.videoFormats,
                                                audioFormats = finalResolvedSource.audioFormats
                                            )
                                            val vFmts = finalResolvedSource.videoFormats.mapIndexed { idx, f -> f.toDeepEyeFormat(groupIndex = 0, trackIndex = idx) }
                                            val aFmts = finalResolvedSource.audioFormats.mapIndexed { idx, f -> f.toDeepEyeFormat(groupIndex = 1, trackIndex = idx) }
                                            val autoVideo = com.deepeye.musicpro.player.format.DeepEyeFormat(
                                                id = "video_auto",
                                                groupIndex = -1,
                                                trackIndex = -1,
                                                type = com.deepeye.musicpro.player.format.FormatType.VIDEO,
                                                mimeType = "video/adaptive",
                                                codecName = "Adaptive Auto",
                                                qualityLabel = "Auto",
                                                isAuto = true,
                                                isSelected = true
                                            )
                                            val autoAudio = com.deepeye.musicpro.player.format.DeepEyeFormat(
                                                id = "audio_auto",
                                                groupIndex = -1,
                                                trackIndex = -1,
                                                type = com.deepeye.musicpro.player.format.FormatType.AUDIO,
                                                mimeType = "audio/adaptive",
                                                codecName = "Auto (Highest Quality)",
                                                qualityLabel = "Auto",
                                                isAuto = true,
                                                isSelected = true
                                            )
                                            val fullVFmts = if (vFmts.isNotEmpty()) listOf(autoVideo) + vFmts else emptyList()
                                            val fullAFmts = if (aFmts.isNotEmpty()) listOf(autoAudio) + aFmts else emptyList()
                                            updateState {
                                                it.copy(
                                                    availableVideoFormats = fullVFmts.toImmutableList(),
                                                    availableAudioFormats = fullAFmts.toImmutableList()
                                                )
                                            }
                                        }
                                    }

                                    if (finalUrl != null) {
                                        // Keep the original isVideo flag so the UI renders the correct surface
                                        // (shared WebView for video items) while ExoPlayer plays the stream's audio.
                                        item.copy(
                                            streamUri = Uri.parse(finalUrl),
                                            isVideo = item.isVideo,
                                        )
                                    } else {
                                        if (item.isVideo) {
                                            // IMPORTANT: Never hand ExoPlayer a watch-page HTML URL — ExoPlayer
                                            // cannot parse HTML and throws UnrecognizedInputFormatException
                                            // (Source error), then the Smart Recovery retries 3x and skips.
                                            android.util.Log.e("PlayerController", "All stream extraction attempts failed for video ${item.title} (${item.id}).")
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Failed to extract stream for ${item.title}",
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                            throw Exception("Failed to extract stream for ${item.id}")
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

                    // Fetch lyrics asynchronously
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val lyrics = lyricsRepository.getLyricsForTrack(finalItem.title, finalItem.artist)
                            updateState { it.copy(currentLyrics = lyrics) }
                        } catch (e: Exception) {
                            android.util.Log.e("LyricsEngine", "Failed to fetch lyrics", e)
                            updateState { it.copy(currentLyrics = null) }
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

                        // Media3 automatically promotes MediaSessionService to foreground when playback starts,
                        // so we don't need to manually start it (which causes crashes on Android 12+ from background).

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

    /**
     * Append a single item to the end of the current playback queue.
     * Used by "Add to queue" actions (search results, library rows).
     * Playback continues unaffected; the queue snapshot is updated automatically.
     */
    fun addToQueue(item: MediaItem) {
        queueManager.addItems(listOf(item))
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
            android.util.Log.w(
                "AutoplayEngine",
                "Queue empty on next(). Triggering personalized autoplay emergency fallback. Last played: ${current?.title}"
            )
            scope.launch {
                try {
                    val activeArtists = queueManager.queue.value.map { it.artist }
                    val candidates =
                        withContext(Dispatchers.IO) {
                            autoplayRepository.generateNextQueue(current, _autoplayState.value, activeArtists)
                        }

                    if (candidates.isEmpty()) {
                        android.util.Log.w("AutoplayEngine", "No autoplay candidates found. Stopping playback.")
                        return@launch
                    }

                    val validCandidates = candidates.filter {
                        !recentAutoplayTrackIds.contains(it.videoId) && it.videoId != (current?.id ?: "")
                    }

                    if (validCandidates.isNotEmpty()) {
                        android.util.Log.d("AutoplayEngine", "Emergency fallback generated ${validCandidates.size} valid candidates. Appending to queue.")
                        val mediaItems = validCandidates.map { c ->
                            MediaItem.Remote(
                                id = c.videoId,
                                title = c.title,
                                artist = c.artist,
                                artworkUri = android.net.Uri.parse("https://img.youtube.com/vi/${c.videoId}/hqdefault.jpg"),
                                isVideo = (current as? MediaItem.Remote)?.isVideo ?: false,
                                duration = 0L,
                            )
                        }
                        
                        queueManager.addItems(mediaItems)
                        val newIds = validCandidates.map { it.videoId }
                        recentAutoplayTrackIds.addAll(newIds)
                        
                        if (recentAutoplayTrackIds.size > 1000) {
                            recentAutoplayTrackIds.subList(0, recentAutoplayTrackIds.size - 500).clear()
                        }
                        
                        val firstCandidate = validCandidates.first()
                        
                        _autoplayState.update { state ->
                            state.copy(
                                history = (state.history + (current?.id ?: "")).takeLast(50),
                                sessionHistory = state.sessionHistory + newIds,
                                skipStreak = if (isTrackSkipped) state.skipStreak + 1 else 0,
                                lastGeneratedAt = System.currentTimeMillis(),
                                discoveryMode = firstCandidate.score < 0.4f,
                                familiarMode = firstCandidate.score >= 0.4f,
                            )
                        }
                        
                        val nextTrack = queueManager.next()
                        if (nextTrack != null) {
                            playMedia(nextTrack)
                        }
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
                    val bufferedDuration = player.totalBufferedDuration.coerceAtLeast(0)
                    val bufferedPct = player.bufferedPercentage
                    updateState {
                        it.copy(
                            position = currentPos,
                            duration = player.duration.coerceAtLeast(0),
                            bufferedDurationMs = bufferedDuration,
                            bufferedPercentage = bufferedPct,
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
    fun setQualityPreset(preset: com.deepeye.musicpro.player.format.QualityPreset) {
        updateState { it.copy(qualityPreset = preset) }
        when (preset) {
            com.deepeye.musicpro.player.format.QualityPreset.HIGH_QUALITY -> smartTubePlaybackFormatRepository.setVideoPreset(com.deepeye.musicpro.player.smarttube.VideoQualityPreset.HIGH_QUALITY)
            com.deepeye.musicpro.player.format.QualityPreset.ULTRA_HD -> smartTubePlaybackFormatRepository.setVideoPreset(com.deepeye.musicpro.player.smarttube.VideoQualityPreset.BEST_COMPATIBLE)
            com.deepeye.musicpro.player.format.QualityPreset.DATA_SAVER -> smartTubePlaybackFormatRepository.setVideoPreset(com.deepeye.musicpro.player.smarttube.VideoQualityPreset.DATA_SAVER)
            com.deepeye.musicpro.player.format.QualityPreset.BALANCED -> smartTubePlaybackFormatRepository.setVideoPreset(com.deepeye.musicpro.player.smarttube.VideoQualityPreset.BALANCED)
            com.deepeye.musicpro.player.format.QualityPreset.AUTO -> smartTubePlaybackFormatRepository.setVideoPreset(com.deepeye.musicpro.player.smarttube.VideoQualityPreset.AUTO)
            com.deepeye.musicpro.player.format.QualityPreset.CUSTOM -> smartTubePlaybackFormatRepository.setVideoPreset(com.deepeye.musicpro.player.smarttube.VideoQualityPreset.CUSTOM)
        }
        qualitySelectionEngine.applyPreset(player, preset)
    }

    fun setVideoFormat(format: com.deepeye.musicpro.player.format.DeepEyeFormat) {
        val currentItem = _playerState.value.currentItem
        android.util.Log.d("DeepEyeHQ", "event=user_select_video_format formatId=${format.id} isAuto=${format.isAuto} label=${format.qualityLabel}")
        updateState {
            it.copy(
                selectedVideoFormat = if (format.isAuto) null else format,
                qualityPreset = if (format.isAuto) com.deepeye.musicpro.player.format.QualityPreset.AUTO else com.deepeye.musicpro.player.format.QualityPreset.CUSTOM
            )
        }
        smartTubePlaybackFormatRepository.selectVideoFormat(format.id)
        
        val tracks = player.currentTracks
        val hasMatchingVideoTrack = tracks.groups.any { group ->
            group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO &&
            (0 until group.length).any { tIdx ->
                val tf = group.getTrackFormat(tIdx)
                tf.height == format.height && (format.bitrate == 0 || Math.abs(tf.bitrate - format.bitrate) < 500000)
            }
        }
        
        if (hasMatchingVideoTrack || format.isAuto) {
            qualitySelectionEngine.selectVideoFormat(player, format)
        } else {
            val snapshot = smartTubePlaybackFormatRepository.snapshot.value
            val targetFormat = snapshot.videoFormats.firstOrNull { it.stableId == format.id }
            val directUrl = targetFormat?.streamUrl
            if (directUrl != null && currentItem != null) {
                scope.launch {
                    try {
                        val pos = player.currentPosition
                        val isPlaying = player.isPlaying
                        val updatedItem = when (currentItem) {
                            is MediaItem.Remote -> currentItem.copy(streamUri = Uri.parse(directUrl), isVideo = true)
                            is MediaItem.Local -> currentItem
                        }
                        player.setMediaItem(updatedItem.toMedia3Item())
                        player.seekTo(pos)
                        player.prepare()
                        if (isPlaying) player.play()
                        android.util.Log.i("DeepEyeHQ", "event=video_format_switched_direct formatId=${format.id}")
                    } catch (e: Exception) {
                        android.util.Log.e("DeepEyeHQ", "event=video_format_switch_failed error=${e.message}")
                    }
                }
            }
        }
    }

    fun setAudioFormat(format: com.deepeye.musicpro.player.format.DeepEyeFormat) {
        val currentItem = _playerState.value.currentItem
        android.util.Log.d("DeepEyeHQ", "event=user_select_audio_format formatId=${format.id} isAuto=${format.isAuto} label=${format.qualityLabel}")
        updateState {
            it.copy(
                selectedAudioFormat = if (format.isAuto) null else format
            )
        }
        smartTubePlaybackFormatRepository.selectAudioFormat(format.id)
        
        val tracks = player.currentTracks
        val hasMatchingAudioTrack = tracks.groups.any { group ->
            group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO &&
            (0 until group.length).any { tIdx ->
                val tf = group.getTrackFormat(tIdx)
                tf.bitrate == format.bitrate || (format.bitrate > 0 && Math.abs(tf.bitrate - format.bitrate) < 50000)
            }
        }
        
        if (hasMatchingAudioTrack || format.isAuto) {
            qualitySelectionEngine.selectAudioFormat(player, format)
        } else {
            val snapshot = smartTubePlaybackFormatRepository.snapshot.value
            val targetFormat = snapshot.audioFormats.firstOrNull { it.stableId == format.id }
            val directUrl = targetFormat?.streamUrl
            if (directUrl != null && currentItem != null) {
                scope.launch {
                    try {
                        val pos = player.currentPosition
                        val isPlaying = player.isPlaying
                        val updatedItem = when (currentItem) {
                            is MediaItem.Remote -> currentItem.copy(streamUri = Uri.parse(directUrl))
                            is MediaItem.Local -> currentItem
                        }
                        player.setMediaItem(updatedItem.toMedia3Item())
                        player.seekTo(pos)
                        player.prepare()
                        if (isPlaying) player.play()
                        android.util.Log.i("DeepEyeHQ", "event=audio_format_switched_direct formatId=${format.id}")
                    } catch (e: Exception) {
                        android.util.Log.e("DeepEyeHQ", "event=audio_format_switch_failed error=${e.message}")
                    }
                }
            }
        }
    }

    fun setBufferProfile(profile: com.deepeye.musicpro.player.format.BufferProfile) {
        updateState { it.copy(bufferProfile = profile) }
    }

    fun getPlaybackDiagnostics(): com.deepeye.musicpro.player.format.PlaybackDiagnostics {
        val state = playerState.value
        val currentItem = state.currentItem
        val vFormat = player.videoFormat
        val aFormat = player.audioFormat
        val isHw = state.selectedVideoFormat?.isHardwareAccelerated ?: true

        return com.deepeye.musicpro.player.format.PlaybackDiagnostics(
            videoId = (currentItem as? MediaItem.Remote)?.id ?: currentItem?.id ?: "",
            mediaTitle = currentItem?.title ?: "No Media Active",
            activeVideoFormat = state.selectedVideoFormat,
            activeAudioFormat = state.selectedAudioFormat,
            activeVideoDecoder = state.activeVideoDecoderName.ifEmpty { vFormat?.sampleMimeType ?: "Default Hardware Sink" },
            activeAudioDecoder = state.activeAudioDecoderName.ifEmpty { aFormat?.sampleMimeType ?: "Opus Low-Latency Sink" },
            currentPositionMs = state.position,
            totalDurationMs = state.duration,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
            bufferedDurationMs = state.bufferedDurationMs,
            bufferedPercentage = state.bufferedPercentage,
            playbackSpeed = state.playbackSpeed,
            estimatedBandwidthBps = state.estimatedBandwidthBps,
            droppedFrames = state.droppedFrames,
            qualityPreset = state.qualityPreset,
            bufferProfile = state.bufferProfile,
            isHardwareAccelerated = isHw,
            audioSinkSpec = "48000Hz • 2ch Float32 • Low-Latency DSP",
            playerStateName = if (state.isPlaying) "PLAYING" else if (state.isLoading) "BUFFERING" else "PAUSED"
        )
    }




    private var isAppInForeground = true

    fun setAppInForeground(foreground: Boolean) {
        isAppInForeground = foreground
        updateState { it.copy(isAppInForeground = foreground) }
        if (!foreground) {
            syncCurrentProgressToDatabaseAndCloud()
        }
    }

    private fun syncCurrentProgressToDatabaseAndCloud() {
        val currentItem = playerState.value.currentItem ?: return
        val currentId = currentTrackId ?: return
        val duration = player.duration.coerceAtLeast(0)
        val currentPos = player.currentPosition.coerceAtLeast(0)

        if (currentPos > 5000) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val isVideo = (currentItem as? MediaItem.Remote)?.isVideo == true
                val source = if (currentItem is MediaItem.Local) "local" else "youtube"

                if (isVideo) {
                    historyRepository.recordVideoProgress(
                        videoId = currentId,
                        title = currentItem.title,
                        thumbnailUri = currentItem.artworkUri?.toString(),
                        positionMs = currentPos,
                        durationMs = duration
                    )
                } else {
                    historyRepository.recordPlayback(
                        mediaId = currentId,
                        title = currentItem.title,
                        artist = currentItem.artist,
                        album = (currentItem as? MediaItem.Local)?.song?.album ?: "",
                        artworkUri = currentItem.artworkUri?.toString(),
                        playDurationMs = currentPos,
                        totalDurationMs = duration,
                        source = source
                    )
                }
                cloudSyncManager.syncHistory()
            }
        }
    }

    private fun updateState(transform: (PlayerState) -> PlayerState) {
        _playerState.update(transform)
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

                // Trigger cloud sync of watch history so it syncs immediately
                cloudSyncManager.syncHistory()

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

    fun captureRecoverySnapshot(): PlaybackRecoverySnapshot? {
        val currentItem = _playerState.value.currentItem ?: return null
        val currentPos = player.currentPosition.coerceAtLeast(0)
        val dur = player.duration.coerceAtLeast(0)
        val state = _playerState.value
        val formatRepoSnap = smartTubePlaybackFormatRepository.snapshot.value
        val dspParams = dspEngine.currentParams.value

        return PlaybackRecoverySnapshot(
            mediaId = currentItem.id,
            title = currentItem.title,
            isVideo = currentItem is MediaItem.Remote && currentItem.isVideo,
            queueIndex = queueManager.currentIndex.value,
            queueSize = queueManager.queue.value.size,
            positionMs = currentPos,
            durationMs = dur,
            wasPlaying = player.isPlaying || state.isPlaying,
            selectedVideoFormatId = formatRepoSnap.currentVideoFormatId ?: state.selectedVideoFormat?.id,
            selectedAudioFormatId = formatRepoSnap.currentAudioFormatId ?: state.selectedAudioFormat?.id,
            qualityMode = state.qualityPreset.name,
            playbackSpeed = player.playbackParameters.speed,
            repeatMode = player.repeatMode,
            shuffleEnabled = player.shuffleModeEnabled,
            dspEnabled = dspParams.enabled,
            dspPresetId = dspParams.tubeMode.name,
            captionsEnabled = false,
            selectedAudioLanguage = player.trackSelectionParameters.preferredAudioLanguages.firstOrNull()
        )
    }

    /**
     * Debug-only test hook for simulating HTTP 403 stream expiry.
     */
    fun simulateStreamExpiryForTesting(): Boolean {
        val snapshot = captureRecoverySnapshot() ?: return false
        val simulatedError = PlaybackRecoveryError.HttpStatus(
            statusCode = 403,
            safeReason = "DEBUG_SIMULATED_STREAM_EXPIRY"
        )
        scope.launch {
            playbackRecoveryCoordinator.recover(
                error = simulatedError,
                snapshot = snapshot,
                player = player,
                onApplyState = { refreshedItem, restoredPos, wasPlaying, fallbackReason ->
                    updateState { cur ->
                        val vFormats = smartTubePlaybackFormatRepository.snapshot.value.videoFormats.map { it.toDeepEyeFormat() }
                        val aFormats = smartTubePlaybackFormatRepository.snapshot.value.audioFormats.map { it.toDeepEyeFormat() }
                        cur.copy(
                            currentItem = refreshedItem,
                            isPlaying = wasPlaying,
                            isLoading = false,
                            position = restoredPos,
                            availableVideoFormats = if (vFormats.isNotEmpty()) vFormats.toImmutableList() else cur.availableVideoFormats,
                            availableAudioFormats = if (aFormats.isNotEmpty()) aFormats.toImmutableList() else cur.availableAudioFormats,
                            selectedVideoFormat = vFormats.firstOrNull { it.id == smartTubePlaybackFormatRepository.snapshot.value.currentVideoFormatId } ?: cur.selectedVideoFormat,
                            selectedAudioFormat = aFormats.firstOrNull { it.id == smartTubePlaybackFormatRepository.snapshot.value.currentAudioFormatId } ?: cur.selectedAudioFormat
                        )
                    }
                }
            )
        }
        return true
    }
}
