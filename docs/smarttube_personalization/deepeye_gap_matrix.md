# YouTube Account Wire Connect — Evidence-Bounded DeepEye Audit

**DeepEye HEAD:** `6740b663b87dc6a0d948dedfabcd3e9e7e91151a` (`main`).  
**Scope:** read-only Phase A cross-check using current-tree references and SmartTube pinned-source comparison.  
**Caution:** some DeepEye source files are dirty. Existing audits reference earlier code commit `baf23a...`; assertions below are categorized accordingly.

## Current DeepEye account wire (known structure)

1. UI entry: `app/src/main/java/com/deepeye/musicpro/ui/screens/YouTubeLoginScreen.kt` instantiates `YouTubeDeviceAuthManager(OkHttpClient())` (source path was confirmed by symbol search).
2. Token provider: `app/src/main/java/com/deepeye/musicpro/domain/auth/YouTubeDeviceAuthManager.kt` handles device-code/poll/token APIs; `domain/auth/InnerTubeAuthManager.kt` coordinates session-facing token/store/cache behavior.
3. Consumers: `AuthenticatedYouTubeClient.kt` and `InnerTubeRemoteClient.kt` use auth manager; `SmartTubeEngine.kt` also injects `InnerTubeAuthManager`. `AutoplayRepository.kt` calls both SmartTube and InnerTube paths.
4. Cache boundary: `AuthCacheManager.purgeAllCaches()` exists and is called from `InnerTubeAuthManager` at several paths (symbol search). Exact per-account cache-key coverage and refresh single-flight behavior still need line-level read before Phase B.
5. Existing audit (older commit, not current verification): `docs/youtube_personalization_auth_audit.md` / `docs/youtube_personalization_auth_diagnostics.md` report device authorization, Bearer token, missing persisted expiresAt, presence-only Home auth gate, and swallowed failures. Their recorded commit is `baf23a435b737abb16b055e16082ea5ed1415812`, so all these details are historical until rechecked against current dirty files.

## Current tree findings from direct source/search evidence

- `AutoRadioCoordinator.kt`: `NOT FOUND` at expected path. Current path names show `AutoplayRepository` is a central coordinator, with `PlayerController` handling queue/player transition.
- `InnerTubeRemoteClient.fetchNextAutoplay(videoId, playlistId?)` still exposes optional playlistId in current dirty worktree, but `SmartTubeEngine.getAlgorithmicNext(videoId)` now sends bare videoId after removing optional playlist context. The current `AutoplayRepository` and `ContentFetcher` diffs use bare videoId for that SmartTube path. There are therefore potentially different contracts in parallel `/next` clients; no HTTP probe was made.
- Current dirty `AuthenticatedYouTubeClient.kt` adds signed-in-empty honesty: when account music fetch returns empty, it returns empty instead of guest trending fallback. The diff comment attributes this to a live 400/guest-filler report; this research does not independently verify that live trace.
- Current dirty `InnerTubeRemoteClient.kt` adds typed `MusicShelfFailureReason`, requires a token before `FEmusic_home`, throws typed errors for network/body/parser failures, and rethrows coroutine cancellation at several failure boundaries. This is a user-authored dirty change, not independently tested here.
- Current dirty `QueueManager.kt` makes one-item Repeat ALL return null (instead of reselecting current item), so autoplay can take over. Existing `same_song_loop_red_green.md` documents prior RED/GREEN runs, but current Phase A did not rerun tests.
- Current dirty `AutoplayRepository.kt` scrubs cached suggestions against current/recent IDs and propagates cancellation in some paths. This is not proof all stale/duplicate/failed candidates are guarded.

## Cross-system comparison to SmartTube

