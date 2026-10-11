// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import android.content.res.Configuration
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
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.ui.components.MusicCardSkeleton
import com.deepeye.musicpro.ui.components.SectionHeaderSkeleton
import com.deepeye.musicpro.ui.motion.premiumScrollHaptics
import com.deepeye.musicpro.ui.theme.*
import com.deepeye.musicpro.ui.util.evenCarouselCardWidth

// ─── Premium Color Tokens ────────────────────────────────────────────────────
private val CardSurface = Color(0xFF12121A)
private val CardSurfaceElevated = Color(0xFF1A1A26)
private val CyanAccent = Color(0xFF00E5FF)
private val VioletAccent = Color(0xFF7C4DFF)
private val GoldBadge = Color(0xFFFFD54F)
private val EmeraldBadge = Color(0xFF00E676)
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                if (isLandscape) {
                    // Sleek Single-Line Landscape Header (Title + Compact Search + Segmented Tabs + Refresh)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CardGeometry.ScreenGutter, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Music",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }

                        // Compact Search Bar Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))
                                    )
                                )
                                .border(
                                    width = 0.5.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(NeonCyan.copy(alpha = 0.3f), ElectricViolet.copy(alpha = 0.3f))
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable(onClick = onNavigateToSearch)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Search songs, artists...",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Refresh button (single cloud-driven feed)
                        IconButton(
                            onClick = { viewModel.refreshPersonalizedFeed() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // Portrait Expansive Header Section
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Top Row: Title and Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CardGeometry.ScreenGutter, vertical = 8.dp),
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Music",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Refresh button
                            IconButton(onClick = { viewModel.refreshPersonalizedFeed() }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                            }
                        }

                        // Premium Search Bar (Clickable Entry Point)
                        PremiumSearchBar(onClick = onNavigateToSearch)
                        Spacer(Modifier.height(8.dp))
                    }

                    // YouTube account connectivity prompt
                    if (!uiState.hasAuth) {
                        MusicAccountBanner(onConnect = onConnectAccount)
                        Spacer(Modifier.height(4.dp))
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            DiscoveryTab(
                uiState,
                viewModel,
                onNavigateToNowPlaying,
                onConnectAccount,
                paddingValues,
            )
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
            // Bounded with the rest of the tab's content. `boundedContent`
            // must precede `clip`, because clip draws against the node's own
            // measured size and the cap is what gives that size a ceiling.
            .boundedContent()
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
    val songSections = remember(feed.sections) {
        feed.sections
            .filterNot { it.type == PersonalizedSectionType.YOUR_PLAYLISTS }
            .map { section ->
                section.copy(
                    items = section.items.filter {
                        it.itemType == PersonalizedItemType.SONG &&
                            it.durationMs !in 1..59_000L &&
                            MusicFilter.isMusicTrack(it.title, it.artist, it.durationMs / 1000L)
                    }
                )
            }
            .filter { it.items.isNotEmpty() || it.isLoading || it.error != null }
    }
    
    var explanationItem by remember { mutableStateOf<PersonalizedFeedItem?>(null) }
    var explanationSection by remember { mutableStateOf<PersonalizedSection?>(null) }
    var showExplanationSheet by remember { mutableStateOf(false) }

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
                feed.isLoading && songSections.isEmpty() -> {
                    DiscoveryLoadingSkeleton()
                }

                songSections.isEmpty() && feed.globalError != null -> {
                    DiscoveryGlobalError(
                        message = feed.globalError,
                        onRetry = { viewModel.refreshPersonalizedFeed() },
                    )
                }

                songSections.isEmpty() && !feed.isLoading -> {
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
                            items = songSections,
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
                                onShowWhyThis = { item -> 
                                    explanationItem = item
                                    explanationSection = section
                                    showExplanationSheet = true
                                },
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
    onShowWhyThis: (PersonalizedFeedItem?) -> Unit,
    onRetry: () -> Unit,
    onConnectAccount: () -> Unit,
) {
    // Loading state for this section (only when no items cached yet)
    if (section.isLoading && section.items.isEmpty()) {
        // Wrapped so the skeleton rail can be sized by exactly the same
        // evenCarouselCardWidth call the loaded rail below uses. Sizing it
        // differently here is what made the section jump sideways on load.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val skeletonCardWidth = evenCarouselCardWidth(
                availableWidth = maxWidth,
                minItemWidth = CardGeometry.Music.minWidth,
            )
            Column(Modifier.fillMaxWidth()) {
                SectionHeaderShimmer()
                SectionRowSkeleton(cardWidth = skeletonCardWidth)
            }
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
        // Spotify-style dense rail. A fixed 150.dp card left a wide gap at the end
        // of every row on a landscape phone; this fills the row instead. Square
        // 1:1 cover art survives a narrower column than a 16:9 video poster, hence
        // CardGeometry.Music.minWidth (120.dp) over VideoCardMinWidth.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val musicCardWidth = evenCarouselCardWidth(
            availableWidth = maxWidth,
            minItemWidth = CardGeometry.Music.minWidth,
        )
        Column(Modifier.fillMaxWidth()) {
            // Section header: title + truthful source label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardGeometry.ScreenGutter, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        // titleSmall: section headers repeat down a long scrolling
                        // list, so a large style compounds into an oversized feel.
                        style = MaterialTheme.typography.titleSmall.copy(
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
                            if (section.isFromCache) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "• Cached",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary.copy(alpha = 0.6f),
                                    letterSpacing = 0.4.sp,
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (section.explanation != null) {
                        IconButton(
                            onClick = { onShowWhyThis(null) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "Why this section?",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
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
            }

            val rowState = rememberLazyListState()
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = CardGeometry.ScreenGutter),
                horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumScrollHaptics(rowState)
            ) {
                items(section.items, key = { it.id }) { item ->
                    PersonalizedMusicCard(
                        item = item,
                        cardWidth = musicCardWidth,
                        onClick = { onPlay(item) },
                        onPlayNext = { onPlayNext(item) },
                        onAddToQueue = { onAddToQueue(item) },
                        onWhyThis = { onShowWhyThis(item) },
                    )
                }
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
    onWhyThis: () -> Unit = {},
    // Supplied by the caller from evenCarouselCardWidth so the rail fills its
    // row. Defaults keep the card usable in previews and future call sites.
    cardWidth: Dp = CardGeometry.Music.minWidth,
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
            .width(cardWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Geometry from CardGeometry (SSOT). MusicCardSkeleton reads the same
            // fields, so the loading box matches the loaded box exactly.
            .clip(CardGeometry.Music.shape)
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
                shape = CardGeometry.Music.shape
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Column(modifier = Modifier.padding(CardGeometry.Music.contentPadding)) {
            // Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CardGeometry.Music.aspectRatio)
                    .clip(RoundedCornerShape(CardGeometry.Music.cornerRadius / 1.5f))
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
                            fontSize = 11.sp,
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
                            fontSize = 11.sp,
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
                        fontSize = 10.sp,
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
                maxLines = CardGeometry.Music.titleMaxLines,
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
                        DropdownMenuItem(
                            text = { Text("Why this recommendation?", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                menuExpanded = false
                                onWhyThis()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Info,
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
/**
 * Full-screen loading state for the discovery feed.
 *
 * Wrapped in BoxWithConstraints so the rail skeleton can size its cards exactly
 * as [PersonalizedSectionRow] sizes the real ones — the same
 * evenCarouselCardWidth call against the same available width. Without this the
 * placeholder rail is a different width from the loaded rail and the whole
 * section jumps sideways on load.
 */
@Composable
private fun DiscoveryLoadingSkeleton() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardWidth = evenCarouselCardWidth(
            availableWidth = maxWidth,
            minItemWidth = CardGeometry.Music.minWidth,
        )
        Column(Modifier.fillMaxSize()) {
            repeat(3) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeaderShimmer()
                    SectionRowSkeleton(cardWidth = cardWidth)
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardGeometry.ScreenGutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeaderSkeleton()
    }
}

/**
 * Loading placeholder for one discovery rail.
 *
 * [cardWidth] must be the same value the loaded rail computes via
 * evenCarouselCardWidth, and the LazyRow config matches
 * [PersonalizedSectionRow]'s — otherwise the rail reflows when data lands.
 */
@Composable
private fun SectionRowSkeleton(cardWidth: Dp) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = CardGeometry.ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(CardGeometry.CardSpacing),
        userScrollEnabled = false,
    ) {
        items(4) {
            MusicCardSkeleton(width = cardWidth)
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
