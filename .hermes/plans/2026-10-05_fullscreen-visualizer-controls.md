# Fullscreen Visualizer ("Visuals") Action Engine Plan

## Goal
Port and integrate the "Visuals" action button from the Now Playing quick tools into the fullscreen video/audio player overlay (`DeepEyeVideoPlayerOverlay.kt`), allowing instant access to GPU shader/particle visualizer selection and tuning directly from the fullscreen HUD.

## Current Context & Assumptions
- In `NowPlayingScreen.kt` (lines 152, 476, 1027), the `"Visuals"` button (`Icons.Default.AutoAwesome`) opens `showVisualizerLibrary = true`, which renders `VisualizerLibraryScreen` (AGSL Shaders / Waveforms / Triangles) and `VisualizerSettingsSheet` (intensity / reduced motion).
- The fullscreen HUD (`DeepEyeVideoPlayerOverlay.kt`) currently hosts action chips for `Lock`, `PiP`, `Stats`, `Like`, `Dislike`, `Subscribe`, `Download`, `Aspect`, `Speed`, `Quality`, `EQ`, `Lyrics`, `CC`, `Sleep`, `TV Link`, and `Info`.
- Users want dedicated access to the `"Visuals"` library sheet directly from the fullscreen video player HUD without having to exit fullscreen mode.

## Architecture & Approach
1. **Extend `VideoPlayerOverlayActions.kt`**:
   - Add `openVisualizer()` to the interface and `fromLambdas` companion builder.
2. **Add "Visuals" Chip in `DeepEyeVideoPlayerOverlay.kt`**:
   - Add `ActionChip(Icons.Default.AutoAwesome, "Visuals") { resetTimer(); actions.openVisualizer() }` in the Media & Visuals group alongside `Quality`, `EQ`, and `Lyrics`.
3. **Wire Visualizer Sheet Callback in `NowPlayingScreen.kt`**:
   - Wire `openVisualizer = { showVisualizerLibrary = true }` in both `YouTubeVideoPlayerLayout` and `AudioFullscreenVisualizerLayout`.
4. **Unit Test & Deployment**:
   - Add `visualsButton_visible_and_exists` unit test in `DeepEyeVideoPlayerOverlayTest.kt`.
   - Compile, test, install debug APK on Realme device, bump version to `v3.0.1.51` (`versionCode = 30061`), and push release to GitHub Actions.

---

## Step-by-Step Implementation Tasks

### Task 1: Update `VideoPlayerOverlayActions.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/overlay/VideoPlayerOverlayActions.kt`
- **Action**: Add `openVisualizer()` method to interface and companion `fromLambdas`.
- **Code to Add**:
```kotlin
    fun openVisualizer() {}
```
and in `fromLambdas`:
```kotlin
    openVisualizer: () -> Unit = {},
```
and:
```kotlin
    override fun openVisualizer() = openVisualizer()
```
- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 2: Add "Visuals" Chip in `DeepEyeVideoPlayerOverlay.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/overlay/DeepEyeVideoPlayerOverlay.kt`
- **Action**: Add `ActionChip(Icons.Default.AutoAwesome, "Visuals")` in the Media Group next to `EQ` and `Lyrics`.
- **Code**:
```kotlin
                        ActionChip(Icons.Default.HighQuality, "Quality", enabled = hasQuality) { resetTimer(); actions.openQuality() }
                        ActionChip(Icons.Default.Tune, "EQ") { resetTimer(); actions.openDsp() }
                        ActionChip(Icons.Default.MusicNote, "Lyrics") { resetTimer(); actions.openLyrics() }
                        ActionChip(Icons.Default.AutoAwesome, "Visuals") { resetTimer(); actions.openVisualizer() }
```
- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 3: Wire `openVisualizer` in `NowPlayingScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- **Action**: In `YouTubeVideoPlayerLayout` (line 1280) and `AudioFullscreenVisualizerLayout` (line 2715), wire:
```kotlin
    openVisualizer = { showVisualizerLibrary = true },
```
- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 4: Add Unit Test in `DeepEyeVideoPlayerOverlayTest.kt`
- **File**: `app/src/test/java/com/deepeye/musicpro/ui/player/overlay/DeepEyeVideoPlayerOverlayTest.kt`
- **Action**: Add unit test:
```kotlin
    @Test
    fun visualsButton_visible_and_exists() {
        val testActions = VideoPlayerOverlayActions.fromLambdas(openVisualizer = { })

        composeTestRule.setContent {
            DeepEyeVideoPlayerOverlay(playerState = playerState, actions = testActions)
        }

        composeTestRule.onNodeWithContentDescription("Visuals")
            .assertExists()
    }
```
- **Verification Command**:
```bash
./gradlew --no-daemon testDebugUnitTest --tests "com.deepeye.musicpro.ui.player.overlay.DeepEyeVideoPlayerOverlayTest"
```
- **Expected Output**: `BUILD SUCCESSFUL` with all tests passing.

---

### Task 5: Build, Install & Release v3.0.1.51
- **Files**:
  - `app/build.gradle.kts` -> `versionCode = 30061`, `versionName = "3.0.1.51"`
  - `app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt` -> Changelog entry for v3.0.1.51
- **Action**:
  1. `./gradlew --no-daemon assembleDebug`
  2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  3. `git commit`, `git tag -a v3.0.1.51`, and `git push origin main --tags`
- **Verification Command**:
```bash
gh run list --limit 2
```
- **Expected Output**: CI and Release pipelines green on GitHub Actions.

---

## Risks & Mitigations
- **Sheet Dismissal vs Fullscreen HUD**:
  - *Mitigation*: ModalBottomSheet handles back press and background tap dismissal cleanly without forcing the player out of fullscreen mode.
