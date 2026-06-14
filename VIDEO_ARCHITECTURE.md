# DeepEye Music Pro — Video Tab Architecture (STAGE 6)

This document specifies the video playback architecture, PlayerView surface rendering, Picture-in-Picture (PiP) transitions, and audio-path routing constraints.

---

## 1. Playback Pipeline & Video Surface

Unlike the legacy hybrid layout which loaded video inside a muted WebView and intercepted audio stream bytes, the updated Video tab routes both video rendering and audio playback through the same single `ExoPlayer` instance:

```
Video Item Click
  │
  ▼
PlayerController (resolves stream URL, sets MediaItem)
  │
  ├─► Video Path ──► ExoPlayer ──► PlayerView (natively rendered in Now Playing Screen)
  │
  └─► Audio Path ──► ExoPlayer ──► AudioSessionManager ──► DSPEngine ──► V4A Hardware
```

---

## 2. Media3 PlayerView Integration

Video frames are rendered natively using a Compose wrapper over Media3's `PlayerView`:

* **Jetpack Compose Wrapper**: Wraps `androidx.media3.ui.PlayerView` in an `AndroidView`.
* **Zero Controllers**: `useController = false` is set on PlayerView to disable default media controls, letting the custom Compose player control overlay handle the play/pause, seek, and quality selections.
* **Aspect Ratio**: Renders in `16:9` ratio using `AspectRatioFrameLayout.RESIZE_MODE_FIT` by default, expanding to full screen crop when rotation locks trigger.

---

## 3. Picture-in-Picture (PiP) Mechanics

PiP is managed natively without hacky WebView artwork layers:
* Entering PiP triggers `enterPictureInPictureMode(params)` in `MainActivity`.
* The system window crops to show the native `PlayerView` bounds.
* Audio focus is maintained continuously in the background using Hilt service bindings, ensuring that the V4A DSP hardware chain remains attached to the player's active audio session ID.
* The WebView fallback is completely disabled. All video matches standard media resolution pipelines.
