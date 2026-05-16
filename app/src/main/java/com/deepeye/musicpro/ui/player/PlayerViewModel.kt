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
    private val colorExtractor: ColorExtractor
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.playerState
    
    private val _dominantColor = MutableStateFlow(Color(0xFF7B3FE4))
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

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
}
