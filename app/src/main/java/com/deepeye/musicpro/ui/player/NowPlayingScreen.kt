package com.deepeye.musicpro.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.platform.LocalConfiguration
import com.deepeye.musicpro.core.utils.TimeFormatter
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.ui.LocalPipMode
import com.deepeye.musicpro.ui.LocalFullscreenMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToV4A: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val fftData by viewModel.fftData.collectAsStateWithLifecycle()
    val dominantColor by viewModel.dominantColor.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    var showTasteDetailDialog by remember { mutableStateOf(false) }
    
    // Fully responsive threshold mapping (Widescreen, Landscape, or Tablet)
    val isWideScreen = configuration.screenWidthDp >= 600 || 
                      (configuration.screenWidthDp >= 480 && configuration.screenHeightDp < 500)

    val isInPipMode = LocalPipMode.current
    val fullscreenMode = LocalFullscreenMode.current
    val isFullscreen = fullscreenMode.isFullscreen
    val isVideoMode = playerState.currentItem is MediaItem.Remote && playerState.isVideo
    val currentItem = playerState.currentItem

    val playerCard = remember {
        androidx.compose.runtime.movableContentOf { modifier: Modifier ->
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
                    modifier = modifier
                )
            } else {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (innerItem != null) {
                        AsyncImage(
                            model = innerItem.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f)
                                .blur(36.dp)
                                .alpha(0.65f)
                                .graphicsLayer {
                                    translationY = 15f
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(1f)
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
                        AsyncImage(
                            model = innerItem?.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

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
        }
    }

    // PiP or Fullscreen mode: show ONLY the video player filling the screen
    if ((isFullscreen || isInPipMode) && isVideoMode && playerState.currentItem != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            playerCard(Modifier.fillMaxSize())
        }
        return
    }

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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Navigation Bar
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

            Spacer(Modifier.height(16.dp))

            val isVideoMode = playerState.currentItem is com.deepeye.musicpro.domain.model.MediaItem.Remote && playerState.isVideo
            val currentItem = playerState.currentItem

            // Modular Composable elements mapped internally for responsive rendering
            @Composable
            fun ArtworkOrVideoSection(modifier: Modifier = Modifier) {
                playerCard(modifier)
            }

            @Composable
            fun TrackMetadataSection() {
                val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
                val tasteProfile by viewModel.tasteProfile.collectAsStateWithLifecycle()
                
                Column(modifier = Modifier.fillMaxWidth()) {
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
                    
                    Spacer(Modifier.height(8.dp))
                    
                    val item = playerState.currentItem
                    if (item != null) {
                        val tags = remember(item, feedback, tasteProfile) {
                            val list = mutableListOf<String>()
                            if (feedback?.liked == true) {
                                list.add("Liked Song")
                            }
                            
                            val isFavArtist = tasteProfile.favoriteArtists.any { it.equals(item.artist, ignoreCase = true) }
                            if (isFavArtist) {
                                list.add("Favorite Artist")
                            }
                            
                            val titleLower = item.title.lowercase()
                            val detectedLanguage = when {
                                titleLower.contains("hindi") -> "Hindi"
                                titleLower.contains("punjabi") -> "Punjabi"
                                titleLower.contains("bhojpuri") -> "Bhojpuri"
                                titleLower.contains("tamil") -> "Tamil"
                                titleLower.contains("telugu") -> "Telugu"
                                titleLower.contains("english") -> "English"
                                titleLower.contains("haryanvi") -> "Haryanvi"
                                titleLower.contains("bengali") -> "Bengali"
                                titleLower.contains("korean") -> "Korean"
                                else -> ""
                            }
                            if (detectedLanguage.isNotEmpty() && tasteProfile.preferredLanguages.contains(detectedLanguage)) {
                                list.add(detectedLanguage)
                            }
                            
                            if (list.isEmpty()) {
                                list.add("Personalized Mix")
                            }
                            list
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTasteDetailDialog = true }
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = tag.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
            @Composable
            fun FeedbackSection() {
                val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
                val isLiked = feedback?.liked == true
                val isBlocked = feedback?.dontPlayAgain == true

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = { viewModel.likeTrack(!isLiked) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isLiked) Color.White.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like Track",
                                tint = if (isLiked) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.blockTrack() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isBlocked) MaterialTheme.colorScheme.error
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isBlocked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Block Track",
                                tint = if (isBlocked) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (playerState.autoplayEnabled) Color.White.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (playerState.autoplayEnabled) Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.4f)))
                                            else SolidColor(Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .combinedClickable(
                                    onClick = { viewModel.toggleAutoplay() },
                                    onLongClick = {
                                        if (playerState.autoplayEnabled) {
                                            showTasteDetailDialog = true
                                        }
                                    }
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Autoplay Mode",
                                    tint = if (playerState.autoplayEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (playerState.autoplayEnabled) "AUTOPLAY: TASTE" else "AUTOPLAY OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playerState.autoplayEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.downloadCurrentTrack() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            @Composable
            fun GainBudgetSection() {
                val gainBudget by viewModel.gainBudget.collectAsStateWithLifecycle()
                com.deepeye.musicpro.ui.components.GainBudgetMeter(
                    budget = gainBudget,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            @Composable
            fun ProgressSliderSection() {
                Column {
                    Slider(
                        value = playerState.position.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier.fillMaxWidth()
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
            }

            @Composable
            fun ControlsSection() {
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
                        color = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Black
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
            }

            // Adaptive UI rendering branch based on screen size
            if (isWideScreen) {
                // ==========================================
                // 1. TWO-COLUMN RESPONSIVE LAYOUT (TABLETS / LANDSCAPE)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Artwork / Video Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkOrVideoSection(modifier = Modifier.fillMaxHeight(0.85f))
                    }

                    // Right Column: Details, Slider, and Action Controls
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TrackMetadataSection()
                        FeedbackSection()
                        GainBudgetSection()
                        ProgressSliderSection()
                        ControlsSection()
                    }
                }
            } else {
                // ==========================================
                // 2. STANDARD SCALED COLUMN LAYOUT (MOBILE PORTRAIT)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.weight(0.2f))
                    ArtworkOrVideoSection(modifier = Modifier.weight(3.5f, fill = false))
                    Spacer(Modifier.weight(0.2f))
                    
                    Column(
                        modifier = Modifier.weight(4f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TrackMetadataSection()
                        FeedbackSection()
                        GainBudgetSection()
                        ProgressSliderSection()
                        ControlsSection()
                    }
                    Spacer(Modifier.weight(0.2f))
                }
            }
        }

        val item = playerState.currentItem
        val feedback by viewModel.currentSongFeedback.collectAsStateWithLifecycle()
        val tasteProfile by viewModel.tasteProfile.collectAsStateWithLifecycle()

        if (showTasteDetailDialog && item != null) {
            AlertDialog(
                onDismissRequest = { showTasteDetailDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Personalization Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Here is how our taste algorithm scored this track for you:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val isFavArtist = tasteProfile.favoriteArtists.any { it.equals(item.artist, ignoreCase = true) }
                        val titleLower = item.title.lowercase()
                        val detectedLanguage = when {
                            titleLower.contains("hindi") -> "Hindi"
                            titleLower.contains("punjabi") -> "Punjabi"
                            titleLower.contains("bhojpuri") -> "Bhojpuri"
                            titleLower.contains("tamil") -> "Tamil"
                            titleLower.contains("telugu") -> "Telugu"
                            titleLower.contains("english") -> "English"
                            titleLower.contains("haryanvi") -> "Haryanvi"
                            titleLower.contains("bengali") -> "Bengali"
                            titleLower.contains("korean") -> "Korean"
                            else -> "English"
                        }
                        val isPrefLang = tasteProfile.preferredLanguages.contains(detectedLanguage)

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        ScoringRow(
                            title = "Preferred Language ($detectedLanguage)",
                            score = if (isPrefLang) "+3" else "0",
                            matched = isPrefLang,
                            icon = Icons.Default.Language
                        )
                        ScoringRow(
                            title = "Favorite Artist (${item.artist})",
                            score = if (isFavArtist) "+4" else "0",
                            matched = isFavArtist,
                            icon = Icons.Default.Person
                        )
                        ScoringRow(
                            title = "User Feedback (Like)",
                            score = if (feedback?.liked == true) "+5" else "0",
                            matched = feedback?.liked == true,
                            icon = Icons.Default.ThumbUp
                        )
                        ScoringRow(
                            title = "User Feedback (Quick Skip)",
                            score = if (feedback?.skippedQuickly == true) "-6" else "0",
                            matched = feedback?.skippedQuickly == true,
                            icon = Icons.Default.ThumbDown
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showTasteDetailDialog = false
                                onNavigateToSettings()
                            }
                        ) {
                            Text("Train Taste")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { showTasteDetailDialog = false }) {
                            Text("Close")
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ScoringRow(
    title: String,
    score: String,
    matched: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (matched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (matched) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Text(
            text = score,
            fontWeight = FontWeight.Bold,
            color = if (score.startsWith("+")) MaterialTheme.colorScheme.primary else if (score.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
