# Plan: Visualizer Scenes Polish, Precision Clock Timing & Ambient Lighting Fix

## Goal
Fix visual glitches, precision timing issues, and opaque background occlusions across all visualizer scenes (`VvavyTriangle`, `SpectrumBars`, `Waveform`, `RadialPulse`, `ParticleField`, `AgslShaders`) so that live Monet ambient artwork shines through with locked 60/120 FPS liquid animation.

## Root Cause Analysis & Deep Research Findings
1. **Opaque Solid Black Background Overwrite**:
   - `SpectrumBarsVisualizer`, `WaveformVisualizer`, `RadialPulseVisualizer`, and `ParticleFieldVisualizer` all execute `drawRect(color = VvavyBg)` (`Color(0xFF090B10)` solid opaque).
   - In `AudioFullscreenVisualizerLayout`, `AmbientArtworkBackground` renders the hardware-blurred album art and Monet ambient light right behind `VisualizerHost`.
   - The solid black `drawRect` completely erased the ambient artwork and replaced the backdrop with a pitch-black box.
2. **`nowNanos` Floating-Point Clock Jitter in `AudioFrameInterpolator`**:
   - Canvas scenes were calculating `nowNanos = (time * 1_000_000_000f).toLong()` from a relative `time` float offset.
   - This caused precision quantization errors and `dt = 0` during the first few seconds of playback.
   - Passing `System.nanoTime()` directly ensures monotonic high-precision delta time across all visualizers.
3. **AGSL Shader Lighting & Transparency Harmonization**:
   - In `AgslShaders.kt`, the ambient backgrounds for `LIQUID_PLASMA`, `CRYSTAL_TUNNEL`, and `CYBER_GRID` now blend smoothly with `iColorPrimary` and `iColorSecondary` with soft alpha edges.

## Proposed Architecture & Changes
1. **Translucent Glassmorphic Canvas Backgrounds**:
   - Replace opaque `drawRect(color = VvavyBg)` with subtle radial vignette/scrim `Color(0x66090B10)` in `SpectrumBarsVisualizer`, `RadialPulseVisualizer`, `WaveformVisualizer`, and `ParticleFieldVisualizer`.
   - Allows the vibrant Monet ambient album artwork to glow through the visualizer waveforms and particles.
2. **Precision High-Speed Monotonic Frame Clocks**:
   - Standardize `System.nanoTime()` across all visualizers in `interpolator.update(...)`.
3. **AGSL & Canvas Shader Geometry Polishing**:
   - Ensure `AgslVisualizer.kt` and `VisualizerHost.kt` render with clean composition parameters and full dynamic contrast.

## Step-by-Step Implementation Tasks

### Task 1: Polish `SpectrumBarsVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/SpectrumBarsVisualizer.kt`
- Remove opaque `drawRect(color = VvavyBg)`.
- Pass `System.nanoTime()` to `interpolator.update`.
- Refine bar glow with `primaryColor` and `secondaryColor` gradients.

### Task 2: Polish `RadialPulseVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/RadialPulseVisualizer.kt`
- Remove opaque `drawRect(color = VvavyBg)`.
- Pass `System.nanoTime()` to `interpolator.update`.
- Refine shockwave ring pulse radius and alpha blending.

### Task 3: Polish `WaveformVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/WaveformVisualizer.kt`
- Remove opaque `drawRect(color = VvavyBg)`.
- Pass `System.nanoTime()` to `interpolator.update`.
- Refine oscilloscope multi-layer neon gradient ribbons.

### Task 4: Polish `ParticleFieldVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/ParticleFieldVisualizer.kt`
- Remove opaque `drawRect(color = VvavyBg)`.
- Pass `System.nanoTime()` to `interpolator.update`.
- Refine 140-particle swarm kinetic orbits and spectral responsiveness.

### Task 5: Verify Unit Tests & Build Debug APK
- Run `./gradlew --no-daemon testDebugUnitTest` and `./gradlew --no-daemon assembleDebug`.

### Task 6: Sideload & Verify on Realme Device
- Install APK and capture live screenshot showing ambient artwork + dynamic visualizer scenes.

### Task 7: Tag & Release `v3.0.1.61`
- Bump version to `versionCode = 30071`, `versionName = "3.0.1.61"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag, and push to GitHub.
