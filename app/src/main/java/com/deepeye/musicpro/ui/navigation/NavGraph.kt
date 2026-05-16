package com.deepeye.musicpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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

/**
 * Main NavGraph for DeepEyeMusicPro.
 *
 * Defines all screen destinations and navigation transitions.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier
    ) {
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
                onNavigateToQueue = { /* queue bottom sheet handled internally */ }
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
