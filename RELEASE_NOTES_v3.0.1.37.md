name: v3.0.1.37 — Selectable Multi-Scene Visualizer 🎛️

## 🚀 DeepEyeMusicPro v3.0.1.37 (Build 30047)

### 🎛️ Selectable Multi-Scene Visualizer
The audio visualizer is no longer hardcoded. Pick a scene from the in-player
visualizer library (gear icon on the visualizer surface):
- **Tumbling Triangle** — 3D audio-reactive quad-tree (VVavy-inspired, independently implemented)
- **Spectrum Bars** — mirrored FFT analyzer
- **Waveform Ribbon** — scrolling oscilloscope trace
- **Radial Pulse** — concentric rings driven by bass and treble
- **Particle Field** — orbiting swarm reacting to spectral flux

Scene, intensity (0.5–1.5×) and reduced-motion now persist across launches.
Reduced-motion slows the clocks, damps the shockwaves and freezes orbital drift.

### ⚡ Smoother Motion (Performance)
The FFT feed runs at ~19 Hz while the UI draws at 48–60 fps, so band values
stepped roughly every third frame. A new UI-thread interpolator smooths toward
the newest sample with a delta-time-corrected coefficient and asymmetric
attack/release — transients stay sharp, decays stay smooth.

### 🔧 Fixes
- **Visualizer scene catalog was lying:** `VisualizerSceneId` declared 5 scenes
  but only 2 had metadata, and 3 had no renderer. Selecting them did nothing.
  The catalog can no longer drift — the renderer `when` is exhaustive, so
  adding a scene without one is a compile error.
- **Robolectric aligned with targetSdk 35** via the version catalog (4.12 → 4.14.1).
  4.12 caps at `maxSdkVersion=34`, so every Robolectric test aborted with
  "Package targetSdkVersion=35 > maxSdkVersion=34".
- **9 stale unit tests corrected** (overlay semantics + SmartTube format selection).
  The semantics test was missing `@RunWith(RobolectricTestRunner)` and crashed on a
  null `Build.FINGERPRINT` before running a single assertion.

### 📊 Measured (Realme RMX3945, Android 16 / API 36, 720×1604, 60 Hz)
- No crashes, no ANRs, no `Visualizer` session resets during sustained playback
- 189 unit tests, 0 failures
- p50 frame time 21 ms (GPU 9 ms) — see caveats below

> **Note on frame timing:** p50 is 21 ms against a 16.67 ms budget at 60 Hz
> (~48 fps effective). The `0.00%` jank counter reports 0 while the legacy
> counters report 100% — this should **not** be read as a smooth 60 fps pipeline.
> The interpolation fix addresses visual stepping, but the frame-time cost itself
> is not yet re-measured on device.

**Full Changelog**: https://github.com/DeepEyeCrypto/DeepEyeMusic/compare/v3.0.1.36...v3.0.1.37
