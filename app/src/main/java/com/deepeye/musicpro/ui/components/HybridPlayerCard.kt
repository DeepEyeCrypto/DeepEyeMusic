// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.deepeye.musicpro.domain.model.MediaItem
import androidx.compose.material.icons.filled.PictureInPicture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.deepeye.musicpro.ui.LocalFullscreenMode
import com.deepeye.musicpro.ui.LocalPipMode

@Composable
fun VideoPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            keepScreenOn = true
        }
    }

    DisposableEffect(player) {
        playerView.player = player
        onDispose {
            playerView.player = null
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = modifier
    )
}

@Composable
fun HybridPlayerCard(
    item: MediaItem,
    player: ExoPlayer,
    isVideo: Boolean,
    isLoading: Boolean,
    isPlaying: Boolean,
    playbackPosition: Long = 0L,
    modifier: Modifier = Modifier,
    onTogglePlayPause: () -> Unit
) {
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    var interactionCount by remember { mutableStateOf(0) }

    fun resetTimer() {
        interactionCount++
    }

    LaunchedEffect(controlsVisible, isPlaying, playbackSpeed, isMuted, interactionCount) {
        android.util.Log.e("HybridPlayerCard", "LaunchedEffect: controlsVisible=$controlsVisible, isPlaying=$isPlaying, interactionCount=$interactionCount")
        if (controlsVisible && isPlaying) {
            delay(3000)
            android.util.Log.e("HybridPlayerCard", "Auto-hiding controls after 3 seconds")
            controlsVisible = false
        }
    }



    LaunchedEffect(playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed)
    }

    // ExoPlayer audio track is disabled in video mode via setTrackTypeDisabled (PlayerController).
    // The mute button toggles WebView audio via isMuted param. For audio mode, ExoPlayer handles volume natively.
    LaunchedEffect(isMuted) {
        if (!isVideo) {
            player.volume = if (isMuted) 0f else 1f
        }
    }
    
    // Tap-seeking animated indicator visual triggers
    var showLeftRipple by remember { mutableStateOf(false) }
    var showRightRipple by remember { mutableStateOf(false) }
    
    // Fullscreen mode from CompositionLocal — toggles activity orientation
    val fullscreenMode = LocalFullscreenMode.current
    val isFullscreen = fullscreenMode.isFullscreen
    
    val scope = rememberCoroutineScope()

    // PiP mode awareness
    val isInPipMode = LocalPipMode.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val webView = com.deepeye.musicpro.ui.LocalSharedWebView.current ?: remember {
        createYouTubeWebView(context)
    }

    // ── Fullscreen Dialog Overlay Removed ──

    val cardModifier = if (isFullscreen && !isInPipMode) {
        Modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxSize()
    }

    Box(
        modifier = cardModifier,
        contentAlignment = Alignment.Center
    ) {
        // PREMIUM AMBILIGHT DYNAMIC GLOW (Breathing ambient aura matching video colors)
        if (!isFullscreen || isInPipMode) {
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
        }

        // MAIN HIGH-FIDELITY MEDIA CONTAINER CARD
        val containerModifier = if (isFullscreen && !isInPipMode) {
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        } else {
            Modifier
                .fillMaxSize()
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }

        Box(
            modifier = containerModifier,
            contentAlignment = Alignment.Center
        ) {
            if (isVideo) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val videoPlaybackPosition = sliderPosition?.toLong() ?: playbackPosition
                    YouTubeVideoPlayer(
                        webView = webView,
                        videoId = item.id,
                        isPlaying = isPlaying,
                        playbackPosition = videoPlaybackPosition,
                        playbackSpeed = playbackSpeed,
                        isMuted = isMuted,
                        muteWebViewAudio = false,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Close / Exit Fullscreen Button Overlay (top right)
                    if (isFullscreen && !isInPipMode) {
                        AnimatedVisibility(
                            visible = controlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                IconButton(
                                    onClick = { fullscreenMode.exit() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // GESTURE SKIP ZONES + OVERLAYS: Hidden in PiP for clean view
                    if (!isInPipMode) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Column Zone: Skip Backward / Single Tap Play-Pause
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                android.util.Log.e("HybridPlayerCard", "Double tap left zone! Seeking backward.")
                                                resetTimer()
                                                val newPos = (player.currentPosition - 10000L).coerceAtLeast(0L)
                                                player.seekTo(newPos)
                                                showLeftRipple = true
                                                scope.launch {
                                                    delay(650)
                                                    showLeftRipple = false
                                                }
                                            },
                                            onTap = {
                                                android.util.Log.e("HybridPlayerCard", "Single tap left zone! Toggling controls. Current visibility: $controlsVisible")
                                                resetTimer()
                                                controlsVisible = !controlsVisible
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (showLeftRipple) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.75f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FastRewind,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("-10s", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Right Column Zone: Skip Forward / Single Tap Play-Pause
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                android.util.Log.e("HybridPlayerCard", "Double tap right zone! Seeking forward.")
                                                resetTimer()
                                                val duration = player.duration
                                                val newPos = if (duration > 0) {
                                                    (player.currentPosition + 10000L).coerceAtMost(duration)
                                                } else {
                                                    player.currentPosition + 10000L
                                                }
                                                player.seekTo(newPos)
                                                showRightRipple = true
                                                scope.launch {
                                                    delay(650)
                                                    showRightRipple = false
                                                }
                                            },
                                            onTap = {
                                                android.util.Log.e("HybridPlayerCard", "Single tap right zone! Toggling controls. Current visibility: $controlsVisible")
                                                resetTimer()
                                                controlsVisible = !controlsVisible
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (showRightRipple) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.75f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("+10s", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.FastForward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // CENTER PLAY OVERLAY when player is paused
                        AnimatedVisibility(
                            visible = controlsVisible && !isPlaying,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { onTogglePlayPause() }
                                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // BOTTOM GLASSMORPHIC QUICK MEDIA CONTROL PANEL OVERLAY
                        AnimatedVisibility(
                            visible = controlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null,
                                            onClick = { resetTimer() }
                                        )
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                            ),
                                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    // Seekbar (Slider) Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val displayPosition = sliderPosition?.toLong() ?: playbackPosition
                                        Text(
                                            text = com.deepeye.musicpro.core.utils.TimeFormatter.formatDuration(displayPosition),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        
                                        Spacer(Modifier.width(8.dp))
                                        
                                        val duration = player.duration.coerceAtLeast(0L)
                                        Slider(
                                            value = sliderPosition ?: playbackPosition.toFloat(),
                                            onValueChange = {
                                                sliderPosition = it
                                                player.seekTo(it.toLong())
                                                resetTimer()
                                            },
                                            onValueChangeFinished = {
                                                sliderPosition = null
                                            },
                                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFFFB300),
                                                activeTrackColor = Color(0xFFFFB300),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        Spacer(Modifier.width(8.dp))
                                        
                                        Text(
                                            text = com.deepeye.musicpro.core.utils.TimeFormatter.formatDuration(duration),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))
                                    
                                    // Row of Control buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Interactive Play/Pause Button
                                        IconButton(
                                            onClick = {
                                                resetTimer()
                                                onTogglePlayPause()
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 2. Interactive Mute/Unmute Indicator Button
                                        IconButton(
                                            onClick = {
                                                resetTimer()
                                                isMuted = !isMuted
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Mute",
                                                tint = if (isMuted) Color.White.copy(alpha = 0.6f) else Color(0xFFFFB300),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 3. VLC-style Playback Speed Selector
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (playbackSpeed != 1.0f) Color(0xFF00BFA5).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier
                                                .clickable {
                                                    resetTimer()
                                                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                                                    val currentIdx = speeds.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 2
                                                    playbackSpeed = speeds[(currentIdx + 1) % speeds.size]
                                                }
                                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Speed,
                                                    contentDescription = null,
                                                    tint = if (playbackSpeed != 1.0f) Color(0xFF00BFA5) else Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "${playbackSpeed}x",
                                                    color = if (playbackSpeed != 1.0f) Color(0xFF00BFA5) else Color.White,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // 4. PiP Button
                                        if (!isInPipMode) {
                                            IconButton(
                                                onClick = {
                                                    resetTimer()
                                                    (context as? com.deepeye.musicpro.MainActivity)?.enterPipMode()
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureInPicture,
                                                    contentDescription = "Picture in Picture",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // 5. Fullscreen Overlay Button
                                        IconButton(
                                            onClick = {
                                                resetTimer()
                                                if (isFullscreen) fullscreenMode.exit() else fullscreenMode.enter()
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        } // end if (!isInPipMode)
                    }
            } else {
                // Premium Fallback Audio card
                Box(modifier = Modifier.fillMaxSize()) {
                    // Blurred background artwork
                    AsyncImage(
                        model = item.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Contrast gradient overlay
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

                    // Audio Layout Content
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        val isPlayingAudio = player.isPlaying
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onTogglePlayPause() }
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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

// VLC-style Media Information Overlay
@Composable
fun MediaInfoOverlay(player: ExoPlayer) {
    var videoFormat by remember { mutableStateOf(player.videoFormat) }
    var audioFormat by remember { mutableStateOf(player.audioFormat) }

    LaunchedEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                videoFormat = player.videoFormat
                audioFormat = player.audioFormat
            }
        }
        player.addListener(listener)
        // Cleanup not strictly necessary since effect scope ties to lifecycle, 
        // but good practice if player outlives compose
    }

    val vCodec = videoFormat?.sampleMimeType?.substringAfter("/") ?: "unknown"
    val aCodec = audioFormat?.sampleMimeType?.substringAfter("/") ?: "unknown"
    val res = if (videoFormat != null) "${videoFormat!!.width}x${videoFormat!!.height}" else "Audio Only"
    val aBitrate = if (audioFormat != null && audioFormat!!.bitrate > 0) "${audioFormat!!.bitrate / 1000}kbps" else ""

    Box(
        modifier = Modifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = "Media Info", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Video: $vCodec • $res", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("Audio: $aCodec • $aBitrate", color = Color.White.copy(alpha=0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
