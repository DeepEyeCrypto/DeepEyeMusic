# DeepEye Music Pro — Music Tab Architecture (STAGE 5)

This document specifies the audio-only content architecture, data source pipelines, and DSP constraints for the Music tab.

---

## 1. Content Boundaries

The Music tab displays **audio-only content exclusively**. All video cards, YouTube thumbnails, web view players, or video recommendations are excluded to maintain clean premium listening boundaries.

### Content Sources:
* **Local Storage**: Track files discovered on the device storage using Android `MediaStore` scanner.
* **Downloaded Media**: Audio tracks saved locally from remote streams, containing stored tags and local album art cache.
* **Audio-only Remote Streams**: YouTube tracks containing valid audio resolution formats, resolved to direct raw audio streams.

---

## 2. Audio-only Playback Routing

All items clicked on the Music tab route through a single audio processing pipeline:

```
Content Select (Audio Only)
  │
  ▼
PlayerController (sets MediaItem, initializes status)
  │
  ▼
ExoPlayer (Starts decoding, configures AudioTrack)
  │
  ▼
AudioSessionManager (detects audioSessionId, binds system effects)
  │
  ▼
DSPEngine (Applies Equalizer, BassBoost, Reverb, DynamicsProcessing, Loudness)
  │
  ▼
V4A Hardware Pipeline (Applies system-level tuning presets)
  │
  ▼
System Output (Bluetooth, Wired Headset, Speakers, USB DAC)
```

---

## 3. Local Search & Metadata Caching

To guarantee zero latency on keypress, all lists (Songs, Albums, Artists, Playlists) are:
* Indexed in the Room Database.
* Exposed as reactive `Flow<List<Song>>` queries from `LocalMusicRepositoryImpl.kt` utilizing SQLite indexing.
* Loaded asynchronously inside Hilt ViewModels to avoid blocking main UI layouts.
