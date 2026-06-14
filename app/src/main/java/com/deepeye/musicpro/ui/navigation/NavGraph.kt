// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.deepeye.musicpro.ui.downloads.DownloadsScreen
import com.deepeye.musicpro.ui.dsp.DSPScreen
import com.deepeye.musicpro.ui.history.HistoryScreen
import com.deepeye.musicpro.ui.homehub.HomeHubScreen
import com.deepeye.musicpro.ui.library.AlbumDetailScreen
import com.deepeye.musicpro.ui.library.ArtistDetailScreen
import com.deepeye.musicpro.ui.library.LibraryScreen
import com.deepeye.musicpro.ui.music.MusicScreen
import com.deepeye.musicpro.ui.onboarding.OnboardingScreen
import com.deepeye.musicpro.ui.playlist.PlaylistDetailScreen
import com.deepeye.musicpro.ui.search.SearchScreen
import com.deepeye.musicpro.ui.settings.SettingsScreen
import com.deepeye.musicpro.ui.youtube.YouTubeScreen

import com.deepeye.musicpro.ui.auth.AuthViewModel
import com.deepeye.musicpro.ui.auth.LoginScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    gateViewModel: OnboardingGateViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onExpandPlayer: () -> Unit = {},
) {
    val onboardingState by gateViewModel.onboardingState.collectAsStateWithLifecycle()

    if (onboardingState == null) {
        // High-Fidelity Premium Splash Loading Gate Screen
        Box(
            modifier =
            androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color(0xFF030307)),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            // Ambient neon backlights
            Box(
                modifier =
                androidx.compose.ui.Modifier
                    .size(250.dp)
                    .blur(80.dp)
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), CircleShape),
            )
            Box(
                modifier =
                androidx.compose.ui.Modifier
                    .size(250.dp)
                    .blur(80.dp)
                    .background(Color(0xFF00D2FF).copy(alpha = 0.15f), CircleShape),
            )

            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00D2FF),
                    strokeWidth = 3.dp,
                    modifier = androidx.compose.ui.Modifier.size(48.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "DEEPEYE MUSIC PRO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF00D2FF),
                    letterSpacing = 3.sp,
                )
            }
        }
        return
    }

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    val startDestination =
        remember(onboardingState, currentUser) {
            if (currentUser == null) Routes.Login.route
            else if (onboardingState == true) Routes.Home.route
            else Routes.Onboarding.route
        }

    val transitionEasing = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val tweenSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
        durationMillis = 420,
        easing = transitionEasing
    )
    val fadeSpec = androidx.compose.animation.core.tween<Float>(
        durationMillis = 420,
        easing = transitionEasing
    )

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tweenSpec
            ) + androidx.compose.animation.fadeIn(animationSpec = fadeSpec)
        },
        exitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tweenSpec
            ) + androidx.compose.animation.fadeOut(animationSpec = fadeSpec)
        },
        popEnterTransition = {
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tweenSpec
            ) + androidx.compose.animation.fadeIn(animationSpec = fadeSpec)
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tweenSpec
            ) + androidx.compose.animation.fadeOut(animationSpec = fadeSpec)
        }
    ) {
        // Login Screen
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(if (onboardingState == true) Routes.Home.route else Routes.Onboarding.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Screen
        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        // ── Bottom Nav Destinations ──
        composable(Routes.Home.route) {
            HomeHubScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToVideo = { videoId ->
                    onExpandPlayer()
                },
                onNavigateToMusic = { musicId ->
                    onExpandPlayer()
                },
                onNavigateToLibrary = { navController.navigate(Routes.Library.route) },
                onOpenV4A = { navController.navigate(Routes.DSP.route) },
                onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
            )
        }

        composable(Routes.YouTube.route) {
            YouTubeScreen(
                onNavigateToVideo = { videoId ->
                    onExpandPlayer()
                },
            )
        }

        composable(Routes.Music.route) {
            MusicScreen(
                onNavigateToNowPlaying = { musicId ->
                    onExpandPlayer()
                },
                onNavigateToSearch = { navController.navigate(Routes.Search.route) }
            )
        }

        composable(Routes.Library.route) {
            LibraryScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                },
                onNavigateToDownloads = {
                    navController.navigate(Routes.Downloads.route)
                },
                onNavigateToNowPlaying = {
                    onExpandPlayer()
                },
            )
        }

        composable(Routes.Search.route) {
            SearchScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToNowPlaying = { onExpandPlayer() },
                onNavigateToArtist = { artistName ->
                    navController.navigate(Routes.ArtistPage.createRoute(artistName))
                },
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen(windowSizeClass = windowSizeClass)
        }

        // ── Full-screen Destinations ──

        composable(Routes.DSP.route) {
            DSPScreen(
                windowSizeClass = windowSizeClass,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Detail Destinations ──
        composable(
            route = Routes.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = { onExpandPlayer() },
            )
        }

        composable(
            route = Routes.ArtistDetail.route,
            arguments = listOf(navArgument("artistId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getLong("artistId") ?: return@composable
            ArtistDetailScreen(
                artistId = artistId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = { onExpandPlayer() },
            )
        }

        composable(
            route = Routes.ArtistPage.route,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
        ) { backStackEntry ->
            com.deepeye.musicpro.ui.artist.ArtistPageScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Downloads.route) {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
