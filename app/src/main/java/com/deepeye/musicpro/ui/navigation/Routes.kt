package com.deepeye.musicpro.ui.navigation

/**
 * Navigation route definitions for DeepEyeMusicPro.
 *
 * Each sealed route maps to a unique screen in the NavGraph.
 */
sealed class Routes(val route: String) {
    data object Home     : Routes("home")
    data object Library  : Routes("library")
    data object Search   : Routes("search")
    data object Settings : Routes("settings")
    data object NowPlaying : Routes("now_playing")
    data object V4A      : Routes("v4a")

    // Parameterized routes
    data object AlbumDetail  : Routes("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }
    data object ArtistDetail : Routes("artist/{artistId}") {
        fun createRoute(artistId: Long) = "artist/$artistId"
    }
    data object PlaylistDetail : Routes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}
