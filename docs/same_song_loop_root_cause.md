# Same-Song Loop — Root Cause

- Observed loop type: C (autoplay path yields no new track) + G-adjacent
  (single-item Repeat ALL wraps modulo to index 0, replaying the same audio).
- Failure stage: E (candidate filtering / queue policy) + C (request contract).
- Exact code paths:
  1. `QueueManager.next()` — `RepeatMode.ALL` branch:
     `(_currentIndex + 1) % q.size` with `q.size == 1` always returns index 0,
     i.e. the current item. Natural completion (`onTrackEnded() -> next()`)
     and manual Next therefore replay the same song instead of reaching the
     autoplay emergency fallback in `PlayerController.next()`.
  2. `/next` HTTP 400 — fabricated `playlistId = RDAMVM<videoId>` appended to
     the TVHTML5 `/next` request (SmartTubeEngine.fetchUpNext /
     getAlgorithmicNext, AutoplayRepository, ContentFetcher). Device logcat
     evidence: `InnerTubeRemoteClient: Autoplay /next returned HTTP 400` then
     `autoplay_loop_guard merged=0 ... effective=0` then
     `AutoplayEngine: Prefetch returned empty candidates!`.
- Rejected hypotheses: expired token (no 401 in trace; 401 path is separate
  and bounded), parser-shape drift (parser never ran — response was HTTP 400),
  ExoPlayer thread violation (addMediaItem already on Dispatchers.Main),
  resolver cache cross-contamination (cache key includes videoId).
- Remaining uncertainty: exact server-side invalid-field reason string for
  HTTP 400 (error body not captured through a sanitized diagnostic path).
  The fix removes the fabricated field rather than guessing a new one.
