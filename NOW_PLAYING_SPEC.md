# DeepEye Music Pro — Hybrid Now Playing 3.0 (STAGE 3)

This document specifies the UX layouts, visual controls, dynamic transitions, and video resolution controllers for Now Playing 3.0 in DeepEye Music Pro.

---

## 1. Hybrid Layout Concept

The Now Playing screen automatically switches between **Audio Mode** and **Video Mode** depending on the metadata type (`mediaItem.isVideo`). It avoids heavy dialog transitions or separate activities by swapping the rendering canvas dynamically in Compose.

```
┌──────────────────────────────────────┐
│             Top Nav Bar              │
├──────────────────────────────────────┤
│                                      │
│         [ Audio Canvas ]             │
│                 OR                   │
│         [ Video Canvas ]             │
│                                      │
├──────────────────────────────────────┤
│           Metadata Details           │
├──────────────────────────────────────┤
│          Waveform / Progress         │
├──────────────────────────────────────┤
│          Playback Controls           │
├──────────────────────────────────────┤
│           Utility Toolbar            │
└──────────────────────────────────────┘
```

---

## 2. Playback Modes Specifications

### Audio Mode Components
1. **Dynamic Album Art**: Renders the artwork inside a rounded box (`radius = 24dp`) using dominant color extraction to tint the screen background gradient dynamically.
2. **Audio Visualizer**: An overlay visualizer drawn using Canvas bounds responding directly to ExoPlayer's output amplitude bands.
3. **Interactive Lyrics Sheet**: An overlay bottom sheet synced to playback progression timing.
4. **Sleep Timer Control**: A timer controller allowing "end of current track" or customized minute countdown stops.

### Video Mode Components
1. **Native Video Surface**: Renders using Media3 `PlayerView` embedded inside Compose `AndroidView`. It supports Picture-in-Picture (PiP) rendering using Android's native system window transitions.
2. **Quality Selector**: A premium glassmorphic dropdown menu mapping directly to `PlayerController#setVideoQuality(quality)`.
3. **Playback Speed Controls**: Speeds ranging from `0.25x` to `2.0x` adjusting speed via `ExoPlayer#setPlaybackSpeed(...)`.

---

## 3. Dynamic Resolution Capping

Video resolution capping uses ExoPlayer's `TrackSelectionParameters` constraints to limit band allocation natively:

```kotlin
fun setVideoQuality(quality: String) {
    val maxHeight = when (quality.lowercase().substringBefore(" ")) {
        "1080p" -> 1080
        "720p" -> 720
        "480p" -> 480
        else -> Integer.MAX_VALUE
    }
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setMaxVideoSize(
            if (maxHeight < Integer.MAX_VALUE) maxHeight * 16 / 9 else Integer.MAX_VALUE,
            maxHeight
        )
        .build()
}
```

---

## 4. UI Transition Aesthetics
* **Shared Element Transition**: Sliding the card from the compact mini-player to full screen is animated using a physics-based spring model.
* **Ambient Glow**: The dominant color extracted from the album artwork or active video frame generates a background blur radial gradient, giving an premium glow effect.
* **Rotation Handling**: Entering video fullscreen triggers landscope orientation rotation natively using Android sensor locks.
