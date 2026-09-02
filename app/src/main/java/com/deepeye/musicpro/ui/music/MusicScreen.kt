// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SpatialAudioOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.model.Song
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.ui.components.ShimmerBox
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.*

// ─── Premium Color Tokens ────────────────────────────────────────────────────
private val CardSurface = Color(0xFF12121A)
private val CardSurfaceElevated = Color(0xFF1A1A26)
private val CyanAccent = Color(0xFF00E5FF)
private val VioletAccent = Color(0xFF7C4DFF)
private val GoldBadge = Color(0xFFFFD54F)
private val EmeraldBadge = Color(0xFF00E676)
private val HiResBadge = Color(0xFFFFB300)
private val FlacBadge = Color(0xFF00E676)
private val DspColor = Color(0xFF00D2FF)
private val V4aColor = Color(0xFF7C4DFF)
private val ShineWhite = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onNavigateToNowPlaying: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onConnectAccount: () -> Unit = {},
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discovery", "Library")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RichBlack.copy(alpha = 0.95f),
                                RichBlack.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                // Premium Header Section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Row: Title and Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Music",
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimary
                            )
                        }

                        // Refresh button
                        if (selectedTab == 0) {
                            IconButton(onClick = { viewModel.refreshPersonalizedFeed() }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                            }
                        } else {
                            IconButton(onClick = { viewModel.syncLibrary() }) {
                                Icon(Icons.Default.Refresh, "Sync Library", tint = TextSecondary)
                            }
                        }
                    }

                    // Premium Search Bar (Clickable Entry Point)
                    PremiumSearchBar(onClick = onNavigateToSearch)
                    Spacer(Modifier.height(8.dp))
                }

                // YouTube account connectivity prompt — lets the user connect so the
                // music feed is fetched from their account (by YouTube id).
                if (!uiState.hasAuth) {
                    MusicAccountBanner(onConnect = onConnectAccount)
                    Spacer(Modifier.height(4.dp))
                }

                // Premium segmented tab bar
                PremiumTabBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(Modifier.height(4.dp))
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> DiscoveryTab(
                    uiState,
                    viewModel,
                    onNavigateToNowPlaying,
                    onConnectAccount,
                    paddingValues,
                )
                1 -> LibraryTab(uiState, viewModel, onNavigateToNowPlaying, paddingValues)
            }
        }
    }
}
// ─── YouTube Account Connect Banner ─────────────────────────────────────────
/**
 * Shown when the user hasn't connected their YouTube account yet. Tapping it opens
 * the device-auth login so music recommendations can be fetched from the account.
 */
