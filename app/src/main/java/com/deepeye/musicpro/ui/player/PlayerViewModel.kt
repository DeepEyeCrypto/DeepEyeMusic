package com.deepeye.musicpro.ui.player

import android.net.Uri
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
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val visualizerEngine: VisualizerEngine,
    private val colorExtractor: ColorExtractor,
    private val downloadManager: com.deepeye.musicpro.player.download.MusicDownloadManager,
    private val tasteProfileRepository: com.deepeye.musicpro.domain.repository.TasteProfileRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.playerState
    val player = playerController.player

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentSongFeedback: StateFlow<com.deepeye.musicpro.data.db.UserFeedback?> = playerController.playerState
        .map { it.currentItem?.id }
        .flatMapLatest { songId ->
            if (songId != null) {
                tasteProfileRepository.getFeedbackFlow(songId)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val tasteProfile: StateFlow<com.deepeye.musicpro.data.prefs.TasteProfile> = tasteProfileRepository.getTasteProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.deepeye.musicpro.data.prefs.TasteProfile())

    private val _dominantColor = MutableStateFlow(Color(0xFF7B3FE4))
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

    val gainBudget = playerController.gainBudget

    val fftData = visualizerEngine.fftData.map { bytes ->
        if (bytes.isEmpty()) FloatArray(0)
        else {
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
                .collectLatest { mediaItem ->
                    mediaItem?.artworkUri?.let { uri ->
                        val colors = colorExtractor.extractColors(uri)
                        _dominantColor.value = colors?.primary ?: Color(0xFF7B3FE4)
                    }
                }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleRepeat() = playerController.toggleRepeat()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleAutoplay() = playerController.toggleAutoplay()

    fun playMedia(item: com.deepeye.musicpro.domain.model.MediaItem) {
        playerController.playMedia(item)
    }

    fun setQueue(items: List<com.deepeye.musicpro.domain.model.MediaItem>, startIndex: Int = 0) {
        playerController.setQueue(items, startIndex)
    }

    fun downloadCurrentTrack() {
        playerState.value.currentItem?.let {
            downloadManager.downloadTrack(it)
        }
    }

    fun likeTrack(liked: Boolean) {
        val currentId = playerState.value.currentItem?.id ?: return
        viewModelScope.launch {
            tasteProfileRepository.recordFeedback(currentId, liked = liked, dontPlayAgain = false)
        }
    }

    fun blockTrack() {
        val currentId = playerState.value.currentItem?.id ?: return
        viewModelScope.launch {
            // Block is dislike + dontPlayAgain
            tasteProfileRepository.recordFeedback(currentId, liked = false, dontPlayAgain = true)
            // Automatically advance to the next track!
            next()
        }
    }
}