| System | SmartTube source evidence | DeepEye current path | Phase A assessment |
|---|---|---|---|
| Authorized account | `YTSignInPresenter` → `YouTubeSignInService` → account manager; selected account yields bearer + optional page ID | `YouTubeDeviceAuthManager` → `InnerTubeAuthManager` → auth clients | DeepEye has expiry-aware refresh/single-flight in one path, but `AuthenticatedYouTubeClient.getValidAccessToken()` bypasses it and refreshes only on 401. |
| Home | `YouTubeContentService` → `BrowseService2` → provider shelves and continuation | `HomeFeedRepository` / `AuthenticatedYouTubeClient` / `PersonalizationRepositoryImpl` | Auth gating uses access-token presence and per-call failures can become empty; other caller paths can show public/trending fallback. Provenance is not reliably implied by sign-in state. |
| Music | distinct liked row and Music browse surface | `AuthenticatedYouTubeClient` / `InnerTubeRemoteClient` / `MusicViewModel` | Dirty work adds honest empty and typed errors, but ordinary `browseMusic()` still returns empty on many errors; callers must retain typed status. |
| Up Next | parser exposes `nextVideoRenderer`, `autoplayVideoRenderer`; SmartTube automatic path consumes `nextVideoRenderer` through `MediaItemMetadata.getNextVideo()` | `SmartTubeEngine` and `InnerTubeRemoteClient` are parallel paths | DeepEye parser/consumer provenance parity NOT established; no evidence that its selected candidate corresponds to SmartTube’s explicit provider next model. |
| Continuation | next token and playlist context preserved in `SuggestionsGroup` | `InnerTubeRemoteClient` and model/parser paths | DeepEye next call accepts playlistId but not demonstrated index/params/continuation; request context needs mode-specific tests. |
| Repeat/queue | SmartTube user queue priority, playlist next, section next and provider next traced; repeat modes ALL/ONE are separate playback policies | `QueueManager` and `PlayerController` | Dirty fix prevents one-item Repeat ALL self-loop; full autoplay OFF/ON interaction not tested in this research. |
| Error/stale/cancel | SmartTube Rx actions disposed when video changes; failures call UI error path; queue transitions bounded by mode handling | `AutoplayRepository`, `PlayerController`, auth/cache components | Some cancellation propagation exists in dirty diff; account-generation stale result prevention and all fallback retry bounds remain unproven. |
| History | pause/resume/clear controls observed; playback event reporting not established | DeepEye local history and playback lifecycle | Provider reporting path is unknown; do not submit synthetic watch events. |
| Cache/account isolation | service-manager invalidation hook exists; complete per-account scoping unknown | `AuthCacheManager.purgeAllCaches()` invoked by auth manager; account cache API is no-op | Global clear exists, per-account keys and concurrent stale commit behavior need further trace. |

- `AutoplayRepository.generateNextQueue()` launches SmartTube `/next` and related-content fetch concurrently, then merges SmartTube first and related items second before de-duplication/filtering. Even if the SmartTube source item is provider-selected, merging local related items means queue ordering beyond first candidate is not purely provider-returned.
- Failure/error: `InnerTubeRemoteClient.fetchNextAutoplay()` attempts one authenticated 401 refresh then falls back to its secondary client for non-success or parse miss; `AutoplayRepository` also tries related content if primary candidate is null. The `CancellationException` propagation was added in the current dirty diff.
- Cache: `AutoplayRepository` returns cached queue after current/recent-history scrub if any candidate remains; cache-exhausted falls through to fetch. No cache account partitioning is evident in this method.
- Repeat/manual queue: one-item Repeat ALL behavior in `QueueManager` has a dirty fix; manual queue priority in `PlayerController`/`QueueManager` remains to trace. Pause/stop and late completion generation checks need explicit current-tree tracing/tests.

### Phase A cross-system status

