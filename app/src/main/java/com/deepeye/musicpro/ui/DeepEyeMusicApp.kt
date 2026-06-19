// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deepeye.musicpro.ui.navigation.NavGraph
import com.deepeye.musicpro.ui.navigation.Routes
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.layout.height

/** CompositionLocal for PiP state — accessible anywhere in the Compose tree */
val LocalPipMode = compositionLocalOf { false }

/** CompositionLocal for the hoisted YouTube WebView */
val LocalSharedWebView = androidx.compose.runtime.staticCompositionLocalOf<android.webkit.WebView?> { null }

val LocalHazeState = androidx.compose.runtime.compositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }

/**
 * Root composable for the DeepEyeMusicPro app.
 * Hosts the Scaffold with bottom navigation and the NavGraph.
 */

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun DeepEyeMusicApp(
    playerController: com.deepeye.musicpro.player.controller.PlayerController,
    isInPipMode: Boolean = false,
    fullscreenMode: FullscreenMode = FullscreenMode(),
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedWebView = remember { com.deepeye.musicpro.ui.components.createYouTubeWebView(context) }

    val playerState by playerController.playerState.collectAsStateWithLifecycle()
    val sheetViewModel: com.deepeye.musicpro.ui.player.MiniPlayerSheetViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val sheetState by sheetViewModel.state.collectAsStateWithLifecycle()
    
    // Initialize DSP globally on app launch
    val dspViewModel: com.deepeye.musicpro.dsp.engine.DSPViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val changelogViewModel: com.deepeye.musicpro.updates.ChangelogViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val changelogState by changelogViewModel.state.collectAsStateWithLifecycle()

    val settingsViewModel: com.deepeye.musicpro.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    if (changelogState.shouldShowChangelog) {
        com.deepeye.musicpro.ui.update.ChangelogDialog(
            entries = changelogState.changelogEntries,
            onDismiss = { changelogViewModel.onDismiss() },
            onLater = { changelogViewModel.snooze() }
        )
    }

    LaunchedEffect(Unit) {
        changelogViewModel.checkForUpdate(com.deepeye.musicpro.BuildConfig.VERSION_CODE)
        settingsViewModel.checkForUpdate()
    }

    LaunchedEffect(playerState.currentItem, playerState.isPlaying, playerState.playbackSpeed, playerState.isVideo) {
        val currentItem = playerState.currentItem
        val isVideo = currentItem is com.deepeye.musicpro.domain.model.MediaItem.Remote && playerState.isVideo

        if (isVideo) {
            val videoId = currentItem.id
            val updateState =
                sharedWebView.tag as? com.deepeye.musicpro.ui.components.YouTubePlayerState
                    ?: com.deepeye.musicpro.ui.components.YouTubePlayerState().also { sharedWebView.tag = it }

            // 1. Initial html load vs subsequent loadVideo
            if (updateState.videoId == null) {
                updateState.videoId = videoId
                updateState.networkError.value = false
                val startSec = (playerState.position / 1000).toInt()

                val htmlContent = com.deepeye.musicpro.ui.components.getYouTubeHtmlTemplate(videoId, startSec)
                sharedWebView.loadDataWithBaseURL(
                    com.deepeye.musicpro.ui.components.YOUTUBE_WEBVIEW_BASE_URL,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            } else if (updateState.videoId != videoId) {
                updateState.videoId = videoId
                updateState.networkError.value = false
                sharedWebView.evaluateJavascript("if (typeof loadVideo === 'function') loadVideo('$videoId');", null)
            }

            // 2. Sync play/pause
            if (playerState.isPlaying) {
                sharedWebView.evaluateJavascript("if (typeof playVideo === 'function') playVideo();", null)
            } else {
                sharedWebView.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
            }

            // 3. Sync playback speed
            sharedWebView.evaluateJavascript(
                "if (typeof setSpeed === 'function') setSpeed(${playerState.playbackSpeed});",
                null
            )
        } else {
            // Only pause WebView when explicitly switching AWAY from a video track.
            // Don't pause during navigation/recomposition transients — prevents the
            // "video stops when navigating to Settings" bug.
            val updateState = sharedWebView.tag as? com.deepeye.musicpro.ui.components.YouTubePlayerState
            if (updateState?.videoId != null) {
                sharedWebView.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
                updateState.videoId = null
            }
        }
    }

    LaunchedEffect(playerState.position) {
        val currentItem = playerState.currentItem
        val isVideo = currentItem is com.deepeye.musicpro.domain.model.MediaItem.Remote && playerState.isVideo

        if (isVideo) {
            val updateState =
                sharedWebView.tag as? com.deepeye.musicpro.ui.components.YouTubePlayerState
                    ?: com.deepeye.musicpro.ui.components.YouTubePlayerState().also { sharedWebView.tag = it }

            // Sync position (only when user has seeked significantly, e.g. via lock screen or Bluetooth)
            val diff = playerState.position - updateState.lastSentPosition
            if (updateState.lastSentPosition == -1L || diff > 2500L || diff < -2500L) {
                updateState.lastSentPosition = playerState.position
                val seconds = playerState.position / 1000f
                sharedWebView.evaluateJavascript(
                    "if (document.getElementById('player') && document.getElementById('player').contentWindow) { " +
                        "document.getElementById('player').contentWindow.postMessage(JSON.stringify({\"event\": \"command\", \"func\": \"seekTo\", \"args\": [$seconds, true]}), \"*\"); }",
                    null,
                )
            } else {
                updateState.lastSentPosition = playerState.position
            }
        }
    }

    CompositionLocalProvider(
        LocalPipMode provides isInPipMode,
        LocalFullscreenMode provides fullscreenMode,
        LocalSharedWebView provides sharedWebView,
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val bottomNavItems =
            remember {
                listOf(
                    BottomNavItem(Routes.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
                    BottomNavItem(
                        Routes.YouTube.route,
                        "YouTube",
                        Icons.Filled.Subscriptions,
                        Icons.Outlined.Subscriptions
                    ),
                    BottomNavItem(Routes.Music.route, "Music", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
                    BottomNavItem(
                        Routes.Library.route,
                        "Library",
                        Icons.Filled.LibraryMusic,
                        Icons.Outlined.LibraryMusic
                    ),
                    BottomNavItem(Routes.DSP.route, "DSP", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
                    BottomNavItem(Routes.Settings.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
                )
            }

        // Hide bottom nav on full-screen destinations AND in PiP mode
        val showBottomBar =
            !isInPipMode && !fullscreenMode.isFullscreen && currentDestination?.hierarchy?.any { dest ->
                bottomNavItems.any { it.route == dest.route }
            } == true

        @OptIn(
            androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi::class
        )
        val adaptiveInfo = androidx.compose.material3.adaptive.currentWindowAdaptiveInfo()
        val navLayoutType =
            if (showBottomBar) {
                androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                    adaptiveInfo
                )
            } else {
                androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType.None
            }

        val isBottomBar = navLayoutType == androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType.NavigationBar

        val hazeState = remember { dev.chrisbanes.haze.HazeState() }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.deepeye.musicpro.R.drawable.spatial_bg),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)))

            if (isBottomBar) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ) { innerPadding ->
                    val exactDockHeight = if (showBottomBar) {
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp
                    } else {
                        0.dp
                    }
                    val miniPlayerPadding by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (playerState.currentItem != null && !isInPipMode) 88.dp else 0.dp,
                        label = "miniPlayerPadding"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = exactDockHeight + miniPlayerPadding),
                    ) {
                        NavGraph(
                            navController = navController,
                            windowSizeClass = windowSizeClass,
                            onExpandPlayer = { sheetViewModel.expand() }
                        )
                    }
                }
            } else {
            @OptIn(
                androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi::class
            )
            androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold(
                layoutType = navLayoutType,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                navigationSuiteColors = androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.colors(
                    navigationBarContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    navigationRailContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    navigationDrawerContainerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                navigationSuiteItems = {
                    bottomNavItems.forEach { item ->
                        val selected =
                            currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                        item(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                },
            ) {
                Scaffold(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    bottomBar = {
                        // Empty bottom bar, space handled by AnchoredMiniPlayer
                    },
                ) { innerPadding ->
                    val miniPlayerPadding by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (playerState.currentItem != null && !isInPipMode) 88.dp else 0.dp,
                        label = "miniPlayerPadding"
                    )

                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(bottom = miniPlayerPadding),
                    ) {
                        NavGraph(
                            navController = navController,
                            windowSizeClass = windowSizeClass,
                            onExpandPlayer = { sheetViewModel.expand() }
                        )
                    }
                }
            }
        }
            } // Close the Box with hazeSource

            // Dock Overlay
            if (isBottomBar && showBottomBar) {
                val selectedIndex = bottomNavItems.indexOfFirst { item ->
                    currentDestination?.hierarchy?.any { it.route == item.route } == true
                }.coerceAtLeast(0)

                val magicItems = remember(bottomNavItems) {
                    bottomNavItems.map {
                        com.deepeye.musicpro.ui.components.MagicNavItem(
                            route = it.route,
                            label = it.label,
                            selectedIcon = it.selectedIcon,
                            unselectedIcon = it.unselectedIcon
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    com.deepeye.musicpro.ui.components.MagicNavigationBar(
                        items = magicItems,
                        selectedIndex = selectedIndex,
                        onItemClick = { index ->
                            // Collapse the player whenever a dock item is clicked
                            sheetViewModel.collapse()
                            
                            val item = bottomNavItems[index]
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        indicatorColor = androidx.compose.ui.graphics.Color(0xFF00E5C3).copy(alpha = 0.2f),
                        activeIconColor = androidx.compose.ui.graphics.Color(0xFF00E5C3), // Cyan accent
                        inactiveIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                        surfaceColor = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            if (!fullscreenMode.isFullscreen) {
                // Status Bar Glass Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .align(Alignment.TopCenter)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                )

                // Global Update Banner
                if (settingsUiState.updateState is com.deepeye.musicpro.data.source.remote.update.UpdateState.UpdateAvailable || 
                    settingsUiState.updateState is com.deepeye.musicpro.data.source.remote.update.UpdateState.Downloading ||
                    settingsUiState.updateState is com.deepeye.musicpro.data.source.remote.update.UpdateState.Downloaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .align(Alignment.TopCenter)
                    ) {
                        com.deepeye.musicpro.ui.update.UpdateDialog(
                            state = settingsUiState.updateState,
                            onDownloadClick = { url, version -> settingsViewModel.downloadUpdate(url, version) },
                            onInstallClick = { file -> settingsViewModel.installApk(file) },
                            onDismissClick = { settingsViewModel.resetUpdateState() }
                        )
                    }
                }
            }
        
        // Overlay AnchoredMiniPlayer (Outside the scaffold conditional, so it covers everything)
        if (playerState.currentItem != null) {
            androidx.activity.compose.BackHandler(
                enabled = sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.EXPANDED ||
                    sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.HALF_EXPANDED
            ) {
                sheetViewModel.collapse()
            }

            val exactDockHeight = if (isBottomBar && showBottomBar) {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp
            } else {
                0.dp
            }

            com.deepeye.musicpro.ui.player.AnchoredMiniPlayer(
                sheetState = sheetState,
                onExpand = { sheetViewModel.expand() },
                onCollapse = { sheetViewModel.collapse() },
                onHalfExpand = { sheetViewModel.halfExpand() },
                onNext = { sheetViewModel.nextTrack() },
                onPrev = { sheetViewModel.previousTrack() },
                onPlayPause = { sheetViewModel.togglePlayPause() },
                bottomBarHeight = exactDockHeight,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().hazeEffect(state = hazeState, style = dev.chrisbanes.haze.HazeStyle(blurRadius = 20.dp, tint = dev.chrisbanes.haze.HazeTint(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)), noiseFactor = 0f)),
                ) {
                    com.deepeye.musicpro.ui.player.NowPlayingScreen(
                        windowSizeClass = windowSizeClass,
                        onNavigateBack = { sheetViewModel.collapse() },
                        onNavigateToV4A = {
                            sheetViewModel.collapse()
                            navController.navigate(Routes.DSP.route)
                        },
                        onNavigateToQueue = { /* Implement full queue view logic later */ },
                        onNavigateToSettings = {
                            sheetViewModel.collapse()
                            navController.navigate(Routes.Settings.route)
                        },
                    )
                }
            }
        }
        } // end of Spatial Background Box
    } // end CompositionLocalProvider
}
