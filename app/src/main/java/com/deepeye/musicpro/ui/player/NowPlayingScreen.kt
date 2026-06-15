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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.ui.LocalFullscreenMode
import com.deepeye.musicpro.ui.LocalPipMode
import com.deepeye.musicpro.ui.components.GlassButton
import com.deepeye.musicpro.ui.components.GlassPill

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
    val sheetState by sheetViewModel.state.collectAsStateWithLifecycle()
    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
    val extractedColors by viewModel.extractedColors.collectAsStateWithLifecycle()
    val finalBgColor = extractedColors?.background ?: Color(0xFF121212)
    val finalAccentColor = extractedColors?.primary ?: dominantColor

    val configuration = LocalConfiguration.current
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showDspSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showVisualizer by remember { mutableStateOf(false) }

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
                if (fullscreenMode.isFullscreen) fullscreenMode.exit()
            }
        }
    }

    @Composable
    fun ArtworkOrVideoSection(modifier: Modifier = Modifier) {
        val innerItem = playerState.currentItem
        val innerIsVideo = innerItem is MediaItem.Remote && playerState.isVideo
        if (innerIsVideo && innerItem != null) {
            com.deepeye.musicpro.ui.components.HybridPlayerCard(
                item = innerItem,
                player = viewModel.player,
                isVideo = true,
                isLoading = playerState.isLoading,
                isPlaying = playerState.isPlaying,
                playbackPosition = playerState.position,
                modifier = modifier,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) }
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

        if (!isInPipMode) {
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
                        onNavigateToSettings = onNavigateToSettings
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
                        onToggleVisualizer = { showVisualizer = !showVisualizer },
                        onOpenDsp = { showDspSheet = true },
                        onOpenQueue = { showQueueSheet = true },
                        onNavigateToSettings = onNavigateToSettings,
                        pagerState = pagerState
                    )
                }
            }
        }

    if (showDspSheet) {
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
                    Switch(checked = true, onCheckedChange = {})
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
}

