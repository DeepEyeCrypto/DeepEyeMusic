# SmartTube Home and Music Personalization Flow

**Pinned source:** SmartTube `ffa16426ea3c7c82077e0f00228818b85f373588`; `MediaServiceCore` `7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5`. **Research date:** 2026-10-18. **Evidence class:** static source only.

## SmartTube Home

**Entry → request → mapping:** `ContentService.getHomeObserve()` → `YouTubeContentService.getHomeObserve()` → `checkSigned()` → `BrowseService2Wrapper.INSTANCE.getHome()` → `BrowseService2.getHome()` → `BrowseApiHelper.getHomeQuery(clientTV)` → TV browse endpoint → `BrowseResult` shelf parser → `ShelfSectionMediaGroup` list plus continuation token. `YouTubeContentService.getHome()` preserves shelf order and expands empty/chip groups with `continueEmptyGroup()`.

- `checkSigned()` invokes `YouTubeSignInService.checkAuth()`; it is not just an OAuth presence check in the UI. The API request uses selected account’s shared auth headers when present. This is source-observed, not runtime proof of personalized response.
- No explicit guest Home fallback was proven in this code path. Wrapper defaults and server response under no account remain unverified. Do not treat HTTP 200 or generic shelves as personalized.
- `BrowseService2.getBrowseRowsTV()` returns shelves and a continuation token. `YouTubeContentService.emitGroupsPartial()` repeatedly requests continuation while data/key remains and emits rows in source order.
- Failure/empty handling can log and complete without typed source/provenance, so empty response vs provider failure may be indistinguishable to a higher layer.
- Cache partitioning by account is not proven. Manager-level invalidation exists, but per-account cache keys were not traced.

## SmartTube Music

**Entry → request → mapping:** `YouTubeContentService.getMusicObserve()` → `checkSigned()` → `BrowseService2.getLikedMusic()` emitted first → `BrowseService2.getMusic()` emitted after. Music uses a distinct provider browse surface; liked Music is separately fetched from a continuation/WEB path. These are provider-returned rows, not locally filtered Home results (`VERIFIED_IN_SOURCE`).

- Auth check is called before the Music flow; successful live account provenance is not verified.
- History disabled/insufficient behavior is unknown; UI reapplies the configured history setting after account selection, but the feed fallback behavior was not established.
- Parser failure and valid empty can both lead to no emitted items; no complete typed distinction was found.
- Wrapper/cached data path was not exhaustively traced; cache-per-account isolation is unknown.

## Home and Music trace (pinned SmartTube source)

### Home

- Entry: `MediaServiceCore/mediaserviceinterfaces/.../ContentService.java` exposes `getHomeObserve()`.
- Call: `MediaServiceCore/youtubeapi/.../YouTubeContentService.java#getHomeObserve()` calls `checkSigned()` and emits `BrowseService2.getHome()` rows.
- Request: `MediaServiceCore/youtubeapi/.../BrowseService2.kt#getHome()` invokes `getBrowseRowsTV(BrowseApiHelper::getHomeQuery, TYPE_HOME)`; browse helper selects TV client config.
- Parse: TV browse result shelves map to ordered `ShelfSectionMediaGroup`s; pair returns groups plus continuation. `YouTubeContentService.emitGroupsPartial()` requests continuation repeatedly while token/groups remain; empty groups can be expanded.
- Failure/empty: missing groups/pair can log/complete without typed error. Guest response behavior and user-visible provenance are not proven.

### Music

- Entry: `YouTubeContentService.getMusicObserve()` calls `checkSigned()`, emits `getLikedMusic()` first, then Music browse shelves from `BrowseService2.getMusic()`.
- Request: Music uses its own `getMusicQuery(clientTV)`; Liked Music has TV continuation path and separate WEB helper.
- Parse: provider shelves map to Music MediaGroups; continuation token retained in browse pair. It is not a local post-filtered Home feed in this source path.
- Failure/empty: no complete typed distinction between parser failure and valid empty feed established.

## DeepEye current-tree Home/Music audit (HEAD `6740b663...`, dirty files)

| Path | Evidence | Finding |
|---|---|---|
| Home auth gate | `HomeFeedRepository.kt:50-74` | `hasAuth` means stored access token is non-null; five auth fetches catch exceptions to `emptyList()`. |
| Home selection | `HomeFeedRepository.kt:122-161` and later branches | Auth branch uses auth results; non-auth branch uses public/search results. Empty/failing account result can be visually indistinguishable from empty successful account feed unless error/provenance is propagated. |
| Account music | `AuthenticatedYouTubeClient.kt:177-234` | Merges Music home, liked and history; dirty diff returns empty instead of trending filler when token present and all account sources empty. Guest-only trending search remains. |
| Music shelves | `AuthenticatedYouTubeClient.kt:262-269`; `InnerTubeRemoteClient.kt:687+` | Dedicated shelf method delegates to token-gated parser with typed errors in dirty worktree. Ordinary `browseMusic()` catches many failures and returns empty (`InnerTubeRemoteClient.kt:488-507`). |
| Local section assembly | `PersonalizationRepositoryImpl.kt:214-269` | Local history/queue, account shelves, hidden-content/short/music filters and optional ranking are composed locally. This local filtering/ranking is not YouTube ranking. |
| Account session cache | `PersonalizationRepositoryImpl.kt:72-79,115-117` | `collectLatest` switches hidden-content scope, but `invalidateAccountCache(accountKey)` is no-op and explicit generation-based stale completion guard was not found. |
| UI state | `MusicViewModel.kt:74-80,125-170`; `MusicScreen.kt:374-376` | ViewModel differentiates signed-in guest/account state and several account unavailable outcomes; exact HomeFeedRepository UI provenance remains caller-specific. |

## Cross-system comparison

| Capability | SmartTube evidence | DeepEye current path | Status |
|---|---|---|---|
| Home/Music provider order | Provider shelves emitted in returned order, continuation retained | Auth list merged/filtered with local content in some paths; dedicated shelves are separate | `VERIFIED_IN_SOURCE`; do not claim all DeepEye rails preserve provider order |
| Auth provenance | Selected account headers applied after `checkAuth()` | Some surfaces key off access-token presence; `MusicViewModel` has explicit session state | `PARTIAL`; runtime provider acceptance unknown |
| Parser failure vs empty | Some SmartTube paths return null/complete with log | Several DeepEye paths convert exception/parser failures to empty list; dedicated shelves typed in dirty path | `VERIFIED_IN_SOURCE`; residual ambiguity exists |
| Cache isolation | global invalidation hook, exact account-keying unknown | global purge; per-account cache hook no-op | isolation gap `VERIFIED_IN_SOURCE` |
| Local ranking | Provider result mapped and rendered; local UI behavior varies | hidden content, music/short filtering and local ranker exist | local personalization `VERIFIED_IN_SOURCE`; not provider personalization |

## Status

- SmartTube Home/Music flow: `VERIFIED_IN_SOURCE`.
- Successful signed-in personalization: `NOT VERIFIED ON DEVICE`.
- DeepEye current paths: `VERIFIED_IN_SOURCE` for the lines above; tree is dirty.
- Account switch stale-result behavior, cache isolation, provider API contract: `UNKNOWN/PARTIAL`.
