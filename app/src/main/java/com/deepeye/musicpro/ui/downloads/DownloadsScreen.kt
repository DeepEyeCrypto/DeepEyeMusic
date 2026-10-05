// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import com.deepeye.musicpro.player.download.DownloadProgressState
import com.deepeye.musicpro.player.download.MusicDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val downloadManager: MusicDownloadManager,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    val activeDownloadStates: StateFlow<Map<Long, DownloadProgressState>> =
        downloadManager.activeDownloadStates
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val completedDownloads: StateFlow<List<LibraryItem>> =
        libraryRepository.observeLibraryHome()
            .map { it.downloads }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelDownload(downloadId: Long) {
        downloadManager.cancelDownload(downloadId)
    }

    fun deleteDownload(context: android.content.Context, videoId: String) {
        viewModelScope.launch {
            libraryRepository.deleteDownload(context, videoId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activeDownloadStates by viewModel.activeDownloadStates.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Downloads",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        if (activeDownloadStates.isEmpty() && completedDownloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = "No Downloads",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (activeDownloadStates.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active Downloads (${activeDownloadStates.size})",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(activeDownloadStates.entries.toList(), key = { it.key }) { entry ->
                        val downloadState = entry.value
                        val item = downloadState.item
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF131722).copy(alpha = 0.85f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF00E5FF),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                        )
                                        val pct = (downloadState.progress * 100f).roundToInt().coerceIn(0, 100)
                                        val subtitle = if (downloadState.totalBytes > 0) {
                                            val dlMb = downloadState.bytesDownloaded / (1024f * 1024f)
                                            val totMb = downloadState.totalBytes / (1024f * 1024f)
                                            String.format("%d%% • %.1f MB / %.1f MB", pct, dlMb, totMb)
                                        } else {
                                            "Downloading..."
                                        }
                                        Text(
                                            text = subtitle,
                                            color = Color(0xFF00E5FF),
                                            fontSize = 13.sp,
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.cancelDownload(entry.key) },
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close, 
                                            contentDescription = "Cancel", 
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                if (downloadState.progress > 0f) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadState.progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF00E5FF),
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (completedDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed (${completedDownloads.size})",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = if (activeDownloadStates.isNotEmpty()) 16.dp else 0.dp,
                                bottom = 8.dp
                            )
                        )
                    }
                    items(completedDownloads, key = { it.id }) { item ->
                        Card(
                            onClick = {
                                val localUri = item.localPath?.let { android.net.Uri.parse(it) }
                                playerViewModel.playMedia(
                                    com.deepeye.musicpro.domain.model.MediaItem.Remote(
                                        id = item.videoId ?: item.id,
                                        title = item.title,
                                        artist = item.artist ?: item.subtitle,
                                        artworkUri = item.artworkUrl?.let { android.net.Uri.parse(it) },
                                        duration = 0L,
                                        streamUri = localUri
                                    )
                                )
                                onNavigateToNowPlaying()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF131722).copy(alpha = 0.85f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 64.dp)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteDownload(context, item.videoId ?: item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Download",
                                        tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
