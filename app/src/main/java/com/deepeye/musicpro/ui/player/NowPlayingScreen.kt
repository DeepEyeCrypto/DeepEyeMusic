package com.deepeye.musicpro.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.PlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToV4A: () -> Unit,
    onNavigateToQueue: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Background Gradient based on current item
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onNavigateToV4A) {
                    Icon(Icons.Default.Tune, contentDescription = "V4A Settings")
                }
            }

            Spacer(Modifier.weight(0.5f))

            val isVideoMode = playerState.currentItem is com.deepeye.musicpro.domain.model.MediaItem.Remote && playerState.isVideo
            val currentItem = playerState.currentItem
            
            // Parent Container Box supporting Ambilight Backlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentItem != null) {
                    // PREMIUM AMBILIGHT DYNAMIC GLOW (Breathing ambient aura matching video colors)
                    AsyncImage(
                        model = currentItem.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(if (isVideoMode) 16 / 9f else 1f)
                            .blur(36.dp)
                            .alpha(0.65f)
                            .graphicsLayer {
                                translationY = 15f
                            },
                        contentScale = ContentScale.Crop
                    )
                }

                // MAIN ARTWORK / VIDEO CARD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isVideoMode) 16 / 9f else 1f)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isVideoMode && currentItem != null) {
                        com.deepeye.musicpro.ui.components.YouTubeVideoPlayer(
                            videoId = currentItem.id,
                            isPlaying = playerState.isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = currentItem?.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                if (!isVideoMode) {
                    // Visualizer Overlay only for audio music playback
                    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
                    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
                    
                    NowPlayingVisualizerOverlay(
                        fftData = fftData,
                        dominantColor = dominantColor,
                        isPlaying = playerState.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }

            Spacer(Modifier.weight(0.5f))

            // Track Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.currentItem?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = playerState.currentItem?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                IconButton(
                    onClick = { viewModel.downloadCurrentTrack() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Gain Budget Meter
            val gainBudget by viewModel.gainBudget.collectAsStateWithLifecycle()
            com.deepeye.musicpro.ui.components.GainBudgetMeter(
                budget = gainBudget,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(32.dp))

            // Progress Slider
            Column {
                Slider(
                    value = playerState.position.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        TimeFormatter.formatDuration(playerState.position),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        TimeFormatter.formatDuration(playerState.duration),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                }
                IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }
                Surface(
                    onClick = { viewModel.togglePlayPause() },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat")
                }
            }

            Spacer(Modifier.weight(0.5f))
        }
    }
}
