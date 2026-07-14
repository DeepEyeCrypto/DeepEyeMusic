// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.deepeye.musicpro.data.repository.HomeFeedRepository
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.HomeFeedState
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.dsp.engine.DSPEngine
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.visualizer.VisualizerEngine
import com.deepeye.musicpro.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeHubViewModel
@Inject
constructor(
    private val feedRepo: HomeFeedRepository,
    private val dspEngine: DSPEngine,
    private val visualizerEngine: VisualizerEngine,
    private val playerController: PlayerController,
    private val musicRepository: MusicRepository,
    private val gamificationEngine: com.deepeye.musicpro.domain.gamification.GamificationEngine,
    private val gamificationRepository: com.deepeye.musicpro.domain.repository.GamificationRepository,
    private val aiRadioEngine: com.deepeye.musicpro.domain.ai.AIRadioEngine,
    private val cloudSyncManager: com.deepeye.musicpro.domain.sync.CloudSyncManager,
    val rankingRepository: com.deepeye.musicpro.domain.ranking.RankingRepository,
    val rankingEngine: com.deepeye.musicpro.domain.ranking.RankingEngine,
) : ViewModel() {
    private val _feedState = MutableStateFlow(HomeFeedState(isLoading = true))
    val feedState: StateFlow<HomeFeedState> = _feedState.asStateFlow()

    val gamificationState = gamificationRepository.getGamificationState().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = com.deepeye.musicpro.domain.gamification.GamificationState()
    )

    val achievementEvents = gamificationEngine.achievementEvents

    val isDspAttached = dspEngine.engineState

    val top3Users = rankingRepository.getTopUsers(3).stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadFeed()
        viewModelScope.launch { 
            gamificationEngine.restoreFromFirestore()
            gamificationEngine.checkAndUpdateStreak() 
            cloudSyncManager.syncAllData()
            gamificationEngine.forceSyncToFirestore()
        }
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
        val state = _feedState.value
        val items = state.trending.takeIf { it.any { v -> v.id == video.id } }
            ?: state.shorts.takeIf { it.any { v -> v.id == video.id } }
            ?: state.continueWatching.takeIf { it.any { v -> v.id == video.id } }
            ?: listOf(video)
            
        val index = items.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        
        val queue = items.map {
            MediaItem.Remote(
                id = it.id,
                title = it.title,
                artist = it.channelName,
                artworkUri = Uri.parse(it.thumbnailUrl),
                duration = it.duration * 1000L,
                isVideo = true,
            )
        }
        playerController.setQueue(queue, index)
    }

    fun playMusic(music: HomeMusicItem) {
        val state = _feedState.value
        val items = state.quickPicks.takeIf { it.any { m -> m.id == music.id } }
            ?: state.continueListening.takeIf { it.any { m -> m.id == music.id } }
            ?: state.localResume.takeIf { it.any { m -> m.id == music.id } }
            ?: listOf(music)
            
        val index = items.indexOfFirst { it.id == music.id }.coerceAtLeast(0)
        
        val queue = items.map {
            MediaItem.Remote(
                id = it.id,
                title = it.title,
                artist = it.artist,
                artworkUri = Uri.parse(it.thumbnailUrl),
                duration = it.duration * 1000L,
                isVideo = false,
            )
        }
        playerController.setQueue(queue, index)
    }

    fun openV4A(navController: NavController) {
        navController.navigate("dsp")
    }

    val isAIGenerating = aiRadioEngine.isGenerating

    fun onMoodClick(mood: com.deepeye.musicpro.domain.model.home.MoodMix) {
        viewModelScope.launch {
            try {
                aiRadioEngine.playMoodMix(mood)
            } catch (e: Exception) {
                Log.e("HomeHubVM", "Mood search failed: ${e.message}")
            }
        }
    }

    fun onAIPromptSubmitted(prompt: String) {
        viewModelScope.launch {
            try {
                aiRadioEngine.generateRadioFromPrompt(prompt)
            } catch (e: Exception) {
                Log.e("HomeHubVM", "AI radio generation failed: ${e.message}")
            }
        }
    }

    fun playContinueWatchingItem(video: com.deepeye.musicpro.domain.model.home.HomeVideoItem) {
        playVideo(video)
    }

    fun playContinueListeningItem(music: com.deepeye.musicpro.domain.model.home.HomeMusicItem) {
        playMusic(music)
    }

    fun playLocalResume(music: com.deepeye.musicpro.domain.model.home.HomeMusicItem) {
        val songId = music.id.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                musicRepository.getSongById(songId).first()?.let { song ->
                    playerController.playMedia(MediaItem.Local(song))
                }
            } catch (e: Exception) {
                Log.e("HomeHubVM", "Failed to resolve local resume song $songId: ${e.message}")
            }
        }
    }
}
