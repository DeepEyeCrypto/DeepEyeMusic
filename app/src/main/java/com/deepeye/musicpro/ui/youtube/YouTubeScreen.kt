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
import com.deepeye.musicpro.ui.components.ShimmerBox
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics

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
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    val neonCyan = Color(0xFF00D2FF)
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
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Resolution: 1920x1080 @60fps",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    "Decoder: MediaCodec hardware VP9",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    "Connection Speed: 48.3 Mbps",
                                    color = Color.Green,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Buffer Health: 28.2s (Steady)",
                                    color = Color.Green,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    "Active Shield: SponsorBlock Server connected",
                                    color = neonCyan,
                                    fontSize = 9.sp,
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
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(14.dp)),
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
                                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
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
                                    HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                                                androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                                androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
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
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
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
                                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                                },
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isStandard) {
                                                    Color(
                                                        0xFFFF3D00
                                                    )
                                                } else {
                                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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
                                            fontSize = 12.sp,
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
                                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                                },
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isAggressive) {
                                                    Color(
                                                        0xFFFF3D00
                                                    )
                                                } else {
                                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
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
                                            fontSize = 12.sp,
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
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
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
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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
                                                fontSize = 9.sp
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
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Advanced Preferences",
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )

                                HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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
                                            fontSize = 12.sp,
                                        )
                                        Text(
                                            "Dynamically strip Shorts videos from Trending & Search feeds",
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontSize = 9.sp,
                                        )
                                    }
                                    Switch(
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
                                            fontSize = 12.sp,
                                        )
                                        Text(
                                            "Drop connection requests to known tracking servers instantly",
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontSize = 9.sp,
                                        )
                                    }
                                    Switch(
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

    if (isLandscape) {
        // ==========================================
        // 1. CINEMATIC LANDSCAPE SIDEBAR MODE
        // ==========================================

        // In PiP mode: show ONLY the video player, nothing else
        if (isInPipMode && playerState.isVideo) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surface),
            ) {
                HeaderPlayerArea()
            }
            return
        }

        Row(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Color.Transparent),
        ) {
            Column(
                modifier =
                Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(Color.Transparent)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Logo
                Box(
                    modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(neonPurple, neonCyan))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                val tabs =
                    listOf(
                        SidebarTabItem("Home", Icons.Default.Home),
                        SidebarTabItem("Search", Icons.Default.Search),
                        SidebarTabItem("Music", Icons.Default.PlayArrow),
                        SidebarTabItem("Gaming", Icons.Default.Star),
                        SidebarTabItem("News", Icons.Default.Info),
                        SidebarTabItem("SponsorBlock", Icons.Default.Settings),
                    )

                tabs.forEach { tab ->
                    val isSelected = uiState.selectedCategory == tab.title
                    Column(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectCategory(tab.title) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) neonCyan else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) neonCyan else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            Box(
                modifier =
                Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            )

            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // HeaderPlayerArea() // Removed as per user request
                Box(
                    modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    TabContentRouterView()
                }
            }
        }
    } else {
        // ==========================================
        // 2. ADAPTIVE PORTRAIT MOBILE MODE (STUNNING!)
        // ==========================================

        // In PiP mode: show ONLY the video player, nothing else
        if (isInPipMode && playerState.isVideo) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surface),
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
            // Header Bar
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(neonPurple, neonCyan))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "SmartTube Edition",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Active Shield indicator
                Box(
                    modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Green.copy(alpha = 0.12f))
                        .border(0.5.dp, Color.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "SHIELD ON",
                        color = Color.Green,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Top Horizontal category ribbon
            val tabs = listOf("Home", "Search", "Music", "Gaming", "News", "SponsorBlock")
            LazyRow(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tabs) { tab ->
                    val isSelected = uiState.selectedCategory == tab
                    Box(
                        modifier =
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) {
                                    neonCyan.copy(alpha = 0.15f)
                                } else {
                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                },
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) neonCyan else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .clickable { viewModel.selectCategory(tab) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) neonCyan else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(top = 10.dp),
            )

            // Content
            Column(
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // HeaderPlayerArea() // Removed as per user request
                Box(
                    modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    TabContentRouterView()
                }
            }
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
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(6) {
                    ShimmerBox(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16 / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                    )
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
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 180.dp)
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    val filteredVideos = videos.filter { it.id != featuredVideo?.id }
                    items(filteredVideos) { video ->
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
                                color = Color(0xFF00D2FF),
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
) {
    val neonCyan = Color(0xFF00D2FF)
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
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (isHovered || isPressed) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(16.dp),
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
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp)),
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
                        Text("4K", color = Color(0xFFFF3D00), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (isHdr) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF00E676).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("HDR", color = Color(0xFF00E676), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (hasAtmos) {
                    Box(
                        modifier = Modifier
                            .background(neonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, neonCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("ATMOS", color = neonCyan, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Duration Badge (Bottom-Right)
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
                        imageVector = Icons.Default.PlayArrow,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val firstChar = video.channelName.firstOrNull()?.toString() ?: "Y"
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(neonPurple, neonCyan, neonPurple),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = firstChar.uppercase(),
                    color = neonCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = video.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        letterSpacing = 0.1.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = video.channelName,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 10.sp)
                    Text(
                        text = formatViews(video.viewCount),
                        color = neonCyan.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
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
        else -> "128K views"
    }
}
