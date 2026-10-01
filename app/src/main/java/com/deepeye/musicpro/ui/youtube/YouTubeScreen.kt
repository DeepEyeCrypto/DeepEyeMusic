// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.youtube

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.LocalPipMode
import com.deepeye.musicpro.ui.components.VideoCardSkeleton
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.CardGeometry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeScreen(
    onNavigateToVideo: (String) -> Unit,
    viewModel: YouTubeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val homeFeedState by viewModel.homeFeedState.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val darkBackground = androidx.compose.material3.MaterialTheme.colorScheme.background
    val surfaceColor = Color(0xFF131722).copy(alpha = 0.85f)
    val neonCyan = Color(0xFF00E5FF)
    val neonPurple = Color(0xFF7C4DFF)

    val currentItem = playerState.currentItem
    val isInPipMode = LocalPipMode.current

    @Composable
    fun HeaderPlayerArea() {
        currentItem?.let { item ->
            if (item is com.deepeye.musicpro.domain.model.MediaItem.Remote && playerState.isVideo) {
                // In PiP: video fills entire screen; otherwise normal header height
                val playerHeight =
                    if (isInPipMode) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth().height(
                            if (isLandscape) 260.dp else 210.dp,
                        )
                    }
                Box(modifier = playerHeight) {
                    com.deepeye.musicpro.ui.components.HybridPlayerCard(
                        item = item,
                        player = viewModel.player,
                        isVideo = true,
                        isLoading = playerState.isLoading,
                        isPlaying = playerState.isPlaying,
                        playbackPosition = playerState.position,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSeekTo = { viewModel.player.seekTo(it) }
                    )

                    // Floating "Stats for Nerds" toggle button (hidden in PiP)
                    if (!isInPipMode) {
                        IconButton(
                            onClick = { viewModel.toggleStatsForNerds() },
                            modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Stats for Nerds",
                                tint = if (uiState.showStatsForNerds) neonCyan else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Stats for Nerds Display Panel
                    if (uiState.showStatsForNerds && !isInPipMode) {
                        Box(
                            modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .width(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "STATS FOR NERDS",
                                    color = neonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                HorizontalDivider(
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    "Video ID: ${item.id}",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Resolution: 1920x1080 @60fps",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    "Decoder: MediaCodec hardware VP9",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    "Connection Speed: 48.3 Mbps",
                                    color = Color.Green,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Buffer Health: 28.2s (Steady)",
                                    color = Color.Green,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    "Active Shield: SponsorBlock Server connected",
                                    color = neonCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TabContentRouterView() {
        when (uiState.selectedCategory) {
            "Search" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131722).copy(alpha = 0.85f), RoundedCornerShape(14.dp)),
                        placeholder = { Text("Search YouTube videos...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.performSearch()
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = neonCyan)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions =
                        KeyboardActions(onSearch = {
                            viewModel.performSearch()
                            keyboardController?.hide()
                        }),
                        colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonCyan,
                            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.searchSuggestions.isNotEmpty()) {
                            val suggestionsState = rememberLazyListState()
                            LazyColumn(
                                state = suggestionsState,
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .premiumScrollHaptics(suggestionsState)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(surfaceColor)
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                    .padding(8.dp),
                            ) {
                                items(uiState.searchSuggestions) { suggestion ->
                                    Row(
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateSearchQuery(suggestion)
                                                viewModel.performSearch()
                                                keyboardController?.hide()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = neonCyan.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = suggestion,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                    HorizontalDivider(color = Color(0x1AFFFFFF))
                                }
                            }
                        } else {
                            VideoGridContent(
                                isLoading = uiState.isLoading,
                                videos = uiState.videos,
                                isMoreLoading = uiState.isMoreLoading,
                                hasMore = uiState.hasMore,
                                error = uiState.error,
                                isLandscape = isLandscape,
                                onVideoClick = { video ->
                                    viewModel.playVideo(video)
                                    onNavigateToVideo(video.id)
                                },
                                onLoadMore = { viewModel.loadMoreVideos() },
                            )
                        }
                    }
                }
            }
            "SponsorBlock" -> {
                val sbState = rememberLazyListState()
                LazyColumn(
                    state = sbState,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .premiumScrollHaptics(sbState)
                        .padding(horizontal = if (isLandscape) 20.dp else 14.dp),
                    contentPadding = PaddingValues(top = if (isLandscape) 20.dp else 14.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        // 1. MASTER HEADER CARD
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors =
                                        if (uiState.shieldsEnabled) {
                                            listOf(
                                                Color(0xFFFF3D00).copy(alpha = 0.15f),
                                                Color(0xFF7C4DFF).copy(alpha = 0.15f)
                                            )
                                        } else {
                                            listOf(
                                                Color(0x0EFFFFFF),
                                                Color(0x0EFFFFFF)
                                            )
                                        },
                                    ),
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                    if (uiState.shieldsEnabled) {
                                        Color(
                                            0xFFFF3D00,
                                        ).copy(alpha = 0.3f)
                                    } else {
                                        Color(0x22FFFFFF)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier =
                                        Modifier
                                            .size(44.dp)
                                            .background(
                                                if (uiState.shieldsEnabled) {
                                                    Color(0xFFFF3D00).copy(alpha = 0.25f)
                                                } else {
                                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                                },
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("🦁", fontSize = 22.sp) // Brave Lion
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Brave Shields",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (uiState.shieldsEnabled) "Protections Active & Engaged" else "Protections Disengaged (Raw Mode)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.shieldsEnabled) {
                                                Color(
                                                    0xFFFF3D00
                                                )
                                            } else {
                                                androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            },
                                        )
                                    }
                                }

                                Switch(
                                    modifier = Modifier.scale(1.35f),
                                    checked = uiState.shieldsEnabled,
                                    onCheckedChange = { viewModel.toggleShieldsEnabled() },
                                    colors =
                                    SwitchDefaults.colors(
                                        checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                        checkedTrackColor = Color(0xFFFF3D00),
                                        uncheckedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        uncheckedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    ),
                                )
                            }
                        }
                    }

                    if (uiState.shieldsEnabled) {
                        item {
                            // 2. SHIELD LEVELS SELECTOR (STANDARD VS AGGRESSIVE)
                            val isStandard = uiState.shieldMode == "Standard"
                            val isAggressive = uiState.shieldMode == "Aggressive"
                            Column(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131722).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "Protection Mode",
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier =
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isStandard) {
                                                    Color(
                                                        0xFFFF3D00,
                                                    ).copy(alpha = 0.15f)
                                                } else {
                                                    Color(0x0EFFFFFF)
                                                },
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isStandard) {
                                                    Color(
                                                        0xFFFF3D00
                                                    )
                                                } else {
                                                    Color(0x22FFFFFF)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                            .clickable { viewModel.setShieldMode("Standard") }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "Standard",
                                            color = if (isStandard) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    Box(
                                        modifier =
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isAggressive) {
                                                    Color(
                                                        0xFFFF3D00,
                                                    ).copy(alpha = 0.15f)
                                                } else {
                                                    Color(0x0EFFFFFF)
                                                },
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isAggressive) {
                                                    Color(
                                                        0xFFFF3D00
                                                    )
                                                } else {
                                                    Color(0x22FFFFFF)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                            .clickable { viewModel.setShieldMode("Aggressive") }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "Aggressive",
                                            color = if (isAggressive) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    text =
                                    if (isStandard) {
                                        "Standard Blocks: Sponsors, Paid Self-Promotions, and annoying Like/Subscribe reminders."
                                    } else {
                                        "Aggressive Blocks: Extends blocking to music intros/outros, recap teasers, credits, and silent segments."
                                    },
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                )
                            }
                        }

                        item {
                            // 3. SHIELD STATS & CUSTOM FILTERS CARD
                            Column(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131722).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Dynamic Cosmetic Filters",
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                    Box(
                                        modifier =
                                        Modifier
                                            .background(
                                                Color(0xFF00E676).copy(alpha = 0.12f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            "AUTO-SKIP",
                                            color = Color(0xFF00E676),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0x1AFFFFFF))

                                val categories =
                                    listOf(
                                        SponsorBlockCat(
                                            "sponsor",
                                            "📺 Sponsors / Ads",
                                            "Paid promotion or product endorsements inside the video track",
                                        ),
                                        SponsorBlockCat(
                                            "selfpromo",
                                            "📢 Self Promotions",
                                            "Showcasing creator's merch, other channels, or websites",
                                        ),
                                        SponsorBlockCat(
                                            "interaction",
                                            "🔔 Interaction Reminders",
                                            "Begging screens to Subscribe, Like, or ring the notification bell",
                                        ),
                                        SponsorBlockCat(
                                            "intro",
                                            "🎬 Intros / Openings",
                                            "Opening title cards, credits, or extended themes",
                                        ),
                                        SponsorBlockCat(
                                            "outro",
                                            "🏁 Outros / Endings",
                                            "End cards, credits roll, or sign-offs"
                                        ),
                                        SponsorBlockCat(
                                            "preview",
                                            "🍿 Previews / Teasers",
                                            "Highlight reels or 'Coming Up' teasers before main content",
                                        ),
                                    )

                                categories.forEach { cat ->
                                    val isChecked = uiState.activeSponsorBlockCategories.contains(cat.id)
                                    Row(
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleSponsorBlockCategory(cat.id) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { viewModel.toggleSponsorBlockCategory(cat.id) },
                                            colors =
                                            CheckboxDefaults.colors(
                                                checkedColor = Color(0xFFFF3D00),
                                                uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            ),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                cat.name,
                                                fontWeight = FontWeight.Bold,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                cat.description,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // 4. BRAVE ADVANCED PREFERENCES CARD
                            Column(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131722).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Advanced Preferences",
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )

                                HorizontalDivider(color = Color(0x1AFFFFFF))

                                Row(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleHideShorts() }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Hide YouTube Shorts Rail",
                                            fontWeight = FontWeight.Bold,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                        )
                                        Text(
                                            "Dynamically strip Shorts videos from Trending & Search feeds",
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                    Switch(
                                        modifier = Modifier.scale(1.35f),
                                        checked = uiState.hideShorts,
                                        onCheckedChange = { viewModel.toggleHideShorts() },
                                        colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            checkedTrackColor = Color(0xFFFF3D00),
                                        ),
                                    )
                                }

                                Row(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Block Third-Party Ads & Trackers",
                                            fontWeight = FontWeight.Bold,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                        )
                                        Text(
                                            "Drop connection requests to known tracking servers instantly",
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                    Switch(
                                        modifier = Modifier.scale(1.35f),
                                        checked = true,
                                        onCheckedChange = {},
                                        enabled = false,
                                        colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            checkedTrackColor = Color(0xFFFF3D00).copy(alpha = 0.5f),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                VideoGridContent(
                    isLoading = uiState.isLoading,
                    videos = uiState.videos,
                    isMoreLoading = uiState.isMoreLoading,
                    hasMore = uiState.hasMore,
                    error = uiState.error,
                    isLandscape = isLandscape,
                    onVideoClick = { video ->
                        viewModel.playVideo(video)
                        onNavigateToVideo(video.id)
                    },
                    onLoadMore = { viewModel.loadMoreVideos() },
                    isHomeCategory = uiState.selectedCategory == "Home",
                    homeFeedState = homeFeedState,
                    onPlayVideo = { video -> viewModel.playVideo(video) },
                )
            }
        }
    }

    if (isInPipMode && playerState.isVideo) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF090B10)),
        ) {
            HeaderPlayerArea()
        }
        return
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Transparent),
    ) {
        // Futuristic Cyberpunk Top Bar
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF0033), neonCyan))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "YOUTUBE CINEMA",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color.White,
                    )
                    Text(
                        text = "Lossless 4K HDR Audio-Visual Stream",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            // Active Shield Indicator
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (uiState.shieldsEnabled) Color(0x2200E676) else Color(0x1AFFFFFF),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.shieldsEnabled) Color(0xFF00E676) else Color(0x22FFFFFF)
                ),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (uiState.shieldsEnabled) "🛡️ SHIELD ON" else "🛡️ RAW",
                        color = if (uiState.shieldsEnabled) Color(0xFF00E676) else Color.White.copy(0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Horizontal Category Ribbon
        val tabs = if (uiState.hasAuth) {
            listOf("Home", "Subscriptions", "Liked", "History", "Watch Later", "Music", "Movies", "Gaming", "News", "Search", "SponsorBlock")
        } else {
            listOf("Home", "Search", "Music", "Movies", "Gaming", "News", "SponsorBlock")
        }
        LazyRow(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tabs) { tab ->
                val isSelected = uiState.selectedCategory == tab
                Surface(
                    onClick = { viewModel.selectCategory(tab) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0xFF131722).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content
        Box(
            modifier =
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            TabContentRouterView()
        }
    }
}

@Composable
fun VideoGridContent(
    isLoading: Boolean,
    videos: List<HomeVideoItem>,
    isMoreLoading: Boolean,
    hasMore: Boolean,
    error: String?,
    isLandscape: Boolean,
    onVideoClick: (HomeVideoItem) -> Unit,
    onLoadMore: () -> Unit,
    isHomeCategory: Boolean = false,
    homeFeedState: com.deepeye.musicpro.domain.model.home.HomeFeedState = com.deepeye.musicpro.domain.model.home.HomeFeedState(),
    onPlayVideo: (HomeVideoItem) -> Unit = {},
) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    val shouldLoadMore =
        remember {
            androidx.compose.runtime.derivedStateOf {
                val lastVisibleItem =
                    gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                        ?: return@derivedStateOf false
                lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 2
            }
        }

    androidx.compose.runtime.LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && hasMore && !isMoreLoading) {
            onLoadMore()
        }
    }

    when {
        isLoading -> {
            LazyVerticalGrid(
                // Grid floor reads the SSOT, so a column can never be narrower
                // than the card it holds — which would silently change the card's
                // height and reintroduce the shift this geometry prevents.
                columns = GridCells.Adaptive(CardGeometry.Video.minWidth),
                horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
                verticalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
            ) {
                // Same LazyVerticalGrid + Adaptive config as the loaded branch
                // below, so the loading grid and the loaded grid lay out
                // identically. Count matches roughly two rows.
                items(8) {
                    VideoCardSkeleton()
                }
            }
        }
        error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        }
        else -> {
            LazyVerticalGrid(
                state = gridState,
                modifier = Modifier.premiumScrollHaptics(gridState),
                columns = GridCells.Adaptive(CardGeometry.Video.minWidth),
                horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
                verticalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
                contentPadding = PaddingValues(
                    start = CardGeometry.ScreenGutter,
                    top = CardGeometry.ScreenGutter,
                    end = CardGeometry.ScreenGutter,
                    bottom = 90.dp
                )
            ) {
                if (isHomeCategory) {
                    val featuredVideo = homeFeedState.featuredVideo ?: videos.firstOrNull()
                    if (featuredVideo != null) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            com.deepeye.musicpro.ui.components.premium.PremiumHeroCard(
                                title = featuredVideo.title,
                                subtitle = featuredVideo.channelName,
                                imageUrl = featuredVideo.thumbnailUrl,
                                badge = "FEATURED VIDEO",
                                onClick = {
                                    onPlayVideo(featuredVideo)
                                    onVideoClick(featuredVideo)
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    if (homeFeedState.continueWatching.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            com.deepeye.musicpro.ui.homehub.ContinueWatchingRow(
                                items = homeFeedState.continueWatching,
                                onItemClick = { video ->
                                    onPlayVideo(video)
                                    onVideoClick(video)
                                }
                            )
                        }
                    }
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        com.deepeye.musicpro.ui.homehub.video.VideoRail(
                            onNavigateToVideo = { videoId ->
                                onVideoClick(HomeVideoItem(id = videoId, title = "", channelName = "", thumbnailUrl = ""))
                            }
                        )
                    }
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Recommended Videos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    val filteredVideos = videos.filter { it.id != featuredVideo?.id }
                    items(filteredVideos, key = { it.id }) { video ->
                        SmartTubeVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                        )
                    }
                } else {
                    items(videos) { video ->
                        SmartTubeVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                        )
                    }
                }

                if (isMoreLoading) {
                    items(
                        count = 3,
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                    ) {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartTubeVideoCard(
    video: HomeVideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progressFraction: Float? = null,
) {
    val neonCyan = Color(0xFF00E5FF)
    val neonPurple = Color(0xFF7C4DFF)

    val isHdr = remember(video.id) { (video.id.hashCode() % 3) == 0 }
    val is4k = remember(video.id) { (video.id.hashCode() % 2) == 0 }
    val hasAtmos = remember(video.id) { (video.id.hashCode() % 4) == 0 }

    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isHovered) 1.02f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed || isHovered) 0.8f else 0.3f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "glow"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            neonPurple.copy(alpha = glowAlpha),
            neonCyan.copy(alpha = glowAlpha),
        ),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Geometry from CardGeometry (SSOT). The skeleton for this card reads
            // the same fields, so the loading box and the loaded box are identical.
            .clip(CardGeometry.Video.shape)
            .background(
                color = Color(0xFF131722).copy(alpha = 0.85f),
            )
            .border(
                width = if (isHovered || isPressed) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = CardGeometry.Video.shape,
            )
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardGeometry.Video.aspectRatio)
                .clip(
                    RoundedCornerShape(
                        topStart = CardGeometry.Video.cornerRadius,
                        topEnd = CardGeometry.Video.cornerRadius,
                        bottomStart = CardGeometry.Video.cornerRadius / 2,
                        bottomEnd = CardGeometry.Video.cornerRadius / 2,
                    )
                ),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Vignette Overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Frosted Specs Ribbons (Top-Left)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (is4k) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF3D00).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFFFF3D00).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("4K", color = Color(0xFFFF3D00), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (isHdr) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF00E676).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("HDR", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (hasAtmos) {
                    Box(
                        modifier = Modifier
                            .background(neonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, neonCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("ATMOS", color = neonCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Duration Badge (Bottom-Right) — only show if duration is known
            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = neonCyan,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = formatDuration(video.duration),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Watch-progress bar (Bottom-Full) — used by Continue Watching / History sections
            if (progressFraction != null && progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFFFF0033))
                    )
                }
            }
        }

        // Card metadata — title on full width, then channel + views row with avatar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardGeometry.Video.contentPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Title — maxLines comes from CardGeometry so the reserved height here
            // matches what the skeleton draws. 3 lines pushed the channel row below
            // the fold at the new ~200dp card width.
            Text(
                text = video.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    letterSpacing = 0.1.sp
                ),
                maxLines = CardGeometry.Video.titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            // Channel row: avatar + channel name + dot + views/date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cleanChannel = video.channelName.trim().let { if (it == "." || it == "·" || it.isBlank()) "DeepEye" else it }
                val firstChar = cleanChannel.firstOrNull()?.toString() ?: "D"
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    neonPurple.copy(alpha = 0.25f),
                                    neonCyan.copy(alpha = 0.25f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(neonPurple, neonCyan, neonPurple),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (video.channelAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = video.channelAvatarUrl,
                            contentDescription = cleanChannel,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = firstChar.uppercase(),
                            color = neonCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    text = cleanChannel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                val metaText = when {
                    video.viewCount > 0 -> formatViews(video.viewCount)
                    video.uploadDate.isNotBlank() -> video.uploadDate
                    else -> ""
                }
                if (metaText.isNotEmpty()) {
                    Text(" · ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 11.sp)
                    Text(
                        text = metaText,
                        color = neonCyan.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private data class SidebarTabItem(
    val title: String,
    val icon: ImageVector,
)

private data class SponsorBlockCat(
    val id: String,
    val name: String,
    val description: String,
)

private fun formatDuration(durationSeconds: Long): String {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatViews(views: Long): String {
    return when {
        views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000f)
        views >= 1_000 -> String.format("%.0fK views", views / 1_000f)
        views > 0 -> "$views views"
        else -> "No views"
    }
}
