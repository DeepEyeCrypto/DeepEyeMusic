// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.recovery

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.toMedia3Item
import com.deepeye.musicpro.player.smarttube.SmartTubePlaybackFormatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DeepEyeRecovery"

@Singleton
class PlaybackRecoveryCoordinator @Inject constructor(
    private val refreshUseCase: SmartTubeSourceRefreshUseCase,
    private val policy: StreamRecoveryPolicy,
    private val smartTubePlaybackFormatRepository: SmartTubePlaybackFormatRepository,
    private val dspProfileManager: com.deepeye.musicpro.dsp.profile.DspProfileManager,
    private val dspEngine: com.deepeye.musicpro.dsp.engine.DSPEngine,
    @ApplicationContext private val context: Context,
) {
    private val recoveryMutex = Mutex()
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    private val _recoveryMessage = MutableStateFlow<String?>(null)
    val recoveryMessage: StateFlow<String?> = _recoveryMessage.asStateFlow()

    private val recoveryAttemptCounts = mutableMapOf<String, Int>()

    fun canRecover(mediaId: String): Boolean {
        val attempts = recoveryAttemptCounts[mediaId] ?: 0
        return attempts < StreamRecoveryPolicy.MAX_RECOVERY_ATTEMPTS
    }

    fun recordSuccessfulPlayback(mediaId: String) {
        recoveryAttemptCounts.remove(mediaId)
    }

    suspend fun recover(
        error: PlaybackRecoveryError,
        snapshot: PlaybackRecoverySnapshot,
        player: ExoPlayer,
        onApplyState: suspend (MediaItem, Long, Boolean, String?) -> Unit
    ): StreamRecoveryResult {
        if (!recoveryMutex.tryLock()) {
            Log.w(TAG, "event=recovery_skipped reason=already_recovering mediaKeyHash=${snapshot.mediaId.hashCode()}")
            return StreamRecoveryResult(false, false, null, snapshot.wasPlaying, "Already recovering")
        }

        return try {
            val mediaId = snapshot.mediaId
            val currentAttempts = recoveryAttemptCounts[mediaId] ?: 0
            val safeHash = mediaId.hashCode().toString()
            val category = when (error) {
                is PlaybackRecoveryError.HttpStatus -> "http_status"
                is PlaybackRecoveryError.SourceLoad -> "source_load"
                is PlaybackRecoveryError.Decoder -> "decoder"
                is PlaybackRecoveryError.Unknown -> "unknown"
            }
            val status = (error as? PlaybackRecoveryError.HttpStatus)?.statusCode ?: 0
            Log.d(TAG, "event=error_detected mediaKeyHash=$safeHash category=$category status=$status decision=refresh_source attempt=${currentAttempts + 1}")

            if (currentAttempts >= StreamRecoveryPolicy.MAX_RECOVERY_ATTEMPTS) {
                Log.w(TAG, "event=recovery_skipped reason=retry_limit mediaKeyHash=$safeHash attempts=$currentAttempts")
                return StreamRecoveryResult(false, false, null, snapshot.wasPlaying, "Retry limit reached")
            }

            _isRecovering.value = true
            _recoveryMessage.value = "Refreshing stream…"
            recoveryAttemptCounts[mediaId] = currentAttempts + 1

            val dspParams = dspEngine.currentParams.value
            val dspEnabled = dspParams.enabled

            val refreshResult = refreshUseCase.refreshCurrentSource(
                mediaId = snapshot.mediaId,
                isVideo = snapshot.isVideo,
                selectedVideoFormatId = snapshot.selectedVideoFormatId,
                selectedAudioFormatId = snapshot.selectedAudioFormatId,
                qualityMode = snapshot.qualityMode,
                selectedAudioLanguage = snapshot.selectedAudioLanguage
            )

            val refreshedSource = refreshResult.getOrNull()
            if (refreshedSource == null || refreshedSource.directUrl.isEmpty()) {
                val failReason = refreshResult.exceptionOrNull()?.message ?: "Empty stream URL"
                Log.e(TAG, "event=recovery_failed mediaKeyHash=$safeHash reason=$failReason")
                _isRecovering.value = false
                _recoveryMessage.value = "Playback could not be restored. Try another quality."
                return StreamRecoveryResult(false, false, null, snapshot.wasPlaying, "Failed to resolve fresh stream")
            }

            Log.i(TAG, "event=source_refreshed mediaKeyHash=$safeHash isVideo=${refreshedSource.isVideo} vFormats=${refreshedSource.videoFormats.size} aFormats=${refreshedSource.audioFormats.size}")
            applyRefreshedPlayback(snapshot, refreshedSource, player, dspEnabled, onApplyState)
        } catch (e: Exception) {
            Log.e(TAG, "event=recovery_exception reason=${e.message}")
            _recoveryMessage.value = "Playback could not be restored."
            StreamRecoveryResult(false, false, null, snapshot.wasPlaying, e.message ?: "Unknown recovery exception")
        } finally {
            _isRecovering.value = false
            recoveryMutex.unlock()
        }
    }

    private suspend fun applyRefreshedPlayback(
        snapshot: PlaybackRecoverySnapshot,
        source: RefreshedPlaybackSource,
        player: ExoPlayer,
        dspEnabled: Boolean,
        onApply: suspend (MediaItem, Long, Boolean, String?) -> Unit
    ): StreamRecoveryResult {
        val safeHash = snapshot.mediaId.hashCode().toString()
        smartTubePlaybackFormatRepository.setFormats(
            mediaKey = snapshot.mediaId,
            videoFormats = source.videoFormats,
            audioFormats = source.audioFormats,
            activeVideoId = source.selectedVideoFormatId,
            activeAudioId = source.selectedAudioFormatId
        )
        source.selectedVideoFormatId?.let { smartTubePlaybackFormatRepository.selectVideoFormat(it) }
        source.selectedAudioFormatId?.let { smartTubePlaybackFormatRepository.selectAudioFormat(it) }

        val clampedPos = if (snapshot.durationMs > 0L) {
            snapshot.positionMs.coerceIn(0L, (snapshot.durationMs - 1000L).coerceAtLeast(0L))
        } else snapshot.positionMs.coerceAtLeast(0L)

        val item = MediaItem.Remote(
            id = snapshot.mediaId,
            title = snapshot.title,
            artist = "",
            streamUri = Uri.parse(source.directUrl),
            artworkUri = Uri.parse("https://img.youtube.com/vi/${snapshot.mediaId}/hqdefault.jpg"),
            isVideo = snapshot.isVideo,
            duration = snapshot.durationMs
        )

        player.pause()
        delay(50)
        player.setMediaItem(item.toMedia3Item())
        if (clampedPos > 0L) player.seekTo(clampedPos)
        player.prepare()
        if (snapshot.wasPlaying) player.play()

        try {
            dspProfileManager.loadAndApplyProfile(snapshot.mediaId)
            Log.d(TAG, "event=dsp_restore enabled=$dspEnabled preset=${snapshot.dspPresetId ?: "Default"} result=preserved")
        } catch (e: Exception) {
            Log.w(TAG, "event=dsp_restore enabled=$dspEnabled result=failed reason=${e.message}")
        }

        onApply(item, clampedPos, snapshot.wasPlaying, source.fallbackReason)
        _recoveryMessage.value = if (source.fallbackReason != null) "Stream refreshed at compatible quality" else "Stream refreshed"
        Log.i(TAG, "event=recovery_completed mediaKeyHash=$safeHash positionMs=$clampedPos playing=${snapshot.wasPlaying} fallback=${source.fallbackReason != null}")
        return StreamRecoveryResult(true, source.fallbackReason != null, clampedPos, snapshot.wasPlaying, "Recovery succeeded")
    }
}
