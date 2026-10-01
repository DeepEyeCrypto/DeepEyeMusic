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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import com.deepeye.musicpro.ui.components.HeroEmptyState
import com.deepeye.musicpro.ui.components.SecondaryLabel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.ui.components.bouncyClickable
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.components.ShimmerBox
import com.deepeye.musicpro.ui.components.rememberSparseAwareRailLayout
import com.deepeye.musicpro.ui.theme.CardGeometry
import com.deepeye.musicpro.ui.youtube.SmartTubeVideoCard
import com.deepeye.musicpro.ui.components.premium.PremiumHeroCard
import com.deepeye.musicpro.ui.components.premium.SplitMediaHero
import com.deepeye.musicpro.ui.components.premium.DotMatrixClock
import com.deepeye.musicpro.ui.theme.GlassBorder
import com.deepeye.musicpro.ui.theme.sdp
import com.deepeye.musicpro.ui.components.glassCard
import com.deepeye.musicpro.ui.components.hoverable
import com.deepeye.musicpro.ui.gamification.Top3LeaderboardCard


@Composable
fun HomeHubScreen(
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToMusic: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onOpenV4A: () -> Unit,
    onLaunchTvMode: () -> Unit = {},

    onNavigateToSettings: () -> Unit,
    viewModel: HomeHubViewModel = hiltViewModel(),
    playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel(),
    recommendationViewModel: com.deepeye.musicpro.ui.home.RecommendationViewModel = hiltViewModel(),
) {
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val recs by recommendationViewModel.recommendations.collectAsStateWithLifecycle()
    val isRecsLoading by recommendationViewModel.isRefreshing.collectAsStateWithLifecycle()
    val dspEngineState by viewModel.isDspAttached.collectAsStateWithLifecycle()
    val gamificationState by viewModel.gamificationState.collectAsStateWithLifecycle()
    val top3Users by viewModel.top3Users.collectAsStateWithLifecycle()

    val isExpanded = windowSizeClass.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded

    // ── Data-scarcity handling ────────────────────────────────────────────────
    // Two distinct situations need different treatments, and conflating them is
    // what produces a screen full of empty boxes:
    //
    //  1. The feed resolved to nothing at all → render ONE prominent hero state
    //     that owns the viewport.
    //  2. The feed has some content but individual rails are empty → fill those
    //     gaps with a compact placeholder, but only for the rails the user is
    //     guaranteed to expect.
    //
    // A placeholder is NOT rendered for every empty section. HomeHub declares
    // nine conditional rails, so a fully-sparse feed emitting one per rail
    // would stack nine 160dp dashed boxes — 1440dp of scrolling placeholders,
    // which is a worse-looking void than the one it replaces.
    //
    // `recs != null` is deliberately NOT used as the "has content" test: the
    // engine publishes a non-null RecommendationResult even when every row in
    // it is empty (a new account with no history scores nothing). Counting
    // that as content would suppress the hero state on precisely the screen
    // that needs it, leaving the user with a header and an empty column.
    val hasRecommendationContent = recs?.let { result ->
        result.becauseYouListened.any { it.items.isNotEmpty() } ||
            result.favoriteArtists.any { it.items.isNotEmpty() } ||
            result.genreDive.any { it.items.isNotEmpty() } ||
            result.perfectForNow.items.isNotEmpty() ||
            result.trending.items.isNotEmpty() ||
            result.hiddenGems.items.isNotEmpty()
    } == true

    val hasFeedContent = feedState.continueListening.isNotEmpty() ||
        feedState.moodMixes.isNotEmpty() ||
        feedState.trending.isNotEmpty() ||
        feedState.shorts.isNotEmpty() ||
        feedState.supermix.isNotEmpty() ||
        feedState.discoverMix.isNotEmpty() ||
        feedState.becauseYouLikedMix.isNotEmpty() ||
        feedState.newReleases.isNotEmpty() ||
        feedState.quickPicks.isNotEmpty() ||
        feedState.localResume.isNotEmpty() ||
        hasRecommendationContent

    // Suppressed while loading: showing "no content" during a fetch is a lie that
    // flashes before real data arrives. The recommendation pass is still in
    // flight while the feed itself has settled, so it suppresses the hero too.
    val showFeedEmptyState = !hasFeedContent && !feedState.isLoading && !isRecsLoading

    var showGamificationSheet by remember { mutableStateOf(false) }
    var showRankingSheet by remember { mutableStateOf(false) }

    if (showGamificationSheet) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showGamificationSheet = false },
            containerColor = Color(0xFF1E1E1E)
        ) {
            com.deepeye.musicpro.ui.gamification.GamificationBottomSheet(
                streak = gamificationState.streak,
                rewardPoints = gamificationState.rewardPoints,
                unlockedBadges = gamificationState.unlockedBadges,
                lockedBadges = gamificationState.lockedBadges,
                onClaimReward = { showGamificationSheet = false }
            )
        }
    }

    if (showRankingSheet) {
        com.deepeye.musicpro.ui.ranking.RankingBottomSheet(
            rankingRepository = viewModel.rankingRepository,
            rankingEngine = viewModel.rankingEngine,
            onDismissRequest = { showRankingSheet = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.TopCenter,
    ) {
        var achievementToShow by remember { mutableStateOf<com.deepeye.musicpro.domain.gamification.UserAchievement?>(null) }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        LaunchedEffect(Unit) {
            viewModel.achievementEvents.collect { event ->
                // Find badge in state
                val badge = gamificationState.unlockedBadges.find { it.requirement == event.requirement }
                if (badge != null) {
                    achievementToShow = badge
                }
            }
        }

        achievementToShow?.let { badge ->
            com.deepeye.musicpro.ui.gamification.AchievementUnlockedPopup(
                badge = badge,
                onDismiss = { achievementToShow = null },
                onShare = {
                    achievementToShow = null
                    // MOCK Instagram share
                    android.widget.Toast.makeText(
                        context,
                        "Sharing to Instagram Stories...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        // ── Root-level branch: hero vs. feed ───────────────────────────────────
        // Evaluated *before* the feed container is composed so an empty feed
        // never lays out a LazyColumn at all. Composing it and then hiding it
        // would still pay for ten rail measurements, ten `item {}` subtrees and
        // the divider/spacer work between them, for a screen that displays none
        // of it. Branching here also guarantees the two are mutually exclusive —
        // there is no window in which a half-drawn feed and the hero coexist.
        if (showFeedEmptyState) {
            HeroEmptyState(
                title = if (feedState.hasAuth) "Nothing to Play Yet" else "Your Feed Is Empty",
                subtitle = if (feedState.hasAuth) {
                    "We couldn't find anything new for you. Try again, or explore something different."
                } else {
                    "Play a few tracks or connect your account and we'll build your feed around what you love."
                },
                actionText = "Discover Music",
                onAction = { viewModel.loadFeed() },
                icon = if (feedState.hasAuth) Icons.Rounded.Refresh else Icons.Rounded.Search,
                // Deliberately NOT padded clear of the dock by 140dp the way the
                // LazyColumn below is. That 140dp is scroll runway for a
                // scrolling list, but the host (DeepEyeMusicApp) has already
                // inset this whole screen above the dock. Applying it here too
                // double-counted the inset: on a 360dp-tall landscape phone it
                // left the hero ~110dp of height, which is less than the hero
                // needs, so the copy and CTA were pushed off-screen. Measured
                // via the accessibility bounds: the host granted 536px and the
                // hero's own padding cut it to 219px.
                contentPadding = PaddingValues(
                    start = 32.sdp,
                    top = 32.sdp,
                    end = 32.sdp,
                    bottom = 24.sdp,
                ),
                modifier = if (isExpanded) Modifier.widthIn(max = 1200.dp) else Modifier,
            )
        } else {
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (isExpanded) Modifier.widthIn(max = 1200.dp) else Modifier,
                ),
            verticalArrangement = Arrangement.spacedBy(24.sdp),
            contentPadding = PaddingValues(
                start = if (isExpanded) 24.sdp else 16.sdp,
                top = 12.sdp,
                end = if (isExpanded) 24.sdp else 16.sdp,
                // 140.dp is deliberately NOT `.sdp`: it clears the bottom dock
                // plus the mini-player sheet above it, both of which are sized
                // against fixed 48dp touch targets. Scaling this clearance
                // while the dock stays fixed would let the two drift apart and
                // bury the last row of the list.
                bottom = 140.dp,
            ),
        ) {
            item {
                val btcPrice by viewModel.btcPrice.collectAsStateWithLifecycle()
                HomeGreetingHeader(
                    btcPrice = btcPrice,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToChat = onNavigateToChat,
                    onOpenGamification = { showGamificationSheet = true },
                    onOpenRanking = { showRankingSheet = true }
                )
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
                    modifier = Modifier.padding(horizontal = 16.sdp, vertical = 8.sdp),
                )
            }

            // 5. Mood Chips (Phase 7)
            if (feedState.moodMixes.isNotEmpty()) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    MoodChipsRow(
                        moods = feedState.moodMixes,
                        onMoodClick = { mood -> 
                            android.widget.Toast.makeText(context, "Generating playlist for ${mood.label}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.onMoodClick(mood) 
                        },
                    )
                }
            }

            // 5b. YouTube / Trending Rails
            if (feedState.trending.isNotEmpty()) {
                item {
                    HomeVideoRail(
                        title = if (feedState.hasAuth) "🌟 For You" else "🔥 Trending",
                        items = feedState.trending,
                        onClick = { 
                            viewModel.playVideo(it)
                            onNavigateToVideo(it.id) 
                        }
                    )
                }
            }

            if (feedState.shorts.isNotEmpty()) {
                item {
                    ShortsRail(
                        items = feedState.shorts,
                        onClick = { 
                            viewModel.playVideo(it)
                            onNavigateToVideo(it.id) 
                        }
                    )
                }
            }

            if (feedState.supermix.isNotEmpty()) {
                item {
                    HomeMusicRail(
                        title = "✨ My Supermix",
                        items = feedState.supermix,
                        onClick = { 
                            viewModel.playMusic(it)
                            onNavigateToMusic(it.id) 
                        }
                    )
                }
            }

            if (feedState.discoverMix.isNotEmpty()) {
                item {
                    HomeMusicRail(
                        title = "🔭 Discover Mix",
                        items = feedState.discoverMix,
                        onClick = { 
                            viewModel.playMusic(it)
                            onNavigateToMusic(it.id) 
                        }
                    )
                }
            }

            if (feedState.becauseYouLikedMix.isNotEmpty() && feedState.becauseYouLikedArtist != null) {
                item {
                    HomeMusicRail(
                        title = "❤️ Because You Liked ${feedState.becauseYouLikedArtist}",
                        items = feedState.becauseYouLikedMix,
                        onClick = {
                            viewModel.playMusic(it)
                            onNavigateToMusic(it.id)
                        }
                    )
                }
            }

            if (feedState.newReleases.isNotEmpty()) {
                item {
                    HomeMusicRail(
                        title = "🆕 New Releases For You",
                        items = feedState.newReleases,
                        onClick = {
                            viewModel.playMusic(it)
                            onNavigateToMusic(it.id)
                        }
                    )
                }
            }

            if (feedState.quickPicks.isNotEmpty()) {
                item {
                    HomeMusicRail(
                        title = "🎵 Quick Picks",
                        items = feedState.quickPicks,
                        onClick = { 
                            viewModel.playMusic(it)
                            onNavigateToMusic(it.id) 
                        }
                    )
                }
            }

            // Phase 4: AI Prompt Bar
            item {
                val isGenerating by viewModel.isAIGenerating.collectAsStateWithLifecycle()
                val context = androidx.compose.ui.platform.LocalContext.current
                AIPromptBar(
                    isGenerating = isGenerating,
                    onSubmitPrompt = { prompt ->
                        android.widget.Toast.makeText(context, "AI is curating based on: $prompt", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.onAIPromptSubmitted(prompt)
                    }
                )
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
                    modifier = Modifier.padding(horizontal = 16.sdp, vertical = 8.sdp),
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
        } // Close LazyColumn here
        } // Close root-level hero/feed branch
    }
}

@Composable
private fun HomeGreetingHeader(
    btcPrice: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onOpenGamification: () -> Unit = {},
    onOpenRanking: () -> Unit = {},
) {
    val greeting = remember { getDynamicGreeting() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Branding & Dynamic Greeting
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DEEPEYE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.sdp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFF00E5FF))
                )
                Spacer(modifier = Modifier.width(8.sdp))
                Text(
                    text = "PRO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFFF9100),
                    modifier = Modifier
                        .background(Color(0x33FF9100), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.sdp))
            Text(
                text = greeting,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Right Cyberpunk Action Dock
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF131722).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.sdp, vertical = 4.sdp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.sdp)
            ) {
                // Bitcoin Ticker Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1AFFFFFF))
                        .clickable { onNavigateToChat() }
                        .padding(horizontal = 10.sdp, vertical = 6.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("₿", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.sdp))
                    Text(btcPrice.replace("₿ ", ""), color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Chat Icon
                IconButton(onClick = onNavigateToChat, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = "Chat", tint = Color(0xFF7B3FE4), modifier = Modifier.size(20.dp))
                }

                // Gamification Star
                IconButton(onClick = onOpenGamification, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "Gamification", tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                }

                // Ranking / Leaderboard
                IconButton(onClick = onOpenRanking, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Person, contentDescription = "Leaderboard", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                }

                // Settings
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun getDynamicGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning ☀️"
        in 12..16 -> "Good Afternoon 🌤️"
        in 17..21 -> "Good Evening 🌆"
        else -> "Night Owl Session 🌙"
    }
}

