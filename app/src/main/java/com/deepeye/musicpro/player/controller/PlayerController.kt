package com.deepeye.musicpro.player.controller

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.ShuffleMode
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.player.queue.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level player controller that wraps ExoPlayer.
 *
 * Exposes a reactive [PlayerState] flow and provides play/pause/seek/queue operations.
 */
@Singleton
class PlayerController @Inject constructor(
    private val player: ExoPlayer,
    private val queueManager: QueueManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var positionUpdateJob: Job? = null

    init {
        // Listen to ExoPlayer state changes
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else stopPositionUpdates()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded()
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateState {
                    it.copy(duration = player.duration.coerceAtLeast(0))
                }
            }
        })

        // Observe queue changes
        scope.launch {
            combine(
                queueManager.queue,
                queueManager.currentIndex,
                queueManager.repeatMode,
                queueManager.shuffleMode
            ) { queue, index, repeat, shuffle ->
                _playerState.value.copy(
                    queue = queue,
                    currentIndex = index,
                    currentSong = queue.getOrNull(index),
                    repeatMode = repeat,
                    shuffleMode = shuffle
                )
            }.collect { newState ->
                _playerState.value = newState
            }
        }
    }

    // ── Playback Controls ──

    fun playSong(song: Song) {
        val mediaItem = song.toMediaItem()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        updateState { it.copy(currentSong = song) }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        queueManager.setQueue(songs, startIndex)
        queueManager.currentSong?.let { playSong(it) }
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        updateState { it.copy(position = positionMs) }
    }

    fun next() {
        queueManager.next()?.let { playSong(it) }
    }

    fun previous() {
        // If more than 3 seconds into the song, restart it instead
        if (player.currentPosition > 3000) {
            seekTo(0)
        } else {
            queueManager.previous()?.let { playSong(it) }
        }
    }

    fun toggleRepeat() = queueManager.toggleRepeatMode()

    fun toggleShuffle() = queueManager.toggleShuffleMode()

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        updateState { it.copy(playbackSpeed = speed) }
    }

    fun stop() {
        player.stop()
        stopPositionUpdates()
    }

    // ── Internal ──

    private fun onTrackEnded() {
        val nextSong = queueManager.next()
        if (nextSong != null) {
            playSong(nextSong)
        } else {
            updateState { it.copy(isPlaying = false, position = 0) }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                updateState {
                    it.copy(
                        position = player.currentPosition.coerceAtLeast(0),
                        duration = player.duration.coerceAtLeast(0)
                    )
                }
                delay(250) // Update 4x per second
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private inline fun updateState(transform: (PlayerState) -> PlayerState) {
        _playerState.value = transform(_playerState.value)
    }

    private fun Song.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artUri)
                    .build()
            )
            .build()
    }
}