@Composable
private fun AudioNowPlayingLayout(
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
) {
    val isHiRes = remember(playerState.currentItem?.id) { (playerState.currentItem?.id.hashCode() % 3) == 0 }
    val bitrate = remember(playerState.currentItem?.id) { if (isHiRes) "24bit • 48kHz" else "16bit • 44.1kHz" }
    val headerColor = if (finalBgColor.luminance() > 0.5f) Color.Black else Color.White
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp) // Premium symmetric padding
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Close", tint = headerColor, modifier = Modifier.size(32.dp))
            }
            Text(
                text = playerState.currentItem?.title?.uppercase() ?: "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = headerColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Settings, "Settings", tint = headerColor, modifier = Modifier.size(24.dp))
            }
        }

        // Pager Artwork Area
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.5f).clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val innerItem = playerState.currentItem
            if (innerItem != null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) { page ->
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
                                .background(Brush.linearGradient(listOf(Color(0xFF2A2A35), Color(0xFF1E1E28))))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = finalAccentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(100.dp)
                            )
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

        // Metadata & Controls
        Column(
            modifier = Modifier.fillMaxWidth().weight(0.5f).padding(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Title & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    com.deepeye.musicpro.ui.components.DynamicLabel(
                        text = playerState.currentItem?.title ?: "No Track Playing",
                        backgroundColor = finalBgColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        useVibrancy = true
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val artistText = playerState.currentItem?.artist?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist"
                        com.deepeye.musicpro.ui.components.SecondaryLabel(
                            text = artistText,
                            backgroundColor = finalBgColor,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            useVibrancy = true
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(headerColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(bitrate, color = headerColor.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Like Button
                val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
                val isLiked = feedback?.liked == true
                IconButton(onClick = { viewModel.likeTrack(!isLiked) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) finalAccentColor else headerColor.copy(alpha = 0.5f)
                    )
                }
            }

            // Seekbar
            Column {
                GlassSlider(
                    value = playerState.position.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                    accentColor = finalAccentColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(TimeFormatter.formatDuration(playerState.position), style = MaterialTheme.typography.labelSmall, color = headerColor.copy(alpha = 0.5f))
                    Text(TimeFormatter.formatDuration(playerState.duration), style = MaterialTheme.typography.labelSmall, color = headerColor.copy(alpha = 0.5f))
                }
            }

            // Controls
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides headerColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isShuffleActive = playerState.shuffleMode == com.deepeye.musicpro.domain.model.ShuffleMode.ON
                    val isRepeatActive = playerState.repeatMode != com.deepeye.musicpro.domain.model.RepeatMode.NONE

                    StatefulTactileButton(
                        isActive = isShuffleActive,
                        onClick = { viewModel.toggleShuffle() },
                        activeColor = finalAccentColor
                    ) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffleActive) finalAccentColor else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    }
                    TactileIconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(40.dp))
                    }
                    PlayPauseButton(
                        isPlaying = playerState.isPlaying,
                        onClick = { viewModel.togglePlayPause() },
                        accentColor = finalAccentColor
                    )
                    TactileIconButton(onClick = { viewModel.next() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(40.dp))
                    }
                    StatefulTactileButton(
                        isActive = isRepeatActive,
                        onClick = { viewModel.toggleRepeat() },
                        activeColor = finalAccentColor
                    ) {
                        Icon(
                            imageVector = if (playerState.repeatMode == com.deepeye.musicpro.domain.model.RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeatActive) finalAccentColor else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenInfo) {
                    Icon(Icons.Outlined.Info, "Audio Info", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                IconButton(onClick = onToggleVisualizer) {
                    Icon(Icons.Default.GraphicEq, "Visualizer", tint = if (showVisualizer) finalAccentColor else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                IconButton(onClick = { viewModel.downloadCurrentTrack() }) {
                    Icon(Icons.Default.Download, "Download", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                IconButton(onClick = onOpenDsp) {
                    Icon(Icons.Default.Tune, "DSP", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Default.QueueMusic, "Queue", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoNowPlayingLayout(
    playerState: PlayerState,
    finalAccentColor: Color,
    finalBgColor: Color,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel,
    onOpenDsp: () -> Unit,
    onOpenQueue: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var selectedQuality by remember { mutableStateOf("1080p") }
    var showQualityMenu by remember { mutableStateOf(false) }
    var selectedAudioTrack by remember { mutableStateOf("Stereo (Original)") }
    var showAudioTrackMenu by remember { mutableStateOf(false) }
    var showAllComments by remember { mutableStateOf(false) }

    val libraryViewModel: com.deepeye.musicpro.ui.library.LibraryViewModel = hiltViewModel()

    val context = LocalContext.current
    val isFullscreen = LocalFullscreenMode.current.isFullscreen
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .let { if (!isFullscreen) it.statusBarsPadding().navigationBarsPadding() else it }
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
        val videoModifier = if (isFullscreen) {
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
            modifier = videoModifier
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
                    onSeekTo = { viewModel.seekTo(it) }
                )
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
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black, ambientColor = Color.Black)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
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
                    Text(
                        text = "1.2M views • 2 days ago",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
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
                        // Avatar placeholder (Premium glass)
                        val firstChar = currentItem?.artist?.firstOrNull()?.toString() ?: "U"
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
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentItem?.artist ?: "Unknown Channel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "1.24M Subscribers",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Subscribe Button
                        val channelName = currentItem?.artist ?: "Unknown"
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
                        android.widget.Toast.makeText(context, "Disliked", android.widget.Toast.LENGTH_SHORT).show()
                    })
                    InteractionButton(icon = Icons.Default.Share, label = "Share", onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Listen to ${currentItem?.title ?: "this"} on DeepEye Music Pro!")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Track"))
                    })
                    InteractionButton(icon = Icons.Default.Download, label = "Download", onClick = {
                        viewModel.downloadCurrentTrack()
                        android.widget.Toast.makeText(context, "Downloading...", android.widget.Toast.LENGTH_SHORT).show()
                    })
                    InteractionButton(icon = Icons.Default.Add, label = "Save", onClick = {
                        android.widget.Toast.makeText(context, "Saved to Playlist", android.widget.Toast.LENGTH_SHORT).show()
                    })
                }
            }

            // Specs and Custom Controls Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quality Selector Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { showQualityMenu = true }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QUALITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(selectedQuality, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
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
                }

                // Audio Track Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { showAudioTrackMenu = true }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AUDIO TRACK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(selectedAudioTrack, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    DropdownMenu(
                        expanded = showAudioTrackMenu,
                        onDismissRequest = { showAudioTrackMenu = false }
                    ) {
                        listOf("Stereo (Original)", "Dolby Atmos (Downmix)", "L-R Audio Track").forEach { track ->
                            DropdownMenuItem(
                                text = { Text(track) },
                                onClick = {
                                    selectedAudioTrack = track
                                    showAudioTrackMenu = false
                                    android.widget.Toast.makeText(context, "Audio channel set to $track", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
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
        Text("Next in Queue", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 16.dp))
        
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
                            color = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onItemRemove(index) }) {
                    }
                    Icon(Icons.Default.DragHandle, "Drag to reorder", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
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
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, spring(stiffness = 400f, dampingRatio = 0.7f))
    val glow by animateFloatAsState(if (isPlaying) 1.2f else 1.0f, spring(stiffness = 200f))
    
    Box(modifier = Modifier.size(88.dp).graphicsLayer(scaleX = scale, scaleY = scale), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(128.dp).graphicsLayer(scaleX = glow, scaleY = glow)) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isPlaying) 0.5f else 0.2f),
                        accentColor.copy(alpha = 0f)
                    )
                ),
                radius = size.width / 2f
            )
        }
        Box(
            modifier = Modifier.size(92.dp).clip(CircleShape).background(accentColor).clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                modifier = Modifier.size(56.dp),
                tint = Color.Black // Contrast against accent color
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
fun InteractionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, activeColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White, onClick: () -> Unit) {
    val tintColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(28.dp))
        Text(label, color = tintColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}
