# DeepEye Music Pro — Premiumization & Product Architecture Strategy
## Sound, Visuals, Motion, and Media-Session Reliability

This document outlines the architectural research, component design system, audio pipeline specifications, and implementation roadmap to elevate DeepEye Music Pro into the most premium music player on Android.

---

## 1. Audible Quality (Audio Premiumization Strategy)

To match audiophile engines like Poweramp, the audio subsystem must enforce a deterministic DSP pipeline, advanced low-frequency processing, dynamic route-awareness, and clipping prevention.

```
[ExoPlayer / WebView Output]
             │
             ▼
┌────────────────────────┐
│ Pre-Gain Control (PGC) │ ──► Normalizes track loudness (LUFS calculation)
└────────────────────────┘
             │
             ▼
┌────────────────────────┐
│   10-Band EQ Engine    │ ──► Primary acoustic signature customization
└────────────────────────┘
             │
             ▼
┌────────────────────────┐
│   Bass Boost Module    │ ──► Sub-bass, Mid-bass, and Harmonic saturation
└────────────────────────┘
             │
             ▼
┌────────────────────────┐
│ Virtualizer / Reverb   │ ──► Spatial widening and environment simulation
└────────────────────────┘
             │
             ▼
┌────────────────────────┐
│   Harmonic Exciter     │ ──► Clarity and high-frequency "air" enhancement
└────────────────────────┘
             │
             ▼
┌────────────────────────┐
│  Dynamics Processor    │ ──► Multiband compressor controls transients
└────────────────────────┘
             │
             ▼
┌────────────────────────┐
│   Peak Peak Limiter    │ ──► Brickwall limiter (-0.5 dBFS ceiling)
└────────────────────────┘
             │
             ▼
       [AudioTrack]
```

### 1.1 DSP Pipeline Order & Rationale
1. **Pre-Gain Control (PGC):** Normalizes the raw signal first. Doing this at the input stage ensures that subsequent EQ and compression modules receive a consistent signal amplitude, avoiding premature saturation or distortion.
2. **10-Band Equalizer (System-level + AGSL):** Shapes the primary tone.
3. **Bass Boost & Sub-Bass Synthesizer:** Adds physical weight. Placing this before spatial effects allows bass to be positioned cleanly without phase cancellations.
4. **Spatializer / Virtualizer & Reverb:** Expands the soundstage.
5. **Harmonic Exciter (Clarity):** Adds high-frequency details (excites 8kHz - 16kHz) to compensate for lossy compression.
6. **Dynamics Processing (Multiband Compressor):** Balances dynamics across low, mid, and high bands.
7. **Peak Limiter (Clipping Protection):** Must be the *absolute final stage* before sending to `AudioTrack` to intercept any transients exceeding 0 dBFS.

### 1.2 Multi-Layered Bass Enhancement Strategy
To deliver a punchy, clean low-end without muddying the midrange:
* **Sub-Bass Synthesis (20Hz - 60Hz):** Uses a high-order low-pass filter (24dB/octave) to isolate and boost deep rumble.
* **Mid-Bass Punch (60Hz - 120Hz):** Applies a tight, high-Q parametric boost to enhance the impact of kick drums.
* **Harmonic Saturation (Psychoacoustic Bass):** Synthesizes upper harmonics (second-order $2f$ and third-order $3f$) from low-frequency fundamentals. This allows small headphones and built-in speakers to trick the brain into hearing sub-bass that the hardware cannot physically reproduce.
* **Bass Limiter:** A dedicated compressor loop clamped specifically below 150Hz to prevent speaker over-excursion and distortion.

### 1.3 EQ Presets & Acoustic Profiles
* **Headphone Target (Harman Curve):** Features a sub-bass shelf below 100Hz, a slight scoop at 200Hz to prevent muddiness, and a targeted rise at 3kHz to mimic the ear canal's natural resonance.
* **Speaker Loudness (Fletcher-Munson Compensation):** Automatically boosts lows and highs at lower master volumes, compensating for the human ear's reduced sensitivity to extreme frequencies at low amplitudes.
* **Car Audio Profile:** Subtracts energy around 150Hz - 250Hz (cancels cabin boominess) and shifts the stereo image slightly to compensate for offset speaker positioning.
* **Flat Studio Profile:** Bypasses active filters, keeping processing perfectly linear (transparent peak limiting only).

