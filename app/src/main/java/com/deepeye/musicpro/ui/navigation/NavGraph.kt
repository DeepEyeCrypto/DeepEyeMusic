package com.deepeye.musicpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.musicpro.ui.homehub.HomeHubScreen
import com.deepeye.musicpro.ui.library.AlbumDetailScreen
import com.deepeye.musicpro.ui.library.ArtistDetailScreen
import com.deepeye.musicpro.ui.library.LibraryScreen
import com.deepeye.musicpro.ui.player.NowPlayingScreen
import com.deepeye.musicpro.ui.playlist.PlaylistDetailScreen
import com.deepeye.musicpro.ui.playlist.PlaylistScreen
import com.deepeye.musicpro.ui.search.SearchScreen
import com.deepeye.musicpro.ui.settings.SettingsScreen
import com.deepeye.musicpro.ui.v4a.V4AScreen
import com.deepeye.musicpro.ui.youtube.YouTubeScreen
import com.deepeye.musicpro.ui.music.MusicScreen
import com.deepeye.musicpro.ui.onboarding.OnboardingScreen

/**
 * Main NavGraph for DeepEyeMusicPro.
 *
 * Defines all screen destinations and navigation transitions.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    gateViewModel: OnboardingGateViewModel = hiltViewModel()
) {
    val onboardingState by gateViewModel.onboardingState.collectAsStateWithLifecycle()

    if (onboardingState == null) {
        // High-Fidelity Premium Splash Loading Gate Screen
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color(0xFF030307)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            // Ambient neon backlights
            Box(
                modifier = androidx.compose.ui.Modifier
                    .size(250.dp)
                    .blur(80.dp)
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), CircleShape)
            )
            Box(
                modifier = androidx.compose.ui.Modifier
                    .size(250.dp)
                    .blur(80.dp)
                    .background(Color(0xFF00D2FF).copy(alpha = 0.15f), CircleShape)
            )

            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00D2FF),
                    strokeWidth = 3.dp,
                    modifier = androidx.compose.ui.Modifier.size(48.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "DEEPEYE MUSIC PRO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF00D2FF),
                    letterSpacing = 3.sp
                )
            }
        }
        return
    }

    val startDestination = remember(onboardingState) {
        if (onboardingState == true) Routes.Home.route else Routes.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding Screen
        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Bottom Nav Destinations ──
        composable(Routes.Home.route) {
            HomeHubScreen(
                onNavigateToVideo = { videoId -> 
                    navController.navigate(Routes.NowPlaying.route) 
                },
                onNavigateToMusic = { musicId -> 
                    navController.navigate(Routes.NowPlaying.route) 
                },
                onNavigateToLibrary = { navController.navigate(Routes.Library.route) },
                onOpenV4A = { navController.navigate(Routes.V4A.route) }
            )
        }

        composable(Routes.YouTube.route) {
            YouTubeScreen(
                onNavigateToVideo = { videoId ->
                    navController.navigate(Routes.NowPlaying.route)
                }
            )
        }

        composable(Routes.Music.route) {
            MusicScreen(
                onNavigateToNowPlaying = { musicId ->
                    navController.navigate(Routes.NowPlaying.route)
                }
            )
        }

        composable(Routes.Library.route) {
            LibraryScreen(
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        composable(Routes.Search.route) {
            SearchScreen(
                onNavigateToNowPlaying = { navController.navigate(Routes.NowPlaying.route) }
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen()
        }

        // ── Full-screen Destinations ──
        composable(Routes.NowPlaying.route) {
            NowPlayingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToV4A = { navController.navigate(Routes.V4A.route) },
                onNavigateToQueue = { /* queue bottom sheet handled internally */ },
                onNavigateToSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.V4A.route) {
            V4AScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Detail Destinations ──
        composable(
            route = Routes.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = { navController.navigate(Routes.NowPlaying.route) }
            )
        }

        composable(
            route = Routes.ArtistDetail.route,
            arguments = listOf(navArgument("artistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getLong("artistId") ?: return@composable
            ArtistDetailScreen(
                artistId = artistId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
