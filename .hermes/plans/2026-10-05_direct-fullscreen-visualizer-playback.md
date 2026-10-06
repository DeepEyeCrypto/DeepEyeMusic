# Direct Fullscreen Visualizer Playback Protocol Plan

## Goal
Bypass/eliminate the intermediate Now Playing bottom sheet when initiating song playback from the Music section and launch immediately into full-screen immersive GPU Visualizer mode.

---

## Current Context & Root Cause Analysis
1. **Current Playback Flow**:
   - When a user selects a track in the **Music Hub** (`HomeHubScreen` Music tab, Continue Listening rail, or Library):
     - `HomeHubScreen` triggers `onNavigateToMusic(id)`.
     - `NavGraph` forwards this to `onExpandPlayer()`.
     - `DeepEyeMusicApp` executes `sheetViewModel.expand()`, opening the portrait/half-expanded `NowPlayingScreen` bottom sheet.
     - The user must then manually tap the *"Fullscreen"* button or rotate the device to enter full-screen visualizer mode.
2. **Target Behavior**:
   - Tapping a song from the Music hub immediately initiates audio playback and enters **Fullscreen Visualizer Mode** (`fullscreenMode.enter(forceLandscape = true)`), bypassing the intermediate portrait sheet.
   - The full-screen 60FPS GPU Visualizer (`AgslVisualizer` / Monet dynamic shader) renders across the entire display with audio-reactive animations, ambient blur, lyrics, and gesture controls.

---

## Architecture & Proposed Approach
- **Direct Fullscreen Launch (`DeepEyeMusicApp.kt` & `NavGraph.kt`)**:
  - Introduce `onPlayFullscreenMusic` in `NavGraph` and `HomeHubScreen`.
  - When music playback starts:
    1. Send media item to `playerViewModel.playMedia(item)`.
    2. Directly trigger `fullscreenMode.enter(forceLandscape = true)`.
    3. Collapse or bypass the modal bottom sheet (`sheetViewModel.collapse()`).
- **Fullscreen Visualizer Engagement (`NowPlayingScreen.kt`)**:
  - Ensure that when `isFullscreen` is active for an audio track, the immersive `VisualizerCanvas` / `AgslVisualizer` fills the screen with HUD overlay controls.
- **CI Test Fix (`DeepEyeVideoPlayerOverlayTest.kt`)**:
  - Update `likeButton_dispatchesToggle` to test the new `EQ` and `Lyrics` action chips added to the overlay.

---

## Step-by-Step Tasks

### Task 1: Fix Overlay Unit Test for CI Pipeline
- **File**: `app/src/test/java/com/deepeye/musicpro/ui/player/overlay/DeepEyeVideoPlayerOverlayTest.kt`
- **Action**: Replace `likeButton_dispatchesToggle` with `eqButton_dispatchesOpen` and `lyricsButton_dispatchesOpen`.
```kotlin
    @Test
    fun eqButton_dispatchesOpen() {
        var eqOpened = false
        val testActions = VideoPlayerOverlayActions.fromLambdas(openDsp = { eqOpened = true })
        composeTestRule.setContent {
            DeepEyeVideoPlayerOverlay(playerState = playerState, actions = testActions)
        }
        composeTestRule.onNodeWithText("EQ")
            .performClick()
        assertTrue(eqOpened)
    }

    @Test
    fun lyricsButton_dispatchesOpen() {
        var lyricsOpened = false
        val testActions = VideoPlayerOverlayActions.fromLambdas(openLyrics = { lyricsOpened = true })
        composeTestRule.setContent {
            DeepEyeVideoPlayerOverlay(playerState = playerState, actions = testActions)
        }
        composeTestRule.onNodeWithText("Lyrics")
            .performClick()
        assertTrue(lyricsOpened)
    }
```

### Task 2: Route Music Playback Directly to Fullscreen in `DeepEyeMusicApp.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/DeepEyeMusicApp.kt`
- **Action**: Provide dedicated `onPlayFullscreenMusic` callback to `NavGraph`:
```kotlin
onPlayFullscreenMusic = {
    fullscreenMode.enter(forceLandscape = true)
}
```

### Task 3: Update `NavGraph.kt` and `HomeHubScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/navigation/NavGraph.kt`
- **Action**: In `composable(Routes.Home.route)`:
  - Wire `onNavigateToMusic = { onPlayFullscreenMusic() }`.
  - In `playRecMusic`: trigger direct fullscreen entrance upon track selection.

### Task 4: Ensure Seamless Fullscreen Visualizer Activation
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- **Action**:
  - When in audio mode + `isFullscreen`, render the full-screen GPU Visualizer (`AgslVisualizer` / `SpectrumBarsVisualizer`) with ambient Monet glow and OSD touch overlay.

---

## Verification & Testing
1. **Run Unit Tests Locally**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   - Expected Output: `262 tests completed, 0 failed, 4 skipped. BUILD SUCCESSFUL`.
2. **Build Debug APK & Install on Device**:
   ```bash
   ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **On-Device Interaction Test**:
   - Open App -> Tap "Music" / "Continue Listening" -> Tap any song.
   - Verify: Skips Now Playing modal sheet and immediately opens full-screen visualizer.
