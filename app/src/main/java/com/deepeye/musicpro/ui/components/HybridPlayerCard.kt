package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.deepeye.musicpro.domain.model.MediaItem

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun HybridPlayerCard(
    item: MediaItem,
    player: ExoPlayer,
    isVideo: Boolean,
    isLoading: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // PREMIUM AMBILIGHT DYNAMIC GLOW (Breathing ambient aura matching video colors)
        AsyncImage(
            model = item.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(16 / 9f)
                .blur(36.dp)
                .alpha(0.65f)
                .graphicsLayer {
                    translationY = 15f
                },
            contentScale = ContentScale.Crop
        )

        // MAIN HIGH-FIDELITY MEDIA CONTAINER CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
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
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
        if (isVideo) {
            // Actual Video Playback view using our custom WebView YouTube player
            YouTubeVideoPlayer(
                videoId = item.id,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Premium Fallback Audio card
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Blurred background of the song artwork
                AsyncImage(
                    model = item.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp),
                    contentScale = ContentScale.Crop
                )
                
                // 2. Dark contrast overlay with a subtle purple-indigo neon gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color(0xFF1E0B36).copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // 3. Audio Player Content Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art with premium rounded borders and a subtle shadow
                    AsyncImage(
                        model = item.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.width(16.dp))

                    // Track title, channel details, and fallback notice
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // Beautiful dynamic indicator chip for Audio Mode
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🎧 High-Fidelity Audio Mode",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Circular Premium Play/Pause controller
                    val isPlaying = player.isPlaying
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { if (isPlaying) player.pause() else player.play() }
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Global spinner overlay when buffering or loading streams
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
}
