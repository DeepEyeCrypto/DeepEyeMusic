// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.youtube.SmartTubeVideoCard
import coil3.compose.AsyncImage
import com.deepeye.musicpro.data.db.PlaybackHistoryEntity
import com.deepeye.musicpro.data.db.SearchHistoryEntity
import com.deepeye.musicpro.data.db.VideoHistoryEntity
import com.deepeye.musicpro.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val recentPlaybacks: StateFlow<List<PlaybackHistoryEntity>> =
        historyRepository.getRecentPlaybacks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentVideos: StateFlow<List<VideoHistoryEntity>> =
        historyRepository.getRecentVideos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryEntity>> =
        historyRepository.getRecentSearches()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel()
) {
    val playbacks by viewModel.recentPlaybacks.collectAsStateWithLifecycle()
    val videos by viewModel.recentVideos.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Memory Engine", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear All", color = Color(0xFFFF453A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A14))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (videos.isNotEmpty()) {
                item {
                    Text(
                        text = "Continue Watching",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(videos) { video ->
                            VideoHistoryCard(
                                video = video,
                                onClick = {
                                    playerViewModel.playMedia(
                                        com.deepeye.musicpro.domain.model.MediaItem.Remote(
                                            id = video.videoId,
                                            title = video.title,
                                            artist = "",
                                            artworkUri = video.thumbnailUri?.let { android.net.Uri.parse(it) },
                                            duration = video.durationMs,
                                            isVideo = true
                                        )
                                    )
                                    onNavigateToNowPlaying()
                                }
                            )
                        }
                    }
                }
            }

            if (playbacks.isNotEmpty()) {
                item {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }
                items(playbacks) { playback ->
                    PlaybackHistoryRow(
                        playback = playback,
                        onClick = {
                            playerViewModel.playMedia(
                                com.deepeye.musicpro.domain.model.MediaItem.Remote(
                                    id = playback.mediaId,
                                    title = playback.title,
                                    artist = playback.artist,
                                    artworkUri = playback.artworkUri?.let { android.net.Uri.parse(it) },
                                    duration = playback.totalDurationMs,
                                    isVideo = false
                                )
                            )
                            onNavigateToNowPlaying()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoHistoryCard(
    video: VideoHistoryEntity,
    onClick: () -> Unit = {}
) {
    SmartTubeVideoCard(
        video = HomeVideoItem(
            id = video.videoId,
            title = video.title,
            channelName = "",
            thumbnailUrl = video.thumbnailUri ?: "",
            duration = video.durationMs / 1000,
        ),
        onClick = onClick,
        modifier = Modifier.width(280.dp),
        progressFraction = video.completionPercent / 100f,
    )
}

@Composable
fun PlaybackHistoryRow(
    playback: PlaybackHistoryEntity,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playback.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playback.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playback.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.PlayCircle, "Play", tint = Color.White)
        }
    }
}
