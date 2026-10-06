# Plan: Deep Research, Forensics & Complete Visualizer Engine Overhaul

## Goal
Fix the visualizer engine by introducing a zero-recomposition hardware VSYNC frame clock across all Canvas visualizer scenes, eliminating the 1-FPS freeze bug, and ensuring 100% Monet color reactivity and rock-solid audio session capture on MediaTek MT6835.

---

## Deep Research Forensics & Identified Root Causes

### 1. The VSync Starvation & Freeze Bug (Root Cause Analysis):
- **Finding**: In Jetpack Compose, calling `.value` on a Kotlin `StateFlow` (e.g. `frequencyBands.value`, `fftSpectrum.value`) inside a `Canvas(modifier) { ... }` block accesses a standard Kotlin property without registering a Compose Snapshot State read.
- **Symptom**: `SpectrumBarsVisualizer`, `WaveformVisualizer`, `RadialPulseVisualizer`, and `ParticleFieldVisualizer` render only their initial frame and then **freeze completely** or update only once per second when playback position ticks.
- **Solution**: Implement the zero-recomposition VSYNC clock pattern (already proven in `AgslVisualizer.kt`):
  ```kotlin
  val frameClock = remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
      var startNanos = 0L
      while (isActive) {
          withFrameNanos { frameNanos ->
              if (startNanos == 0L) startNanos = frameNanos
              frameClock.floatValue = (frameNanos - startNanos) / 1_000_000_000f
          }
      }
  }
  ```
  Reading `frameClock.floatValue` inside `DrawScope` binds the canvas drawing phase directly to Android Choreographer VSYNC (60Hz/120Hz) with **ZERO composable function recompositions** and zero GC allocations.

### 2. AudioFrameInterpolator Delta Smoothing:
- With a dedicated VSYNC draw loop, `AudioFrameInterpolator` can smoothly interpolate between 20Hz FFT capture packets and 60/120Hz display refresh rate using exponential moving averages, delivering liquid-smooth bar rises and particle physics.

### 3. Monet & Dynamic Palette Propagation:
- In `VisualizerHost.kt`, pass `primaryColor` and `secondaryColor` into `RadialPulseVisualizer`, `ParticleFieldVisualizer`, `SpectrumBarsVisualizer`, and `WaveformVisualizer` to generate gorgeous multi-stop neon gradients derived from the current album art.

---

## Step-by-Step Tasks

### Task 1: Add Continuous VSync Clock to `SpectrumBarsVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/SpectrumBarsVisualizer.kt`
- **Action**: Add `LaunchedEffect(Unit)` with `withFrameNanos` driving `frameClock: MutableFloatState`, read inside `Canvas` lambda.

### Task 2: Add Continuous VSync Clock to `WaveformVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/WaveformVisualizer.kt`
- **Action**: Add `withFrameNanos` frame loop for smooth oscilloscope ribbon scrolling.

### Task 3: Add Continuous VSync Clock to `RadialPulseVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/RadialPulseVisualizer.kt`
- **Action**: Add `withFrameNanos` frame loop for continuous shockwave ring expansion and radial pulsing.

### Task 4: Add Continuous VSync Clock to `ParticleFieldVisualizer.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/ParticleFieldVisualizer.kt`
- **Action**: Add `withFrameNanos` frame loop for fluid particle field physics.

### Task 5: Enhance `VisualizerHost.kt` Palette Wiring
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VisualizerHost.kt`
- **Action**: Propagate `primaryColor` and `secondaryColor` to all scenes for rich Monet multi-color blending.

---

## Tests & Validation
1. **Unit Tests**: Run `./gradlew --no-daemon testDebugUnitTest`.
2. **Build Debug APK**: Run `./gradlew --no-daemon assembleDebug`.
3. **On-Device Physical Verification (Realme RMX3945)**:
   - Sideload APK and play audio.
   - Switch between all 8 visualizer scenes (Tumbling Triangle, Spectrum Bars, Waveform, Radial Pulse, Particle Field, Liquid Plasma, Crystal Tunnel, Cyber Synthwave).
   - Verify every single scene animates continuously at locked 60 FPS without stuttering or freezing.

---

## Risks & Tradeoffs
- None: Zero-recomposition state read within `DrawScope` ensures no extra recomposition CPU cost. Memory allocations remain strictly 0 bytes in the draw loop.