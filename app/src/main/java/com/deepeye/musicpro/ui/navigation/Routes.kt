// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.navigation

/**
 * Navigation route definitions for DeepEyeMusicPro.
 *
 * Each sealed route maps to a unique screen in the NavGraph.
 */
sealed class Routes(val route: String) {
    data object Home : Routes("home")

    data object YouTube : Routes("youtube")

    data object NetMirror : Routes("netmirror")

    data object Music : Routes("music")

    data object Library : Routes("library")

    data object Search : Routes("search")

    data object Settings : Routes("settings")

    data object NowPlaying : Routes("now_playing")

    data object DSP : Routes("dsp")

    data object Login : Routes("login")

    data object Onboarding : Routes("onboarding")

    data object Downloads : Routes("downloads")

    data object History : Routes("history")

    data object LikedSongs : Routes("liked_songs")

    data object SavedItems : Routes("saved_items")

    data object Playlists : Routes("playlists")

    data object Gamification : Routes("gamification")

    // Parameterized routes
    data object AlbumDetail : Routes("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }

    data object ArtistDetail : Routes("artist/{artistId}") {
        fun createRoute(artistId: Long) = "artist/$artistId"
    }

    data object ArtistPage : Routes("artist_page/{artistName}") {
        fun createRoute(artistName: String) = "artist_page/${android.net.Uri.encode(artistName)}"
    }

    data object PlaylistDetail : Routes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}
