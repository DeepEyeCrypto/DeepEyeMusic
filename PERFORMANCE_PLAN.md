# DeepEye Music Pro — Performance Optimization Plan (STAGE 13)

This document specifies the buffer sizes, memory bounds, database debounces, and Compose optimizations for DeepEye Music Pro.

---

## 1. Startup & Playback Target Latencies

We enforce ultra-low latencies to ensure an editor-grade typing and playback experience:

| Performance Metric | Target Threshold | Implementation Strategy |
| :--- | :--- | :--- |
| **Cold Launch Time** | `< 2.0 seconds` | Lazy-load Hilt ViewModels, defer non-essential diagnostic services. |
| **Playback Startup** | `< 500 milliseconds` | Pre-resolve next track stream URL, optimize buffer load controls. |
| **UI Frame-rate** | `> 60 frames/sec` | Minimize re-compositions using stable keys, cache graphics layers. |
| **Memory Headroom** | `< 120 MB RAM` | Release visualizer buffers, recycle album art bitmaps. |

---

## 2. ExoPlayer Buffer Load Controls

ExoPlayer's `DefaultLoadControl` is optimized to reduce stream buffering delay, starting audio playbacks as quickly as possible:

* **BufferForPlaybackMs**: `1000ms` (minimum data required to start playing).
* **BufferForPlaybackAfterRebufferMs**: `2000ms`.
* **MinBufferMs**: `15,000ms` (conserves battery).
* **MaxBufferMs**: `50,000ms`.

```kotlin
val loadControl = DefaultLoadControl.Builder()
    .setBufferParameters(
        15_000, // minBufferMs
        50_000, // maxBufferMs
        1_000,  // bufferForPlaybackMs
        2_000   // bufferForPlaybackAfterRebufferMs
    )
    .build()
```

---

## 3. Database Debouncing & Asynchronous Storage

* **Debounced Queue Serialization**: Writing the queue snapshot to SQL database is debounced by 1 second. This ensures that rapid track skipping or track drag-and-drops do not trigger continuous JSON serializations, preserving disk I/O.
* **Coroutines Dispatchers**: All database reads/writes are strictly constrained to `Dispatchers.IO` using database transactions, keeping the main thread free for fluid UI animations.
* **Bitmap Recycling**: Artworks are loaded using dynamic image scale-down routines and cached in memory, preventing garbage collection spikes.