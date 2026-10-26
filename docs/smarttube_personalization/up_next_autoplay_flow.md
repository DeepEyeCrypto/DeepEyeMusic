# SmartTube Up Next, Autoplay, and Queue Trace

**Pinned source:** SmartTube `ffa16426ea3c7c82077e0f00228818b85f373588`; `MediaServiceCore` `7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5`. **Research date:** 2026-10-18. **Scope:** static source; no player build/run.

## Request and parse path (verified)

`WatchNextService.getMetadata(MediaItem)` forwards the current item’s `videoId`, `playlistId`, and `playlistIndex` to `getMetadata(videoId, playlistId, playlistIndex, playlistParams)`. That constructs `WatchNextApiHelper.getWatchNextQuery(clientTV, ...)`, calls watch-next API and parses `WatchNextResult`.

The helper documents two request shapes in source: current-video request with video ID; playlist-context request with video ID, playlist ID, index, optional params. For the v2 helper, `videoId` becomes empty when absent; playlist ID/index/params are passed only when provided. Do not invent playlist/radio IDs.

The parsed response has distinct fields:
- `autoplay.autoplay.sets[0].nextVideoRenderer`
- `autoplay.autoplay.sets[0].autoplayVideoRenderer`
- `autoplay.autoplay.replayVideoRenderer`

`WatchNextHelper.getNextVideoItem()` extracts `nextVideoRenderer`; `getAutoplayVideoItem()` extracts `autoplayVideoRenderer`; each resolves renderer variants including history endpoint/autoplay endpoint/wrapped autoplay endpoint. `NextVideoItem` can preserve `videoId`, `playlistId`, `playlistIndex`, `params` from endpoint. This is strong source evidence that provider-selected next/autoplay candidates are modeled separately from the visible suggestions shelf.

Suggested videos are separately obtained from pivot/section-list shelves and mapped into `SuggestionsGroup`. Continuation uses next-page key; continuation parser maps item wrappers, sets fallback playlist index when missing, preserves base-group params, and propagates continuation token.

## Selection and transition policy (consumer now traced)

- `MediaItemMetadataImpl` converts `watchNextResult.getNextVideoItem()` into `nextMediaItem` and exposes it as `MediaItemMetadata.getNextVideo()`; this parser path uses `nextVideoRenderer`, not `autoplayVideoRenderer` directly. The `getAutoplayVideoItem()` accessor exists in `WatchNextHelper.kt`, but no consumer of that accessor was found in SmartTube app or MediaServiceCore.
- `Video.sync(metadata)` stores `metadata.getNextVideo()` via `findNextVideo()`. When provider next is null and the item is remote, a fallback can select the first playable non-blocked item from `suggestions[1]`. Therefore provider next and local suggestion fallback remain distinct.
- `SuggestionsController.getNext()` selection priority is: user `Playlist.instance().getNext()` first; then, if not shuffled and current playlist ID starts with `RD` and next item matches playlist, provider next; then a locally chosen `mNextSectionVideo` if not shuffled; then provider `nextMediaItem`. This is source-observed selection policy in the current pinned source.
- `VideoLoaderController.waitMetadataSync()` automatically calls `openVideoInt(Video.from(current.nextMediaItem))` when next metadata is ready. It retries metadata wait after 1 second near end when current item is not synced. This is a playback transition path, distinct from `setNextTitle()` UI display.
- SmartTube shuffles user-created playlists by requesting metadata for a random playlist index; queue priority and shuffle are explicit. Repeat mode interaction (Repeat One/All) still needs exact player mode trace and is not inferred from these methods.
- `PlaybackFragment.setNextTitle()` only updates visible title; it is not the selection authority. `BackboneQueueNavigator` is a MediaSession stub and not the internal playlist authority.
- `WatchNextServiceWrapper` can replace metadata next/suggestions using a cached playlist group when playlist context does not match first suggestion; it sets `nextVideo` to cached current index + 1 and retains provider suggestions after the cached group.
- Prefetch timing, stale completion discard, cancellation through to player, actual audio source identity, and bounded unplayable fallback remain **UNKNOWN/PARTIAL**.

## Continuation and failure

`WatchNextService.continueGroup()` extracts continuation key; first requests continuation with normal auth handling, and retries with `auth=false` only if null/empty. `SuggestionsGroup.from()` rejects null response/base group; maps item wrappers; retains `params`, playlist indices/IDs, title and continuation token. Exact network retry policy and HTTP errors depend on Retrofit wrappers and are not fully traced.

## DeepEye current tree comparison

