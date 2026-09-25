# DeepEye Triangle Visualizer Runtime Report

All values below are **measured**, not assumed. Every "Unknown" placeholder from the
previous revision has been replaced with a value obtained from `getprop`, `dumpsys display`,
`dumpsys gfxinfo`, `dumpsys meminfo`, `dumpsys battery`, and `logcat` on the connected device.

## Environment

| Property | Measured value |
| --- | --- |
| Device | Realme RMX3945 (`RMX3945IN`, MTK, `RE6092`) |
| Android | 16 (API 36), security patch 2026-08-01 |
| Display | 720 x 1604, 320 dpi (265.0 x 264.6 effective) |
| Active mode | `modeId=3`, `renderFrameRate=60.000004` Hz |
| Supported refresh | 50 / 60 / 90 / 120 Hz (default mode id=1 @ 120) |
| App build | `com.deepeye.musicpro` 3.0.1.36-debug (versionCode 30046), minSdk 24, targetSdk 35 |
| PID under test | 9409 |
| Rendering | Pipeline `Skia (OpenGL)`, 1 context, 0 stopped |

The installed `versionCode`/`versionName` match `app/build.gradle.kts:23-24`, confirming the
measured build corresponds to the current working tree.

## Capture conditions

- Battery 81%, 41.4 °C, USB powered, `status=2` (charging) — thermal or low-battery
  throttling is **not** a confounder for these numbers.
- Playback active for the whole capture window; visualizer attached to session `921`.

## FFT pipeline

`VisualizerEngine` emits one `fft_frame` diagnostic line per 40 captured frames
(`DIAG_LOG_INTERVAL_FRAMES = 40`, `VisualizerEngine.kt:26`, gate at `VisualizerEngine.kt:187-191`).

Measured over 10 diagnostic lines spanning 18.954 s:

- Inter-line deltas: 2100, 2108, 2101, 2106, 2100, 2107, 2120, 2103, 2109 ms (mean 2106 ms)
- **Steady-state FFT capture rate: 40 / 2.106 s ≈ 19.0 Hz**
  (span-based figure 400 / 18.954 s ≈ 21.1 Hz; the steady-state value is the honest one)
- `sessionId=921`, `playbackActive=true` on every sample
- `peakMagnitude` 62.97 – 130.97; `bass` 0.2692 – 0.3121; `mid` 0.0181 – 0.0415; `treble` 0.0097 – 0.0141

Stability: all 16 `VisualizerManager` events in the retained logcat buffer are `fft_frame`.
Zero `attach_failed`, zero `zero_fft_reset`, zero `zero_fft_reset_backoff`.
No `FATAL EXCEPTION`, no ANR, no `Force finishing` for this package in the scanned window.

The zero-data watchdog never fired, so the capture path stayed genuinely live rather than
being silently reconstructed.

## Frame timing

`dumpsys gfxinfo com.deepeye.musicpro` over 1222 rendered frames:

| Metric | Value |
| --- | --- |
| p50 / p90 / p95 / p99 frame time | 21 / 24 / 28 / 30 ms |
| GPU p50 / p90 / p95 / p99 | 9 / 12 / 13 / 15 ms |
| Janky frames (modern) | 0 (0.00%) |
| Janky frames (legacy) | 1222 (100.00%) |
| Missed Vsync | 0 |
| Slow UI thread / bitmap uploads / issue draw commands | 0 / 0 / 0 |
| Frame deadline missed (modern) | 0 |
| Frame deadline missed (legacy) | 1081 |
| High input latency | 1363 |

## Memory

| Metric | Value |
| --- | --- |
| TOTAL PSS | 611,652 KB (~597 MB) |
| TOTAL RSS | 613,408 KB |
| Java Heap | 92,260 KB |
| Native Heap | 40,336 KB |
| Graphics (GL mtrack) | 125,736 KB |
| Stack | 3,260 KB |
| TOTAL SWAP PSS | 223,068 KB |

## Findings

1. **The previous report's stability claim rested on a misread log.** `fft_frame` is a
   1-in-40 diagnostic sample, not the frame loop. "11 log lines" is ~440 real FFT frames.
   The loop was genuinely healthy, but the evidence has to be scaled by 40 to be honest.
2. **Capture rate is ~19 Hz, not 30–60 Hz.** `deepeye_visualizer_performance_plan.md` targets
   30–60 fps. The FFT feed is roughly 3.2x slower than the 60 Hz panel, so the spectrum
   updates ~19 times per second regardless of how fast the UI thread draws. Band energies
   (bass/treble) are heavily smoothed, so this reads as responsive, but the reactive
   character is bounded by the audio capture rate, not by Compose.
3. **The 0.00% jank figure must not be reported as "smooth 60 fps".** p50 frame time is
   21 ms against a 16.67 ms budget at 60 Hz, i.e. ~48 fps effective. The modern counters
   (0 janky, 0 missed deadline) directly contradict the legacy counters (100% janky, 1081
   missed) and the 21 ms median. This is characteristic of frame-rate voting / a deadline
   that is not actually 60 Hz, not of a comfortably smooth pipeline. GPU time is only 9 ms
   p50, so the cost is in UI-thread work and composition, not rasterization.
4. **"High input latency" (1363) exceeds total frames (1222).** Interaction events are
   queueing behind the 21 ms UI-thread frame. This is a real responsiveness risk on this
   device and is worth a dedicated touch-latency pass.
5. **Memory is high for a single screen.** 597 MB PSS with 223 MB swapped, of which
   90 MB is Java heap and 123 MB graphics. Observed, not attributed — see follow-ups.

## Verdict

Visualizer **stability: PASS** (live capture, no resets, no crash/ANR).
Visualizer **performance: NOT VERIFIED as 60 fps** — measured ~48 fps UI-thread cadence and
~19 Hz FFT feed. The 0.00% jank counter should not be used as proof of smoothness.

## Follow-ups

- Profile the 21 ms UI-thread frame; 9 ms GPU leaves ~12 ms unexplained on the main thread.
- Measure touch latency directly to confirm or dismiss finding 4.
- Attribute the 597 MB PSS (heap vs. graphics vs. swap) before treating it as a leak.
- Consider whether ~19 Hz capture is the intended ceiling; if smoother reactivity is wanted,
  interpolate band state on the UI thread rather than adding capture frequency.

