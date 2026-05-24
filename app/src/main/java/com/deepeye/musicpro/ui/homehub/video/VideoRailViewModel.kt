// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.repository.video.VideoRailRepository
import com.deepeye.musicpro.domain.model.home.VideoRailCategory
import com.deepeye.musicpro.domain.model.home.VideoRailState
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoRailViewModel @Inject constructor(
    private val repository: VideoRailRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _state = MutableStateFlow(VideoRailState())
    val state = _state.asStateFlow()

    // Which card is currently expanded for inline playback
    private val _expandedCardId = MutableStateFlow<String?>(null)
    val expandedCardId = _expandedCardId.asStateFlow()

    init {
        loadRail()
    }

    fun loadRail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val sections = repository.loadAllSections()
                _state.update { it.copy(sections = sections, isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't load videos"
                    )
                }
            }
        }
    }

    // Tap on card thumbnail -> expand inline player
    fun onCardTap(videoId: String) {
        if (_expandedCardId.value == videoId) {
            // Already expanded -> open full NowPlaying
            playSelectedVideo(videoId)
        } else {
            // First tap -> expand inline preview
            _expandedCardId.value = videoId
        }
    }

    // Tap expanded card again -> open full player
    fun onExpandedCardTap(videoId: String) {
        _expandedCardId.value = null
        playSelectedVideo(videoId)
    }

    private fun playSelectedVideo(videoId: String) {
        val section = state.value.sections.find { it.items.any { item -> item.videoId == videoId } }
        val item = section?.items?.find { it.videoId == videoId } ?: return
        
        val mediaItem = com.deepeye.musicpro.domain.model.MediaItem.Remote(
            id = item.videoId,
            title = item.title,
            artist = item.channelName,
            artworkUri = android.net.Uri.parse(item.thumbnailUrl),
            duration = 180000L, // Mock
            isVideo = true
        )
        playerController.playMedia(mediaItem)
    }

    // Scroll away -> collapse
    fun onCardScrolledAway() {
        _expandedCardId.value = null
    }

    fun switchSection(category: VideoRailCategory) {
        _state.update { it.copy(activeSection = category) }
    }
}
