// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.deepeye.musicpro.ui.screens.NetMirrorScreen
import com.deepeye.musicpro.ui.player.NowPlayingScreen
import com.deepeye.musicpro.ui.music.MusicScreen
import com.deepeye.musicpro.ui.playlist.PlaylistDetailScreen
import com.deepeye.musicpro.ui.search.SearchScreen
import com.deepeye.musicpro.ui.settings.SettingsScreen
import com.deepeye.musicpro.ui.youtube.YouTubeScreen
import com.deepeye.musicpro.ui.library.LikedSongsScreen
import com.deepeye.musicpro.ui.library.PlaylistsScreen
import com.deepeye.musicpro.ui.library.SavedItemsScreen
import com.deepeye.musicpro.ui.chat.ChatListScreen
import com.deepeye.musicpro.ui.chat.ChatAuthScreen
import com.deepeye.musicpro.ui.chat.ChatRoomScreen
import com.deepeye.musicpro.ui.auth.AuthViewModel
import com.deepeye.musicpro.ui.auth.LoginScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    onExpandPlayer: () -> Unit = {},
    onPlayFullscreenMusic: () -> Unit = onExpandPlayer,
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    val startDestination =
        remember(currentUser) {
            if (currentUser == null) Routes.Login.route
            else Routes.Home.route
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

    // Sign-out routing.
    //
    // Fires only after [AuthViewModel.signOut] has finished tearing down the
    // session, so the login screen never renders against a still-valid user.
    //
    // `popUpTo(0) { inclusive = true }` empties the back stack rather than just
    // popping Settings. That is what stops the hardware Back button from
    // returning the user to Home, Library or any other signed-in screen: with
    // no prior entry, Back exits the activity instead of re-entering the app
    // as a logged-in user. `launchSingleTop` guards against a duplicate Login
    // entry if the graph is re-entered.
    LaunchedEffect(navController, authViewModel) {
        authViewModel.signOutCompleted.collect {
            android.util.Log.i("NavGraph", "event=sign_out_nav stage=purge_backstack result=success")
            navController.navigate(Routes.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
        composable(Routes.YouTubeLogin.route) {
            com.deepeye.musicpro.ui.screens.YouTubeLoginScreen(
                onLoginSuccess = { _, _ -> navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Routes.Login.route) {
            LoginScreen(
                onYouTubeLoginClick = { navController.navigate(Routes.YouTubeLogin.route) },
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
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
                    onPlayFullscreenMusic()
                },
                onNavigateToLibrary = { navController.navigate(Routes.Library.route) },
                onNavigateToChat = { navController.navigate(Routes.ChatAuth.route) },
                onOpenV4A = { navController.navigate(Routes.DSP.route) },
                onLaunchTvMode = { navController.navigate(Routes.TvDashboard.route) },
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

        composable(Routes.NetMirror.route) {
            NetMirrorScreen(onExpandPlayer = onExpandPlayer)
        }

        composable(Routes.Music.route) {
            MusicScreen(
                onNavigateToNowPlaying = { onExpandPlayer() },
                onNavigateToSearch = { navController.navigate(Routes.Search.route) },
                onConnectAccount = { navController.navigate(Routes.YouTubeLogin.route) }
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
                onNavigateToHistory = {
                    navController.navigate(Routes.History.route)
                },
                onNavigateToLikedSongs = {
                    navController.navigate(Routes.LikedSongs.route)
                },
                onNavigateToSavedItems = {
                    navController.navigate(Routes.SavedItems.route)
                },
                onNavigateToPlaylists = {
                    navController.navigate(Routes.Playlists.route)
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
        composable(Routes.TvDashboard.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val tvRepo = remember(context) {
                com.deepeye.musicpro.hometheater.scanner.MediaLibraryRepository(
                    com.deepeye.musicpro.hometheater.scanner.LibraryScanner(context)
                )
            }
            val playerViewModel: com.deepeye.musicpro.ui.player.PlayerViewModel = hiltViewModel()
            val movies by tvRepo.movies.collectAsStateWithLifecycle(emptyList())

            androidx.compose.runtime.LaunchedEffect(Unit) {
                tvRepo.refreshLibrary()
            }

            com.deepeye.musicpro.hometheater.ui.TvDashboardScreen(
                movies = movies,
                onMediaSelected = { media ->
                    val item = com.deepeye.musicpro.domain.model.MediaItem.Remote(
                        id = media.id,
                        title = media.title,
                        artist = "",
                        artworkUri = media.posterUrl?.let(android.net.Uri::parse),
                        duration = media.durationMs,
                        streamUri = android.net.Uri.parse(media.uri),
                        isVideo = media.mediaType == com.deepeye.musicpro.hometheater.model.MediaType.MOVIE ||
                            media.mediaType == com.deepeye.musicpro.hometheater.model.MediaType.EPISODE
                    )
                    playerViewModel.playMedia(item)
                    onExpandPlayer()
                },
                onBack = { navController.popBackStack() }
            )
        }


        composable(Routes.Settings.route) {
            SettingsScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToAEOS = { navController.navigate(Routes.AEOS.route) },
                onYouTubeLoginClick = { navController.navigate(Routes.YouTubeLogin.route) },
                onGoogleSignInClick = { navController.navigate(Routes.Login.route) },
                onLaunchTvMode = { navController.navigate(Routes.TvDashboard.route) },
                onSignOut = { authViewModel.signOut() },
                isSignedIn = currentUser != null,
                signedInEmail = currentUser?.email,
            )
        }
        
        composable(Routes.AEOS.route) {
            com.deepeye.musicpro.aeos.ui.dashboard.MusicDashboard()
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = onExpandPlayer
            )
        }
        composable(Routes.Downloads.route) {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = onExpandPlayer
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = onExpandPlayer
            )
        }

        composable(Routes.LikedSongs.route) {
            LikedSongsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = onExpandPlayer
            )
        }

        composable(Routes.SavedItems.route) {
            SavedItemsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Playlists.route) {
            PlaylistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                }
            )
        }

        composable(Routes.ChatAuth.route) {
            ChatAuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.ChatList.route) {
                        popUpTo(Routes.ChatAuth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { chatId, receiverId ->
                    navController.navigate(Routes.ChatRoom.createRoute(chatId, receiverId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ChatRoom.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("receiverId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: return@composable
            ChatRoomScreen(
                chatId = chatId,
                receiverId = receiverId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
