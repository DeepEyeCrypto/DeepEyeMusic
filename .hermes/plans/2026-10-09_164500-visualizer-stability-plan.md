# DeepEye Visualizer Stability & Audio-Reactivity Architecture Plan

## Goal
Stabilize the visualizer engine across UI, AGSL shader pipelines, and audio session lifecycle, eliminating GC micro-stutters, JNI crashes, and wiring missing GPU scenes to the UI.

## Current Context & Critical Findings
1. **Scene Mapping Bug**: `AgslScene.NEON_TRIANGLE_GRID` exists in `AgslShaders.kt`, but `VisualizerHost.kt` only uses the Canvas-based `VvavyTriangleVisualizer`. Furthermore, `AgslVisualizer.kt` mistakenly routes `VisualizerTheme.VvavyTriangle` to `crystalShader` instead of `neonTriangleShader`.
2. **Missing Catalog Exposure**: `VisualizerSceneId` does not include `NEON_TRIANGLE_GRID`, making the new true-FFT shader unselectable in the scene library.
3. **GC Overhead / Frame Drops**: `AudioVisualizerManager.kt` continuously allocates `ByteArray(fft.size / 2)` 20–30 times a second inside `onFftDataCapture`, creating garbage collector pressure and frame drops on high-refresh-rate displays.
4. **Fragile AudioFx Lifecycle**: Fast track skipping or audio focus changes can lead to orphan `android.media.audiofx.Visualizer` instances, resulting in native leaks or silent audio-pipeline deadlocks.
5. **JNI Crash Surface**: Calling `activeShader.setFloatUniform` on Android 13+ with a uniform name not declared in the SkSL shader will natively fault or throw uncatchable JNI exceptions on specific GPU drivers (Mali/Adreno).

---

## Architecture & Proposed Approach
- **Deterministic Routing**: Wire all AGSL shaders into `VisualizerSceneId`, `VisualizerHost`, and `AvailableVisualizerScenes` with a single source of truth.
- **Zero-Allocation Audio Pipeline**: Refactor `AudioVisualizerManager` to reuse a pooled array or timestamped event wrapper so `onFftDataCapture` produces zero heap allocations.
- **Thread-Safe AudioFx Bridge**: Guard `attachToAudioSession` and `detach` with a thread-safe lock to prevent race conditions during rapid player transitions.
- **Guaranteed Uniform Declarations**: Inject an authoritative `AGSL_STANDARD_UNIFORMS` header that guarantees every uniform queried by the Kotlin renderer exists in SkSL, eliminating JNI faults.

---

## Step-by-Step Implementation Tasks

### Task 1: Expose `NEON_TRIANGLE_GRID` in the Scene Catalog
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VisualizerState.kt`
- **Action**: Add `NEON_TRIANGLE_GRID` to the `VisualizerSceneId` enum and register its metadata in `AvailableVisualizerScenes`.

### Task 2: Route `NEON_TRIANGLE_GRID` in `VisualizerHost` & Fix Theme Routing
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VisualizerHost.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslVisualizer.kt`
- **Action**:
  - In `VisualizerHost.kt`, add the branch `VisualizerSceneId.NEON_TRIANGLE_GRID -> AgslVisualizer(scene = AgslScene.NEON_TRIANGLE_GRID)`.
  - In `AgslVisualizer.kt`, map `VisualizerTheme.VvavyTriangle` to `neonTriangleShader` instead of `crystalShader`.

### Task 3: Zero-Allocation Optimization in `AudioVisualizerManager`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AudioVisualizerManager.kt`
- **Action**:
  - Pre-allocate a persistent `reusableMagnitudeBuffer = ByteArray(256)` and a generation counter.
  - Update values in place inside `onFftDataCapture` to eliminate allocations.

### Task 4: Thread-Safe AudioFX Attach/Detach & Cleanup
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AudioVisualizerManager.kt`
- **Action**:
  - Synchronize `attachToAudioSession` and `detach()` with an internal lock to prevent race conditions between ExoPlayer analytics listeners and UI lifecycle changes.

### Task 5: Complete Uniform Sanitization & JNI Protection
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslShaderEngine.kt`
- **Action**:
  - Ensure all uniforms (`iResolution`, `iTime`, `iBass`, `iMid`, `iTreble`, `iPeak`, `iAccentColor`, `iColorPrimary`, `iColorSecondary`) are strictly included in standard header transpilation.

---

## Verification & Validation Plan
1. **Compilation Check**:
   ```bash
   ./gradlew compileDebugKotlin -x test
   ```
2. **Unit Tests**:
   - Run `VisualizerHostTest` to guarantee all `VisualizerSceneId` enum entries are accounted for and no compilation errors exist.
   ```bash
   ./gradlew testDebugUnitTest --tests "com.deepeye.musicpro.ui.player.visualizer.*"
   ```
3. **On-Device Profile Verification**:
   - Connect the Realme device, start playback, and run `adb logcat | grep -E "AudioVisualizerMgr|AgslVisualizer"` to confirm clean attachment without JNI aborts or allocation spikes.

---

## Risks & Tradeoffs
- **Driver Discrepancies**: Older Android 13 Mali GPU drivers may still have SkSL compilation quirks. The fallback mechanism in `AgslVisualizer` cleanly switches to standard Compose Canvas on compile failure.
- **AudioSession 0**: Some devices don't supply valid AudioFx on session 0. The code should gracefully fall back to zero-amplitude/dummy texture modes rather than crashing.
