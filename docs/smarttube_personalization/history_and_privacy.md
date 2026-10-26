# History, Playback Signals, and Privacy

**Pinned source:** SmartTube `ffa16426ea3c7c82077e0f00228818b85f373588`; `MediaServiceCore` `7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5`. **Research date:** 2026-10-18. Static inspection only.

## SmartTube evidence

- `ContentService` defines history enable/clear methods. `YouTubeContentService.enableHistory(true)` invokes `ActionsService.resumeWatchHistory()`; false invokes `pauseWatchHistory()`; `clearHistory()` invokes `clearWatchHistory()`.
- `AccountSelectionPresenter.selectAccount()` reapplies configured history state after account selection, except when the preference is automatic.
- These methods control account history state; the inspected path does not prove a playback progress/watch-event reporting endpoint or that playback completion changes future recommendation ranking.
- Watch-history browse data is a separate provider surface. Whether history is disabled, empty, or unavailable affects recommendations is `UNKNOWN` from this source trace.
- `SuggestionsController`/`VideoLoaderController` dispose some outstanding metadata/Rx actions on new video and next transition. This helps prevent stale callbacks, but transport cancellation and all race behavior are not guaranteed by the inspected call sites.
- `WatchNextService.continueGroup()` retries a null/empty continuation once with auth disabled. Broader retry/backoff/idempotency behavior was not found.

## DeepEye current-tree evidence

- `InnerTubeAuthManager` purges global caches on token receipt, refresh success, invalid grant and logout. `AuthCacheManager` calls `YouTubeRepository.clearCache()`; `PersonalizationRepositoryImpl.invalidateAccountCache(accountKey)` is currently a no-op.
- `PersonalizationRepositoryImpl.observeAccountAndQueue()` uses `collectLatest`, updates account session, switches HiddenContentManager scope, and invokes feed rebuild. The inspected code has no explicit generation/version guard when asynchronous requests complete. Stale-result rejection after account switch remains `UNKNOWN` pending full path tracing.
- `AutoplayRepository` marks candidates fetched/merged and applies current/recent-history filters; the shown code does not itself prove queued or prefetched tracks are marked as actually played. Follow lifecycle acknowledgements before changing history.
- DeepEye local playback history and provider YouTube watch history are separate stores/concepts. Do not claim local plays influence YouTube recommendations.

## Privacy constraints / telemetry audit scope

Phase A did not perform a complete application-wide logging audit. The DeepEye files read in this task include `InnerTubeAuthManager.kt`, `YouTubeDeviceAuthManager.kt`, `AuthCacheManager.kt`, `HomeFeedRepository.kt`, `PersonalizationRepositoryImpl.kt`, `AuthenticatedYouTubeClient.kt`, `InnerTubeRemoteClient.kt`, `AutoplayRepository.kt`, and relevant SmartTube sources. A specific finding: `AuthenticatedYouTubeClient.browseInternal()` currently reads and logs `response.body?.string()` on non-success (`AuthenticatedYouTubeClient.kt:117-120`); content may contain private response data and should be reviewed under separate authorization before production changes.

Never log tokens, cookies, account IDs, titles, search queries, raw payloads, signed URLs, or watch history. Safe telemetry, if separately authorized, remains default-off and limited to correlation ID, endpoint category/status, auth state enum, counts, provenance enum, retry count and stale-discard reason. No real credentials or private recommendation payloads were used in this research.

## Status

- History pause/resume/clear controls: `VERIFIED_IN_SOURCE`.
- Playback watch-event reporting mechanism and recommendation effect: `UNKNOWN`.
- DeepEye account switch stale result prevention: `UNKNOWN/PARTIAL`.
- Global cache purge exists; per-account cache isolation: `NOT ESTABLISHED`.
- Device/runtime verification: `NOT PERFORMED`.