### 1.4 Loudness Normalization & Clipping Protection
* **LUFS Target:** Calculate track loudness (EBU R128) and target -14 LUFS.
* **Clipping Protection:** The Gain Budget Calculator tracks total cumulative gain of active DSP modules. If headroom drops below 0 dBFS, PGC dynamically attenuates the input signal. The final dynamics stage applies a brickwall limiter at -0.5 dBFS with 0.1ms attack and 50ms release.

### 1.5 Gapless Playback & Crossfading
* **Pre-Buffering:** Media3 player pre-warms the next media item in the playlist queue 10 seconds before the current track ends.
* **Volume Ramping:** Applies a linear power crossfade (0.8s overlap) by adjusting the gain of the outbound and inbound players in parallel, preventing sudden spikes.

### 1.6 Route-Aware DSP Profiles
* **Automated Profiling:** The app monitors `AudioDeviceCallback`. When the user switches from Bluetooth (LDAC/aptX) to Wired Headphones or Car Audio, the DSP engine automatically recalls the last active profile saved in the Room database for that specific route.
* **Hi-Res Audio Indicator:** Renders a glowing "Hi-Res" badge (e.g., 24-bit/192kHz) only when the active audio device supports high-sample rates and the media source is lossless (FLAC/WAV).

---

## 2. Liquid Glass Visual System (Visual Luxury Strategy)

Liquid Glass is treated as a unified mathematical token matrix that combines blurred backdrops, chromatic refraction, dynamic specular reflection, and noise grain to feel physically real.

### 2.1 The Glass Token Matrix
All premium glass components utilize the following design tokens:

```kotlin
object PremiumGlassTokens {
    // Blur Radius mapping
    val BlurThin = 16.dp       // Category chips, small badges
    val BlurMedium = 28.dp     // Cards, search bars, player controls
    val BlurThick = 48.dp      // Now Playing screen background
    val BlurSheet = 56.dp      // Slidable bottom sheets

    // Opacity / Tint parameters
    val TintAlphaLight = 0.08f // Subtle overlays
    val TintAlphaMedium = 0.14f// Cards and widgets
    val TintAlphaDark = 0.22f  // Sheets and popups

    // Specular Highlight
    val SpecularReflectionAlpha = 0.10f
    val SpecularHighlightSpread = 0.45f

    // Textural Noise
    val FrostNoiseIntensity = 0.04f

    // Dimensional Borders
    val InnerBorderWidth = 1.dp
    val InnerBorderAlpha = 0.18f
}
```

### 2.2 Glass Component Library Blueprints

```
┌───────────────────────────────────────────────────────────────┐
│ GlassCard / GlassSurface                                      │
├───────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────────────────┐ │
│ │ Specular Highlight (Dynamic Gyro Gradient)                │ │
│ └───────────────────────────────────────────────────────────┘ │
│ ┌───────────────────────────────────────────────────────────┐ │
│ │ Chromatic Refraction (AGSL pixel shift at borders)        │ │
│ └───────────────────────────────────────────────────────────┘ │
│ ┌───────────────────────────────────────────────────────────┐ │
│ │ Frosted Noise Grain (adds tangible texture)               │ │
│ └───────────────────────────────────────────────────────────┘ │
│ ┌───────────────────────────────────────────────────────────┐ │
│ │ Haze Blur Child (dynamic backdrop sampling)               │ │
│ └───────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

1. **`GlassCard` / `GlassSurface`:** An isolated box containing a backdrop blur layer (`hazeChild`), an inner border reflecting light (linear gradient from Top-Left to Bottom-Right), a frosted noise grain, and a gyro-shifted specular overlay.
2. **`GlassPill`:** A capsule shape with a highly compressed blur (16dp) and an accent-tinted border, used for tabs and category selections.
3. **`GlassSheet`:** Used for drawer layers (e.g., Queue, Lyrics). Features heavy blur (56dp) to visually isolate foreground content.
4. **`GlassButton`:** Micro-interaction target. On touch, the glass refracts more deeply (increases chromatic aberration parameter from 0.1 to 0.25) and pulses the specular glow.
5. **`GlassProgressBar`:** Transparent glass track (white with 0.1f alpha) with a glowing, semi-opaque thumb and active line containing a subtle horizontal neon gradient.
6. **`GlassTabBar`:** Floating navigation bar with rounded corners (24dp) and a dynamic backdrop blur that shrinks slightly during scrolling.

### 2.3 Album-Art Refraction & Tint Blending
To prevent muddy themes, color extraction uses the following algorithm:
1. Extract swatches using `androidx.palette.graphics.Palette`.
2. Convert the dominant color to HSL.
3. **Clamp SATURATION:** Max 40% (prevents glowing, neon colors that strain the eyes).
4. **Clamp LUMINANCE:** Max 25% (ensures text written in white always achieves a minimum contrast ratio of 4.5:1).
5. Blend this sanitized base color with the background color (e.g., `#0F0F12`) at 15% opacity to tint the glass surface organically.

