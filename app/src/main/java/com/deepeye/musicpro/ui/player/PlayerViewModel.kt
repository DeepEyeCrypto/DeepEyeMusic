package com.deepeye.musicpro.ui.player

import androidx.lifecycle.ViewModel
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.visualizer.VisualizerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val visualizerEngine: VisualizerEngine
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.playerState
    val fftData: StateFlow<FloatArray> = visualizerEngine.fftData

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleRepeat() = playerController.toggleRepeat()
    fun toggleShuffle() = playerController.toggleShuffle()
}
