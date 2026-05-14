package com.deepeye.musicpro.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.ShuffleMode
import com.deepeye.musicpro.ui.components.AudioVisualizer
import com.deepeye.musicpro.ui.components.MarqueeText

@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToV4A: () -> Unit,
    onNavigateToQueue: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
    val song = playerState.currentSong

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Background: blurred album art ──
        song?.artUri?.let { artUri ->
            AsyncImage(
                model = artUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Dark overlay gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                IconButton(onClick = onNavigateToQueue) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Album Art ──
            AsyncImage(
                model = song?.artUri,
                contentDescription = "Album art",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Visualizer ──
            AudioVisualizer(
                fftData = fftData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                barCount = 48,
                barWidth = 3.dp,
                barSpacing = 2.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Song Info ──
            MarqueeText(
                text = song?.title ?: "No song playing",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Progress Slider ──
            val progress = if (playerState.duration > 0) {
                playerState.position.toFloat() / playerState.duration.toFloat()
            } else 0f

            Slider(
                value = progress,
                onValueChange = { newValue ->
                    viewModel.seekTo((newValue * playerState.duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TimeFormatter.formatDuration(playerState.position),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = TimeFormatter.formatDuration(playerState.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Playback Controls ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleMode == ShuffleMode.ON)
                            MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { viewModel.previous() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.next() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playerState.repeatMode != RepeatMode.OFF)
                            MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── V4A DSP Button ──
            IconButton(onClick = onNavigateToV4A) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = "V4A DSP Engine",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
