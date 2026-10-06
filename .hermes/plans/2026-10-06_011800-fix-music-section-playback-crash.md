# Plan: Fix Music Section Playback Crash & Normalize Media Routing

## Goal
Fix the playback crash occurring when playing songs from the Music section by normalizing media item types (`isVideo = false` for songs), standardizing navigation expansion to `onExpandPlayer()`, and adding safe guards to `MusicViewModel.kt`.

## Root Cause Analysis
1. **Misconfigured Video Flag (`isVideo = true`) on Audio Tracks**:
   - In `MusicViewModel.kt` (`playPersonalizedItem`), fallback items were instantiated with `isVideo = true` unconditionally:
     ```kotlin
     it.mediaItem ?: MediaItem.Remote(
         id = it.id,
         title = it.title,
         artist = it.artist,
         artworkUri = it.artworkUrl?.let { url -> Uri.parse(url) },
         duration = it.durationMs,
         isVideo = true, // BUG: Audio music tracks forced into VideoPlayer surface
     )
     ```
   - Forcing audio-only streams into the native `VideoPlayerView` surface caused decoder / surface mismatch and renderer errors.
2. **Forced Orientation Collision on Navigation**:
   - `NavGraph.kt` was calling `onPlayFullscreenMusic()` (which forced `fullscreenMode.enter(forceLandscape = true)`) specifically for `MusicScreen`, while other screens called standard `onExpandPlayer()`. Abrupt landscape rotation during bottom sheet expansion caused configuration jank.
3. **Empty/Null ID Guard**:
   - `playPersonalizedItem` lacked a validation check for empty/blank item IDs.

## Architecture & Proposed Approach
1. **Fix `MusicViewModel.kt` Media Item Mapping**:
   - Dynamically determine `isVideo` based on `item.itemType == PersonalizedItemType.VIDEO`.
   - Add validation filter ensuring only valid, non-blank items enter the queue.
2. **Standardize `NavGraph.kt`**:
   - Route `MusicScreen`'s `onNavigateToNowPlaying` to standard `onExpandPlayer()`.
3. **Harmonize `DeepEyeMusicApp.kt` Expansion**:
   - Ensure `onExpandPlayer` smoothly expands the player sheet without jarring forced orientation changes.

## Step-by-Step Implementation Tasks

### Task 1: Patch `MusicViewModel.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/music/MusicViewModel.kt`
- Correct `playPersonalizedItem` to use `isVideo = (it.itemType == PersonalizedItemType.VIDEO)`.
- Ensure `itemsInSection.filter { it.id.isNotBlank() }` is used.

### Task 2: Patch `NavGraph.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/navigation/NavGraph.kt`
- Align `MusicScreen`'s `onNavigateToNowPlaying` callback with `onExpandPlayer`:
  ```kotlin
  composable(Routes.Music.route) {
      MusicScreen(
          onNavigateToNowPlaying = { onExpandPlayer() },
          onNavigateToSearch = { navController.navigate(Routes.Search.route) },
          onConnectAccount = { navController.navigate(Routes.YouTubeLogin.route) }
      )
  }
  ```

### Task 3: Unit Tests & Build Debug APK
- Run `./gradlew --no-daemon testDebugUnitTest`.
- Run `./gradlew --no-daemon assembleDebug`.

### Task 4: Sideload & Verify on Realme Device
- Deploy debug APK to Realme RMX3945.
- Navigate to Music section, tap multiple songs/sections, and verify 100% smooth playback without crash.
- Capture screen forensics.

### Task 5: Bump Version & Release `v3.0.1.65`
- Bump version to `versionCode = 30075`, `versionName = "3.0.1.65"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag `v3.0.1.65`, and push to GitHub.
