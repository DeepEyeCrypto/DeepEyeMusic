package com.deepeye.musicpro.updates

/**
 * Hardcoded release notes. Easy to maintain, no parser needed, works offline.
 * Add new entries at the top when releasing a new version.
 */
object AppChangelog {
    val entries =
        listOf(
            ChangelogEntry(
                versionCode = 302,
                versionName = "3.0.0.3",
                releaseDate = "June 2026",
                title = "Glassmorphic Updates & Zero Warnings 🚀",
                highlight = true,
                items =
                    listOf(
                        "Completely redesigned the update popup with a stunning glass-morphic aesthetic.",
                        "Fixed profile picture rendering in the Settings screen.",
                        "Massive under-the-hood engine upgrades and deprecation cleanups for extreme stability.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 209,
                versionName = "2.0.9",
                releaseDate = "June 2026",
                title = "Hardware Rendering Unleashed 🚀",
                items =
                    listOf(
                        "Enabled Widevine L1 Hardware Tunneling for premium video playback.",
                        "Optimized ExoPlayer hardware rendering for smoother 1080p.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 13,
                versionName = "2.0.8",
                releaseDate = "June 2026",
                title = "High Quality Video Unlocked",
                highlight = true,
                items =
                listOf(
                    "Video playback defaults to the highest possible and premium 1080p high bitrate quality.",
                    "Fixed an issue with alternative extractors grabbing low-res streams.",
                ),
            ),
            ChangelogEntry(
                versionCode = 12,
                versionName = "2.0.7",
                releaseDate = "June 2026",
                title = "Subscriptions on Home",
                highlight = true,
                items =
                listOf(
                    "The Home and YouTube tabs will now exclusively prioritize and show content from the channels you have subscribed to within the app.",
                ),
            ),
            ChangelogEntry(
                versionCode = 11,
                versionName = "2.0.6",
                releaseDate = "June 2026",
                title = "YouTube Video Share Fix",
                highlight = true,
                items =
                listOf(
                    "Shared YouTube links will now open and play as video directly instead of just audio.",
                ),
            ),
            ChangelogEntry(
                versionCode = 10,
                versionName = "2.0.5",
                releaseDate = "June 2026",
                title = "YouTube Share Integration",
                highlight = true,
                items =
                listOf(
                    "You can now share ANY YouTube video directly to DeepEye Music Pro!",
                    "Just tap 'Share' on a video in the YouTube app or your browser, select DeepEye Music Pro, and it will instantly play as high-quality audio.",
                    "Supports standard YouTube videos and YouTube Shorts."
                ),
            ),
            ChangelogEntry(
                versionCode = 9,
                versionName = "2.0.4",
                releaseDate = "June 2026",
                title = "File Manager Integration",
                highlight = true,
                items =
                listOf(
                    "DeepEye Music Pro now officially registers as an audio player in Android!",
                    "You can now directly open any song from your file manager and it will instantly play in the app.",
                ),
            ),
            ChangelogEntry(
                versionCode = 8,
                versionName = "2.0.3",
                releaseDate = "June 2026",
                title = "Local Audio Recommendations",
                highlight = true,
                items =
                listOf(
                    "Fixed an issue where playing local audio files from the file manager wouldn't generate autoplay recommendations.",
                    "The app now intelligently searches YouTube for the local song's title and artist to build your queue!",
                ),
            ),
            ChangelogEntry(
                versionCode = 7,
                versionName = "2.0.2",
                releaseDate = "June 2026",
                title = "Glassmorphic Now Playing Screen",
                highlight = true,
                items =
                listOf(
                    "Cleared the opaque background from the Now Playing screen.",
                    "The gorgeous glassmorphic haze effect now shines through when the player is expanded!",
                ),
            ),
            ChangelogEntry(
                versionCode = 6,
                versionName = "2.0.1",
                releaseDate = "June 2026",
                title = "Auto-Update & Changelog Fixes",
                highlight = true,
                items =
                listOf(
                    "Fixed auto-updater checking logic.",
                    "Fixed changelog dialog not appearing for new versions.",
                ),
            ),
            ChangelogEntry(
                versionCode = 5,
                versionName = "2.0.0",
                releaseDate = "June 2026",
                title = "Massive UI Redesign & Stability",
                highlight = true,
                items =
                listOf(
                    "Redesigned Home UI with fixed glassmorphic top header and dock.",
                    "Added real-time Bitcoin ticker header via Binance WebSocket.",
                    "Fixed critical playback race condition caused by prefetchers.",
                    "Removed Maroon color and replaced with Neon Green/Gold themes.",
                    "Improved 0-latency playback response times.",
                ),
            ),
            ChangelogEntry(
                versionCode = 4,
                versionName = "1.0.4",
                releaseDate = "May 2026",
                title = "Bug Fixes & Slider Overhaul",
                highlight = true,
                items =
                listOf(
                    "Fixed the thick slider bug in the player.",
                    "Performance optimizations and logging improvements.",
                ),
            ),
            ChangelogEntry(
                versionCode = 3,
                versionName = "1.0.3",
                releaseDate = "May 2026",
                title = "Library & Search Overhaul",
                highlight = false,
                items =
                listOf(
                    "Full offline library with liked songs, playlists, and downloads.",
                    "Premium search with smart filter chips and artist pages.",
                    "Dynamic color theming and glassmorphism system.",
                    "Gesture-rich mini player with swipe queue controls.",
                    "Download manager with progress tracking.",
                ),
            ),
            ChangelogEntry(
                versionCode = 2,
                versionName = "1.0.2",
                releaseDate = "May 2026",
                title = "Video & Fullscreen Improvements",
                items =
                listOf(
                    "Added Picture-in-Picture support for video mode.",
                    "Immersive fullscreen with gesture controls.",
                    "Improved playback sync between UI and player.",
                    "DSP equalizer with custom presets.",
                    "Autoplay and smart queue generation.",
                ),
            ),
            ChangelogEntry(
                versionCode = 1,
                versionName = "1.0.0",
                releaseDate = "May 2026",
                title = "Initial Release",
                items =
                listOf(
                    "YouTube Music streaming with background playback.",
                    "Local music library from MediaStore.",
                    "Taste profile onboarding for personalized recommendations.",
                    "Premium dark-first Material 3 design.",
                    "Edge-to-edge UI with splash screen.",
                ),
            ),
        )
}
