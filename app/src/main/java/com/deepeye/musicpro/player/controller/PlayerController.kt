package com.deepeye.musicpro.player.controller

import android.net.Uri
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
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for the media player.
 * Orchestrates ExoPlayer, QueueManager, and DSP Engine.
 */
@Singleton
class PlayerController @Inject constructor(
    private val player: ExoPlayer,
    private val queueManager: QueueManager,
    private val youtubeDataSource: YoutubeRemoteDataSource,
    private val audioSessionManager: com.deepeye.musicpro.dsp.session.AudioSessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var positionUpdateJob: Job? = null

    init {
        // Wire DSP engine to the player session
        audioSessionManager.attachToPlayer(player)

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
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlayerController", "ExoPlayer Error: ${error.message}", error)
            }
        })
    }

    val nowPlaying = playerState.map { it.currentItem }

    fun playMedia(item: MediaItem) {
        scope.launch {
            try {
                val finalItem = when (item) {
                    is MediaItem.Local -> item
                    is MediaItem.Remote -> {
                        if (item.streamUri == null) {
                            val streamUrl = youtubeDataSource.getAudioStreamUrl(item.id)
                            item.copy(streamUri = streamUrl?.let { Uri.parse(it) })
                        } else {
                            item
                        }
                    }
                }

                val media3Item = finalItem.toMedia3Item()
                withContext(Dispatchers.Main) {
                    player.setMediaItem(media3Item)
                    player.prepare()
                    player.play()
                    updateState { it.copy(currentItem = finalItem, currentSong = (finalItem as? MediaItem.Local)?.song) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerController", "Exception in playMedia: ${e.message}", e)
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun next() {
        // Queue logic implementation
    }

    fun previous() {
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        }
    }

    fun toggleRepeat() {
        queueManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        queueManager.toggleShuffleMode()
    }

    private fun onTrackEnded() {
        // Auto-next implementation
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
                delay(250)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
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
}
