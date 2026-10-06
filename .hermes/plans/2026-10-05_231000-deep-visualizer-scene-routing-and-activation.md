# Plan: Visualizer Scene Activation & Seamless Ambient Integration

## Goal
Ensure instantaneous visualizer scene activation across all 8 visualizer scenes upon selection from `VisualizerLibraryScreen`, and resolve background occlusion in `VvavyTriangleVisualizer` so that ambient album art and Monet theming glow smoothly across all scenes.

## Current Context & Forensics
1. **Scene Activation Flag**:
   - In `NowPlayingScreen.kt`, `onSceneSelected` inside `VisualizerLibraryScreen` sheet was only executing `viewModel.selectVisualizerScene(id)` without setting `showVisualizer = true`.
   - If the visualizer overlay was toggled off, selecting a scene from the library did not automatically bring up the visualizer on the main now-playing card.
2. **`VvavyTriangleVisualizer` Solid Background Occlusion**:
   - `VvavyTriangleVisualizer.kt` was still rendering `drawRect(color = VvavyBg)` (`#090B10` 100% opaque), blocking the blurred ambient artwork behind it.
3. **Full Scene Spectrum Coverage**:
   - All 8 visualizer scenes (`TRIANGLE_REACTIVE`, `SPECTRUM_BARS`, `WAVEFORM`, `RADIAL_PULSE`, `PARTICLE_FIELD`, `LIQUID_PLASMA`, `CRYSTAL_TUNNEL`, `CYBER_GRID`) must have 100% responsive selection, real-time live PCM reaction, and translucent scrim backgrounds.

## Proposed Architecture & Changes
1. **`NowPlayingScreen.kt`**:
   - In `VisualizerLibraryScreen` sheet:
     ```kotlin
     onSceneSelected = { id ->
         viewModel.selectVisualizerScene(id)
         showVisualizer = true
         showVisualizerLibrary = false
     }
     ```
   - In `AudioFullscreenVisualizerLayout`: Ensure `openVisualizer` callback immediately brings up the visualizer library modal in fullscreen mode.
2. **`VvavyTriangleVisualizer.kt`**:
   - Replace opaque `drawRect(color = VvavyBg)` with translucent vertical gradient scrim `listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.55f))`.

## Step-by-Step Implementation Tasks

### Task 1: Update `NowPlayingScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- In `showVisualizerLibrary` sheet `onSceneSelected`, set `showVisualizer = true` alongside `viewModel.selectVisualizerScene(id)`.

### Task 2: Update `VvavyTriangleVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VvavyTriangleVisualizer.kt`
- Replace opaque `drawRect(color = VvavyBg)` with translucent scrim.

### Task 3: Unit Tests & Build Debug APK
- Run `./gradlew --no-daemon testDebugUnitTest`.
- Run `./gradlew --no-daemon assembleDebug`.

### Task 4: Sideload & Verify on Realme Device
- Deploy to Realme RMX3945 and capture screenshot.

### Task 5: Version Bump & Release `v3.0.1.62`
- Bump version in `app/build.gradle.kts` and `AppChangelog.kt`.
- Tag `v3.0.1.62`, push to GitHub.
