// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deepeye.musicpro.ui.navigation.NavGraph
import com.deepeye.musicpro.ui.navigation.Routes

/** CompositionLocal for PiP state — accessible anywhere in the Compose tree */
val LocalPipMode = compositionLocalOf { false }

/** CompositionLocal for the hoisted YouTube WebView */
val LocalSharedWebView = androidx.compose.runtime.staticCompositionLocalOf<android.webkit.WebView?> { null }

/**
 * Root composable for the DeepEyeMusicPro app.
 * Hosts the Scaffold with bottom navigation and the NavGraph.
 */

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun DeepEyeMusicApp(
    playerController: com.deepeye.musicpro.player.controller.PlayerController,
    isInPipMode: Boolean = false,
    fullscreenMode: FullscreenMode = FullscreenMode(),
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedWebView = remember { com.deepeye.musicpro.ui.components.createYouTubeWebView(context) }

    val playerState by playerController.playerState.collectAsStateWithLifecycle()

    LaunchedEffect(playerState.currentItem, playerState.isPlaying, playerState.playbackSpeed, playerState.isVideo) {
        val currentItem = playerState.currentItem
        val isVideo = currentItem is com.deepeye.musicpro.domain.model.MediaItem.Remote && playerState.isVideo
        
        if (isVideo && sharedWebView != null) {
            val videoId = currentItem.id
            val updateState = sharedWebView.tag as? com.deepeye.musicpro.ui.components.YouTubePlayerState 
                ?: com.deepeye.musicpro.ui.components.YouTubePlayerState().also { sharedWebView.tag = it }
            
            // 1. Initial html load vs subsequent loadVideo
            if (updateState.videoId == null) {
                updateState.videoId = videoId
                updateState.networkError.value = false
                val startSec = (playerState.position / 1000).toInt()
                
                val htmlContent = com.deepeye.musicpro.ui.components.getYouTubeHtmlTemplate(videoId, startSec)
                sharedWebView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
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
            sharedWebView.evaluateJavascript("if (typeof setSpeed === 'function') setSpeed(${playerState.playbackSpeed});", null)
        } else if (sharedWebView != null) {
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
        
        if (isVideo && sharedWebView != null) {
            val updateState = sharedWebView.tag as? com.deepeye.musicpro.ui.components.YouTubePlayerState 
                ?: com.deepeye.musicpro.ui.components.YouTubePlayerState().also { sharedWebView.tag = it }
            
            // Sync position (only when user has seeked significantly, e.g. via lock screen or Bluetooth)
            val diff = playerState.position - updateState.lastSentPosition
            if (updateState.lastSentPosition == -1L || diff > 2500L || diff < -2500L) {
                updateState.lastSentPosition = playerState.position
                val seconds = playerState.position / 1000f
                sharedWebView.evaluateJavascript(
                    "if (document.getElementById('player') && document.getElementById('player').contentWindow) { " +
                    "document.getElementById('player').contentWindow.postMessage(JSON.stringify({\"event\": \"command\", \"func\": \"seekTo\", \"args\": [$seconds, true]}), \"*\"); }", 
                    null
                )
            } else {
                updateState.lastSentPosition = playerState.position
            }
        }
    }

    CompositionLocalProvider(
        LocalPipMode provides isInPipMode,
        LocalFullscreenMode provides fullscreenMode,
        LocalSharedWebView provides sharedWebView
    ) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Routes.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem(Routes.YouTube.route, "YouTube", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
            BottomNavItem(Routes.Music.route, "Music", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
            BottomNavItem(Routes.Library.route, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
            BottomNavItem(Routes.Search.route, "Search", Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem(Routes.DSP.route, "DSP", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq)
        )
    }

    // Hide bottom nav on full-screen destinations AND in PiP mode
    val showBottomBar = !isInPipMode && currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    @OptIn(androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
    val adaptiveInfo = androidx.compose.material3.adaptive.currentWindowAdaptiveInfo()
    val navLayoutType = if (showBottomBar) {
        androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    } else {
        androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType.None
    }

    @OptIn(androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
    androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold(
        layoutType = navLayoutType,
        navigationSuiteItems = {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route == item.route
                } == true

                item(
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
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
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                // Mini Player inside responsive space
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    com.deepeye.musicpro.ui.player.MiniPlayer(
                        onClick = {
                            navController.navigate(Routes.NowPlaying.route)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavGraph(
                    navController = navController,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
    } // end CompositionLocalProvider
}
