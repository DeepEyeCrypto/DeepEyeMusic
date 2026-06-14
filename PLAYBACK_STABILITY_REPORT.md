# PLAYBACK_STABILITY_REPORT

## Current Assessment
Playback is generally stable but suffers from edge-case synchronization errors, primarily due to the bridging between ExoPlayer (Audio) and WebView (Video data extraction).

## Key Stability Issues
1. **Rapid Skipping**: Spamming "Next" can cause the `WebViewBridgeResolver` to queue multiple background loads. If an older load resolves after a newer one, the player might switch back to the wrong track.
2. **State Thrashing**: `PlayerController` emits too many intermediate states during initialization (e.g., `BUFFERING` -> `READY` -> `BUFFERING`), causing UI flickers.
3. **Queue Reordering**: Drag-and-drop operations on the queue occasionally misalign the `MediaController`'s internal index with the UI's list.

## Action Plan
- **Debounce Inputs**: Implement a `Flow.debounce` or similar throttle on the `skip` actions in the ViewModel.
- **Resolver Cancellation**: When a new track is requested, strictly cancel any pending `SourceResolver` coroutine jobs.
- **Media3 Queue Management**: Only interact with the queue via `MediaController.moveMediaItem()`, ensuring the single source of truth is the MediaSession, not local state.
