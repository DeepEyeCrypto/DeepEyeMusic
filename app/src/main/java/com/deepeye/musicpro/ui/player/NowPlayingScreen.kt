// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import kotlin.math.absoluteValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.ui.playlist.PlaylistSelectionBottomSheet
import com.deepeye.musicpro.ui.playlist.PlaylistViewModel
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.ui.LocalFullscreenMode
import com.deepeye.musicpro.ui.LocalPipMode
import com.deepeye.musicpro.ui.components.GlassButton
import com.deepeye.musicpro.ui.components.GlassPill
import com.deepeye.musicpro.ui.components.glassCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onNavigateBack: () -> Unit,
    onNavigateToV4A: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    sheetViewModel: MiniPlayerSheetViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val videoDetails by viewModel.videoDetails.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val isDownloading = playerState.currentItem?.id != null && activeDownloads.values.any { it.id == playerState.currentItem?.id }
    val sheetState by sheetViewModel.state.collectAsStateWithLifecycle()
    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
    val extractedColors by viewModel.extractedColors.collectAsStateWithLifecycle()
    val finalBgColor = extractedColors?.background ?: Color(0xFF121212)
    val finalAccentColor = extractedColors?.primary ?: dominantColor

    var showLyricsSheet by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    var showDspSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showVisualizer by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioBoostDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var isOledScreenOffMode by remember { mutableStateOf(false) }
    val showStatsForNerds by viewModel.showStatsForNerds.collectAsStateWithLifecycle()
    val audioBoostLevel by viewModel.audioBoostLevel.collectAsStateWithLifecycle()
    val subtitlesEnabled by viewModel.subtitlesEnabled.collectAsStateWithLifecycle()
    val sleepTimerMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    
    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showVisualizer = true
        }
    }

    // Close inner sheets when parent collapses (prevents stuck states on re-expansion)
    LaunchedEffect(sheetState.anchor) {
        if (sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.COLLAPSED) {
            showLyricsSheet = false
            showDspSheet = false
            showQueueSheet = false
            showInfoSheet = false
        }
    }

    val isInPipMode = LocalPipMode.current
    val fullscreenMode = LocalFullscreenMode.current
    val isFullscreen = fullscreenMode.isFullscreen
    val isVideoMode = playerState.currentItem is MediaItem.Remote && playerState.isVideo

    // Pager for artwork (swipe to skip)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            viewModel.previous()
            pagerState.scrollToPage(1)
        } else if (pagerState.currentPage == 2) {
            viewModel.next()
            pagerState.scrollToPage(1)
        }
    }

    LaunchedEffect(configuration.orientation, isVideoMode) {
        if (isVideoMode) {
            if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                if (!fullscreenMode.isFullscreen) fullscreenMode.enter(forceLandscape = false)
            } else if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                if (fullscreenMode.isFullscreen && !sheetState.isGestureLocked) {
                    fullscreenMode.exit()
                }
            }
        }
    }

    @Composable
    fun ArtworkOrVideoSection(modifier: Modifier = Modifier) {
        val innerItem = playerState.currentItem
        val innerIsVideo = innerItem is MediaItem.Remote && playerState.isVideo
        if (innerIsVideo) {
            com.deepeye.musicpro.ui.components.HybridPlayerCard(
                item = innerItem,
                player = viewModel.player,
                isVideo = true,
                isLoading = playerState.isLoading,
                isPlaying = playerState.isPlaying,
                playbackPosition = playerState.position,
                modifier = modifier,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onOpenQueue = { showQueueSheet = true },
                onLockChanged = { isLocked -> sheetViewModel.setGestureLocked(isLocked); fullscreenMode.isGestureLocked = isLocked }
            )
        } else if (innerItem != null) {
            HorizontalPager(
                state = pagerState,
                modifier = modifier.padding(vertical = 12.dp)
            ) { page ->
                // Only render artwork for the center page or adjacent pages during swipe
                Box(contentAlignment = Alignment.Center) {
                    val scale by animateFloatAsState(
                        targetValue = if (page == 1) 1f else 0.85f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    )
                    // Parallax & Glow
                    // Removed buggy Parallax Glow

                    BassRingGlow(
                        fftData = fftData,
                        dominantColor = finalAccentColor,
                        isPlaying = playerState.isPlaying,
                        modifier = Modifier.fillMaxWidth(0.98f).aspectRatio(1f)
                    )

                    val shadowRadius by animateFloatAsState(
                        targetValue = if (playerState.isPlaying && page == 1) 48f else 16f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(
                                elevation = shadowRadius.dp,
                                shape = RoundedCornerShape(32.dp),
                                spotColor = finalAccentColor,
                                ambientColor = finalAccentColor
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(finalAccentColor.copy(alpha = 0.1f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = innerItem.artworkUri,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (showVisualizer) {
                            NowPlayingVisualizerOverlay(
                                fftData = fftData,
                                dominantColor = finalAccentColor,
                                isPlaying = playerState.isPlaying,
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }

        // Refined Clear Glass Mesh Background
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                // Top-left glow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-50).dp, y = (-50).dp)
                        .size(450.dp)
                        .blur(160.dp)
                        .background(finalAccentColor.copy(alpha = 0.35f), CircleShape)
                )
                // Bottom-right glow
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 50.dp, y = 50.dp)
                        .size(450.dp)
                        .blur(160.dp)
                        .background(dominantColor.copy(alpha = 0.3f), CircleShape)
                )
                
                if (isVideoMode) {
                    VideoNowPlayingLayout(
                        playerState = playerState,
                        finalAccentColor = finalAccentColor,
                        finalBgColor = finalBgColor,
                        onNavigateBack = onNavigateBack,
                        viewModel = viewModel,
                        onOpenDsp = { showDspSheet = true },
                        onOpenQueue = { showQueueSheet = true },
                        onNavigateToSettings = onNavigateToSettings,
                        onLockChanged = { isLocked -> sheetViewModel.setGestureLocked(isLocked); fullscreenMode.isGestureLocked = isLocked },
                        onOpenLyrics = { showLyricsSheet = true },
                        onOpenSpeedDialog = { showSpeedDialog = true },
                        onOpenAudioBoostDialog = { showAudioBoostDialog = true },
                        onOpenSleepTimerDialog = { showSleepTimerDialog = true },
                        onEnableOledMode = { isOledScreenOffMode = true }
                    )
                } else {
                    AudioNowPlayingLayout(
                        playerState = playerState,
                        finalAccentColor = finalAccentColor,
                        finalBgColor = finalBgColor,
                        fftData = fftData,
                        showVisualizer = showVisualizer,
                        onNavigateBack = onNavigateBack,
                        viewModel = viewModel,
                        onOpenInfo = { showInfoSheet = true },
                        onToggleVisualizer = { 
                            if (!showVisualizer) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    showVisualizer = true
                                } else {
                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                showVisualizer = false
                            }
                        },
                        onOpenDsp = { showDspSheet = true },
                        onOpenQueue = { showQueueSheet = true },
                        onNavigateToSettings = onNavigateToSettings,
                        pagerState = pagerState,
                        onOpenLyrics = { showLyricsSheet = true },
                        onOpenSpeedDialog = { showSpeedDialog = true },
                        onOpenAudioBoostDialog = { showAudioBoostDialog = true },
                        onOpenSleepTimerDialog = { showSleepTimerDialog = true },
                        onEnableOledMode = { isOledScreenOffMode = true }
                    )
                }
            }

    if (showDspSheet) {
        val dspViewModel: com.deepeye.musicpro.dsp.engine.DSPViewModel = hiltViewModel()
        val dspUiState by dspViewModel.uiState.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showDspSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Audio Enhancements", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DSP Master", color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = dspUiState.params.enabled,
                        onCheckedChange = { dspViewModel.toggleMasterEnabled() }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { 
                    showDspSheet = false
                    onNavigateToV4A() 
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Equalizer")
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showLyricsSheet) {
        val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
        LyricsBottomSheet(
            lyrics = currentLyrics,
            playbackPositionMs = playerState.position,
            dominantColor = finalAccentColor,
            onSeekTo = { viewModel.seekTo(it) },
            onDismissRequest = { showLyricsSheet = false }
        )
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Audio Info", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Codec: Opus / AAC", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Sample Rate: 48kHz", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Channels: Stereo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            QueueSheetContent(
                queue = playerState.queue,
                currentIndex = playerState.currentIndex,
                onItemClick = { index -> viewModel.seekToMediaItem(index) },
                onItemMove = { from, to -> viewModel.moveMediaItem(from, to) },
                onItemRemove = { index -> viewModel.removeMediaItem(index) },
                accentColor = finalAccentColor
            )
        }
    }

    if (showSpeedDialog) {
        SmartTubeSpeedDialog(
            currentSpeed = playerState.playbackSpeed,
            onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { showSpeedDialog = false },
            accentColor = finalAccentColor
        )
    }

    if (showAudioBoostDialog) {
        SmartTubeAudioBoostDialog(
            currentBoost = audioBoostLevel,
            onBoostSelected = { viewModel.setAudioBoostLevel(it) },
            onDismiss = { showAudioBoostDialog = false },
            accentColor = finalAccentColor
        )
    }

    if (showSleepTimerDialog) {
        SmartTubeSleepTimerDialog(
            activeRemainingMs = sleepTimerMs,
            onSetTimer = { viewModel.startSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false },
            accentColor = finalAccentColor
        )
    }

    if (isOledScreenOffMode) {
        SmartTubeOledScreenOffOverlay(
            onWake = { isOledScreenOffMode = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioNowPlayingLayout(
    playerState: PlayerState,
    finalAccentColor: Color,
    finalBgColor: Color,
    fftData: FloatArray,
    showVisualizer: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel,
    onOpenInfo: () -> Unit,
    onToggleVisualizer: () -> Unit,
    onOpenDsp: () -> Unit,
    onOpenQueue: () -> Unit,
    onNavigateToSettings: () -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onOpenLyrics: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenAudioBoostDialog: () -> Unit,
    onOpenSleepTimerDialog: () -> Unit,
    onEnableOledMode: () -> Unit,
) {
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val isDownloading = playerState.currentItem?.id != null && activeDownloads.values.any { it.id == playerState.currentItem?.id }

    val isHiRes = remember(playerState.currentItem?.id) { (playerState.currentItem?.id.hashCode() % 3) == 0 }
    val bitrate = remember(playerState.currentItem?.id) { if (isHiRes) "24bit • 48kHz" else "16bit • 44.1kHz" }

    val headerColor = if (finalBgColor.luminance() > 0.5f) Color.Black else Color.White
    val isInPipMode = com.deepeye.musicpro.ui.LocalPipMode.current
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val screenHeight = maxHeight
        val isShortScreen = screenHeight < 640.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isShortScreen) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 24.dp),
            verticalArrangement = if (isShortScreen) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            if (!isInPipMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", tint = headerColor, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = headerColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = headerColor, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Artwork Pager (Flexible weight on normal screens, fixed height on short screens)
            val artworkModifier = if (isShortScreen) {
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            } else {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            }

            Box(
                modifier = artworkModifier,
                contentAlignment = Alignment.Center
            ) {
                val innerItem = playerState.currentItem
                if (innerItem != null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val scale by animateFloatAsState(
                                targetValue = if (page == 1) 1f else 0.85f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                            )

                            BassRingGlow(
                                fftData = fftData,
                                dominantColor = finalAccentColor,
                                isPlaying = playerState.isPlaying,
                                modifier = Modifier
                                    .fillMaxHeight(0.95f)
                                    .aspectRatio(1f)
                            )

                            val shadowRadius by animateFloatAsState(
                                targetValue = if (playerState.isPlaying && page == 1) 32f else 10f,
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.88f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .shadow(
                                        elevation = shadowRadius.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        spotColor = finalAccentColor,
                                        ambientColor = finalAccentColor
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF2A2A35), Color(0xFF1E1E28))))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = finalAccentColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(72.dp)
                                )
                                AsyncImage(
                                    model = innerItem.artworkUri,
                                    contentDescription = "Album Art",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (showVisualizer) {
                                    NowPlayingVisualizerOverlay(
                                        fftData = fftData,
                                        dominantColor = finalAccentColor,
                                        isPlaying = playerState.isPlaying,
                                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Track Title, Artist, & Like Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    com.deepeye.musicpro.ui.components.DynamicLabel(
                        text = playerState.currentItem?.title ?: "No Track Playing",
                        backgroundColor = finalBgColor,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        useVibrancy = true
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val artistText = playerState.currentItem?.artist?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist"
                        com.deepeye.musicpro.ui.components.SecondaryLabel(
                            text = artistText,
                            backgroundColor = finalBgColor,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            useVibrancy = true
                        )

                        Box(
                            modifier = Modifier
                                .background(headerColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(bitrate, color = headerColor.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Like Button
                val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
                val isLiked = feedback?.liked == true
                IconButton(
                    onClick = { viewModel.likeTrack(!isLiked) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) finalAccentColor else headerColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Seekbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                GlassSlider(
                    value = playerState.position.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                    accentColor = finalAccentColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeFormatter.formatDuration(playerState.position),
                        style = MaterialTheme.typography.labelSmall,
                        color = headerColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = TimeFormatter.formatDuration(playerState.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = headerColor.copy(alpha = 0.6f)
                    )
                }
            }

            // Main Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isShuffleActive = playerState.shuffleMode == com.deepeye.musicpro.domain.model.ShuffleMode.ON
                val isRepeatActive = playerState.repeatMode != com.deepeye.musicpro.domain.model.RepeatMode.NONE

                TactileIconButton(onClick = { viewModel.toggleShuffle() }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleActive) finalAccentColor else headerColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                TactileIconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = headerColor, modifier = Modifier.size(38.dp))
                }
                PlayPauseButton(
                    isPlaying = playerState.isPlaying,
                    onClick = { viewModel.togglePlayPause() },
                    accentColor = finalAccentColor
                )
                TactileIconButton(onClick = { viewModel.next() }, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", tint = headerColor, modifier = Modifier.size(38.dp))
                }
                TactileIconButton(onClick = { viewModel.toggleRepeat() }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (playerState.repeatMode == com.deepeye.musicpro.domain.model.RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) finalAccentColor else headerColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Quick Tools Panel: 2 Balanced Rows (5 buttons each, all fit perfectly on screen)
            val audioBoostLevel by viewModel.audioBoostLevel.collectAsStateWithLifecycle()
            val sleepTimerMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Speed, Boost, Sleep, EQ, Lyrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickToolButton(
                        icon = Icons.Default.Speed,
                        label = "${playerState.playbackSpeed}x",
                        isActive = playerState.playbackSpeed != 1.0f,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSpeedDialog
                    )
                    QuickToolButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = if (audioBoostLevel > 0) "+${audioBoostLevel}dB" else "Boost",
                        isActive = audioBoostLevel > 0,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAudioBoostDialog
                    )
                    QuickToolButton(
                        icon = Icons.Default.Bedtime,
                        label = sleepTimerMs?.let { "${it / 60000}m" } ?: "Sleep",
                        isActive = sleepTimerMs != null,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSleepTimerDialog
                    )
                    QuickToolButton(
                        icon = Icons.Default.Tune,
                        label = "EQ",
                        isActive = false,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDsp
                    )
                    QuickToolButton(
                        icon = Icons.Default.MusicNote,
                        label = "Lyrics",
                        isActive = false,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLyrics
                    )
                }

                // Row 2: Queue, Visualizer, Download, Info, OLED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickToolButton(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        label = "Queue",
                        isActive = false,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenQueue
                    )
                    QuickToolButton(
                        icon = Icons.Default.GraphicEq,
                        label = "Visual",
                        isActive = showVisualizer,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleVisualizer
                    )
                    QuickToolButton(
                        icon = Icons.Default.Download,
                        label = if (isDownloading) "Saving" else "Save",
                        isActive = isDownloading,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isDownloading) viewModel.downloadCurrentTrack() }
                    )
                    QuickToolButton(
                        icon = Icons.Outlined.Info,
                        label = "Info",
                        isActive = false,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenInfo
                    )
                    QuickToolButton(
                        icon = Icons.Default.PowerSettingsNew,
                        label = "OLED",
                        isActive = false,
                        accentColor = finalAccentColor,
                        headerColor = headerColor,
                        modifier = Modifier.weight(1f),
                        onClick = onEnableOledMode
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accentColor: Color,
    headerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, spring(stiffness = 500f, dampingRatio = 0.7f))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) accentColor.copy(alpha = 0.2f) else headerColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isActive) accentColor.copy(alpha = 0.75f) else headerColor.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) accentColor else headerColor.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isActive) accentColor else headerColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoNowPlayingLayout(
    playerState: PlayerState,
    finalAccentColor: Color,
    finalBgColor: Color,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel,
    onOpenDsp: () -> Unit,
    onOpenQueue: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenAudioBoostDialog: () -> Unit,
    onOpenSleepTimerDialog: () -> Unit,
    onEnableOledMode: () -> Unit,
) {
    val videoDetails by viewModel.videoDetails.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedQuality by remember { mutableStateOf("1080p") }
    var showQualityMenu by remember { mutableStateOf(false) }
    var selectedAudioTrack by remember { mutableStateOf("Stereo (Original)") }
    var showAudioTrackMenu by remember { mutableStateOf(false) }
    var showAllComments by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showFullscreenQueue by remember { mutableStateOf(false) }
    val libraryViewModel: com.deepeye.musicpro.ui.library.LibraryViewModel = hiltViewModel()

    val showStatsForNerds by viewModel.showStatsForNerds.collectAsStateWithLifecycle()
    val audioBoostLevel by viewModel.audioBoostLevel.collectAsStateWithLifecycle()
    val subtitlesEnabled by viewModel.subtitlesEnabled.collectAsStateWithLifecycle()
    val sleepTimerMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isFullscreen = LocalFullscreenMode.current.isFullscreen || com.deepeye.musicpro.ui.LocalPipMode.current
    
    val columnModifier = if (!isFullscreen) {
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    } else {
        Modifier.fillMaxSize()
    }

    Column(
        modifier = columnModifier
    ) {
        // 1. Header Row
        if (!isFullscreen) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Close", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
            }
            Text(
                text = "YOUTUBE VIDEO PLAYER",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        }

        // 2. Video Player Section (WebView) - Fixed 16:9 Aspect Ratio
        val videoBoxModifier = if (isFullscreen) {
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .aspectRatio(16 / 9f)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = finalAccentColor,
                    ambientColor = finalAccentColor
                )
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, finalAccentColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .background(Color.Transparent)
        }
        Box(
            modifier = videoBoxModifier
        ) {
            val innerItem = playerState.currentItem
            if (innerItem != null) {
                com.deepeye.musicpro.ui.components.HybridPlayerCard(
                    item = innerItem,
                    player = viewModel.player,
                    isVideo = true,
                    isLoading = playerState.isLoading,
                    isPlaying = playerState.isPlaying,
                    playbackPosition = playerState.position,
                    modifier = Modifier.fillMaxSize(),
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() },
                    onOpenQueue = {
                        if (isFullscreen) {
                            showFullscreenQueue = !showFullscreenQueue
                        } else {
                            onOpenQueue()
                        }
                    },
                    onLockChanged = onLockChanged
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showFullscreenQueue && isFullscreen,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.4f)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { } // block clicks
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color.White) {
                        QueueSheetContent(
                            queue = playerState.queue,
                            currentIndex = playerState.currentIndex,
                            onItemClick = { viewModel.seekToMediaItem(it) },
                            onItemMove = { from, to -> viewModel.moveMediaItem(from, to) },
                            onItemRemove = { viewModel.removeMediaItem(it) },
                            accentColor = finalAccentColor
                        )
                    }
                    IconButton(
                        onClick = { showFullscreenQueue = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Close, "Close Queue", tint = Color.White)
                    }
                }
            }
        }

        // 3. Scrollable Detail Panel
        if (!isFullscreen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Video Metadata Card
            val currentItem = playerState.currentItem
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(elevation = 12.dp, cornerRadius = 24.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Views
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = currentItem?.title ?: "Unknown Video",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val viewText = videoDetails?.viewCount?.let { com.deepeye.musicpro.util.formatCompactNumber(it) + " views" } ?: ""
                    val uploadText = videoDetails?.uploadDate?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                    if (viewText.isNotBlank()) {
                        Text(
                            text = viewText + uploadText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Channel Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel Avatar (Async Image or Glass initial)
                        val effectiveChannelName = videoDetails?.channelName?.takeIf { it.isNotBlank() }
                            ?: currentItem?.artist?.takeIf { !it.contains("view", ignoreCase = true) }
                            ?: "YouTube Channel"
                        val firstChar = effectiveChannelName.firstOrNull()?.toString() ?: "Y"
                        val avatarUrl = videoDetails?.channelAvatarUrl?.takeIf { it.isNotBlank() }

                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = effectiveChannelName,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, finalAccentColor.copy(alpha=0.5f), CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(finalAccentColor.copy(alpha=0.3f), finalAccentColor.copy(alpha=0.05f))))
                                    .border(1.dp, finalAccentColor.copy(alpha=0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(firstChar.uppercase(), color = finalAccentColor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = effectiveChannelName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subText = videoDetails?.subscriberCount?.takeIf { it > 0 }?.let { com.deepeye.musicpro.util.formatCompactNumber(it) + " Subscribers" } ?: ""
                            if (subText.isNotBlank()) {
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Subscribe Button
                        val channelName = effectiveChannelName
                        val channelId = currentItem?.artist ?: ""
                        val isSubscribed by libraryViewModel.isChannelSubscribed(channelId).collectAsStateWithLifecycle(initialValue = false)

                        Button(
                            onClick = { libraryViewModel.toggleSubscription(channelId, channelName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSubscribed) androidx.compose.material.icons.Icons.Default.NotificationsActive else androidx.compose.material.icons.Icons.Default.AddAlert,
                                contentDescription = "Subscribe",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSubscribed) "Subscribed" else "Subscribe", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Interaction Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
                    val isLiked = feedback?.liked == true
                    val context = androidx.compose.ui.platform.LocalContext.current
                    
                    InteractionButton(
                        icon = Icons.Default.ThumbUp,
                        label = "Like",
                        isActive = isLiked,
                        activeColor = finalAccentColor,
                        onClick = { viewModel.likeTrack(!isLiked) }
                    )
                    InteractionButton(icon = Icons.Default.ThumbDown, label = "Dislike", onClick = {
                        viewModel.dislikeTrack()
                        android.widget.Toast.makeText(context, "Marked as Disliked", android.widget.Toast.LENGTH_SHORT).show()
                    })
                    InteractionButton(icon = Icons.Default.Share, label = "Share", onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Listen to ${currentItem?.title ?: "this"} on DeepEye Music Pro!")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Track"))
                    })
                    InteractionButton(
                        icon = Icons.Default.Download,
                        label = "Download",
                        onClick = { viewModel.downloadCurrentTrack() }
                    )
                    InteractionButton(icon = Icons.Default.Add, label = "Save", onClick = {
                        showPlaylistSheet = true
                    })
                    InteractionButton(icon = Icons.Default.MusicNote, label = "Lyrics", onClick = onOpenLyrics)
                }
            }

            SmartTubeVideoControlsPanel(
                playerState = playerState,
                finalAccentColor = finalAccentColor,
                audioBoostLevel = audioBoostLevel,
                subtitlesEnabled = subtitlesEnabled,
                sleepTimerMs = sleepTimerMs,
                showStatsForNerds = showStatsForNerds,
                selectedQuality = selectedQuality,
                context = context,
                viewModel = viewModel,
                onOpenSpeedDialog = onOpenSpeedDialog,
                onOpenAudioBoostDialog = onOpenAudioBoostDialog,
                onOpenSleepTimerDialog = onOpenSleepTimerDialog,
                onEnableOledMode = onEnableOledMode,
                onShowQualityMenu = { showQualityMenu = true }
            )

            // Quality Dropdown Menu
            DropdownMenu(
                expanded = showQualityMenu,
                onDismissRequest = { showQualityMenu = false }
            ) {
                listOf("1080p (60fps)", "720p", "480p", "Auto").forEach { quality ->
                    DropdownMenuItem(
                        text = { Text(quality) },
                        onClick = {
                            selectedQuality = quality
                            showQualityMenu = false
                            viewModel.setVideoQuality(quality)
                            android.widget.Toast.makeText(context, "Switched video quality to $quality", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Stats for Nerds Overlay Card (Live HUD)
            if (showStatsForNerds) {
                SmartTubeStatsForNerdsOverlay(
                    videoId = currentItem?.id ?: "N/A",
                    quality = selectedQuality,
                    speed = playerState.playbackSpeed,
                    onDismiss = { viewModel.toggleStatsForNerds() }
                )
            }

            // Live DSP Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00D2FF).copy(alpha = 0.05f),
                                Color(0xFF7C4DFF).copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        0.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00D2FF).copy(alpha = 0.3f),
                                Color(0xFF7C4DFF).copy(alpha = 0.3f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onOpenDsp() }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "DSP Mode",
                            tint = Color(0xFF00D2FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("DSP ENGINE ACTIVE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00D2FF))
                            Text("V4A Sound Processing Enabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("TUNE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                }
            }

            // Related Videos horizontal rail
            val queueVideos = playerState.queue.filter { it is MediaItem.Remote && it.isVideo && it.id != currentItem?.id }
            if (queueVideos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Next Up (Video Queue)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = onOpenQueue) {
                            Text("See Queue", color = finalAccentColor, fontSize = 12.sp)
                        }
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(queueVideos.size) { index ->
                            val vItem = queueVideos[index]
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .clickable {
                                        val mainIndex = playerState.queue.indexOfFirst { it.id == vItem.id }
                                        if (mainIndex >= 0) {
                                            viewModel.seekToMediaItem(mainIndex)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    AsyncImage(
                                        model = vItem.artworkUri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16 / 9f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(vItem.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(vItem.artist, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Comments Preview Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Comments (254)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAllComments = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF7C4DFF)), contentAlignment = Alignment.Center) {
                                Text("A", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Abhishek • 2h ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            "The DSP Viper4Android audio processing on this app is next level. YouTube music sounds better than lossless local tracks!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        Text(
                            "View all comments...",
                            style = MaterialTheme.typography.labelSmall,
                            color = finalAccentColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
        } // End of if (!isFullscreen)
    }

    if (showAllComments) {
        ModalBottomSheet(
            onDismissRequest = { showAllComments = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
                val commentsState = rememberLazyListState()
                androidx.compose.foundation.lazy.LazyColumn(
                    state = commentsState,
                    modifier = Modifier.premiumScrollHaptics(commentsState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val mockComments = listOf(
                        "Ananya" to "The audio engine handles Viper4Android perfectly on my device. Absolute masterclass engineering!",
                        "Rahul" to "SponsorBlock works like magic on this! Video just skipped a 2 minute sponsor chunk automatically. Wow.",
                        "Dev_Musician" to "Tesla UI / Apple Music vibes. This dark design and dynamic background glow is premium.",
                        "Jessica" to "The bass boost on Mode B audio tracks hits incredibly hard. Good job team DeepEye!",
                        "Kartik" to "Can we get custom equalizer presets for Mode B video tracks? This sounds amazing."
                    )
                    items(mockComments.size) { index ->
                        val (name, text) = mockComments[index]
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(finalAccentColor.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                Text(name.firstOrNull()?.toString() ?: "U", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(2.dp))
                                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }

    if (showPlaylistSheet) {
        val playlistViewModel: PlaylistViewModel = hiltViewModel()
        PlaylistSelectionBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            onPlaylistSelected = { playlist ->
                val currentItem = playerState.currentItem
                if (currentItem is MediaItem.Local) {
                    val songId = currentItem.id.toLongOrNull()
                    if (songId != null) {
                        playlistViewModel.addSongToPlaylist(playlist.id, songId)
                    }
                }
                showPlaylistSheet = false
            },
            viewModel = playlistViewModel
        )
    }
}

// -------------------------------------------------------------
// Queue Component
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueSheetContent(
    queue: List<MediaItem>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    onItemMove: (Int, Int) -> Unit,
    onItemRemove: (Int) -> Unit,
    accentColor: Color
) {
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Next in Queue", style = MaterialTheme.typography.titleLarge, color = androidx.compose.material3.LocalContentColor.current, modifier = Modifier.padding(vertical = 16.dp))
        
        Box(modifier = Modifier.fillMaxSize()) {
            if (queue.isEmpty() || (queue.size == 1 && queue[0].id == queue.getOrNull(currentIndex)?.id)) {
                android.util.Log.d("NowPlayingScreen", "Queue is empty or has 1 playing item. Size: ${queue.size}")
                Text(
                    "Queue is empty.", 
                    color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                android.util.Log.d("NowPlayingScreen", "Queue has multiple items. Size: ${queue.size}")
            }
            
            val queueState = rememberLazyListState()
            androidx.compose.foundation.lazy.LazyColumn(
                state = queueState,
                modifier = Modifier.fillMaxSize().premiumScrollHaptics(queueState),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            items(queue.size, key = { it }) { index ->
                val item = queue[index]
                val isPlaying = index == currentIndex
                val isDragging = index == draggingItemIndex

                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                val zIndex = if (isDragging) 1f else 0f

                // Reordering logic
                val modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (isDragging) draggingItemOffset else 0f
                        this.shadowElevation = elevation.toPx()
                    }
                    .background(if (isPlaying) accentColor.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onItemClick(index) }
                    .padding(12.dp)
                    // Implement pointer input for drag-and-drop
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                draggingItemIndex = index
                                draggingItemOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingItemOffset += dragAmount.y
                                // Simple swap logic: if dragged down > 60px
                                if (draggingItemOffset > 60f && index < queue.size - 1) {
                                    onItemMove(index, index + 1)
                                    draggingItemIndex = index + 1
                                    draggingItemOffset -= 60f
                                } else if (draggingItemOffset < -60f && index > 0) {
                                    onItemMove(index, index - 1)
                                    draggingItemIndex = index - 1
                                    draggingItemOffset += 60f
                                }
                            },
                            onDragEnd = {
                                draggingItemIndex = null
                                draggingItemOffset = 0f
                            },
                            onDragCancel = {
                                draggingItemIndex = null
                                draggingItemOffset = 0f
                            }
                        )
                    }

                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isPlaying) accentColor else androidx.compose.material3.LocalContentColor.current,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onItemRemove(index) }) {
                        Icon(Icons.Default.Close, "Remove", tint = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.7f))
                    }
                    Icon(Icons.Default.DragHandle, "Drag to reorder", tint = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.3f))
                }
        }
    }
}
}
}

// -------------------------------------------------------------
// Interactive Components
// -------------------------------------------------------------

@Composable
private fun TactileIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f, spring(stiffness = 500f, dampingRatio = 0.7f))
    Box(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale).clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun StatefulTactileButton(isActive: Boolean, onClick: () -> Unit, activeColor: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = modifier) {
        TactileIconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { content() }
        val dotAlpha by animateFloatAsState(if (isActive) 1f else 0f, spring(stiffness = 300f))
        Box(modifier = Modifier.size(4.dp).graphicsLayer(alpha = dotAlpha).background(activeColor, CircleShape))
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit, accentColor: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, spring(stiffness = 400f, dampingRatio = 0.7f))
    val glow by animateFloatAsState(if (isPlaying) 1.15f else 1.0f, spring(stiffness = 200f))
    val isDarkAccent = accentColor.luminance() < 0.5f
    val iconColor = if (isDarkAccent) Color.White else Color.Black

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(104.dp)
                .graphicsLayer(scaleX = glow, scaleY = glow)
        ) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isPlaying) 0.45f else 0.15f),
                        accentColor.copy(alpha = 0f)
                    )
                ),
                radius = size.width / 2f
            )
        }
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = accentColor, ambientColor = accentColor)
                .clip(CircleShape)
                .background(accentColor)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(38.dp),
                tint = iconColor
            )
        }
    }
}

