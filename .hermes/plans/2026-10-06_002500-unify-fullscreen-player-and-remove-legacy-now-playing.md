# Plan: Unify Fullscreen Video/Audio Player & Remove Legacy Portrait NowPlaying

## Goal
Remove the legacy portrait `AudioNowPlayingLayout` entirely and wire all navigation entry points across the app directly to the immersive Fullscreen Video / Visualizer Player layout.

## Current Context & Forensics
- Currently, `NowPlayingScreen.kt` contains three layouts:
  1. `VideoNowPlayingLayout`: High-fidelity fullscreen video player with Ambilight, gesture zoom/pan, double-tap seek, overlay controls, format selector, audio track selector, and action chips.
  2. `AudioFullscreenVisualizerLayout`: High-fidelity fullscreen visualizer layout with Ambient Monet artwork lighting, GPU shaders/canvas scenes, gesture controls, and playback bar.
  3. `AudioNowPlayingLayout`: Legacy portrait card layout with square album art, small seekbar, and static song details.
- User Directive: Delete the legacy portrait `AudioNowPlayingLayout` and wire all NowPlaying invocations (from MiniPlayer, track clicks, Search, Library, Playlists, Home, and Navigation) directly to the unified Fullscreen Player.

## Architecture & Proposed Approach
1. **Unified Fullscreen Player Pipeline**:
   - In `NowPlayingScreen.kt`, simplify the root rendering logic:
     - If `isVideoMode == true` -> render `VideoNowPlayingLayout` (with full video surface and Ambilight).
     - If `isVideoMode == false` -> render `AudioFullscreenVisualizerLayout` (with full AGSL GPU / Canvas visualizer and Ambient artwork lighting).
   - Delete `AudioNowPlayingLayout` (~550 lines of legacy code removed).
2. **Global Navigation & MiniPlayer Expansion Wireup**:
   - In `DeepEyeMusicApp.kt`, `NavGraph.kt`, `HomeScreen.kt`, `SearchScreen.kt`, `MusicScreen.kt`, `LibraryScreen.kt`, `PlaylistDetailScreen.kt`, `HistoryScreen.kt`, and `DownloadsScreen.kt`:
     - Ensure every `onNavigateToNowPlaying` and MiniPlayer click expands the unified Fullscreen Player seamlessly.

## Step-by-Step Implementation Tasks

### Task 1: Clean and Refactor `NowPlayingScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- Remove `AudioNowPlayingLayout` function.
- In `NowPlayingScreen` composable, dispatch directly to `VideoNowPlayingLayout` (when in video mode) or `AudioFullscreenVisualizerLayout` (when in audio mode).
- Ensure back button in both layouts calls `onNavigateBack` to cleanly collapse the sheet.

### Task 2: Verify and Wire Navigation in `DeepEyeMusicApp.kt` & `NavGraph.kt`
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/DeepEyeMusicApp.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/navigation/NavGraph.kt`
- Ensure `onNavigateToNowPlaying` triggers `sheetViewModel.expand()`.

### Task 3: Unit Tests & Build Debug APK
- Run `./gradlew --no-daemon testDebugUnitTest`.
- Run `./gradlew --no-daemon assembleDebug`.

### Task 4: Sideload & Verify on Realme Device
- Install APK on Realme RMX3945.
- Verify clicking any song or video opens the fullscreen player directly with zero glitches.
- Capture screen forensics.

### Task 5: Bump Version & Release `v3.0.1.64`
- Bump version to `versionCode = 30074`, `versionName = "3.0.1.64"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag `v3.0.1.64`, and push to GitHub.