- `SmartTubeEngine.getAlgorithmicNext(videoId)` currently has dirty-tree changes removing `playlistId` argument and sends only videoId. `AutoplayRepository` and `ContentFetcher` also changed to call bare-videoId path. Existing docs tie prior HTTP 400 to synthetic `RDAMVM<videoId>`; current diff removes that field. Root server-side error text remains unknown; H1/H2 status-only probe remains pending.
- `InnerTubeRemoteClient.fetchNextAutoplay()` is a second implementation path. Existing dirty diff adds `CancellationException` propagation in several catch blocks; confirm all remaining catch boundaries before implementation.
- `AutoplayRepository` currently has dirty-tree cache dedup/current/history guard; queue behavior in `QueueManager` yields null on single-item Repeat ALL so player may request autoplay instead.
- Do not call current behavior “YouTube original algorithm cloned.” At most, identify parsed provider autoplay field and preserve provenance if both parser and downstream consumer prove it.

## Repeat and transition policy (pinned-source trace)

At SmartTube SHA `ffa16426ea3c7c82077e0f00228818b85f373588`:

- `VideoLoaderController.onPlayEnd()` calls `applyPlaybackMode()`, except the non-overlay-dialog deferral path (then applies mode when dialog finishes).
- `PLAYBACK_MODE_ALL` and `PLAYBACK_MODE_SHUFFLE` call `loadNext()`. `PLAYBACK_MODE_ONE` seeks to 0 (engine restart on old Android). `PLAYBACK_MODE_LIST` advances only when playlist successor or user queue exists; otherwise stops. `CLOSE`/`PAUSE` usually close/stop but allow queue advance when configured not to respect mode.
- `loadNext()` uses `SuggestionsController.getNext()`; if null, `waitMetadataSync()` opens `current.nextMediaItem` once populated, or schedules another metadata check after 1 second near end if current is unsynced.
- `SuggestionsController.getNext()` order: user queue first; matching radio/mix playlist successor when eligible; local next-section item when unshuffled; then metadata/provider next item.
- This proves source-level policy, not that every endpoint succeeds or audio identity changes on device.

## Stale work / error path

- `SuggestionsController.loadMetadata()` disposes prior Rx actions first; `onNewVideo()` also disposes metadata actions. `VideoLoaderController.openVideoInt()` disposes service/player callbacks before loading new item. This is stale-work mitigation through subscriptions/callback cleanup.
- `SuggestionsController` metadata error callback may show a message; no general exponential retry loop was found in this trace.
- `WatchNextService.continueGroup()` retries a continuation once with auth disabled if the first response is null/empty. This is a narrow fallback, not an auth retry contract.
- `VideoLoaderController.waitMetadataSync()` retries near end at 1-second intervals only while current item remains unsynced. `disposeActions()` removes scheduled metadata-sync callbacks.
- Transport-level cancellation guarantees, stale account-generation checking, and race behavior if disposed callback already dispatches remain **PARTIAL/UNKNOWN**.

## DeepEye current account/cache trace (read-only)

- `InnerTubeAuthManager.getAccessToken()` proactively refreshes expired/near-expiry token, with serialized refresh in `refreshAccessTokenOutcome()`; successful refresh persists expiry and purges global cache.
- `AuthenticatedYouTubeClient.getValidAccessToken()` reads stored token directly; its own `handle401AndRetry()` calls `YouTubeDeviceAuthManager.refreshToken()` and persists access/refresh via a two-argument setter. It does not use the expiry-aware coordinator on this path. This is a confirmed split token lifecycle.
- `PersonalizationRepositoryImpl.observeAccountAndQueue()` uses `collectLatest`, updates session, switches hidden-content account key, and rebuilds. `invalidateAccountCache(accountKey)` is no-op. No feed-generation guard was found in inspected path; stale response commit risk remains to verify.
- `AuthCacheManager` calls repository-wide `clearCache()`. That is a global purge, not proven per-account cache partitioning.
- Current dirty `AutoplayRepository` runs provider and related searches concurrently. It merges provider primary first with locally fetched related candidates; provider order is therefore not the whole candidate order. Cached results are filtered against current/recent history, but no account-scoped key was evident in the shown method.

## Confidence

- SmartTube request parsing, source selection order, repeat modes, stale-subscription disposal and bounded continuation fallback: `VERIFIED_IN_SOURCE`.
- DeepEye source structure, token lifecycle split, global invalidation and no-op account-cache invalidation: `VERIFIED_IN_SOURCE` (dirty current tree).
- Server-side recommendation ranking, all cancellation races, account-specific cache isolation in every implementation, and on-device transition/audio identity: `UNKNOWN` / not device verified.
