// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.ui.components.DynamicLabel
import com.deepeye.musicpro.ui.components.SecondaryLabel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.components.ShimmerBox
import com.deepeye.musicpro.ui.components.premium.PremiumHeroCard
import com.deepeye.musicpro.ui.components.premium.SplitMediaHero
import com.deepeye.musicpro.ui.components.premium.DotMatrixClock
import com.deepeye.musicpro.ui.theme.GlassBorder

@Composable
fun HomeHubScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToMusic: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onOpenV4A: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeHubViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
    recommendationViewModel: com.deepeye.musicpro.ui.home.RecommendationViewModel = hiltViewModel(),
) {
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val recs by recommendationViewModel.recommendations.collectAsStateWithLifecycle()
    val isRecsLoading by recommendationViewModel.isRefreshing.collectAsStateWithLifecycle()
    val dspEngineState by viewModel.isDspAttached.collectAsStateWithLifecycle()

    val isExpanded = windowSizeClass.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (isExpanded) Modifier.widthIn(max = 1200.dp) else Modifier,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = if (isExpanded) 32.dp else 0.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp,
                end = if (isExpanded) 32.dp else 0.dp,
                bottom = 260.dp, // Increased from 200.dp to ensure no overlap with Dock & Mini-Player
            ),
        ) {

            // 1b. Featured Premium Audio Hero Card (AEOS Premium Feature)
            val featuredMusic = feedState.featuredMusic
            if (featuredMusic != null) {
                item {
                    PremiumHeroCard(
                        title = featuredMusic.title,
                        subtitle = featuredMusic.artist,
                        imageUrl = featuredMusic.thumbnailUrl,
                        badge = "Featured Premium Audio",
                        onClick = {
                            viewModel.playMusic(featuredMusic)
                            onNavigateToMusic(featuredMusic.id)
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            // 3. Continue Listening rail (Phase 7)
            if (feedState.continueListening.isNotEmpty()) {
                item {
                    ContinueListeningRow(
                        items = feedState.continueListening,
                        onItemClick = { music ->
                            viewModel.playContinueListeningItem(music)
                            onNavigateToMusic(music.id)
                        },
                    )
                }
            }

            item {
                HorizontalDivider(
                    color = Color.White.copy(0.05f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // 5. Mood Chips (Phase 7)
            if (feedState.moodMixes.isNotEmpty()) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    android.util.Log.d("HomeHubScreen", "Calling MoodChipsRow with ${feedState.moodMixes.size} moods")
                    MoodChipsRow(
                        moods = feedState.moodMixes,
                        onMoodClick = { mood -> 
                            android.widget.Toast.makeText(context, "Generating playlist for ${mood.label}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.onMoodClick(mood.query) 
                        },
                    )
                }
            } else {
                item {
                    android.util.Log.d("HomeHubScreen", "MoodMixes is empty!")
                }
            }

            // 6. DSP Quick Panel (Phase 7)
            item {
                DspQuickPanel(
                    activePresetName = feedState.activeDspPreset,
                    isEngineAttached = dspEngineState == com.deepeye.musicpro.dsp.model.EngineState.ATTACHED ||
                        dspEngineState == com.deepeye.musicpro.dsp.model.EngineState.PROCESSING,
                    onOpenDsp = onOpenV4A,
                )
            }

            // 7. Local Library Resume (Phase 7)
            if (feedState.localResume.isNotEmpty()) {
                item {
                    LocalResumeRail(
                        items = feedState.localResume,
                        onItemClick = { music ->
                            viewModel.playLocalResume(music)
                            onNavigateToMusic(music.id)
                        },
                    )
                }
            }

            item {
                HorizontalDivider(
                    color = Color.White.copy(0.05f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // 8. Dynamic Recommendation Rows
            if (isRecsLoading && recs == null) {
                item { HomeRailShimmer(title = "Loading Recommendations...") }
                item { HomeRailShimmer(title = "") }
                item { HomeRailShimmer(title = "") }
            }

            recs?.let { result ->
                // Because you listened
                items(result.becauseYouListened) { row ->
                    com.deepeye.musicpro.ui.home.RecommendationRowUI(row, windowSizeClass) { video ->
                        playRecMusic(video, row.items, playerViewModel, onNavigateToMusic)
                    }
                }

                // Perfect for right now
                item {
                    com.deepeye.musicpro.ui.home.RecommendationRowUI(
                        result.perfectForNow,
                        windowSizeClass,
                        isHighlighted = true
                    ) { video ->
                        playRecMusic(video, result.perfectForNow.items, playerViewModel, onNavigateToMusic)
                    }
                }

                // Favorite artists
                items(result.favoriteArtists) { row ->
                    com.deepeye.musicpro.ui.home.RecommendationRowUI(row, windowSizeClass) { video ->
                        playRecMusic(video, row.items, playerViewModel, onNavigateToMusic)
                    }
                }

                // Trending
                item {
                    com.deepeye.musicpro.ui.home.RecommendationRowUI(result.trending, windowSizeClass) { video ->
                        playRecMusic(video, result.trending.items, playerViewModel, onNavigateToMusic)
                    }
                }

                // Genre dives
                items(result.genreDive) { row ->
                    com.deepeye.musicpro.ui.home.RecommendationRowUI(row, windowSizeClass) { video ->
                        playRecMusic(video, row.items, playerViewModel, onNavigateToMusic)
                    }
                }

                // Hidden gems
                item {
                    com.deepeye.musicpro.ui.home.RecommendationRowUI(
                        result.hiddenGems,
                        windowSizeClass,
                        isHighlighted = true
                    ) { video ->
                        playRecMusic(video, result.hiddenGems.items, playerViewModel, onNavigateToMusic)
                    }
                }
            }

            // 9. Offline fallback
            if (feedState.isOffline && !feedState.isLoading) {
                item {
                    OfflineFallbackCard(
                        onRetry = viewModel::loadFeed,
                    )
                }
            }
        }

        // Fixed BTC Ticker Overlay
        HomeGreetingHeader(onNavigateToSettings = onNavigateToSettings)
    }
}

@Composable
private fun HomeGreetingHeader(onNavigateToSettings: () -> Unit) {
    var btcPrice by remember { mutableStateOf("Fetching BTC...") }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                btcPrice = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val url = java.net.URL("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val priceStr = response.substringAfter("\"price\":\"").substringBefore("\"")
                    val formattedPrice = String.format("%,.2f", priceStr.toDoubleOrNull() ?: 0.0)
                    "₿ $$formattedPrice"
                }
            } catch (e: Exception) {
                if (btcPrice == "Fetching BTC...") {
                    btcPrice = "₿ ---"
                }
            }
            kotlinx.coroutines.delay(5000) // Update every 5 seconds
        }
    }

    Box(
        modifier = Modifier
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)) // Clear dock background
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp), // Increased vertical padding for bigger text
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFFFD700))) { // Gold for Bitcoin icon
                    append("₿ ")
                }
                withStyle(SpanStyle(color = Color(0xFF00E676))) { // Neon Green for price
                    append(btcPrice.replace("₿ ", ""))
                }
            },
            style = MaterialTheme.typography.displaySmall, // Much bigger text
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun HomeVideoRail(
    title: String,
    items: List<HomeVideoItem>,
    onClick: (HomeVideoItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(items, key = { it.id }) { video ->
                VideoCard(item = video, onClick = onClick)
            }
        }
    }
}

