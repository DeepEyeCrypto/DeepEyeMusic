# Plan: Fix Fullscreen X-Button Dismiss to PiP & Eliminate Intermediate Screen Lag

## Goal
Fix the fullscreen X (close) button behavior so that clicking X immediately exits fullscreen, collapses the player sheet, and smoothly transitions into Floating In-App Video PiP (for video) or docked MiniPlayer (for audio), eliminating the stuck/dead expanded sheet state.

## Root Cause Analysis
1. **Broken `dismiss` Callback in `VideoNowPlayingLayout`**:
   - In `NowPlayingScreen.kt` (line 750), `DeepEyeVideoPlayerOverlay` actions was wired as:
     ```kotlin
     dismiss = { fullscreenMode.exit() }
     ```
   - It only exited fullscreen mode (`fullscreenMode.isFullscreen = false`) but failed to call `callbacks.onNavigateBack()`.
   - Because `callbacks.onNavigateBack()` was never invoked, `sheetViewModel.collapse()` was not executed.
   - Consequently, `sheetState.anchor` remained `MiniSheetAnchor.EXPANDED`.
   - In `DeepEyeMusicApp.kt`:
     ```kotlin
     val isPipActive = !fullscreenMode.isFullscreen && !isPlayerExpanded && isVideo && !isInPipMode && !isPipDismissed
     ```
     `isPipActive` evaluated to `false` because `isPlayerExpanded` was still true.
   - This left the user stranded on a blank/broken expanded sheet instead of entering PiP or returning to the previous screen.

## Architecture & Proposed Approach
1. **Unify Dismiss Actions in `NowPlayingScreen.kt`**:
   - In `VideoNowPlayingLayout`, wire `dismiss = { callbacks.onNavigateBack() }`.
   - In `AudioFullscreenVisualizerLayout`, ensure `dismiss = { callbacks.onNavigateBack() }` (or `onExitFullscreen()`).
2. **Harmonize `DeepEyeMusicApp.kt` Back/Dismiss Pipeline**:
   - `onNavigateBack` cleanly executes `fullscreenMode.exit()` and `sheetViewModel.collapse()`.
   - `FloatingVideoPipOverlay` instantly activates when video is playing and sheet is collapsed.
3. **Verify PiP & MiniPlayer Transitions**:
   - Tapping X on a video track smoothly transitions into the floating draggable PiP window.
   - Tapping X on an audio track smoothly docks into the bottom MiniPlayer.
   - Tapping PiP re-expands to fullscreen player with zero glitches.

## Step-by-Step Tasks

### Task 1: Fix `dismiss` callback in `NowPlayingScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- Update line 750 in `VideoNowPlayingLayout` to `dismiss = { callbacks.onNavigateBack() }`.

### Task 2: Unit Tests & Build Verification
- Run `./gradlew --no-daemon testDebugUnitTest`.
- Run `./gradlew --no-daemon assembleDebug`.

### Task 3: On-Device Verification (Realme RMX3945)
- Sideload APK.
- Play a video in fullscreen, tap X button, verify immediate transition into Floating Video PiP without intermediate dead screens.
- Play an audio track, tap X button, verify immediate collapse into docked MiniPlayer.
- Capture screen forensics.

### Task 4: Bump Version & Release `v3.0.1.66`
- Update `versionCode = 30076`, `versionName = "3.0.1.66"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag `v3.0.1.66`, and push to GitHub.
