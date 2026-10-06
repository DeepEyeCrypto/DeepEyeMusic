# Plan: Deep Research, Direct PCM AudioProcessor & Complete Visualizer All-Visuals Overhaul

## Goal
Implement a zero-latency, HAL-independent `VisualizerAudioProcessor` in ExoPlayer's pipeline and overhaul `VisualizerEngine` and all 7 visualizer scenes (`Triangle`, `Spectrum Bars`, `Waveform`, `Radial Pulse`, `Particle Field`, `Liquid Plasma AGSL`, `Crystal Tunnel AGSL`, `Cyber Grid AGSL`) for locked 60/120 FPS liquid audio-reactive animation.

## Root Cause Analysis & Deep Research Findings
1. **The OEM HAL / Dead Session AudioFx Bug**:
   - On Android 14/15/16 (MediaTek MT6835, Samsung, Xiaomi), `android.media.audiofx.Visualizer(sessionId)` frequently fails to capture audio when ExoPlayer uses fast PCM pipelines or offloaded audio tracks.
   - Device logcat confirmed:
     `D VisualizerManager: [VisualizerManager] fft_frame sessionId=4057 playbackActive=false peakMagnitude=0.0 bass=0.25949934 mid=0.029913703 treble=0.012401473`
   - `peakMagnitude=0.0` was returned continuously because the native HAL AudioFx session had zero data.
2. **The Frozen State Early-Exit Bug**:
   - When `maxMagnitude < 1.0f`, `VisualizerEngine.kt` had an early `return` before updating `_frequencyBands` or `_fftSpectrum`. This locked the visualizers at the last non-zero numbers (`bass=0.25949934`) forever instead of decaying or updating.
3. **The Solution: In-Pipeline `VisualizerAudioProcessor`**:
   - By capturing audio directly inside ExoPlayer's `AudioProcessor` chain (via a high-speed in-memory Radix-2 FFT / band energy calculator in `VisualizerAudioProcessor`), DeepEye gains 100% reliable, zero-latency, permissionless audio FFT capture that works across ALL Android versions and ALL devices.

## Proposed Architecture
1. **`VisualizerAudioProcessor`**:
   - Implements `androidx.media3.common.audio.AudioProcessor`.
   - Passes PCM audio to `AudioSink` completely untouched (`ENCODING_PCM_16BIT` & `ENCODING_PCM_FLOAT`).
   - Slices 512/1024-sample audio windows into a fast real FFT algorithm (Hanning window + Cooley-Tukey / bit-reversal FFT).
   - Feeds normalized 32-bin spectrum and 6-band acoustic energy directly into `VisualizerEngine`.
2. **`VisualizerEngine` Overhaul**:
   - Accepts direct PCM frames from `VisualizerAudioProcessor` as primary source.
   - Smooth exponential decay when audio pauses/stops (decay rate 0.85).
   - Keeps `Visualizer(sessionId)` as fallback.
3. **All Visualizer Scenes Polish**:
   - `VvavyTriangleVisualizer.kt`: Quad-tree 3D tumble driven by live bass/treble.
   - `SpectrumBarsVisualizer.kt`: Dynamic Monet gradient bars + peak caps.
   - `WaveformVisualizer.kt`: Oscilloscope ribbon with continuous VSYNC invalidation.
   - `RadialPulseVisualizer.kt`: Shockwave pulse rings reacting to live transients.
   - `ParticleFieldVisualizer.kt`: 140-node orbital swarm.
   - `AgslVisualizer.kt`: SkSL GPU shaders (`Liquid Plasma`, `Crystal Tunnel`, `Cyber Grid`) receiving live `iBass`, `iMid`, `iTreble`, `iPeak` uniform floats.

## Step-by-Step Implementation Tasks

### Task 1: Implement `VisualizerAudioProcessor`
- **File**: `app/src/main/java/com/deepeye/musicpro/dsp/processor/VisualizerAudioProcessor.kt`
- Implements `androidx.media3.common.audio.AudioProcessor`.
- Contains zero-allocation Hanning window and fast in-place Radix-2 FFT.
- Feeds `VisualizerEngine.feedPcmData(spectrum, bands)`.

### Task 2: Register `VisualizerAudioProcessor` in `PlayerModule.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/di/PlayerModule.kt`
- Inject `VisualizerAudioProcessor` into ExoPlayer's `AudioProcessor` array alongside `lufsAnalyzerProcessor` and `masterLimiterProcessor`.

### Task 3: Overhaul `VisualizerEngine.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/player/visualizer/VisualizerEngine.kt`
- Add `feedPcmData(rawSpectrum: FloatArray, rawBands: FloatArray)`.
- Replace frozen early-return with smooth exponential decay on pause/silence.

### Task 4: Polish All Visualizer Scenes & AGSL Shaders
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/VvavyTriangleVisualizer.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/SpectrumBarsVisualizer.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/WaveformVisualizer.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/RadialPulseVisualizer.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/ParticleFieldVisualizer.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslVisualizer.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/visualizer/agsl/AgslShaders.kt`

### Task 5: Run Unit Tests & Build Debug APK
- **Command**:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon testDebugUnitTest
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon assembleDebug
  ```

### Task 6: Sideload on Realme Device, Verify Live Logcat & Screen
- Verify live logcat shows dynamic fluctuating `peakMagnitude > 0` and non-zero `bass/mid/treble` that dance with music:
  ```bash
  adb logcat -c && adb install -r /Users/enayat/Documents/DeepEyeMusicPro/app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.deepeye.musicpro/.MainActivity
  sleep 3 && adb logcat -d | grep -i -E "VisualizerManager|VisualizerAudioProcessor" | tail -n 30
  ```
- Capture live screenshot of animated visualizer.

### Task 7: Release & Push `v3.0.1.60`
- Bump `versionCode = 30070`, `versionName = "3.0.1.60"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag `v3.0.1.60`, and push to GitHub.
