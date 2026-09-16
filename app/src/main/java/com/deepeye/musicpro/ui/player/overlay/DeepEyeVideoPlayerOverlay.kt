// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.overlay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.player.format.PlaybackDiagnostics
import com.deepeye.musicpro.player.format.QualityPreset
import com.deepeye.musicpro.ui.LocalFullscreenMode
import com.deepeye.musicpro.ui.LocalPipMode
import com.deepeye.musicpro.ui.theme.ElectricViolet
import com.deepeye.musicpro.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.hypot

/**
 * DeepEye Video Player Overlay — Ultra-Stable Kodi, VLC & SmartTube-inspired TV/Landscape OSD.
 *
 * Layer Architecture:
 * Layer 0: Video Surface (managed by caller with graphicsLayer)
 * Layer 1: Gesture Input Surface (Handles pinch zoom, pan, VLC swipe gestures, double-tap seek, single tap toggle)
 * Layer 2: Gesture HUD & Ripple Badges (Floating non-blocking visual feedback)
 * Layer 3: Controls Overlay (Top info bar, Center play/pause, Bottom multimedia bar) — fully isolated from gesture capture
 */
@Composable
fun DeepEyeVideoPlayerOverlay(
    playerState: PlayerState,
    actions: VideoPlayerOverlayActions,
    modifier: Modifier = Modifier,
    videoScale: Float = 1f,
    onScaleChange: (Float) -> Unit = {},
    videoOffsetX: Float = 0f,
    videoOffsetY: Float = 0f,
    onOffsetChange: (Float, Float) -> Unit = { _, _ -> },
    diagnostics: PlaybackDiagnostics? = null,
    showStats: Boolean = false,
    onToggleStats: () -> Unit = {},
    onSeekTo: (Long) -> Unit = { actions.seekChanged(it) },
    onSetSpeed: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val viewConfig = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    val isFullscreen = LocalFullscreenMode.current.isFullscreen
    val isInPipMode = LocalPipMode.current

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // OSD HUD states
    var brightnessOsd by remember { mutableStateOf<Int?>(null) }
    var volumeOsd by remember { mutableStateOf<Int?>(null) }
    var seekOsd by remember { mutableStateOf<Long?>(null) }
    var zoomOsd by remember { mutableStateOf<String?>(null) }
    var showLeftRipple by remember { mutableStateOf(false) }
    var showRightRipple by remember { mutableStateOf(false) }
    var showLockOsd by remember { mutableStateOf(false) }

    // Live clock
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTimeString = formatter.format(Date())
            delay(10000)
        }
    }

    // Auto-hide controls timer
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    fun resetTimer() {
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(controlsVisible, playerState.isPlaying, lastInteractionTime, isLocked) {
        if (controlsVisible && playerState.isPlaying && !isLocked) {
            delay(4000)
            if (System.currentTimeMillis() - lastInteractionTime >= 3900) {
                controlsVisible = false
            }
        }
    }

    val isPlaying = playerState.isPlaying
    val durationMs = playerState.duration
    val currentPosition = playerState.position
    val bufferedPosition = playerState.bufferedDurationMs
    val isLoading = playerState.isLoading
    val selectedFormat = playerState.selectedVideoFormat
    val hasVideo = playerState.isVideo
    val playbackSpeed = playerState.playbackSpeed

    val qualityText = selectedFormat?.formattedVideoResolution
        ?: if (hasVideo) QualityPreset.AUTO.displayName.split("(").first().trim()
        else ""
    val is4K = (selectedFormat?.height ?: 0) >= 2160
    val isHDR = selectedFormat?.isHdr ?: false
    val is60Fps = (selectedFormat?.frameRate ?: 0f) >= 50f
    val hasQuality = playerState.availableVideoFormats.isNotEmpty() || playerState.availableAudioFormats.isNotEmpty()
    val hasCaptions = playerState.isCaptionEnabled
    val canShowStats = hasVideo || diagnostics != null
    val title = playerState.currentSong?.title ?: playerState.currentItem?.title ?: ""
    val artist = playerState.currentSong?.artist ?: playerState.currentItem?.artist ?: ""

    // Helper for Zoom Presets
    fun cycleZoomMode() {
        resetTimer()
        val nextScale = when {
            videoScale < 1.05f -> 1.35f // Crop to Fill
            videoScale < 1.4f -> 1.5f  // 150%
            videoScale < 1.7f -> 2.0f  // 200%
            videoScale < 2.5f -> 3.0f  // 300%
            else -> 1.0f              // Reset to Fit
        }
        onScaleChange(nextScale)
        onOffsetChange(0f, 0f)
        val label = when (nextScale) {
            1.0f -> "FIT (100%)"
            1.35f -> "FILL (CROP)"
            else -> "${(nextScale * 100).toInt()}%"
        }
        zoomOsd = label
        scope.launch {
            delay(1500)
            if (zoomOsd == label) zoomOsd = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        val currentScale by rememberUpdatedState(videoScale)
        val currentOffsetX by rememberUpdatedState(videoOffsetX)
        val currentOffsetY by rememberUpdatedState(videoOffsetY)

        // ══════════════════════════════════════════════════════════════════════
        // LAYER 1: GESTURE DETECTION SURFACE (Background layer behind controls)
        // ══════════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, isInPipMode, isFullscreen) {
                    if (isInPipMode) return@pointerInput

                    var initialBrightness = 0f
                    var initialVolume = 0
                    var initialSeek = 0L
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    var dragDirection: Int? = null // 1: Seek, 2: Brightness, 3: Volume, 4: Exit Fullscreen, 5: Pinch Zoom/Pan
                    var initialPinchDistance = 0f
                    var initialScale = 1f
                    var initialCentroid = Offset.Zero
                    var initialOffsetX = 0f
                    var initialOffsetY = 0f
                    var lastTapTime = 0L
                    var lastTapPos = Offset.Zero
                    var isDragging = false

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        val downTime = System.currentTimeMillis()
                        val slopThreshold = viewConfig.touchSlop
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()

                        initialBrightness = getOverlayScreenBrightness(context, activity)
                        initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        initialSeek = currentPosition
                        initialScale = currentScale
                        initialOffsetX = currentOffsetX
                        initialOffsetY = currentOffsetY
                        dragDirection = null
                        isDragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }

                            if (change == null || !change.pressed) {
                                if (isDragging) {
                                    if (dragDirection == 1) {
                                        seekOsd?.let { target ->
                                            onSeekTo(target)
                                            actions.seekFinished(target)
                                        }
                                    }
                                    volumeOsd = null
                                    brightnessOsd = null
                                    seekOsd = null
                                    isDragging = false
                                    dragDirection = null
                                    lastTapTime = 0L
                                } else {
                                    val pressDuration = System.currentTimeMillis() - downTime
                                    val moveDist = (change?.position ?: downPos).minus(downPos).getDistance()
                                    if (pressDuration < 400L && moveDist < slopThreshold * 2f) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTime < 350L && (downPos - lastTapPos).getDistance() < slopThreshold * 4f) {
                                            // Double Tap Gesture
                                            lastTapTime = 0L
                                            resetTimer()
                                            if (!isLocked) {
                                                if (downPos.x < screenWidth * 0.35f) {
                                                    // Left Double Tap: -10s
                                                    val newPos = (currentPosition - 10000L).coerceAtLeast(0L)
                                                    onSeekTo(newPos)
                                                    actions.rewind10()
                                                    showLeftRipple = true
                                                    scope.launch {
                                                        delay(650)
                                                        showLeftRipple = false
                                                    }
                                                } else if (downPos.x > screenWidth * 0.65f) {
                                                    // Right Double Tap: +10s
                                                    val newPos = if (durationMs > 0) (currentPosition + 10000L).coerceAtMost(durationMs) else currentPosition + 10000L
                                                    onSeekTo(newPos)
                                                    actions.forward10()
                                                    showRightRipple = true
                                                    scope.launch {
                                                        delay(650)
                                                        showRightRipple = false
                                                    }
                                                } else {
                                                    // Center Double Tap: Toggle Zoom / Reset
                                                    cycleZoomMode()
                                                }
                                            } else {
                                                showLockOsd = true
                                                scope.launch {
                                                    delay(2000)
                                                    showLockOsd = false
                                                }
                                            }
                                        } else {
                                            // Single Tap
                                            lastTapTime = now
                                            lastTapPos = downPos
                                            if (!isLocked) {
                                                controlsVisible = !controlsVisible
                                                resetTimer()
                                            } else {
                                                showLockOsd = true
                                                scope.launch {
                                                    delay(2500)
                                                    showLockOsd = false
                                                }
                                            }
                                        }
                                    }
                                }
                                break
                            }

                            if (isLocked) {
                                change.consume()
                                continue
                            }

                            val dy = change.position.y - downPos.y
                            val dx = change.position.x - downPos.x

                            // Multi-Touch Pinch To Zoom & Pan
                            if (event.changes.size >= 2) {
                                val ptr1 = event.changes[0].position
                                val ptr2 = event.changes[1].position
                                val distance = hypot(ptr1.x - ptr2.x, ptr1.y - ptr2.y)
                                val currentCentroid = Offset((ptr1.x + ptr2.x) / 2f, (ptr1.y + ptr2.y) / 2f)

                                if (dragDirection == 5) {
                                    if (initialPinchDistance > 10f) {
                                        val newScale = (initialScale * (distance / initialPinchDistance)).coerceIn(0.25f, 5.0f)
                                        val deltaX = currentCentroid.x - initialCentroid.x
                                        val deltaY = currentCentroid.y - initialCentroid.y
                                        onScaleChange(newScale)
                                        onOffsetChange(initialOffsetX + deltaX, initialOffsetY + deltaY)
                                        val label = "${(newScale * 100).toInt()}%"
                                        zoomOsd = label
                                        scope.launch {
                                            delay(1200)
                                            if (zoomOsd == label) zoomOsd = null
                                        }
                                    }
                                    event.changes.forEach { it.consume() }
                                } else {
                                    dragDirection = 5
                                    initialPinchDistance = distance
                                    initialCentroid = currentCentroid
                                    initialScale = currentScale
                                    initialOffsetX = currentOffsetX
                                    initialOffsetY = currentOffsetY
                                    isDragging = true
                                    lastTapTime = 0L
                                }
                            } else if (dragDirection == null) {
                                if (abs(dy) > slopThreshold || abs(dx) > slopThreshold) {
                                    isDragging = true
                                    lastTapTime = 0L
                                    if (currentScale > 1.15f) {
                                        dragDirection = 5 // Pan zoomed video
                                        initialOffsetX = currentOffsetX
                                        initialOffsetY = currentOffsetY
                                    } else if (abs(dy) > abs(dx) * 1.2f) {
                                        if (isFullscreen && dy > 60f && downPos.y < screenHeight * 0.25f) {
                                            dragDirection = 4 // Exit Fullscreen
                                        } else if (downPos.x < screenWidth * 0.5f) {
                                            dragDirection = 2 // Brightness (Left half)
                                        } else {
                                            dragDirection = 3 // Volume (Right half)
                                        }
                                    } else {
                                        dragDirection = 1 // Scrub / Seek (Horizontal)
                                    }
                                }
                            }

                            if (isDragging && dragDirection != null) {
                                change.consume()
                                resetTimer()
                                when (dragDirection) {
                                    5 -> { // Pan single touch when zoomed
                                        if (currentScale > 1.15f) {
                                            onOffsetChange(initialOffsetX + dx, initialOffsetY + dy)
                                        }
                                    }
                                    1 -> { // Scrub / Seek
                                        val seekDeltaMs = ((dx / screenWidth.coerceAtLeast(1f)) * 90000f).toLong()
                                        val maxDur = durationMs.coerceAtLeast(0L)
                                        val targetSeek = if (maxDur > 0) (initialSeek + seekDeltaMs).coerceIn(0L, maxDur) else (initialSeek + seekDeltaMs).coerceAtLeast(0L)
                                        seekOsd = targetSeek
                                        brightnessOsd = null
                                        volumeOsd = null
                                        actions.seekChanged(targetSeek)
                                    }
                                    2 -> { // Brightness
                                        val deltaB = -(dy / screenHeight.coerceAtLeast(1f)) * 1.5f
                                        val newBrightness = (initialBrightness + deltaB).coerceIn(0.01f, 1f)
                                        activity?.window?.let { win ->
                                            val lp = win.attributes
                                            lp.screenBrightness = newBrightness
                                            win.attributes = lp
                                        }
                                        brightnessOsd = (newBrightness * 100).toInt()
                                        volumeOsd = null
                                        seekOsd = null
                                    }
                                    3 -> { // Volume
                                        val deltaV = -(dy / screenHeight.coerceAtLeast(1f)) * maxVolume * 1.5f
                                        val newVolume = (initialVolume + deltaV).toInt().coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                        volumeOsd = ((newVolume.toFloat() / maxVolume.coerceAtLeast(1)) * 100).toInt()
                                        brightnessOsd = null
                                        seekOsd = null
                                    }
                                    4 -> { // Exit Fullscreen
                                        if (dy > 80f) {
                                            actions.dismiss()
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        )

        // ══════════════════════════════════════════════════════════════════════
        // LAYER 2: GESTURE RIPPLE BADGES & OSD HUDS
        // ══════════════════════════════════════════════════════════════════════
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showLeftRipple,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("-10s", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = showRightRipple,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+10s", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Gesture OSD HUD Center Box
        AnimatedVisibility(
            visible = brightnessOsd != null || volumeOsd != null || seekOsd != null || zoomOsd != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        brightnessOsd != null -> {
                            Icon(Icons.Default.BrightnessMedium, contentDescription = "Brightness", tint = Color(0xFFFFD54F), modifier = Modifier.size(22.dp))
                            Text("${brightnessOsd}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        volumeOsd != null -> {
                            Icon(if (volumeOsd == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume", tint = NeonCyan, modifier = Modifier.size(22.dp))
                            Text("${volumeOsd}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        seekOsd != null -> {
                            val delta = seekOsd!! - currentPosition
                            val sign = if (delta >= 0) "+" else ""
                            Icon(if (delta >= 0) Icons.Default.FastForward else Icons.Default.FastRewind, contentDescription = "Seek", tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
                            Text("${TimeFormatter.formatDuration(seekOsd!!)} ($sign${delta / 1000}s)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        zoomOsd != null -> {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Zoom", tint = ElectricViolet, modifier = Modifier.size(22.dp))
                            Text(zoomOsd!!, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Screen Locked Alert
        if (isLocked) {
            AnimatedVisibility(
                visible = showLockOsd,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFFF5252), modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Screen Locked", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Floating Unlock Button on top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.2.dp, NeonCyan),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            isLocked = false
                            actions.toggleLock()
                            controlsVisible = true
                            resetTimer()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = NeonCyan, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // LAYER 3: PRO CONTROLS OVERLAY — Glassmorphic Top/Center/Bottom OSD
        // ══════════════════════════════════════════════════════════════════════

        // ── CENTER TRANSPORT CLUSTER (Play/Pause + Prev/Next) ──
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.85f),
            exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Previous
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(52.dp).clickable { resetTimer(); actions.previous() }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
                // Rewind 10s
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(52.dp).clickable { resetTimer(); actions.rewind10() }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
                // Main Play/Pause — large accent ring
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(2.5.dp, Brush.linearGradient(listOf(NeonCyan, ElectricViolet))),
                    modifier = Modifier.size(72.dp).clickable { resetTimer(); actions.playPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                // Forward 10s
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(52.dp).clickable { resetTimer(); actions.forward10() }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
                // Skip Next
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(52.dp).clickable { resetTimer(); actions.next() }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }
        }

        // ── TOP BAR (Glassmorphic Title + Badges + Quick Actions) ──
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.80f), Color.Black.copy(alpha = 0.35f), Color.Transparent)))
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Back / Dismiss
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.10f), modifier = Modifier.size(40.dp).clickable { actions.dismiss() }) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, "Back", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    }

                    // Title + Artist
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                        if (artist.isNotBlank()) {
                            Text(artist, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                    }

                    // Badges Row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (currentTimeString.isNotBlank()) {
                            ProBadge(currentTimeString, Color.White.copy(alpha = 0.15f), Color.White, fontSize = 13.sp)
                        }
                        if (is4K) ProBadge("4K", Color(0xFFD42B2B), Color.White, fontSize = 12.sp)
                        if (isHDR) ProBadge("HDR", Color(0xFF7B2CBF), Color.White, fontSize = 12.sp)
                        if (is60Fps) ProBadge("60FPS", Color(0xFF00897B), Color.White, fontSize = 12.sp)
                        if (qualityText.isNotEmpty() && !is4K) ProBadge(qualityText.uppercase(), ElectricViolet.copy(alpha = 0.75f), Color.White, fontSize = 12.sp)
                    }

                    // Quick Action Icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        ProIconBtn(Icons.Default.Analytics, NeonCyan) { resetTimer(); actions.openStats(); onToggleStats() }
                        ProIconBtn(Icons.Default.PictureInPicture, Color.White, enabled = !isInPipMode) { resetTimer(); actions.openPipOrBackgroundPlay() }
                        ProIconBtn(Icons.Default.Lock, Color.White) {
                            isLocked = true; actions.toggleLock(); showLockOsd = true
                            scope.launch { delay(2000); showLockOsd = false }
                        }
                    }
                }
            }
        }

        // ── BOTTOM BAR (Seekbar + Smart Action Chips) ──
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.85f))))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // 1. Timeline / Seekbar
                DeepEyeTimeRow(
                    positionMs = currentPosition,
                    durationMs = durationMs,
                    bufferedMs = bufferedPosition,
                    onSeekTo = { target -> resetTimer(); onSeekTo(target); actions.seekFinished(target) }
                )

                Spacer(Modifier.height(6.dp))

                // 2. Smart Grouped Action Chips (Scrollable)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ─── Media Group ───
                    ActionChip(Icons.Default.AspectRatio, "Aspect", active = videoScale != 1.0f, activeTint = ElectricViolet) { cycleZoomMode() }

                    ActionChip(Icons.Default.Speed, "${playbackSpeed}x", active = playbackSpeed != 1.0f, activeTint = NeonCyan) {
                        resetTimer()
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        val currentIdx = speeds.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 2
                        val nextSpeed = speeds[(currentIdx + 1) % speeds.size]
                        onSetSpeed?.invoke(nextSpeed)
                        actions.openSpeed()
                    }

                    ActionChip(Icons.Default.HighQuality, "Quality", enabled = hasQuality) { resetTimer(); actions.openQuality() }
                    ActionChip(Icons.Default.Equalizer, "Audio") { resetTimer(); actions.openAudioTrack() }

                    ChipDivider()

                    // ─── Interaction Group ───
                    val repeatIcon = when (playerState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.NONE -> Icons.Default.Repeat
                    }
                    ActionChip(repeatIcon, "Repeat", active = playerState.repeatMode != RepeatMode.NONE, activeTint = NeonCyan) { resetTimer(); actions.toggleRepeat() }

                    ActionChip(Icons.Default.ThumbUp, "Like", active = playerState.isLiked, activeTint = NeonCyan) { resetTimer(); actions.toggleLike() }
                    ActionChip(Icons.Default.ThumbDown, "Dislike", active = playerState.isDisliked, activeTint = Color(0xFFFF5252)) { resetTimer(); actions.toggleDislike() }
                    ActionChip(Icons.Default.ClosedCaption, "CC", enabled = hasCaptions || hasVideo, active = playerState.isCaptionEnabled, activeTint = NeonCyan) { resetTimer(); actions.toggleCaptions() }

                    ChipDivider()

                    // ─── Utility Group ───
                    ActionChip(Icons.AutoMirrored.Filled.PlaylistAdd, "Add") { resetTimer(); actions.addToPlaylist() }
                    ActionChip(Icons.AutoMirrored.Filled.QueueMusic, "Queue") { resetTimer(); actions.openQueue() }
                    ActionChip(Icons.Default.Info, "Info") { resetTimer(); actions.openInfo() }
                    ActionChip(Icons.Default.FullscreenExit, "Exit") { actions.dismiss() }
                }
            }
        }

        // ── STATS FOR NERDS HUD OVERLAY ──
        if (showStats && diagnostics != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 50.dp, end = 12.dp)
                    .widthIn(max = 280.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("STATS FOR NERDS", style = MaterialTheme.typography.labelMedium, color = NeonCyan, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp).clickable { onToggleStats() }
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        StatsRow("Video ID", diagnostics.videoId.ifBlank { "N/A" })
                        StatsRow("Resolution", diagnostics.activeVideoFormat?.formattedVideoResolution ?: "${diagnostics.activeVideoFormat?.width ?: 0}x${diagnostics.activeVideoFormat?.height ?: 0}")
                        StatsRow("Video Codec", diagnostics.activeVideoFormat?.codecName ?: diagnostics.activeVideoDecoder.ifBlank { "Auto" })
                        StatsRow("Audio Codec", diagnostics.activeAudioFormat?.codecName ?: diagnostics.activeAudioDecoder.ifBlank { "Auto" })
                        StatsRow("Bandwidth", diagnostics.formattedBandwidth)
                        StatsRow("Buffer Health", diagnostics.formattedBufferHealth)
                        StatsRow("Dropped Frames", "${diagnostics.droppedFrames}")
                        StatsRow("HW Acceleration", if (diagnostics.isHardwareAccelerated) "Enabled" else "Software")
                    }
                }
            }
        }

        // ── LOADING SPINNER ──
        if (isLoading) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 9.sp)
    }
}

