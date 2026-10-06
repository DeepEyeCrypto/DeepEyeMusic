# Fullscreen Real Engagement Controls: Like, Dislike, Subscribe & Download Plan

## Goal
Add fully-functional (zero fake/mock) Like, Dislike, Subscribe, and Download action controls to the fullscreen video and audio visualizer player overlays, wired directly into real backend ViewModels, database repositories, and download managers.

## Current Context & Assumptions
- The app has two fullscreen modes: Fullscreen Video Player (`NowPlayingScreen.kt` line 1322) and Fullscreen Audio Visualizer (`AudioFullscreenVisualizerLayout` line 2751). Both share `DeepEyeVideoPlayerOverlay.kt` for player HUD controls and action chips.
- Backend implementations already exist in the codebase:
  - `PlayerViewModel.likeTrack(Boolean)` & `PlayerViewModel.dislikeTrack()` dispatch real YouTube InnerTube feedback calls and update SQLite `LibraryRepository`.
  - `LibraryViewModel.toggleSubscription(channelId, channelName)` and `LibraryViewModel.isChannelSubscribed(channelId)` record channel subscriptions into `LibraryRepository` and notify background sync workers.
  - `PlayerViewModel.downloadCurrentTrack()` and `MusicDownloadManager.downloadTrack(MediaItem)` trigger background ExoPlayer caching / audio stream downloads and persist to Room database.
- Previous action chips (`EQ`, `Lyrics`, `Sleep`, etc.) are organized in a horizontally scrollable container in `DeepEyeVideoPlayerOverlay.kt`.

## Architecture & Approach
1. **Extend `PlayerState` Domain Model**:
   - Add non-breaking fields `isSubscribed: Boolean = false`, `isDownloaded: Boolean = false`, and `isDownloading: Boolean = false` with default parameters in `PlayerState.kt`.
2. **Extend `VideoPlayerOverlayActions` Interface**:
   - Add `toggleSubscribe()` and `download()` methods to `VideoPlayerOverlayActions` interface and its `fromLambdas` companion factory.
3. **Update Fullscreen Overlay HUD (`DeepEyeVideoPlayerOverlay.kt`)**:
   - Add primary engagement chips to the horizontal action bar:
     - **Like Chip**: Shows `ThumbUp` / `ThumbUpOffAlt` icon with `NeonCyan` active state, calling `actions.toggleLike()`.
     - **Dislike Chip**: Shows `ThumbDown` / `ThumbDownOffAlt` icon with active color `Color(0xFFFF5252)`, calling `actions.toggleDislike()`.
     - **Subscribe Chip**: Shows `NotificationsActive` / `AddAlert` icon with active color `Color(0xFFFF0055)`, labeled `"Subscribed"` / `"Subscribe"`, calling `actions.toggleSubscribe()`.
     - **Download Chip**: Shows `CheckCircle` / `Downloading` / `Download` icon with active color `ElectricViolet`, labeled `"Downloaded"` / `"Downloading..."` / `"Download"`, calling `actions.download()`.
4. **Wire Real ViewModels in `NowPlayingScreen.kt`**:
   - Query live states: `libraryViewModel.isChannelSubscribed(channelId)`, `viewModel.activeDownloads`, `viewModel.isTrackCached(mediaItem.id)`, `viewModel.currentSongFeedback`.
   - Propagate real live states to `DeepEyeVideoPlayerOverlay` via `playerState.copy(...)` and wire callbacks to `actions.toggleLike`, `actions.toggleDislike`, `actions.toggleSubscribe`, and `actions.download`.
5. **Unit & On-Device Validation**:
   - Update `DeepEyeVideoPlayerOverlayTest.kt` to verify that Like, Dislike, Subscribe, and Download chips render and trigger actions without errors.
   - Run `./gradlew testDebugUnitTest` and compile/install debug APK on Realme device.

---

## Step-by-Step Implementation Tasks

