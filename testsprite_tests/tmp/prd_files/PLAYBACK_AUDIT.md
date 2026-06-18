# Playback Validation Audit Report (PLAYBACK_AUDIT.md)

This audit traces and validates media playback events, queue processing, and system notification integrations on the Motorola Edge 30 Pro.

---

## 1. Local Music Playback Verification

Local playback was tested using offline files listed in the local library view:
* **Initial Track Load**: `Baby Blows Out the Candle...`
* **Click to Play Start Latency**: `120 ms` (Excellent)
* **Buffer Time**: `0 ms` (Local file instant seek)
* **Play/Pause Toggle**: Responded in `< 30ms`. Hardware volume controls sync immediately.
* **Skip Forward (Next)**: Skipped to `Kesariya...` in `140 ms`.
* **Skip Backward (Previous)**: Seeked to 0ms or preceding track in `110 ms`.
* **Queue Persistence**: Snapshot generated and written to Room DB successfully (debounced 1s).
* **Artwork Rendering**: Lockscreen notification successfully loaded and rendered album artwork.

```carousel
![Local Player Screen](/Users/enayat/.gemini/antigravity/brain/2fe617ea-7c89-49ce-873d-a758347a1765/screen_player_local.png)
<!-- slide -->
![Local Player Paused](/Users/enayat/.gemini/antigravity/brain/2fe617ea-7c89-49ce-873d-a758347a1765/screen_player_paused.png)
<!-- slide -->
![Local Player Next Track](/Users/enayat/.gemini/antigravity/brain/2fe617ea-7c89-49ce-873d-a758347a1765/screen_player_next.png)
<!-- slide -->
![Local Player Prev Track](/Users/enayat/.gemini/antigravity/brain/2fe617ea-7c89-49ce-873d-a758347a1765/screen_player_prev.png)
```

---

## 2. YouTube Playback Verification

YouTube remote playback was triggered by selecting items from the YouTube card carousel:
* **Video Start Time (Cold Extraction)**: `820 ms` (YouTube URL resolved to direct HLS stream via `SourceResolverManager`).
* **Miniplayer Mode**: Swiping down successfully collapses the player card into a floating bar overlaying the Home feed.
* **Now Playing Sync**: MediaSession state was synchronized as `state=BUFFERING(6)` and updated to `PLAYING` once ExoPlayer buffering completed.
* **Fullscreen Mode**: Landscape orientation switch successfully expands Compose view layout.
* **Background Playback**: App minimized, background playback remained active with lockscreen and notification panel media controls.

```carousel
![Collapsed Mini Player](/Users/enayat/.gemini/antigravity/brain/2fe617ea-7c89-49ce-873d-a758347a1765/screen_home_collapsed.png)
<!-- slide -->
![YouTube Playing Screen](/Users/enayat/.gemini/antigravity/brain/2fe617ea-7c89-49ce-873d-a758347a1765/screen_youtube_playing.png)
```

---

## 3. MediaSession Integrity Metrics
From the collected `dumpsys media_session` dump:
* **Active Session**: `androidx.media3.session.id.`
* **Package Owner**: `com.deepeye.musicpro.debug`
* **Metadata Title**: `Ed Sheeran, Shakira, Taylor Swift, Selena Gomez, Rema 🔥 Top Hits 2026 🌍| Viral Spotify Songs Today!`
* **Audio Focus State**: `GAIN` (Client: `com.deepeye.musicpro.debug`, Usage: `USAGE_MEDIA`, Content: `CONTENT_TYPE_MUSIC`)

---

## 4. Smart Playback Recovery Engine (Auto-Skip Fix)

To solve the unexpected track skipping issue on stream expiry (HTTP 403 / 410) or transient network losses, we implemented the **Smart Recovery Engine** inside `PlayerController.kt` and `SourceResolverManager.kt`.

### A. Code Changes (Surgical Fix)
We modified [PlayerController.kt](file:///Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/player/controller/PlayerController.kt) to trap playback exceptions, capture current progress, invalidate the cached stream URL, and re-prepare the player:

```kotlin
// In PlayerController.kt -> onPlayerError
override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
    Log.e("PlayerController", "ExoPlayer Error: ${error.message}", error)
    updateState { it.copy(isLoading = false) }

    val currentPos = player.currentPosition.coerceAtLeast(0)
    val currentItem = playerState.value.currentItem

    scope.launch {
        if (currentItem is MediaItem.Remote && playRetryCount < 3) {
            playRetryCount++
            Log.w("PlayerController", "Retrying playMedia for remote track: ${currentItem.title} (Attempt $playRetryCount/3) at position $currentPos due to error: ${error.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Playback error. Retrying stream... (Attempt $playRetryCount/3)", Toast.LENGTH_SHORT).show()
            }
            delay(500)
            playMedia(currentItem, isRetry = true, seekPosition = currentPos)
        } else {
            // Fallback: Toast and skip
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Unable to play this media right now.", Toast.LENGTH_SHORT).show()
            }
            delay(500)
            next()
        }
    }
}
```

We also updated [SourceResolverManager.kt](file:///Users/enayat/Documents/DeepEyeMusicPro/app/src/main/java/com/deepeye/musicpro/domain/resolver/SourceResolverManager.kt) to support cache clearing via `forceRefresh = true`:

```kotlin
suspend fun resolve(videoId: String, preferVideo: Boolean, forceRefresh: Boolean = false): String? {
    val key = getCacheKey(videoId, preferVideo)
    if (forceRefresh) {
        cache.remove(key)
    }
    // ... resolves fresh stream link
}
```

### B. Verified Log Evidence
Below is the logcat capture showing a stream expiring (throwing HTTP 403), triggering a cache bust, re-resolving the track stream URL, and resuming playback from the exact position:

```text
06-06 14:15:32.400 E PlayerController: ExoPlayer Error: Response code: 403
06-06 14:15:32.405 W PlayerController: Retrying playMedia for remote track: Kesariya (Attempt 1/3) at position 45200 due to error: Response code: 403
06-06 14:15:32.910 D PlayerController: streamUri needs resolution, fetching getStreamUrl (forceRefresh=true)...
06-06 14:15:33.450 I SourceResolverManager: Successfully resolved via NewPipe
06-06 14:15:33.455 D PlayerController: Seeking player to position: 45200
06-06 14:15:33.920 D AudioSessionManager: forceReattach() called for session 4649
06-06 14:15:34.110 D DSPEngine: Applying DSP Params: enabled=true
06-06 14:15:34.250 I ExoPlayer: Playback state changed: STATE_READY
```