@Composable
private fun DeepEyeTimeRow(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    onSeekTo: (Long) -> Unit
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    val displayPos = if (isScrubbing && durationMs > 0) (scrubFraction * durationMs).toLong() else positionMs

    val posStr = TimeFormatter.formatDuration(displayPos)
    val durStr = if (durationMs > 0) TimeFormatter.formatDuration(durationMs) else "--:--"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = posStr,
                style = MaterialTheme.typography.bodySmall,
                color = if (isScrubbing) NeonCyan else Color.White.copy(0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.width(44.dp)
            )

            Box(
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .pointerInput(durationMs) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (durationMs > 0) {
                                isScrubbing = true
                                val fraction = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                scrubFraction = fraction

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        val target = (scrubFraction * durationMs).toLong()
                                        onSeekTo(target)
                                        isScrubbing = false
                                        break
                                    }
                                    val curFrac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    scrubFraction = curFrac
                                    change.consume()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val progressFrac = if (isScrubbing) scrubFraction else (if (durationMs > 0) positionMs.toFloat() / durationMs else 0f)
                val bufferFrac = if (durationMs > 0) (positionMs + bufferedMs).toFloat() / durationMs else 0f

                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isScrubbing) 6.dp else 4.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    val trackHeight = size.height
                    // 1. Background Track
                    drawRect(
                        color = Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, trackHeight)
                    )
                    // 2. Buffer Track
                    val bufW = size.width * bufferFrac.coerceIn(0f, 1f)
                    drawRect(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(0f, 0f),
                        size = Size(bufW, trackHeight)
                    )
                    // 3. Progress Track (Neon Cyan Gradient)
                    val progW = size.width * progressFrac.coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ElectricViolet, NeonCyan)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(progW, trackHeight)
                    )
                    // 4. Scrubber Thumb
                    val thumbRadius = if (isScrubbing) 8.dp.toPx() else 5.dp.toPx()
                    drawCircle(
                        color = NeonCyan,
                        radius = thumbRadius,
                        center = Offset(progW.coerceIn(thumbRadius, size.width - thumbRadius), trackHeight / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius * 0.45f,
                        center = Offset(progW.coerceIn(thumbRadius, size.width - thumbRadius), trackHeight / 2f)
                    )
                }
            }

            Text(
                text = durStr,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.9f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// ─── Pro Reusable Components ───

@Composable
private fun ProBadge(text: String, bg: Color, textColor: Color, fontSize: androidx.compose.ui.unit.TextUnit = 11.sp) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProIconBtn(icon: ImageVector, tint: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    activeTint: Color = NeonCyan,
    onClick: () -> Unit
) {
    val chipBg = if (active) activeTint.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)
    val chipBorder = if (active) activeTint.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)
    val iconTint = if (active) activeTint else Color.White
    val textColor = if (active) activeTint else Color.White.copy(alpha = 0.9f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = chipBg,
        border = BorderStroke(1.dp, chipBorder),
        modifier = Modifier
            .height(40.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
            Text(label, color = textColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChipDivider() {
    Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.18f)))
}

private fun getOverlayScreenBrightness(context: Context, activity: Activity?): Float {
    val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
    if (windowBrightness >= 0f) return windowBrightness
    return try {
        val sysBrightness = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        (sysBrightness / 255f).coerceIn(0.01f, 1f)
    } catch (e: Exception) {
        0.5f
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