### 2.4 Motion-Reactive Specular Highlights
* **Sensor Integration:** Integrates `Sensor.TYPE_ROTATION_VECTOR` at 60Hz.
* **Low-Pass Filter:** Applies exponential smoothing:
  $$x_{smoothed} = \alpha \cdot x_{raw} + (1 - \alpha) \cdot x_{previous}$$
  where $\alpha = 0.12$.
* **Shader Uniforms:** Feeds the smoothed X/Y coordinate into the AGSL RuntimeShader. The shader calculates the distance between the pixel and the virtual highlight center:
  $$\text{glow} = \text{smoothstep}(\text{spread}, 0.0, \text{distance})$$
  This creates a high-fidelity light reflection that glides across the glass as the device tilts.

### 2.5 AMOLED Black Adaptation
When the user activates "AMOLED Mode":
* Baseline backgrounds are set to pure black `#000000`.
* Glass containers drop their `tintAlpha` to `0.06f` and increase `borderColor` alpha to `0.24f`. This yields highly contrastive, crisp outline borders that separate UI elements without losing the transparent glass aesthetic.

### 2.6 Accessibility Fallbacks
When the system setting "Reduce Transparency" or "High Contrast Text" is active:
* The app automatically disables AGSL shader operations and Haze blur.
* Falls back to solid dark grey surfaces (`#1E1E24` or `#121214`) with solid opaque borders (`#3A3A42`) to maintain readability.

---

## 3. Premium Now Playing Experience (Now Playing Surface)

The Now Playing screen is the emotional center of the player, combining visual feedback with fluid motion.

```
┌───────────────────────────────────────────────────────────┐
│                    Now Playing Layout                     │
├───────────────────────────────────────────────────────────┤
│  [Status Bar Padding]                                     │
│  [Top Glass Navigation Bar (Back, Track Title, Settings)]  │
│                                                           │
│                  ┌──────────────────────┐                 │
│                  │  Bass Ring Glow      │                 │
│                  │  ┌────────────────┐  │                 │
│                  │  │   Album Art    │  │                 │
│                  │  └────────────────┘  │                 │
│                  └──────────────────────┘                 │
│                                                           │
│  [32-Bar Mirror Spectrum Visualizer (Spring Animated)]    │
│  [Track Info: Title, Artist (Marquee-Enabled)]            │
│  [Glass Action Row: Like, Dislike, Autoplay, Download]    │
│  [Glass Progress Slider + Time Indicators]               │
│  [Control Panel: Shuffle, Prev, Play/Pause Pill, Next, Rpt]│
└───────────────────────────────────────────────────────────┘
```

### 3.1 Interaction Hierarchy & Layering (Z-Order)
1. **Background Layer (Z=0):** Full-bleed blurred album art (BlurHeavy = 48dp).
2. **Visualizer Layer (Z=1):** Real-time spectrum bars fading behind the artwork.
3. **Hero Layer (Z=2):** Centered album artwork with dynamic specular reflections and a frequency-reactive bass pulse ring.
4. **Interface Control Layer (Z=3):** Glass control widgets, slider, and buttons.
5. **Overlays (Z=4):** Slide-up lyric sheets or queue sheets (using GlassBottomSheet).

### 3.2 Dynamic Visualizer Integration
* **32-Bar Mirror Spectrum:** The visualizer splits frequency data into 32 discrete bins, mirroring them symmetrically from the center.
* **Spring Dynamics:** Rather than raw FFT jumps, bar heights use a spring physical animation model (stiffness = 300f, damping = 15f) to create smooth, flowing movements.
* **Pulsing Bass Ring:** Low-frequency amplitude (20Hz - 100Hz) scales the artwork container dynamically by a factor of up to 1.05x, while an outer neon circle expands and fades out synchronously with the beat.

