// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import com.deepeye.musicpro.player.download.MusicDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val downloadManager: MusicDownloadManager,
    libraryRepository: LibraryRepository,
) : ViewModel() {
    val activeDownloads: StateFlow<Map<Long, MediaItem>> =
        downloadManager.activeDownloads
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val completedDownloads: StateFlow<List<LibraryItem>> =
        libraryRepository.observeLibraryHome()
            .map { it.downloads }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelDownload(downloadId: Long) {
        downloadManager.cancelDownload(downloadId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A14),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        if (activeDownloads.isEmpty() && completedDownloads.isEmpty()) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "No Downloads",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (activeDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active Downloads",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(activeDownloads.entries.toList(), key = { it.key }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF7B3FE4),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.value.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "Downloading...",
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(onClick = { viewModel.cancelDownload(item.key) }) {
                                    Icon(Icons.Default.Close, "Cancel", tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                if (completedDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(
                                top = if (activeDownloads.isNotEmpty()) 16.dp else 0.dp,
                                bottom = 8.dp
                            )
                        )
                    }
                    items(completedDownloads) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    tint = Color(0xFF00D2FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
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
