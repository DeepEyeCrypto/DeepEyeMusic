// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.deepeye.musicpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.SecondaryLabel
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.ui.LocalFullscreenMode
import com.deepeye.musicpro.ui.LocalPipMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playerView =
        remember {
            val view = android.view.LayoutInflater.from(context).inflate(com.deepeye.musicpro.R.layout.custom_player_view, null, false)
            val pv = view as androidx.media3.ui.PlayerView
            pv.keepScreenOn = true
            pv.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            pv
        }

    DisposableEffect(player) {
        playerView.player = player
        onDispose {
            playerView.player = null
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = modifier,
        update = { it.requestLayout() }
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
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
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
        android.util.Log.e(
            "HybridPlayerCard",
            "LaunchedEffect: controlsVisible=$controlsVisible, isPlaying=$isPlaying, interactionCount=$interactionCount",
        )
        if (controlsVisible && isPlaying) {
            delay(3000)
            android.util.Log.e("HybridPlayerCard", "Auto-hiding controls after 3 seconds")
            controlsVisible = false
        }
    }

    LaunchedEffect(playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed)
    }

    // ExoPlayer handles audio natively for both audio and video modes now.
    // The mute button toggles ExoPlayer volume.
    LaunchedEffect(isMuted) {
        player.volume = if (isMuted) 0f else 1f
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

    // VLC Gesture OSD States
    var volumeOsd by remember { mutableStateOf<Int?>(null) }
    var brightnessOsd by remember { mutableStateOf<Int?>(null) }
    var seekOsd by remember { mutableStateOf<Long?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val activity = remember { context.findActivity() }

    // WebView player removed. ExoPlayer handles all video track rendering natively.

    // ── Fullscreen Dialog Overlay Removed ──

    val cardModifier =
        if (isFullscreen && !isInPipMode) {
            Modifier.fillMaxSize()
        } else {
            modifier
        }

    Box(
        modifier = cardModifier,
        contentAlignment = Alignment.Center,
    ) {
        // PREMIUM AMBILIGHT DYNAMIC GLOW (Breathing ambient aura matching video colors)
        // Hidden during active video playback because the blur Compose layer renders
        // on top of the native WebView, covering the video with a blurred overlay.
        // Only shown for audio-only mode where it creates a premium ambient effect.
        if (!isVideo && (!isFullscreen || isInPipMode)) {
            AsyncImage(
                model = item.artworkUri,
                contentDescription = null,
                modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(16 / 9f)
                    .blur(36.dp)
                    .alpha(0.65f)
                    .graphicsLayer {
                        translationY = 15f
                    },
                contentScale = ContentScale.Crop,
            )
        }

        // MAIN HIGH-FIDELITY MEDIA CONTAINER CARD
        val containerModifier =
            if (isFullscreen && !isInPipMode) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else if (isVideo) {
                // Video mode: minimal container — no background, no clip with rounded corners.
                // The WebView is a native View; Compose clip/background draws over it.
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        brush =
                        Brush.linearGradient(
                            colors =
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }

        Box(
            modifier = containerModifier,
            contentAlignment = Alignment.Center,
        ) {
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isFullscreen) {
                            if (!isFullscreen) return@pointerInput
                            var initialBrightness = 0f
                            var initialVolume = 0
                            var initialSeek = 0L
                            var maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            var dragDirection: Int? = null // 1: Horizontal (Seek), 2: Vertical Left (Brightness), 3: Vertical Right (Volume), 4: Exit
                            
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                val slopThreshold = viewConfiguration.touchSlop
                                val screenWidth = size.width
                                val screenHeight = size.height
                                
                                android.util.Log.e("VLC_GESTURE", "pointerInput started")
                                initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                if (initialBrightness < 0f) initialBrightness = 0.5f // fallback
                                initialVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                initialSeek = player.currentPosition
                                dragDirection = null
                                isDragging = false

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        if (isDragging) {
                                            // Apply Seek if it was a seek drag
                                            seekOsd?.let { 
                                                player.seekTo(it) 
                                                onSeekTo(it)
                                            }
                                            volumeOsd = null
                                            brightnessOsd = null
                                            seekOsd = null
                                            isDragging = false
                                        }
                                        break
                                    }

                                    val dy = change.position.y - down.position.y
                                    val dx = change.position.x - down.position.x
                                    
                                    // Determine direction if not locked yet
                                    if (dragDirection == null) {
                                        if (kotlin.math.abs(dy) > slopThreshold || kotlin.math.abs(dx) > slopThreshold) {
                                            isDragging = true
                                            android.util.Log.e("VLC_GESTURE", "Drag detected. dx=$dx, dy=$dy, slop=$slopThreshold")
                                            if (kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.5f) {
                                                // Vertical drag
                                                if (dy > 50f && down.position.y < screenHeight * 0.2f) {
                                                    dragDirection = 4
                                                    android.util.Log.e("VLC_GESTURE", "Exit Fullscreen")
                                                } else if (down.position.x < screenWidth / 2) {
                                                    dragDirection = 2
                                                    android.util.Log.e("VLC_GESTURE", "Brightness")
                                                } else {
                                                    dragDirection = 3
                                                    android.util.Log.e("VLC_GESTURE", "Volume")
                                                }
                                            } else {
                                                dragDirection = 1
                                                android.util.Log.e("VLC_GESTURE", "Seek")
                                            }
                                        }
                                    }

                                    // Apply Gesture
                                    if (dragDirection != null) {
                                        change.consume() // Consume drag so children don't tap
                                        resetTimer()
                                        when (dragDirection) {
                                            1 -> { // Seek
                                                val seekDeltaMs = ((dx / screenWidth) * 90000f).toLong() // Max 90s swipe per screen width
                                                val targetSeek = (initialSeek + seekDeltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
                                                seekOsd = targetSeek
                                            }
                                            2 -> { // Brightness
                                                val deltaB = -(dy / screenHeight) * 1.5f // Negative because up is minus Y
                                                val newBrightness = (initialBrightness + deltaB).coerceIn(0.01f, 1f)
                                                activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                    screenBrightness = newBrightness
                                                }
                                                brightnessOsd = (newBrightness * 100).toInt()
                                            }
                                            3 -> { // Volume
                                                val deltaV = -(dy / screenHeight) * maxVolume * 1.5f
                                                val newVolume = (initialVolume + deltaV).toInt().coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                                                volumeOsd = ((newVolume.toFloat() / maxVolume) * 100).toInt()
                                            }
                                            4 -> { // Exit
                                                if (dy > 50f) {
                                                    fullscreenMode.exit()
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    VideoPlayerView(
                        player = player,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Close / Exit Fullscreen Button Overlay (top right)
                    if (isFullscreen && !isInPipMode) {
                        AnimatedVisibility(
                            visible = controlsVisible && !isInPipMode,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                IconButton(
                                    onClick = { fullscreenMode.exit() },
                                    modifier =
                                    Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }

                    // GESTURE SKIP ZONES + OVERLAYS: Hidden in PiP for clean view
                    if (!isInPipMode) {
                        // Vertical drag overlay for fullscreen exit — uses Initial pass
                        // VLC-style gesture overlay has been moved to the parent Box above
                        // to ensure it gets hit events BEFORE the sibling Tap Zones row.


                        Row(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Left Column Zone: Skip Backward / Single Tap Play-Pause
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        var lastTapTime = 0L
                                        awaitEachGesture {
                                            val down =
                                                awaitFirstDown(
                                                    requireUnconsumed = false,
                                                    pass = PointerEventPass.Main
                                                )
                                            val now = System.currentTimeMillis()
                                            if (now - lastTapTime < 300L) {
                                                down.consume()
                                                while (true) {
                                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                                    event.changes.forEach { it.consume() }
                                                    if (event.changes.none { it.pressed }) break
                                                }
                                                android.util.Log.e(
                                                    "HybridPlayerCard",
                                                    "Double tap left zone! Seeking backward."
                                                )
                                                resetTimer()
                                                val currentPos = player.currentPosition
                                                val newPos = (currentPos - 10000L).coerceAtLeast(0L)
                                                onSeekTo(newPos)
                                                showLeftRipple = true
                                                scope.launch {
                                                    delay(650)
                                                    showLeftRipple = false
                                                }
                                                lastTapTime = 0L
                                            } else {
                                                lastTapTime = now
                                                var isClick = true
                                                while (true) {
                                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                                    if (event.changes.any { it.isConsumed }) {
                                                        isClick = false
                                                    }
                                                    if (event.changes.none { it.pressed }) break
                                                }
                                                if (isClick && System.currentTimeMillis() - now < 300L) {
                                                    android.util.Log.e(
                                                        "HybridPlayerCard",
                                                        "Single tap left zone! Toggling controls. Current visibility: $controlsVisible",
                                                    )
                                                    controlsVisible = !controlsVisible
                                                    resetTimer()
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showLeftRipple) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FastRewind,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "-10s",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Column Zone: Skip Forward / Single Tap Play-Pause
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        var lastTapTime = 0L
                                        awaitEachGesture {
                                            val down =
                                                awaitFirstDown(
                                                    requireUnconsumed = false,
                                                    pass = PointerEventPass.Main
                                                )
                                            val now = System.currentTimeMillis()
                                            if (now - lastTapTime < 300L) {
                                                down.consume()
                                                while (true) {
                                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                                    event.changes.forEach { it.consume() }
                                                    if (event.changes.none { it.pressed }) break
                                                }
                                                android.util.Log.e(
                                                    "HybridPlayerCard",
                                                    "Double tap right zone! Seeking forward."
                                                )
                                                resetTimer()
                                                val duration = player.duration
                                                val currentPos = player.currentPosition
                                                val newPos =
                                                    if (duration > 0) {
                                                        (currentPos + 10000L).coerceAtMost(duration)
                                                    } else {
                                                        currentPos + 10000L
                                                    }
                                                onSeekTo(newPos)
                                                showRightRipple = true
                                                scope.launch {
                                                    delay(650)
                                                    showRightRipple = false
                                                }
                                                lastTapTime = 0L
                                            } else {
                                                lastTapTime = now
                                                var isClick = true
                                                while (true) {
                                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                                    if (event.changes.any { it.isConsumed }) {
                                                        isClick = false
                                                    }
                                                    if (event.changes.none { it.pressed }) break
                                                }
                                                if (isClick && System.currentTimeMillis() - now < 300L) {
                                                    android.util.Log.e(
                                                        "HybridPlayerCard",
                                                        "Single tap right zone! Toggling controls. Current visibility: $controlsVisible",
                                                    )
                                                    controlsVisible = !controlsVisible
                                                    resetTimer()
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showRightRipple) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "+10s",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.FastForward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // CENTER PLAY OVERLAY when player is paused
                        AnimatedVisibility(
                            visible = controlsVisible && !isPlaying && !isInPipMode,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Box(
                                modifier =
                                Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { onTogglePlayPause() }
                                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        // BOTTOM GLASSMORPHIC QUICK MEDIA CONTROL PANEL OVERLAY
                        AnimatedVisibility(
                            visible = controlsVisible && !isInPipMode,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Column(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null,
                                            onClick = { resetTimer() },
                                        )
                                        .background(
                                            brush =
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.85f)
                                                ),
                                            ),
                                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    // Seekbar (Slider) Row - Only show in fullscreen since NowPlayingScreen has one
                                    if (isFullscreen) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val displayPosition = sliderPosition?.toLong() ?: playbackPosition
                                            Text(
                                                text = com.deepeye.musicpro.core.utils.TimeFormatter.formatDuration(
                                                    displayPosition
                                                ),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            val duration = player.duration.coerceAtLeast(0L)
                                            @OptIn(ExperimentalMaterial3Api::class)
                                            Slider(
                                                value =
                                                (sliderPosition ?: playbackPosition.toFloat()).coerceIn(
                                                    0f,
                                                    duration.toFloat().coerceAtLeast(1f),
                                                ),
                                                onValueChange = {
                                                    sliderPosition = it
                                                    onSeekTo(it.toLong())
                                                    resetTimer()
                                                },
                                                onValueChangeFinished = {
                                                    sliderPosition = null
                                                },
                                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                                modifier = Modifier.weight(1f),
                                                thumb = {
                                                    SliderDefaults.Thumb(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFB300)),
                                                        modifier = Modifier.size(12.dp),
                                                    )
                                                },
                                                track = { sliderState ->
                                                    SliderDefaults.Track(
                                                        colors =
                                                        SliderDefaults.colors(
                                                            activeTrackColor = Color(0xFFFFB300),
                                                            inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                                                        ),
                                                        sliderState = sliderState,
                                                        modifier = Modifier.height(4.dp),
                                                    )
                                                },
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                text = com.deepeye.musicpro.core.utils.TimeFormatter.formatDuration(
                                                    duration
                                                ),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))
                                    }

                                    // Row of Control buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // 1. Interactive Play/Pause Button
                                        IconButton(
                                            onClick = {
                                                resetTimer()
                                                onTogglePlayPause()
                                            },
                                            modifier =
                                            Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }

                                        // 2. Interactive Mute/Unmute Indicator Button
                                        IconButton(
                                            onClick = {
                                                resetTimer()
                                                isMuted = !isMuted
                                            },
                                            modifier =
                                            Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Mute",
                                                tint = if (isMuted) {
                                                    Color.White.copy(
                                                        alpha = 0.6f
                                                    )
                                                } else {
                                                    Color(0xFFFFB300)
                                                },
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }

                                        // 3. VLC-style Playback Speed Selector
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color =
                                            if (playbackSpeed != 1.0f) {
                                                Color(
                                                    0xFF00BFA5,
                                                ).copy(alpha = 0.25f)
                                            } else {
                                                Color.White.copy(alpha = 0.15f)
                                            },
                                            modifier =
                                            Modifier
                                                .clickable {
                                                    resetTimer()
                                                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                                                    val currentIdx = speeds.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 2
                                                    playbackSpeed = speeds[(currentIdx + 1) % speeds.size]
                                                }
                                                .border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.25f),
                                                    RoundedCornerShape(14.dp)
                                                ),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Speed,
                                                    contentDescription = null,
                                                    tint = if (playbackSpeed != 1.0f) {
                                                        Color(
                                                            0xFF00BFA5
                                                        )
                                                    } else {
                                                        Color.White
                                                    },
                                                    modifier = Modifier.size(12.dp),
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "${playbackSpeed}x",
                                                    color = if (playbackSpeed != 1.0f) {
                                                        Color(
                                                            0xFF00BFA5
                                                        )
                                                    } else {
                                                        Color.White
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
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
                                                modifier =
                                                Modifier
                                                    .size(32.dp)
                                                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureInPicture,
                                                    contentDescription = "Picture in Picture",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }

                                        // 5. Fullscreen Overlay Button
                                        IconButton(
                                            onClick = {
                                                resetTimer()
                                                if (isFullscreen) fullscreenMode.exit() else fullscreenMode.enter()
                                            },
                                            modifier =
                                            Modifier
                                                .size(32.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                        ) {
                                            Icon(
                                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // VLC GESTURE OSD OVERLAY
                        AnimatedVisibility(
                            visible = isDragging && (brightnessOsd != null || volumeOsd != null || seekOsd != null),
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (brightnessOsd != null) {
                                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color.White) // Fallback icon
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "${brightnessOsd}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    } else if (volumeOsd != null) {
                                        Icon(imageVector = if (volumeOsd!! == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "${volumeOsd}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    } else if (seekOsd != null) {
                                        val delta = seekOsd!! - player.currentPosition
                                        val sign = if (delta > 0) "+" else ""
                                        Text(text = "$sign${delta / 1000}s", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "[${com.deepeye.musicpro.core.utils.TimeFormatter.formatDuration(seekOsd!!)}]", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyLarge)
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
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .blur(20.dp),
                        contentScale = ContentScale.Crop,
                    )

                    // Contrast gradient overlay
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors =
                                    listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color(0xFF1E0B36).copy(alpha = 0.8f),
                                    ),
                                ),
                            ),
                    )

                    // Audio Layout Content
                    Row(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = item.artworkUri,
                            contentDescription = null,
                            modifier =
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(
                            modifier =
                            Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                        ) {
                            Text(
                                text = item.title,
                                style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = item.artist,
                                style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(Modifier.height(4.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                ),
                            ) {
                                Text(
                                    text = "🎧 High-Fidelity Audio Mode",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        val isPlayingAudio = player.isPlaying
                        Box(
                            modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onTogglePlayPause() }
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }

        // Global spinner overlay when buffering or loading streams
        if (isLoading) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
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
        val listener =
            object : androidx.media3.common.Player.Listener {
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
    val res = videoFormat?.let { "${it.width}x${it.height}" } ?: "Audio Only"
    val aBitrate = audioFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000}kbps" } ?: ""

    Box(
        modifier =
        Modifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Speed,
                contentDescription = "Media Info",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                DynamicLabel(
                    text = "Video: $vCodec • $res", 
                    backgroundColor = Color.Black, 
                    style = MaterialTheme.typography.labelSmall
                )
                SecondaryLabel(
                    text = "Audio: $aCodec • $aBitrate",
                    backgroundColor = Color.Black,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
