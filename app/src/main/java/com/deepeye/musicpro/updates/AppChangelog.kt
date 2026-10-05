package com.deepeye.musicpro.updates

/**
 * Hardcoded release notes. Easy to maintain, no parser needed, works offline.
 * Add new entries at the top when releasing a new version.
 */
object AppChangelog {
    val entries =
        listOf(
            ChangelogEntry(
                versionCode = 30069,
                versionName = "3.0.1.59",
                releaseDate = "October 2026",
                title = "Visualizer Scene Touch & Gesture Conflict Fix + Unified Library Modal 🎨✨",
                highlight = true,
                items =
                    listOf(
                        "Resolved Gesture & Touch Interception: Fixed background pointer handler from hijacking button clicks and horizontal scroll gestures.",
                        "Unified Visualizer Library Modal: Redesigned scene selector with adaptive grid (2-col portrait, 3/4-col landscape), integrated intensity slider, and instantaneous touch feedback.",
                        "Action Chip Ergonomics: Expanded touch targets to 38dp+ with minTouchTarget across all overlay and visualizer controls."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30068,
                versionName = "3.0.1.58",
                releaseDate = "October 2026",
                title = "Landscape Ultra-Compact TopBar & Music Card Geometry Refinements 🎨📱",
                highlight = true,
                items =
                    listOf(
                        "Adaptive Single-Line Landscape Header: Combined Title, Search Pill, Segmented Tabs, and Refresh into an ultra-sleek 46dp top bar, freeing +130dp of vertical viewport on landscape screens.",
                        "Enhanced Music Card Geometry: Sized cards to 124dp with balanced 5-column grid density, 2-line title support, and glassmorphic neon borders.",
                        "Zero-Clipping Multi-Rail Viewport: Full multi-rail carousels now visible simultaneously without vertical collision."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30067,
                versionName = "3.0.1.57",
                releaseDate = "October 2026",
                title = "Visuals Engine VSYNC Overhaul: Locked 60FPS Hardware-Synced Animation 🎨⚡",
                highlight = true,
                items =
                    listOf(
                        "Zero-Recomposition VSYNC Frame Clocks: Injected continuous withFrameNanos hardware clocks across Spectrum Bars, Waveform Ribbon, Radial Pulse, and Particle Field visualizers, completely eliminating the 1-FPS freeze bug.",
                        "Liquid Audio-Reactive Interpolation: Exponential moving average interpolators now render at the display's native refresh rate (60Hz/120Hz).",
                        "Monet Neon Palette Blending: Multi-stop gradients derived dynamically from active album artwork now illuminate all 2D and 3D visualizer scenes."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30066,
                versionName = "3.0.1.56",
                releaseDate = "October 2026",
                title = "PiP Engine Rewrite: Zero Stutter Transitions & True Non-Conflicting Display 🚀🖼️",
                highlight = true,
                items =
                    listOf(
                        "Zero-Stutter OS PiP: Completely eliminated audio drops and video buffering hitches when entering System Picture-in-Picture by silencing redundant preparations.",
                        "Direct PiP Syncing: Removed artificial delays. OS snapshots and bounds calculations now transition instantly down to the millisecond.",
                        "Smart Floating PiP Collision Fix: Floating app PiP cleanly stays out of the way when the full Player Sheet is expanded, fixing double-surface detachment bugs.",
                        "Ergonomic Touch Bounds: Re-calibrated the floating PiP mini-player frame to 220dp for more comfortable layout flow on strict mobile displays."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30065,
                versionName = "3.0.1.55",
                releaseDate = "October 2026",
                title = "100% Authentic YouTube Account Integration: Direct Feeds & Smart Filter Optimization 🎬🎵",
                highlight = true,
                items =
                    listOf(
                        "100% Account Stream Integrity: Trusted authentic YouTube Music home feeds (`FEmusic_home`) and liked songs without dropping personal tracks.",
                        "Smart Music Filtering: Refined regex to allow topic channels, artist names, and authentic non-keyword track titles.",
                        "Direct Category Feeds: Prevented silent fallbacks to generic searches for authenticated users, preserving true personalized feeds.",
                        "Robust Auth Recovery: Enhanced InnerTube OAuth 401 retry handling and streamlined parsing."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30064,
                versionName = "3.0.1.54",
                releaseDate = "October 2026",
                title = "Update & Download Engine Debug: Offline Playback, Live Progress & Storage Control 🚀📦",
                highlight = true,
                items =
                    listOf(
                        "100% Offline Playback: Downloaded tracks now resolve and stream directly from local storage with zero network dependency.",
                        "Live Download Metrics: Real-time progress bar, percentage calculations, and transfer sizes (MB) in downloads hub and OTA dialog.",
                        "Storage Management: Added instant delete actions for downloaded tracks to effortlessly free up on-device storage.",
                        "Dynamic OTA Reporting: Accurate GitHub release asset parsing, rate-limit fallback messages, and dynamic version indicators."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30063,
                versionName = "3.0.1.53",
                releaseDate = "October 2026",
                title = "Android 16 Runtime Verifier Fix & Stable Player Architecture 🛡️⚡",
                highlight = true,
                items =
                    listOf(
                        "Resolved VerifyError: Encapsulated sprawling composable parameter lists into unified `PlayerActionCallbacks` data model, fixing Android 16 ART bytecode verification rejection.",
                        "Zero-Crash Launch: App launches smoothly at locked 60FPS with zero runtime register allocation conflicts.",
                        "Enhanced Floating Dock: Seamlessly connects all action callbacks, Monet palette transitions, and direct visualizer studio controls."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30062,
                versionName = "3.0.1.52",
                releaseDate = "October 2026",
                title = "Polished Neon Dock & Balanced Tool Dimensioning 🎛️💎",
                highlight = true,
                items =
                    listOf(
                        "Centered Floating Pill Dock: Replaced full-width edge-stretching action bar with an elegant centered glass dock, eliminating trailing black voids.",
                        "Uniform Button Geometry: Standardized all action pills with balanced dimensions, consistent min-widths, and high-visibility capsule shapes.",
                        "Vibrant Neon Monet Accents: Injected rich category-specific glass colors and glowing neon active states into fullscreen controls and Now Playing quick tools."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30061,
                versionName = "3.0.1.51",
                releaseDate = "October 2026",
                title = "Fullscreen Visualizer Studio: Direct 'Visuals' Action Chip on Fullscreen HUD ✨🎨",
                highlight = true,
                items =
                    listOf(
                        "Fullscreen 'Visuals' Action Chip: Added direct 'Visuals' button to the fullscreen video & audio player action bar for instantaneous shader switching.",
                        "Live Shader & Particle Studio: Instantly opens the GPU Visualizer library modal sheet (AGSL shaders, Spectrum, 3D particles) with intensity and motion tuning without exiting fullscreen.",
                        "Unified Video Experience: Complete parity between Now Playing quick tools and landscape fullscreen player controls."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30060,
                versionName = "3.0.1.50",
                releaseDate = "October 2026",
                title = "Fullscreen Engagement Engine: Real Like, Dislike, Subscribe & Download Controls 👍🔔📥",
                highlight = true,
                items =
                    listOf(
                        "Real Like & Dislike Controls: Direct InnerTube backend sync with optimistic feedback state and dynamic color glow on full video/audio overlays.",
                        "Live Channel Subscriptions: Real-time channel subscribe/unsubscribe action chips directly accessible from the fullscreen OSD toolbar.",
                        "Direct Offline Downloads: Instant background audio/video downloads with live caching progress indicators and offline checkmarks.",
                        "Unified Video & Visualizer HUD: Seamless integration across both landscape video playback and 60FPS audio visualizer modes."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30059,
                versionName = "3.0.1.49",
                releaseDate = "October 2026",
                title = "Direct Fullscreen GPU Visualizer Playback & CI Hardening 🌌⚡",
                highlight = true,
                items =
                    listOf(
                        "Direct Fullscreen Visualizers: Playing music from Music Hub, Continue Listening, or Library now launches immediately into full-screen 60FPS GPU Visualizer mode without intermediate sheets.",
                        "Fullscreen Audio Visualizer OSD: Integrated Monet Dynamic Palette, ambient blur backdrop, real-time karaoke lyrics, and full gesture controls in audio visualizer mode.",
                        "CI Test Suite Green: Hardened video overlay unit tests and updated test matrix to verify EQ and Synced Lyrics action chips."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30058,
                versionName = "3.0.1.48",
                releaseDate = "October 2026",
                title = "Fullscreen Video Overlay: EQ Equalizer, Synced Lyrics & Clean Action Bar 🎛️",
                highlight = true,
                items =
                    listOf(
                        "EQ & Lyrics on Fullscreen: Added dedicated 'EQ' (DSP Engine) and 'Lyrics' (Synced Karaoke) action chips directly onto the landscape fullscreen video player overlay.",
                        "Sleep Timer Added: Added quick 'Sleep' timer access right inside the video player bottom bar.",
                        "Clean Action Bar: Removed non-essential Like/Dislike action chips from the video overlay for a streamlined, clutter-free viewing experience."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30057,
                versionName = "3.0.1.47",
                releaseDate = "October 2026",
                title = "Floating PiP Responsive Controls & Dismiss Fix 🪟",
                highlight = true,
                items =
                    listOf(
                        "Floating PiP Close Fix: Fixed touch event interception on the floating mini video player, making the 'X' (Close) button instantly responsive to dismiss the floating window.",
                        "Seamless Background Playback: Dismissing the floating PiP window smoothly retains continuous audio playback without interruption in the bottom miniplayer bar.",
                        "Gesture Disambiguation: Separated 1-finger drag and 2-finger pinch gestures from top action buttons for flawless touch responsiveness."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30056,
                versionName = "3.0.1.46",
                releaseDate = "October 2026",
                title = "Play Protect Hardening & Full V1-V4 Signature Schemes 🛡️",
                highlight = true,
                items =
                    listOf(
                        "Play Protect Compliance: Narrowed permission scopes with 'neverForLocation' on Bluetooth/Wi-Fi to prevent unknown scanning flags.",
                        "Full V1-V4 Signature Enforce: Release builds now enforce APK Signature Schemes V1, V2, V3, and V4 for maximum Android 14/15/16 security trust.",
                        "Monet-Omega & Ambient Blur: Dynamic 800ms shader crossfades and 80dp hardware-accelerated background blur.",
                        "Lyrics-Omega Engine: Live synchronized karaoke vocals with InnerTube timed parsing and LRCLIB fallback."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30055,
                versionName = "3.0.1.45",
                releaseDate = "October 2026",
                title = "Omega Triple Threat: Monet AGSL Shaders, Ambient Blur & Synced Lyrics 🚀",
                highlight = true,
                items =
                    listOf(
                        "Monet-Omega Dynamic AGSL Shaders: Real-time 60FPS GPU visualizers dynamically modulated with extracted thumbnail colors (iColorPrimary, iColorSecondary) via 800ms smooth state-driven crossfades.",
                        "Ambient-Omega Immersive UI: 80dp hardware-accelerated ambient blur background, luminance-driven contrast text math, and 3-tier gradient safety scrim.",
                        "Lyrics-Omega Synced Vocals: Dual-engine live karaoke lyrics with YouTube Music InnerTube timed lyrics extraction (/next -> /browse MPLY) and LRCLIB fallback with viewport-centering auto-scroll.",
                        "Enterprise Offline Caching: ExoPlayer 2GB LRU SimpleCache with background WorkManager Opus ITAG 251 caching for gapless offline playback.",
                        "DSP FTZ/DAZ Audio Engine: Zero NaN/Infinity flushing and float sanitization across all ViPER4Android DSP audio processors."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30053,
                versionName = "3.0.1.43",
                releaseDate = "October 2026",
                title = "V4A DSP Live Telemetry Diagnostics & SmartTube-Omega Engine 🚀",
                highlight = true,
                items =
                    listOf(
                        "V4A DSP Live Diagnostics: Real-time telemetry dashboard in Settings > Audio Engine tracking AudioSession IDs, Dynamic Gain Budgets, Clip Risk, and Headroom.",
                        "Verbose Audio Engine Logging: Granular [V4A DEBUG] telemetry switch for live ExoPlayer session attach/detach tracing.",
                        "SmartTube-Omega InnerTube Core: Direct youtubei/v1 endpoint processing for authentic YouTube Music recommendations & zero-latency AutoPlay queue generation.",
                        "Cyberpunk Dual-Pane Auth Architecture: Responsive landscape two-column layout with left Hero branding and right Glassmorphic Auth Card.",
                        "Zero-Interruption Launch: Removed onboarding bottlenecks for direct, instant access to the player dashboard."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30052,
                versionName = "3.0.1.42",
                releaseDate = "October 2026",
                title = "Cyberpunk Two-Column Auth Redesign & Cloud Account Engine 🚀",
                highlight = true,
                items =
                    listOf(
                        "Dual-Pane Auth Architecture: Redesigned Login & Registration with responsive Cyberpunk landscape dual-pane layout, eliminating viewport clipping.",
                        "Direct Google & YouTube TV Pairing: Fast-track authentication with Google SSO and YouTube TV Device Code activation.",
                        "Lifecycle-Scoped Background Sync: App-scoped coroutine dispatching preventing cancellation during auth transitions.",
                        "Accurate Video History Progress: Replaced mock completion statistics in Continue Watching with exact video playback time tracking.",
                        "ViPER4Android DSP & Zero-NaN Master Limiter: Full 60 FPS slider debouncing and -0.5 dBFS hardware audio headroom."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30051,
                versionName = "3.0.1.41",
                releaseDate = "October 2026",
                title = "Google & YouTube Cloud Account Engine & Real Watch Tracking 🚀",
                highlight = true,
                items =
                    listOf(
                        "Unified Cloud & Account Control Center: Integrated Google Account (Firebase Auth/SSO) and YouTube TV Device OAuth Pairing directly in Settings > Account.",
                        "Direct Google & YouTube Login: Fast-track authentication for subscriptions, playlists, and cloud sync across all devices.",
                        "Accurate Video History Progress: Replaced mock completion statistics in Continue Watching with exact video playback time tracking.",
                        "ViPER4Android DSP & Zero-NaN Master Limiter: Full 60 FPS slider debouncing and -0.5 dBFS hardware audio headroom.",
                        "InnerTube Authenticated Client: Full Bearer token support for private streams, playlists, and subscription feeds."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30046,
                versionName = "3.0.1.36",
                releaseDate = "September 2026",
                title = "Material 3 Adaptive & Studio DSP Overhaul 🚀",
                highlight = true,
                items =
                    listOf(
                        "Material 3 Adaptive Architecture: Complete system-wide overhaul across Home Hub, YouTube Cinema, Video Hub, Music, Library, Settings, and Video Overlay with fluid responsive scaling.",
                        "Studio 10-Band EQ & Spline: Hardware-grade parametric Bezier spline curve, 1-tap studio presets, tactile vertical faders with tap-to-set and 0ms audio latency.",
                        "Audio Engine DSP Spline Interpolation: Fixed frequency mismatch by implementing Logarithmic Frequency Spline Interpolation across all hardware bands.",
                        "Expanded Action Dock: Adaptive edge-to-edge layout with 52dp buttons and responsive spacing for all screen sizes.",
                        "Cyberpunk Glassmorphism: Tier-1 automotive dark glass surfaces, neon cyan glowing borders, and high-contrast glanceable typography."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30045,
                versionName = "3.0.1.35",
                releaseDate = "September 2026",
                title = "Ultra-Max Edge-to-Edge Player Controls & Badges Overhaul 🚀",
                highlight = true,
                items =
                    listOf(
                        "Ultra-Max Video Overlay: Expanded bottom action dock with massive 64dp buttons and 46dp padding, filling landscape screens edge-to-edge with zero void trailing gaps.",
                        "Giant Floating Badges: Redesigned 4K, HDR, 60FPS, and Time badges with 18sp extra-bold typography and frosted neon glowing borders.",
                        "Direct Seeker Precision: Enlarged scrubbing track height and touch thumb radius with high-contrast timestamp typography.",
                        "Guest Mode Auth Skip: Instant 'Skip & Continue as Guest' fast track on login screen for direct access without Google sign-in.",
                        "Persistent In-App Changelog: Added manual 'View Changelog' button in Settings > About with offline cached release history."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30044,
                versionName = "3.0.1.34",
                releaseDate = "September 2026",
                title = "Edge-to-Edge Player Controls & Typography Scale",
                highlight = false,
                items =
                    listOf(
                        "Ultra-Max Video Overlay: Expanded bottom action dock with massive 64dp buttons and 46dp padding, filling landscape screens edge-to-edge.",
                        "Giant Floating Badges: Redesigned 4K, HDR, 60FPS, and Time badges with 18sp extra-bold typography and frosted neon glowing borders.",
                        "Direct Seeker Precision: Enlarged scrubbing track height and touch thumb radius with high-contrast timestamp typography.",
                        "Persistent In-App Changelog: Added manual 'View Changelog' button in Settings > About with offline cached release history."
                    ),
            ),
            ChangelogEntry(
                versionCode = 30043,
                versionName = "3.0.1.33",
                releaseDate = "September 2026",
                title = "ViPER4Android Audiophile DSP & Futuristic UI Overhaul 🎛️",
                highlight = false,
                items =
                    listOf(
                        "Anti-Clipping DSP Engine: Implemented hyperbolic soft-knee saturation, automatic dynamic headroom, and decoupled framework effects for zero digital distortion.",
                        "5 Master ViPER4Android Presets: ViPER Bass & X-HiFi, Audiophile Pure Hi-Res, Cyberpunk Spatial 3D, Warm 6J1 Vacuum Tube, and Club EDM Sub-Woofer.",
                        "Ergonomic Video Overlay: Moved Lock, PiP, and Stats controls to bottom scrollable action dock with clean notch-safe top bar.",
                        "Magic Navigation Bar Glow: Added dynamic spring-animated sliding capsule indicator with neon glow border.",
                        "120 FPS Fluid Performance: Assigned stable keys across all home carousels with tactile micro-press physics.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30042,
                versionName = "3.0.1.32",
                releaseDate = "September 2026",
                title = "YouTube Personalization & Like/Dislike/Sub Sync 🚀",
                highlight = true,
                items =
                    listOf(
                        "Full YouTube Account Sync: Like, Dislike, and Subscriptions sync directly with your connected YouTube account.",
                        "Real YouTube Personalization: Subscriptions feed, Liked videos, Watch History, and personalized recommendations.",
                        "Player Lifecycle & Stability: Fixed dead handler crash in background playback service.",
                        "Ergonomic Video Overlay: Shifted top controls downward away from status bar for a cleaner view.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30038,
                versionName = "3.0.1.28",
                releaseDate = "August 2026",
                title = "SmartTube Innertube & SponsorBlock Integration ⚡",
                highlight = true,
                items =
                    listOf(
                        "Native SmartTube Innertube Extractor: fast and stable YouTube video and audio stream extraction.",
                        "SponsorBlock & Return YouTube Dislike integration for enhanced, uninterrupted playback.",
                        "Mobile OAuth enhancements with Chrome Custom Tabs and Android Keystore secure storage.",
                        "Optimized search ranking, stream extraction, and responsive UI across all screens.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30037,
                versionName = "3.0.1.27",
                releaseDate = "August 2026",
                title = "SmartTube-Style Controls & Account Sync 🚀",
                highlight = false,
                items =
                    listOf(
                        "SmartTube-style playback controls (Speed, Audio Boost, Sleep Timer, OLED Black, Stats HUD).",
                        "YouTube Account Sync: Subscriptions, Watch History, Liked Videos, and Watch Later fully connected.",
                        "Dynamic YouTube category ribbon for logged-in accounts.",
                        "Fixed Now Playing bottom action row cut-off behind gesture navigation bar.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30030,
                versionName = "3.0.1.20",
                releaseDate = "July 2026",
                title = "Autoplay Hotfix ⚡",
                highlight = true,
                items =
                    listOf(
                        "Fixed a bug where playlist autoplay and media keys (Next/Prev) were disabled by Android's MediaSession.",
                        "Bluetooth controls and lockscreen Next/Prev buttons now correctly work with custom playlists.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30029,
                versionName = "3.0.1.19",
                releaseDate = "July 2026",
                title = "Playlist Queue & Autoplay Upgrade 🎬",
                highlight = true,
                items =
                    listOf(
                        "Drama & Web Series episodes now play as a playlist — click any episode and the rest auto-play in order!",
                        "Improved Related Videos: uses a direct SmartTube-style Innertube client for more accurate recommendations.",
                        "YouTube API fallback: all content fetchers now gracefully fall back when API quota is exceeded.",
                        "Cloud History Sync: your listening history now backs up to your Google account.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30028,
                versionName = "3.0.1.18",
                releaseDate = "July 2026",
                title = "Now Playing Layout Fix 🔧",
                highlight = true,
                items =
                    listOf(
                        "Fixed a layout issue on the Now Playing screen where the bottom Action Row was being cut off on certain devices.",
                        "The controls now have proper spacing and alignment.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30014,
                versionName = "3.0.1.4",
                releaseDate = "June 2026",
                title = "Music Personalization Sync ☁️",
                highlight = true,
                items =
                    listOf(
                        "Fixed issue where music personalization (languages, genres, artists) was lost on app reinstall.",
                        "Search history and music tastes now securely sync with your Google account.",
                        "Instant cloud backup whenever you update your music preferences.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 30013,
                versionName = "3.0.1.3",
                releaseDate = "June 2026",
                title = "Library & Album Art Fixes 🎵",
                highlight = true,
                items =
                    listOf(
                        "Fixed scrolling and clipping issues in the Library section.",
                        "Resolved blurry album artworks by forcing high-resolution images.",
                    ),
            ),
            ChangelogEntry(
                versionCode = 305,
                versionName = "3.0.0.6",
                releaseDate = "June 2026",
                title = "Real Players Only 🏆",
                highlight = false,
                items =
                    listOf(
                        "Removed fake placeholder players from the leaderboard. You will only see real players now!",
                    ),
            ),
            ChangelogEntry(
                versionCode = 304,
                versionName = "3.0.0.5",
                releaseDate = "June 2026",
                title = "Leaderboard Glow Up 🏆",
                highlight = true,
                items =
                    listOf(
                        "Renamed 'Top Gamers' to 'Top Players'.",
                        "Added default placeholders so the Leaderboard always proudly displays 3 ranks even if you are early to the party!",
                    ),
            ),
            ChangelogEntry(
                versionCode = 303,
                versionName = "3.0.0.4",
                releaseDate = "June 2026",
                title = "Gamification Cloud Sync ☁️",
                highlight = true,
                items =
                    listOf(
                        "Your points, streak, and badges now permanently sync to your Google Account!",
                        "Fixed a massive issue where points would reset upon reinstalling the app.",
                    ),
            ),
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
