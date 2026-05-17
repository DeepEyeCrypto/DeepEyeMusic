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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    val gainBudget = v4aEngine.gainBudget

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
                android.util.Log.e("PlayerController", "ExoPlayer Error: ${error.message}", error)
                updateState { it.copy(isLoading = false) }
            }
        })
    }

    val nowPlaying = playerState.map { it.currentItem }

    fun playMedia(item: MediaItem) {
        scope.launch {
            updateState { it.copy(isLoading = true) }
            try {
                val finalItem = when (item) {
                    is MediaItem.Local -> item
                    is MediaItem.Remote -> {
                        if (item.streamUri == null) {
                            val result = youtubeDataSource.getStreamUrl(item.id, preferVideo = item.isVideo)
                            if (result != null) {
                                item.copy(
                                    streamUri = Uri.parse(result.url),
                                    isVideo = item.isVideo
                                )
                            } else {
                                item
                            }
                        } else {
                            item
                        }
                    }
                }

                val media3Item = finalItem.toMedia3Item()
                if (media3Item.localConfiguration?.uri == Uri.EMPTY) {
                    android.util.Log.e("PlayerController", "Cannot play: Stream URI is empty")
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    // SmartTube approach: ExoPlayer is the SINGLE player for both audio & video
                    player.volume = 1f

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
                                isPlaying = true
                            ) 
                        }
                    } else {
                        updateState { 
                            it.copy(
                                currentItem = finalItem, 
                                currentSong = (finalItem as? MediaItem.Local)?.song,
                                isVideo = false,
                                isLoading = false
                            ) 
                        }
                    }
                }
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
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun next() {
        val nextTrack = queueManager.next()
        if (nextTrack != null) {
            playMedia(nextTrack)
        } else {
            // If queue is empty, try Autoplay for Remote tracks
            val current = playerState.value.currentItem
            if (current is MediaItem.Remote) {
                scope.launch {
                    val related = youtubeDataSource.getRelatedMusic(current.title, current.artist, isVideo = current.isVideo)
                    // Exclude current song from related if it appears
                    val nextTrack = related.firstOrNull { it.id != current.id } ?: related.firstOrNull()
                    nextTrack?.let { playMedia(it) }
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

    private fun onTrackEnded() {
        next()
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