@Composable
private fun MusicAccountBanner(onConnect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accountBannerScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ElectricViolet.copy(alpha = 0.22f),
                        NeonCyan.copy(alpha = 0.12f)
                    )
                )
            )
            .border(
                0.5.dp,
                Brush.horizontalGradient(
                    listOf(ElectricViolet.copy(alpha = 0.45f), NeonCyan.copy(alpha = 0.3f))
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onConnect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Connect YouTube account",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Personalize your Discovery and fetch music from your account by ID",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Premium Tab Bar ─────────────────────────────────────────────────────────
@Composable
private fun PremiumTabBar(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "tabAlpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(
                            listOf(
                                ElectricViolet.copy(alpha = 0.35f),
                                NeonCyan.copy(alpha = 0.2f)
                            )
                        ) else Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.6f),
                                    NeonCyan.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) else Modifier.border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (index == 0) Icons.Rounded.SpatialAudioOff else Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) NeonCyan else TextTertiary
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ─── Discovery Tab ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoveryTab(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: (String) -> Unit,
    onConnectAccount: () -> Unit,
    paddingValues: PaddingValues,
) {
    val feed = uiState.personalizedFeed

    PullToRefreshBox(
        isRefreshing = feed.isRefreshing,
        onRefresh = { viewModel.refreshPersonalizedFeed() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            when {
                feed.isLoading && feed.sections.isEmpty() -> {
                    DiscoveryLoadingSkeleton()
                }

                feed.sections.isEmpty() && feed.globalError != null -> {
                    DiscoveryGlobalError(
                        message = feed.globalError,
                        onRetry = { viewModel.refreshPersonalizedFeed() },
                    )
                }

                feed.sections.isEmpty() && !feed.isLoading -> {
                    DiscoveryEmptyState(
                        onRefresh = { viewModel.refreshPersonalizedFeed() },
                    )
                }

                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .premiumScrollHaptics(listState),
                        contentPadding = PaddingValues(bottom = 200.dp),
                    ) {
                        item {
                            Spacer(Modifier.height(6.dp))
                        }
                        itemsIndexed(
                            items = feed.sections,
                            key = { _, section -> section.id },
                        ) { index, section ->
                            PersonalizedSectionRow(
                                index = index,
                                section = section,
                                hasAuth = uiState.hasAuth,
                                onPlay = { item ->
                                    viewModel.playPersonalizedItem(item, section.items)
                                    onNavigateToNowPlaying(item.id)
                                },
                                onPlayNext = viewModel::playNextPersonalizedItem,
                                onAddToQueue = viewModel::addPersonalizedItemToQueue,
                                onRetry = { viewModel.refreshPersonalizedSection(section.type) },
                                onConnectAccount = onConnectAccount,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Personalized Section Row ───────────────────────────────────────────────
@Composable
private fun PersonalizedSectionRow(
    index: Int,
    section: PersonalizedSection,
    hasAuth: Boolean,
    onPlay: (PersonalizedFeedItem) -> Unit,
    onPlayNext: (PersonalizedFeedItem) -> Unit,
    onAddToQueue: (PersonalizedFeedItem) -> Unit,
    onRetry: () -> Unit,
    onConnectAccount: () -> Unit,
) {
    // Loading state for this section (only when no items cached yet)
    if (section.isLoading && section.items.isEmpty()) {
        Column(Modifier.fillMaxWidth()) {
            SectionHeaderShimmer()
            SectionRowSkeleton()
        }
        return
    }

    // Account-required but no auth: render CTA so the user can connect.
    if (section.isAccountRequired && !hasAuth && section.error == null) {
        AccountRequiredSectionCard(
            title = section.title,
            sourceLabel = section.sourceLabel,
            onConnectAccount = onConnectAccount,
        )
        return
    }

    // Isolated section error
    if (section.error != null && section.items.isEmpty()) {
        SectionErrorCard(
            title = section.title,
            message = section.error,
            onRetry = onRetry,
        )
        return
    }

    // Empty section
    if (section.items.isEmpty()) {
        SectionEmptyCard(
            title = section.title,
            sourceLabel = section.sourceLabel,
            onRetry = onRetry,
        )
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(400, delayMillis = (index % 5) * 60, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(400, delayMillis = (index % 5) * 60, easing = EaseOutCubic),
            initialOffsetY = { it / 4 }
        )
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Section header: title + truthful source label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (section.sourceLabel.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.8f))
                            )
                            Text(
                                text = section.sourceLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }
                }

                if (section.error != null && section.items.isNotEmpty()) {
                    TextButton(onClick = onRetry) {
                        Text(
                            "Retry",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            val rowState = rememberLazyListState()
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumScrollHaptics(rowState)
            ) {
                items(section.items, key = { it.id }) { item ->
                    PersonalizedMusicCard(
                        item = item,
                        onClick = { onPlay(item) },
                        onPlayNext = { onPlayNext(item) },
                        onAddToQueue = { onAddToQueue(item) },
                    )
                }
            }
        }
    }
}

// ─── Personalized Music Card ────────────────────────────────────────────────
@Composable
fun PersonalizedMusicCard(
    item: PersonalizedFeedItem,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CardSurface.copy(alpha = 0.7f),
                        CardSurfaceElevated.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.3f),
                        NeonCyan.copy(alpha = 0.2f),
                        ElectricViolet.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.03f))
            ) {
                if (item.artworkUrl != null) {
                    AsyncImage(
                        model = item.artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                // Bottom gradient fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Item-type badge (playlist vs song/video)
                if (item.itemType == PersonalizedItemType.PLAYLIST) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "PLAYLIST",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                // Source badge (truthful provenance)
                if (item.sourceBadge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            item.sourceBadge,
                            color = NeonCyan,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                        )
                    }
                }

                // Duration
                if (item.durationMs > 0) {
                    Text(
                        text = formatDuration(item.durationMs),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // Play button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.9f),
                                    ElectricViolet.copy(alpha = 0.6f)
                                )
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))

            // Artist + overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = CardSurfaceElevated,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play next", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                menuExpanded = false
                                onPlayNext()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to queue", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                menuExpanded = false
                                onAddToQueue()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── Discovery State Composables ─────────────────────────────────────────────
@Composable
private fun DiscoveryLoadingSkeleton() {
    Column(Modifier.fillMaxSize()) {
        repeat(3) {
            Column(Modifier.fillMaxWidth()) {
                SectionHeaderShimmer()
                SectionRowSkeleton()
            }
        }
    }
}

@Composable
private fun SectionHeaderShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(
            Modifier
                .width(150.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(12.dp))
        ShimmerBox(
            Modifier
                .width(90.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    }
}

@Composable
private fun SectionRowSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(4) {
            ShimmerBox(
                Modifier
                    .width(150.dp)
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

@Composable
private fun DiscoveryGlobalError(
    message: String,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DiscoveryEmptyState(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.SpatialAudioOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextTertiary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Pull down to discover music",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Refresh", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Section State Cards ────────────────────────────────────────────────────
@Composable
private fun AccountRequiredSectionCard(
    title: String,
    sourceLabel: String,
    onConnectAccount: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(ElectricViolet.copy(alpha = 0.12f), NeonCyan.copy(alpha = 0.08f))
                )
            )
            .border(
                0.5.dp,
                Brush.horizontalGradient(
                    listOf(ElectricViolet.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.3f))
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onConnectAccount)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Connect your account to see $sourceLabel",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SectionErrorCard(
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = NeonCyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionEmptyCard(
    title: String,
    sourceLabel: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Nothing here from $sourceLabel yet",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = NeonCyan, fontWeight = FontWeight.Bold)
        }
    }
}
// ─── Library Tab ─────────────────────────────────────────────────────────────
@Composable
private fun LibraryTab(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: (String) -> Unit,
    paddingValues: PaddingValues,
) {
    if (uiState.localSongs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TextTertiary
                )
                Spacer(Modifier.height(12.dp))
                Text("No local music found", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.syncLibrary() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan for Music", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().premiumScrollHaptics(listState),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 4.dp, bottom = 180.dp),
        ) {
            // Song count header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.localSongs.size} tracks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    QualityPill("LOSSLESS READY")
                }
            }

            itemsIndexed(uiState.localSongs, key = { _, it -> it.id }) { index, song ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = tween(300, delayMillis = (index % 10) * 30, easing = EaseOutCubic)
                    ) + slideInVertically(
                        animationSpec = tween(300, delayMillis = (index % 10) * 30, easing = EaseOutCubic),
                        initialOffsetY = { it / 4 }
                    )
                ) {
                    PremiumSongListItem(song, onClick = {
                        viewModel.playMusicLocal(song)
                        onNavigateToNowPlaying(song.id.toString())
                    })
                }
            }
        }
    }
}

