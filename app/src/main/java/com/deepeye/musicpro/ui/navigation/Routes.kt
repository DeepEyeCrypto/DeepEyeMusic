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

    data object VisualizerStudio : Routes("visualizer_studio")

    data object YouTubeLogin : Routes("youtube_login")

    data object Onboarding : Routes("onboarding")

    data object Downloads : Routes("downloads")

    data object History : Routes("history")

    data object LikedSongs : Routes("liked_songs")

    data object SavedItems : Routes("saved_items")

    // Chat feature: Firebase-backed E2E chat was removed. The ChatAuth route
    // is retained only as the typed-unavailable entry point
    // (FeatureUnavailableScreen) — no silent broken Firebase calls.
    data object ChatAuth : Routes("chat_auth")

    data object Playlists : Routes("playlists")

    data object TvDashboard : Routes("tv_dashboard")

    // Removed routes (no longer registered):
    // - Login (duplicate Google/Firebase login UI; replaced by YouTubeLogin)
    // - Gamification (Firebase-backed, removed; UI shows typed-unavailable)
    // - ChatList / ChatRoom (Firebase-backed chat, removed)

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

    data object AEOS : Routes("aeos_dashboard")

    // ChatList / ChatRoom route objects removed — Firebase-backed chat screens
    // deleted; only the typed-unavailable ChatAuth entry point remains.
}
