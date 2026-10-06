# Plan: Comprehensive Forensics, Debugging & Fix for PiP Engine & Floating In-App Video Overlay

## Goal
Diagnose and eliminate all Picture-in-Picture bugs: prevent double-rendering conflicts between the Now Playing screen and in-app floating PiP overlay, remove audio/video stutter on system PiP entry, and ensure smooth touch gestures and aspect ratio tracking across portrait and landscape orientations.

---

## Current Context & Forensics
DeepEyeMusicPro has two complementary PiP subsystems:
1. **System PiP (`PipEngine.kt`, `MainActivity.kt`)**: Android OS-level Picture-in-Picture window when the user presses Home or clicks the "PiP" action chip in the player overlay.
2. **In-App Floating PiP Overlay (`FloatingVideoPipOverlay.kt`, `DeepEyeMusicApp.kt`)**: Draggable, resizable mini video player that allows users to watch video while navigating Home, YouTube, Library, and Settings screens.

### Identified Bugs & Root Causes:
1. **Double-Rendering Collision Bug (`DeepEyeMusicApp.kt`)**:
   - **Root Cause**: `isPipActive` was computed as `!fullscreenMode.isFullscreen && isVideo && !isInPipMode && !isPipDismissed`.
   - **Symptom**: When a user opened `NowPlayingScreen` in portrait mode (where `fullscreenMode.isFullscreen` is `false`), `FloatingVideoPipOverlay` ALSO rendered simultaneously on top of the NowPlayingScreen video player! Both `VideoPlayerView`s competed for the same ExoPlayer surface, causing visual glitching, floating button overlays, and surface detachment.
   - **Fix**: Require `isPlayerCollapsed` condition (`sheetState.anchor == MiniSheetAnchor.COLLAPSED || sheetState.anchor == MiniSheetAnchor.HIDDEN`). Floating PiP must only show when the user has minimized the player sheet to browse other screens.
2. **ExoPlayer Stutter on System PiP Transition (`MainActivity.kt`)**:
   - **Root Cause**: In `onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)`, `playerController.player.prepare()` was invoked every time PiP was entered.
   - **Symptom**: Unnecessary buffer reset and audio/video glitch when entering Android OS PiP mode.
   - **Fix**: Remove redundant `prepare()` call during PiP state change.
3. **Delay & Aspect Ratio Tracking in `PipEngine.kt`**:
   - **Root Cause**: `enterPipMode()` had an arbitrary 300ms `postDelayed` handler that could fail or race with Activity background transitions.
   - **Fix**: Remove arbitrary delay, compute immediate `Rational` aspect ratio from `currentVideoSize` or `player.videoSize`, and update `PictureInPictureParams` synchronously.
4. **Floating PiP Touch & Boundary Handling (`FloatingVideoPipOverlay.kt`)**:
   - Ensure drag/pinch transforms accurately respect orientation bounds and touch targets (Close, Expand, Play/Pause) remain crisp and non-overlapping.

---

## Step-by-Step Tasks

### Task 1: Fix In-App Floating PiP Activation Predicate in `DeepEyeMusicApp.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/DeepEyeMusicApp.kt`
- **Action**: Check that NowPlaying sheet is collapsed before showing `FloatingVideoPipOverlay`:
  ```kotlin
  val isPlayerExpanded = sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.EXPANDED ||
                         sheetState.anchor == com.deepeye.musicpro.ui.player.MiniSheetAnchor.HALF_EXPANDED
  val isPipActive = !fullscreenMode.isFullscreen && !isPlayerExpanded && isVideo && !isInPipMode && !isPipDismissed
  ```

### Task 2: Remove Redundant Buffering Reset in `MainActivity.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/MainActivity.kt`
- **Action**: In `onPictureInPictureModeChanged()`, eliminate `player.prepare()` so playback streams seamlessly without stuttering when entering OS PiP.

### Task 3: Streamline Direct PiP Entry in `PipEngine.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/player/controller/PipEngine.kt`
- **Action**: Enter PiP immediately without async delayed post, safely fallback to 16:9 ratio if `videoSize` is unset, and maintain action callbacks.

### Task 4: Polish `FloatingVideoPipOverlay.kt` Gestures & Sizing
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/FloatingVideoPipOverlay.kt`
- **Action**: Ensure boundary offsets recalibrate on configuration change, and verify controls auto-hide after 3 seconds with snappy touch handlers.

---

## Tests / Validation
1. **Unit Tests**: Run `./gradlew --no-daemon testDebugUnitTest`.
2. **Build Debug APK**: Run `./gradlew --no-daemon assembleDebug`.
3. **On-Device Physical Test**:
   - Start a video in Now Playing screen: verify NO floating overlay covers the portrait player.
   - Collapse Now Playing sheet to browse Home or YouTube: verify floating mini video appears smoothly in bottom-right corner.
   - Tap "Expand" on floating video: verify it expands to full screen cleanly.
   - Tap "PiP" chip or press Home button: verify seamless transition into Android OS Picture-in-Picture with zero audio stutter.

---

## Risks, Tradeoffs, and Open Questions
- **None**: Android 12+ (including target Android 16 on Realme RMX3945) natively supports `autoEnterEnabled` and seamless resizing when `PictureInPictureParams` are properly configured.