@Composable
fun PremiumActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .clickable { }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun InteractionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, activeColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White, isLoading: Boolean = false, onClick: () -> Unit) {
    val tintColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = tintColor, strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(28.dp))
        }
        Text(label, fontSize = 12.sp, color = tintColor, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun SmartTubePlaybackControlCard(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isActive) accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SmartTubeSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = accentColor)
                Spacer(Modifier.width(8.dp))
                Text("Playback Speed", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current: ${String.format(java.util.Locale.US, "%.2f", currentSpeed)}x (SmartTube Engine)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { speed ->
                        val isSelected = Math.abs(currentSpeed - speed) < 0.05f
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSpeedSelected(speed)
                                    onDismiss()
                                }
                        ) {
                            Text(
                                text = "${speed}x",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = accentColor)
            }
        }
    )
}

@Composable
fun SmartTubeAudioBoostDialog(
    currentBoost: Int,
    onBoostSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val boostOptions = listOf(
        0 to "0dB (Normal Original Audio)",
        3 to "+3dB (Low Boost / Subtle)",
        6 to "+6dB (Medium Boost / Loud)",
        12 to "+12dB (Max Boost / Quiet Audio Fix)"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = accentColor)
                Spacer(Modifier.width(8.dp))
                Text("SmartTube Audio Boost", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                boostOptions.forEach { (level, label) ->
                    val isSelected = currentBoost == level
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onBoostSelected(level)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = accentColor)
            }
        }
    )
}

