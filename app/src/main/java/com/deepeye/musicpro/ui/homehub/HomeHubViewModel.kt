// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.repository.HomeFeedRepository
import com.deepeye.musicpro.domain.model.home.HomeFeedState
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.dsp.model.DspParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import androidx.navigation.NavController
import com.deepeye.musicpro.dsp.data.PresetRepository
import com.deepeye.musicpro.player.visualizer.VisualizerEngine
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.domain.model.MediaItem
import android.net.Uri

@HiltViewModel
class HomeHubViewModel @Inject constructor(
    private val feedRepo: HomeFeedRepository,
    private val dspEngine: DSPEngine,
    private val visualizerEngine: VisualizerEngine,
    private val playerController: PlayerController
) : ViewModel() {

    private val _feedState = MutableStateFlow(HomeFeedState(isLoading = true))
    val feedState: StateFlow<HomeFeedState> = _feedState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.value = HomeFeedState(isLoading = true)
            try {
                _feedState.value = feedRepo.getHomeFeed()
            } catch (e: Exception) {
                Log.e("HomeHubVM", "Feed load error: ${e.message}")
                _feedState.value = HomeFeedState(isOffline = true, error = e.message)
            }
        }
    }

    fun playVideo(video: HomeVideoItem) {
        val mediaItem = MediaItem.Remote(
            id = video.id,
            title = video.title,
            artist = video.channelName,
            artworkUri = Uri.parse(video.thumbnailUrl),
            duration = video.duration * 1000L,
            isVideo = true
        )
        playerController.playMedia(mediaItem)
    }

    fun playMusic(music: HomeMusicItem) {
        val mediaItem = MediaItem.Remote(
            id = music.id,
            title = music.title,
            artist = music.artist,
            artworkUri = Uri.parse(music.thumbnailUrl),
            duration = music.duration * 1000L,
            isVideo = false
        )
        playerController.playMedia(mediaItem)
    }

    val dspParams: StateFlow<DspParams> = v4aEngine.currentParams

    val audioRoute = v4aEngine.currentRoute
    val currentPresetName = v4aEngine.currentPresetName

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

    fun openV4A(navController: NavController) {
        navController.navigate("v4a")
    }

    fun toggleDsp(enabled: Boolean) {
        viewModelScope.launch {
            val current = v4aEngine.currentParams.value
            v4aEngine.updateParams(current.copy(enabled = enabled))
        }
    }

    fun toggleBassBoost(enabled: Boolean) {
        viewModelScope.launch {
            val current = v4aEngine.currentParams.value
            v4aEngine.updateParams(current.copy(bassBoostEnabled = enabled))
        }
    }

    fun toggleVirtualizer(enabled: Boolean) {
        viewModelScope.launch {
            val current = v4aEngine.currentParams.value
            v4aEngine.updateParams(current.copy(virtualizerEnabled = enabled))
        }
    }
}