// ─── Quality Pill ────────────────────────────────────────────────────────────
@Composable
private fun QualityPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                Brush.horizontalGradient(
                    listOf(NeonCyan.copy(alpha = 0.1f), ElectricViolet.copy(alpha = 0.1f))
                ),
                RoundedCornerShape(6.dp)
            )
            .border(
                0.5.dp,
                Brush.horizontalGradient(
                    listOf(NeonCyan.copy(alpha = 0.3f), ElectricViolet.copy(alpha = 0.3f))
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ─── Premium Song List Item (Library Tab) ────────────────────────────────────
@Composable
fun PremiumSongListItem(
    song: Song,
    onClick: () -> Unit,
) {
    val isHiRes = remember(song.id) { (song.id.hashCode() % 3) == 0 }
    val isFlac = remember(song.id) { (song.id.hashCode() % 2) == 0 }
    val bitrate = remember(song.id) { if (isHiRes) "24bit · 96kHz" else "16bit · 44.1kHz" }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CardSurface.copy(alpha = 0.6f),
                        CardSurfaceElevated.copy(alpha = 0.4f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Art with badges
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Play overlay on hover
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isHiRes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                HiResBadge,
                                RoundedCornerShape(bottomEnd = 6.dp, topStart = 12.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("HR", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Format badge
                    BadgePill(
                        text = if (isFlac) "FLAC" else "AAC",
                        color = if (isFlac) FlacBadge else TextTertiary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bitrate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right column: duration + DSP badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatDuration(song.duration),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlowBadgePill("DSP", DspColor)
                    GlowBadgePill("V4A", V4aColor)
                }
            }
        }
    }
}

// ─── Glowing Badge (for HI-RES, FLAC on thumbnails) ─────────────────────────
@Composable
private fun GlowingBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), ambientColor = color, spotColor = color)
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = Color.Black,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Badge Pill (inline text badges) ─────────────────────────────────────────
@Composable
private fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

// ─── Glow Badge Pill (DSP/V4A with glow border) ─────────────────────────────
@Composable
private fun GlowBadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
            .border(
                0.5.dp,
                color.copy(alpha = 0.35f),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return "%d:%02d".format(min, sec)
}

// ─── Premium Search Bar ──────────────────────────────────────────────────────
@Composable
fun PremiumSearchBar(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "searchBarScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.3f),
                        ElectricViolet.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                "Search artists, songs, playlists...",
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