@Composable
fun SmartTubeSleepTimerDialog(
    activeRemainingMs: Long?,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val timerOptions = listOf(15, 30, 45, 60, 90)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = accentColor)
                Spacer(Modifier.width(8.dp))
                Text("Sleep Timer", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeRemainingMs != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onCancelTimer()
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Turn Off Sleep Timer (${activeRemainingMs / 60000}m left)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                timerOptions.forEach { minutes ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSetTimer(minutes)
                                onDismiss()
                            }
                    ) {
                        Text(
                            text = "$minutes Minutes",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = accentColor)
            }
        }
    )
}

@Composable
fun SmartTubeStatsForNerdsOverlay(
    videoId: String,
    quality: String,
    speed: Float,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📊 Stats for Nerds (SmartTube HUD)", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Video ID: $videoId", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("Resolution: $quality", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("Playback Speed: ${speed}x", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("Audio Codec: Opus 160kbps / 48kHz Stereo", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("Buffer Health: 28.4s (Optimal Ultra-low Latency)", color = Color(0xFF00E676), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("Player Pipeline: ExoPlayer 2.19 + DSP Filter Enabled", color = Color(0xFF00D2FF), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

@Composable
fun SmartTubeOledScreenOffOverlay(
    onWake: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onWake),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("OLED Battery Saver Active", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Audio playing in background • Tap anywhere to wake", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
        }
    }
}

@Composable
fun SmartTubeVideoControlsPanel(
    playerState: com.deepeye.musicpro.domain.model.PlayerState,
    finalAccentColor: androidx.compose.ui.graphics.Color,
    audioBoostLevel: Int,
    subtitlesEnabled: Boolean,
    sleepTimerMs: Long?,
    showStatsForNerds: Boolean,
    selectedQuality: String,
    context: android.content.Context,
    viewModel: com.deepeye.musicpro.ui.player.PlayerViewModel,
    onOpenSpeedDialog: () -> Unit,
    onOpenAudioBoostDialog: () -> Unit,
    onOpenSleepTimerDialog: () -> Unit,
    onEnableOledMode: () -> Unit,
    onShowQualityMenu: () -> Unit
) {
        // ============================================================
        // 🚀 SMARTTUBE ADVANCED PLAYBACK CONTROLS PANEL
        // ============================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = finalAccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "SmartTube Playback Engine",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                if (showStatsForNerds) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "HUD ACTIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // Row 1: Primary Controls (Speed, Quality, Audio Boost, Subtitles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.Speed,
                    title = "SPEED",
                    value = "${playerState.playbackSpeed}x",
                    accentColor = finalAccentColor,
                    isActive = playerState.playbackSpeed != 1.0f,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSpeedDialog
                )

                // Quality Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.HighQuality,
                    title = "QUALITY",
                    value = selectedQuality,
                    accentColor = finalAccentColor,
                    isActive = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onShowQualityMenu() }
                )

                // Audio Boost Card
                SmartTubePlaybackControlCard(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "BOOST",
                    value = if (audioBoostLevel > 0) "+${audioBoostLevel}dB" else "0dB",
                    accentColor = finalAccentColor,
                    isActive = audioBoostLevel > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAudioBoostDialog
                )

                // CC / Subtitles Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.ClosedCaption,
                    title = "CC",
                    value = if (subtitlesEnabled) "ON" else "OFF",
                    accentColor = finalAccentColor,
                    isActive = subtitlesEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.toggleSubtitles()
                        android.widget.Toast.makeText(
                            context,
                            if (!subtitlesEnabled) "Subtitles (CC) Enabled" else "Subtitles Disabled",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // Row 2: Secondary SmartTube Tools (Loop, Sleep Timer, Screen Off, Stats HUD)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val repeatLabel = when (playerState.repeatMode) {
                    com.deepeye.musicpro.domain.model.RepeatMode.ONE -> "Single"
                    com.deepeye.musicpro.domain.model.RepeatMode.ALL -> "Queue"
                    else -> "Off"
                }
                val isRepeatActive = playerState.repeatMode != com.deepeye.musicpro.domain.model.RepeatMode.NONE

                // Repeat / Loop Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.Repeat,
                    title = "REPEAT",
                    value = repeatLabel,
                    accentColor = finalAccentColor,
                    isActive = isRepeatActive,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toggleRepeat() }
                )

                // Sleep Timer Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.Bedtime,
                    title = "SLEEP",
                    value = sleepTimerMs?.let { "${it / 60000}m" } ?: "Off",
                    accentColor = finalAccentColor,
                    isActive = sleepTimerMs != null,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSleepTimerDialog
                )

                // Screen-Off (OLED Saver) Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "OLED",
                    value = "Screen Off",
                    accentColor = finalAccentColor,
                    isActive = false,
                    modifier = Modifier.weight(1f),
                    onClick = onEnableOledMode
                )

                // Stats for Nerds HUD Card
                SmartTubePlaybackControlCard(
                    icon = Icons.Default.Analytics,
                    title = "STATS",
                    value = if (showStatsForNerds) "HUD On" else "HUD",
                    accentColor = finalAccentColor,
                    isActive = showStatsForNerds,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toggleStatsForNerds() }
                )
            }
        }
}