### Task 1: Extend `PlayerState.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/domain/model/PlayerState.kt`
- **Action**: Add `isSubscribed`, `isDownloaded`, and `isDownloading` fields with default `false`.
- **Code to Add**:
```kotlin
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isSubscribed: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
```
- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 2: Extend `VideoPlayerOverlayActions.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/overlay/VideoPlayerOverlayActions.kt`
- **Action**: Add `toggleSubscribe()` and `download()` to interface and companion `fromLambdas`.
- **Code to Add**:
```kotlin
    fun toggleSubscribe() {}
    fun download() {}
```
and in `fromLambdas`:
```kotlin
    toggleSubscribe: () -> Unit = {},
    download: () -> Unit = {},
```
- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 3: Render Engagement Chips in `DeepEyeVideoPlayerOverlay.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/overlay/DeepEyeVideoPlayerOverlay.kt`
- **Action**: Add Like, Dislike, Subscribe, and Download ActionChips in the bottom horizontal action bar.
- **Code**:
```kotlin
                        // ─── Engagement Group ───
                        ActionChip(
                            icon = if (playerState.isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                            label = "Like",
                            active = playerState.isLiked,
                            activeTint = NeonCyan
                        ) {
                            resetTimer()
                            actions.toggleLike()
                        }

                        ActionChip(
                            icon = if (playerState.isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                            label = "Dislike",
                            active = playerState.isDisliked,
                            activeTint = Color(0xFFFF5252)
                        ) {
                            resetTimer()
                            actions.toggleDislike()
                        }

                        ActionChip(
                            icon = if (playerState.isSubscribed) Icons.Default.NotificationsActive else Icons.Default.AddAlert,
                            label = if (playerState.isSubscribed) "Subscribed" else "Subscribe",
                            active = playerState.isSubscribed,
                            activeTint = Color(0xFFFF0055)
                        ) {
                            resetTimer()
                            actions.toggleSubscribe()
                        }

                        val downloadIcon = when {
                            playerState.isDownloaded -> Icons.Default.CheckCircle
                            playerState.isDownloading -> Icons.Default.Downloading
                            else -> Icons.Default.Download
                        }
                        val downloadLabel = when {
                            playerState.isDownloaded -> "Downloaded"
                            playerState.isDownloading -> "Downloading..."
                            else -> "Download"
                        }
                        ActionChip(
                            icon = downloadIcon,
                            label = downloadLabel,
                            active = playerState.isDownloaded || playerState.isDownloading,
                            activeTint = ElectricViolet
                        ) {
                            resetTimer()
                            actions.download()
                        }

                        ChipDivider()
```
- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 4: Connect Real ViewModel & Repository Bindings in `NowPlayingScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- **Action**: In `NowPlayingScreen.kt` and `AudioFullscreenVisualizerLayout`, calculate:
```kotlin
    val currentItemId = playerState.currentItem?.id ?: ""
    val currentArtist = playerState.currentItem?.artist ?: ""
    val isSubscribed by libraryViewModel.isChannelSubscribed(currentArtist).collectAsStateWithLifecycle(initialValue = false)
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val isDownloading = activeDownloads.values.any { it.id == currentItemId }
    val isDownloaded = remember(currentItemId, activeDownloads) {
        if (currentItemId.isNotBlank()) viewModel.isTrackCached(currentItemId) else false
    }
```
And pass `playerState.copy(isSubscribed = isSubscribed, isDownloaded = isDownloaded, isDownloading = isDownloading)` to `DeepEyeVideoPlayerOverlay`, wiring `actions.toggleSubscribe = { libraryViewModel.toggleSubscription(currentArtist, currentArtist) }` and `actions.download = { viewModel.downloadCurrentTrack() }`.

- **Verification Command**:
```bash
./gradlew --no-daemon compileDebugKotlin -x test
```
- **Expected Output**: `BUILD SUCCESSFUL`

---

### Task 5: Update Unit Tests in `DeepEyeVideoPlayerOverlayTest.kt`
- **File**: `app/src/test/java/com/deepeye/musicpro/ui/player/overlay/DeepEyeVideoPlayerOverlayTest.kt`
- **Action**: Add tests:
  - `likeButton_visible_and_clickable()`
  - `dislikeButton_visible_and_clickable()`
  - `subscribeButton_visible_and_clickable()`
  - `downloadButton_visible_and_clickable()`
- **Verification Command**:
```bash
./gradlew --no-daemon testDebugUnitTest --tests "com.deepeye.musicpro.ui.player.overlay.DeepEyeVideoPlayerOverlayTest"
```
- **Expected Output**: `BUILD SUCCESSFUL` with all unit tests passing.

---

### Task 6: Deploy & Release v3.0.1.50
- **Action**:
  1. Bump version in `app/build.gradle.kts` and `AppChangelog.kt` to `v3.0.1.50` (`versionCode = 30060`).
  2. Assemble debug APK and install on Realme device via ADB.
  3. Git commit, tag `v3.0.1.50`, and push to trigger automated GitHub Actions release.
- **Verification Command**:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
gh run list --limit 2
```
- **Expected Output**: APK installed on device, CI/Release runs green on GitHub Actions.

---

## Risks, Tradeoffs & Mitigations
1. **Network Sync Lag for Likes/Subscriptions**:
   - *Mitigation*: Optimistic UI updates in `LibraryRepository` immediately reflect button state locally before network confirmation.
2. **Channel ID vs Artist Name for Subscriptions**:
   - *Mitigation*: Fall back gracefully to `currentItem.artist` if explicit `channelId` is absent in standard audio metadata tags.
3. **ExoPlayer Cache Status Polling**:
   - *Mitigation*: `isDownloaded` queries `MusicDownloadManager.isFullyCached(videoId)` directly, providing zero-latency offline checkmark indicators.