@Composable
private fun HomeVideoRail(
    title: String,
    items: List<HomeVideoItem>,
    onClick: (HomeVideoItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.sdp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = CardGeometry.ScreenGutter),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        val railLayout = rememberSparseAwareRailLayout(itemCount = items.size)
        LazyRow(
            horizontalArrangement = railLayout.arrangement,
            contentPadding = railLayout.contentPadding,
        ) {
            items(items.size, key = { index -> "$index-${items[index].id}" }) { index ->
                val video = items[index]
                SmartTubeVideoCard(
                    video = video,
                    onClick = { onClick(video) },
                    modifier = Modifier.width(CardGeometry.Video.minWidth),
                )
            }
        }
    }
}

@Composable
private fun ShortsRail(
    items: List<HomeVideoItem>,
    onClick: (HomeVideoItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.sdp)) {
        Text(
            "📱 Shorts",
            modifier = Modifier.padding(horizontal = CardGeometry.ScreenGutter),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold,
        )
        val railLayout = rememberSparseAwareRailLayout(itemCount = items.size)
        LazyRow(
            horizontalArrangement = railLayout.arrangement,
            contentPadding = railLayout.contentPadding,
        ) {
            items(items.size, key = { index -> "$index-${items[index].id}" }) { index ->
                val short = items[index]
                SmartTubeVideoCard(
                    video = short,
                    onClick = { onClick(short) },
                    modifier = Modifier.width(CardGeometry.Video.minWidth),
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
    Column(verticalArrangement = Arrangement.spacedBy(12.sdp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = CardGeometry.ScreenGutter),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Bold,
        )
        // Sparse-aware: a rail holding fewer than SparseRailThreshold items
        // centres itself instead of hugging the leading edge, so a short feed
        // reads as composed rather than as a load that failed halfway.
        val railLayout = rememberSparseAwareRailLayout(itemCount = items.size)
        LazyRow(
            horizontalArrangement = railLayout.arrangement,
            contentPadding = railLayout.contentPadding,
        ) {
            items(items, key = { it.id }) { music ->
                Box(
                    modifier =
                    Modifier
                        .width(CardGeometry.ContinueListening.maxWidth)
                        .clip(CardGeometry.ContinueListening.shape)
                        .background(Color.White.copy(alpha = 0.05f)) // Smoother glass effect
                        .border(1.dp, GlassBorder, CardGeometry.ContinueListening.shape)
                        .bouncyClickable(
                            downScale = 0.95f,
                            onClick = { onClick(music) }
                        )
                        .padding(CardGeometry.ContinueListening.contentPadding),
                ) {
                    Column {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(CardGeometry.Music.aspectRatio)
                                .clip(RoundedCornerShape(CardGeometry.Music.cornerRadius / 1.5f)),
                        ) {
                            AsyncImage(
                                model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(music.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = music.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.height(10.sdp))
                        Text(
                            text = music.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.sdp))
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
    Column(Modifier.padding(vertical = 12.sdp), verticalArrangement = Arrangement.spacedBy(8.sdp)) {
        Text(title, modifier = Modifier.padding(horizontal = 20.sdp), color = Color.White)
        Row(Modifier.padding(horizontal = 20.sdp)) {
            repeat(3) {
                ShimmerBox(Modifier.size(140.dp))
                Spacer(Modifier.width(12.sdp))
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
            verticalArrangement = Arrangement.spacedBy(12.sdp),
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
                artworkUri = android.net.Uri.parse("https://i.ytimg.com/vi/${it.videoId}/hqdefault.jpg"),
                duration = 180000L, // Mock duration
                isVideo = true,
            )
        }
    val index = contextList.indexOfFirst { it.videoId == video.videoId }
    playerViewModel.setQueue(mediaItems, if (index >= 0) index else 0)
    onNavigateToMusic(video.videoId)
}