| Topic | SmartTube source | DeepEye current source | Status |
|---|---|---|---|
| Repeat ALL / ONE | SmartTube `VideoLoaderController.applyPlaybackMode()` distinguishes ALL/SHUFFLE=`loadNext`, ONE=restart, LIST=next only in playlist/queue else stop; queue wins in `SuggestionsController.getNext()` | `QueueManager.kt` single-item ALL yields null in dirty diff; player/autoplay handling exists | SmartTube `VERIFIED_IN_SOURCE`; DeepEye current behavior `PARTIAL` |
| Stale metadata | SmartTube disposes Rx metadata actions on new video and before next load | DeepEye cancellation propagation is present at several catches | SmartTube subscription disposal `VERIFIED_IN_SOURCE`; DeepEye whole-path stale fencing `UNKNOWN` |
| Network/parser failure | SmartTube watch-next returns nullable result; suggestions error callback can surface UI error; continuation retries once auth=false on empty | DeepEye `/next` 401 refresh once, secondary-client fallback, then related fallback | Source flow verified; retry compatibility/runtime `UNKNOWN` |
| Cache on account change | SmartTube manager has global invalidation hook; per-account key isolation unknown | DeepEye global `purgeAllCaches`; `invalidateAccountCache(accountKey)` no-op | Per-account isolation `GAP CONFIRMED` |
| Reuse license | Root SmartTube MIT; submodule roots have no license file; SharedModules includes separately licensed MIT/Apache-2.0/EPL-1.0 vendored components | No code copied | Original submodule source license `UNKNOWN`; do not infer parent MIT |

## Priority / next research versus patch gate

- **P0 (already being worked in dirty tree):** same-track repeat loop; HTTP 400 from synthetic playlist ID on SmartTubeEngine path; user queue preservation; guest filler presented as signed-in personalization. Preserve all user changes; do not overwrite or reimplement blindly.
- **P1:** explicit provider autoplay candidate preservation (requires SmartTube consumer trace and DeepEye parser/consumer evidence); continuation/account context propagation; cache invalidation/account switch; refresh failure and cancellation/stale-completion handling.
- **P1:** account-switch in-flight stale result guard; `PersonalizationRepository.invalidateAccountCache()` no-op; typed auth provenance and Home/Music fallback labeling; player queue priority and pause/stop stale completion; continuation context.
- **P2:** labels/UI and optional local ranking. Never silently rerank provider order.

### Required Phase A follow-up / patch gates

| Item | Evidence needed before Phase B/C conclusion |
|---|---|
| DeepEye autoplay coordinator ownership | Read current `PlayerController`, `QueueManager`, `AutoplayRepository`, scheduler/callbacks; prove exact enqueue/transition path. `AutoRadioCoordinator` is absent, not itself the defect. |
| DeepEye auth client split | Determine why `AuthenticatedYouTubeClient` reads DataStore token while InnerTube path uses `InnerTubeAuthManager`; verify who persists expiresAt and who refreshes, without changing auth design in Phase A. |
| Account switch stale state | Trace `AccountSessionManager` and feed state commit generation; reproduce only via local tests if later authorized. |
| Error logging | Confirm raw browse error bodies are not credential-bearing; do not print/log payloads during this research. Replace sensitive logs only with separate authorization. |
| Submodule legal status | No root LICENSE in either submodule; inspect their repository metadata/headers or record unresolved. Do not claim all code is MIT. |

No implementation was performed. No build/test was run.

- No official public API contract for SmartTube’s private InnerTube request shapes is established.
- No claim that SmartTube always selects the parsed autoplay candidate has been made; downstream playback consumer trace remains incomplete.
- No authorized YouTube account or device was used. No `/next` runtime test, Home/Music sign-in verification, or three-track transition test occurred.
- HTTP 400 underlying server reason remains unknown. Existing worktree documentation says H1/H2 status-only probe remains pending; do not change other request fields based on speculation.

## Phase A exit assessment

**PARTIAL.** Revision-pinned source paths exist for SmartTube account/header mechanics, Home/Music request and mapping, explicit next/autoplay parsers, continuation, and history controls. DeepEye auth boundary and dirty code paths are identified, but a full line-anchored current-tree matrix plus SmartTube’s final candidate-to-player consumer/repeat policy has not yet been traced. The remaining omissions must be completed before Phase B gap matrix or Phase C patch selection.