### 3.3 Glass Lyric Sheet UI
* **Slidable Material:** Slide-up sheet utilizing `GlassBottomSheet` with heavy blur.
* **Acoustic Focus:**
  * Active Line: Bold, opaque white (`#FFFFFF`), scale 1.05x, dynamic glow.
  * Surrounding Lines: Semi-opaque (`#B3FFFFFF` for preceding, `#66FFFFFF` for future).
  * Auto-Scroll: The active line is centered automatically via a smooth vertical scroll spring animation keyed to the track's current timestamp.

---

## 4. Mini Player & Home Surface (Gestures & Layout Specs)

To achieve premium tactile response, the Home screen and Mini Player must operate with zero gesture conflict and fluid spatial layouts.

### 4.1 Mini-Player Interaction & Gestures
* **Draggable State:** Built using Jetpack Compose's `AnchoredDraggable` API.
* **Interaction Rules:**
  * **Swipe Up:** Seamlessly scales and expands the mini-player into the full Now Playing screen.
  * **Swipe Down:** Dismisses the player (collapses to bottom bar/stops playback).
  * **Horizontal Swipe:** Triggers track skip (Swipe Left = Next, Swipe Right = Previous).
* **Gesture Conflict Prevention:**
  * Standard taps on play/pause or skip buttons are intercepted at the pointer input level using Compose `PointerEventPass.Initial` to prevent triggering dragging motions.
  * Horizontal swipe tracking is locked if the vertical drag delta exceeds a threshold of 10dp.

### 4.2 Home Hub Surface Component Strategy
* **Floating Glass Tab Bar:** Placed 12dp above the bottom of the screen. As the user scrolls down, the tab bar height shrinks from 64dp to 52dp, text labels fade out, and background opacity increases to keep content visible.
* **YouTube Video Rail Cards:** Custom glass cards that display thumbnail images with rounded corners, a specular overlay, and a small duration badge in the bottom-right corner.
* **Scrolling Optimization:**
  * Glass cards on home scroll are loaded inside a lazy grid.
  * All backdrop blur operations on scrolling cards are bypassed during high-velocity scrolls (detected via `ScrollState.isScrollInProgress` with velocity checks) to prevent frame drops on mid-range hardware.

---

## 5. OS-Level Reliability (MediaSession & Background Quality)

Premium feel requires absolute background reliability. The audio session must never crash, controls must be instant, and the OS must recognize the app state perfectly.

### 5.1 MediaSession Service Checklist
* [x] **Foreground Service Lifecycle:** Starts the service with `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
* [x] **Media3 ForwardingPlayer:** Connects local audio ExoPlayer and remote YouTube WebView playback states under a unified interface.
* [x] **Transport Controls Configuration:** Maps Play, Pause, Next, Previous, SeekTo, FastForward, Rewind, and Stop commands.
* [x] **Lock Screen Artwork Serialization:** Scales the album art bitmap to $1024 \times 1024$ pixels in the background before attaching it to the `MediaMetadata` object to prevent OS memory overflow crashes.
* [x] **Notification Persistence:** Retains the playback notification card in a paused state when playback stops, allowing the user to resume later without reopening the app.

### 5.2 Notification & Permission Architecture
* **Foreground Service Startup:**
  * The service is launched via `ContextCompat.startForegroundService` when the user starts playback.
  * It executes a self-check on permission requirements (e.g., checks `Manifest.permission.POST_NOTIFICATIONS` on Android 13+).
* **Android 14+ / 15 Hardening:**
  * Implements `onTaskRemoved` handling: when the app is swiped away in the task manager, the service checks if music is active. If active, it stays alive; if paused, it releases the MediaSession and terminates itself cleanly to prevent battery drain.
  * Handles audio focus losses (transient losses mute/duck the volume, permanent loss stops playback).

---

## 6. Color Adaptation & Theme Intelligence

### 6.1 Theme Adaptation Algorithm
The palette extraction and color mixing system behaves as follows:

```kotlin
class PremiumThemeEngine {
    fun processSwatches(palette: Palette): ExtractedColors {
        val rawColor = palette.vibrantSwatch?.rgb 
            ?: palette.dominantSwatch?.rgb 
            ?: 0xFF121214.toInt()
            
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(rawColor, hsl)
        
        // Step 1: Constraint Saturation (Ensure it doesn't cause visual fatigue)
        hsl[1] = hsl[1].coerceIn(0.15f, 0.40f)
        
        // Step 2: Constraint Luminance (Ensure contrast safety for white text overlay)
        hsl[2] = hsl[2].coerceIn(0.10f, 0.25f)
        
        val correctedPrimary = Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        
        // Step 3: Deriving backgrounds and text colors
        val darkBackground = Color(0xFF0F0F12) // Static premium dark base
        val surfaceTinted = Color(
            androidx.core.graphics.ColorUtils.blendARGB(
                darkBackground.toArgb(),
                correctedPrimary.toArgb(),
                0.15f // 15% blend ratio
            )
        )
        
        return ExtractedColors(
            primary = correctedPrimary,
            secondary = Color(palette.mutedSwatch?.rgb ?: rawColor),
            background = darkBackground,
            surface = surfaceTinted,
            isDark = true
        )
    }
}
```

### 6.2 Theme Transitions & Contrast Safety
* **Transition Smoothing:** When transitioning between tracks with contrasting art, colors animate using a `tween(600, easing = FastOutSlowInEasing)` wrapper around `animateColorAsState` to prevent jarring flashes.
* **WCAG Contrast Checker:** Active text elements must maintain a contrast ratio of at least 4.5:1 against the underlying glass tints. If a calculated palette color fails the contrast check, the engine falls back to a pre-defined material accent color (e.g., `#00ADB5` Teal).