@Composable
private fun ShortsRail(
    items: List<HomeVideoItem>,
    onClick: (HomeVideoItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "📱 Shorts",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(items, key = { it.id }) { short ->
                VideoCard(
                    item = short,
                    onClick = onClick,
                    modifier = Modifier.width(160.dp), // Narrower for shorts
                )
            }
        }
    }
}

@Composable
private fun HomeMusicRail(
    title: String,
    items: List<HomeMusicItem>,
    onClick: (HomeMusicItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            items(items, key = { it.id }) { music ->
                val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f)
                )

                Box(
                    modifier =
                    Modifier
                        .width(160.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onClick(music) }
                        .padding(8.dp),
                ) {
                    Column {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                        ) {
                            AsyncImage(
                                model = music.thumbnailUrl,
                                contentDescription = music.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = music.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = music.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRailShimmer(title: String) {
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, modifier = Modifier.padding(horizontal = 20.dp), color = Color.White)
        Row(Modifier.padding(horizontal = 20.dp)) {
            repeat(3) {
                ShimmerBox(Modifier.size(140.dp))
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun OfflineFallbackCard(onRetry: () -> Unit) {
    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(48.dp),
            )
            Text(
                "Offline Mode",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE0E0E0),
            )
            Text(
                "YouTube content unavailable.\nLocal library still works!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B3FE4),
                ),
            ) {
                Text("Retry")
            }
        }
    }
}

private fun playRecMusic(
    video: com.deepeye.musicpro.domain.recommendation.VideoItem,
    contextList: List<com.deepeye.musicpro.domain.recommendation.VideoItem>,
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel,
    onNavigateToMusic: (String) -> Unit,
) {
    val mediaItems =
        contextList.map {
            com.deepeye.musicpro.domain.model.MediaItem.Remote(
                id = it.videoId,
                title = it.title,
                artist = it.artist,
                artworkUri = android.net.Uri.parse("https://i.ytimg.com/vi/${it.videoId}/mqdefault.jpg"),
                duration = 180000L, // Mock duration
                isVideo = false,
            )
        }
    val index = contextList.indexOfFirst { it.videoId == video.videoId }
    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
    onNavigateToMusic(video.videoId)
}
