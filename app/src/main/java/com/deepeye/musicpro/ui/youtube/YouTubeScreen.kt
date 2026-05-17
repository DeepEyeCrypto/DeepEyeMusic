package com.deepeye.musicpro.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deepeye.musicpro.ui.components.ShimmerBox
import com.deepeye.musicpro.domain.model.home.HomeVideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeScreen(
    onNavigateToVideo: (String) -> Unit,
    viewModel: YouTubeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("YouTube", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        val playerState by viewModel.playerState.collectAsStateWithLifecycle()
        
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Show Hybrid Player Card if a remote YouTube track is playing
            playerState.currentItem?.let { currentItem ->
                if (currentItem is com.deepeye.musicpro.domain.model.MediaItem.Remote) {
                    com.deepeye.musicpro.ui.components.HybridPlayerCard(
                        item = currentItem,
                        player = viewModel.player,
                        isVideo = playerState.isVideo,
                        isLoading = playerState.isLoading
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                            items(5) {
                                ShimmerBox(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)))
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                    uiState.error != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(uiState.trendingVideos, key = { it.id }) { video ->
                                VideoItem(video, onClick = { 
                                    viewModel.playVideo(video)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItem(
    video: HomeVideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16/9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.channelName} · ${video.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
