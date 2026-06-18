# Playback Stability & Auto-Skip Root Cause (AUTO_SKIP_ROOT_CAUSE.md)

This audit documents playback interruption patterns, media session resets, and outlines defensive recovery strategies implemented to prevent auto-skips.

---

## 1. Interruptions & Skip Causes

We investigated key triggers causing unexpected playback stops or skips:

1. **Stream Expiration (HTTP 403 / 410)**:
   * **Root Cause**: YouTube stream URLs resolved via WebView or scrape APIs contain expiration timestamps (typically 6 hours). If a playlist remains paused in the background, subsequent play requests return a HTTP Forbidden error.
   * **Action**: ExoPlayer throws a `Source error` (HTTP response 403). Instead of skipping, we must detect 403/410 errors, call `sourceResolverManager.resolve(item.id, forceRefresh = true)`, and re-prepare the player.
2. **Buffer Underruns / Network Dropouts**:
   * **Root Cause**: Fast mobile transitions or cell tower switches drop TCP connections. Standard loaders throw network I/O exceptions.
   * **Action**: Implement a retry handler on standard player error listeners. Re-prepare the source up to 3 times before skipping to next.
3. **Audio Focus Steals**:
   * **Root Cause**: Temporary system notification beeps or assistant overlay requests transient focus loss, causing the player to pause.
   * **Action**: Configure ExoPlayer's `AudioAttributes` to handle focus changes automatically, allowing volume ducking for notifications and pausing only for permanent focus loss.
4. **DSP Effect Volatility**:
   * **Root Cause**: During fast track transitions, releasing effects on the old session and creating new ones on a new session can crash the audio thread if hardware effects slots are saturated.
   * **Action**: Enforce slot checks and release all effects cleanly on error, keeping playback alive in stereo-fallback mode instead of throwing fatal player exceptions.

---

## 2. Smart Recovery Engine Protocol

The following rules are implemented to safeguard playback stability:

| Failure Mode | Trigger Code | Recovery Policy |
| :--- | :--- | :--- |
| HTTP Forbidden | `403` / `410` | Invalidate stream cache → Resolve fresh URL → Seek to current position → Resume |
| Network Timeout | `IO_CONNECTION_FAILED` | Retry connection 3 times with exponential backoff before reporting error |
| Buffer Underrun | `BEHIND_LIVE_WINDOW` | Perform a silent seek to current player time to flush buffers and re-connect stream |
| Focus Loss (Transient) | `AUDIOFOCUS_LOSS_TRANSIENT` | Duck volume to 20% instead of pausing playback |
| DSP Init Failure | `Effect init error` | Disable active V4A effects chain, release resources, and fallback to dry stereo mix |

---

## 3. Playback Recovery Flow

When a playback error is captured:

1. Catch error in `PlaybackPathEnforcer` or `PlayerController`'s error listener.
2. Identify error type. If network/expired stream, count attempt number.
3. If `attempts < 3`:
   - Call resolver to fetch a fresh stream URL.
   - Update player media item with new URL.
   - Prepare and call `play()`.
4. If attempts exceed `3`, skip to the next track in the queue, logging a diagnostics report.