---

## 7. Performance Budgets & Polish (Shader & Blur Optimization)

### 7.1 Performance Budget (Target: 60fps / 120fps)
* **60fps Frame Budget:** 16.6ms total.
* **120fps Frame Budget (High-End Devices):** 8.33ms total.
* **Visual Layer Allocation:**
  * **AGSL Shader Rendering:** Max 1.2ms.
  * **Haze Backdrop Blur Pass:** Max 1.8ms.
  * **Compositing & Layout:** Max 2.0ms.
  * **Audio Visualizer Canvas Draw:** Max 1.0ms.

### 7.2 API Fallback Matrix
To maintain performance parity across devices, a tiered rendering system is enforced:

| Device Tier | API Level | Glass System Strategy | Specular Animation | Performance Target |
|---|---|---|---|---|
| **Tier 1** (High-End) | API 33+ | Full AGSL Liquid Glass + Haze | Gyroscope-driven refraction & specular highlight | 120fps |
| **Tier 2** (Mid-Range) | API 31–32 | Haze Child + standard RenderEffect blur | Dynamic static gradient overlays (no gyro calculations) | 60fps |
| **Tier 3** (Low-End) | API 26–30 | Opaque fallback + static color gradients | Disabled (static layouts) | 60fps |

### 7.3 Recomposition Safety Guidelines
* **State Isolations:** The progress bar slider position and visualizer FFT values are isolated within specialized, narrow-scope Composables. This prevents the parent screen (and heavy blurred background layout) from re-evaluating and recomposing on every millisecond shift.
* **Stable Contracts:** Media structures and custom UI state configurations are marked with Compose's `@Stable` or `@Immutable` annotations to assist the compiler in skipping redundant draws.

---

## 8. Implementation Roadmap (Phase Order)

To execute this plan systematically, work should follow this chronological sequence:

### Phase 1: Core Performance & Asset Setup (Foundation)
* Lock dependencies for `haze.compose` and `androidx.palette`.
* Establish `PremiumGlassTokens.kt` and integrate the `PremiumThemeEngine` HSL clamping algorithm.
* Wire system-level accessibility listeners to toggle fallback render modes.

### Phase 2: OS Session Hardening (Reliability)
* Rework `MusicPlayerService.kt` to start as a standard foreground media service with correct permission structures.
* Complete `ForwardingPlayer` implementations to seamlessly merge video and audio states.
* Implement task-removal listener and audio-focus state transition loops.

### Phase 3: The Component Library (Visuals)
* Build the core custom components: `GlassSurface`, `GlassPill`, `GlassButton`, and `GlassProgressBar`.
* Code the custom AGSL `liquidGlassEffect` modifier and attach rotation sensor listeners for gyro reflections.

### Phase 4: Now Playing Screen (UX Polish)
* Implement full-bleed Haze blurring and build the slide-up lyrics container.
* Add the 32-Bar Mirror Spectrum visualizer and bind its animations to spring dynamics.
* Wire the pulsing bass ring to the ExoPlayer visualizer processor thread.

### Phase 5: Home & Tab Integration (Polish & Delivery)
* Upgrade the Floating Tab Bar to support scroll-reactive shrinking behavior.
* Refactor the Mini Player using `AnchoredDraggable` and test gesture boundary priorities.
* Conduct profiling audits (systrace, frame-rendering checks) to ensure smooth 60fps/120fps performance.
