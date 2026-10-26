# SmartTube Authentication and YouTube Account Wire Flow

**Pinned source:** SmartTube `ffa16426ea3c7c82077e0f00228818b85f373588`; `MediaServiceCore` `7c2451ea9aaff623d53ef1fceaf863a9cfe16cf5`; `SharedModules` `d4f09b11133036d90826baf012e120d06486ae73`. **Research date:** 2026-10-18. **Mode:** static source only, no build/runtime/device.

## Resolved scope questions
- SmartTube pin: exact commit `ffa16426ea3c7c82077e0f00228818b85f373588` from `https://github.com/yuliskov/SmartTube`; selected as the repository HEAD obtained by cloning on the research date, with full SHA recorded before tracing.
- Submodules: `.gitmodules` defines `MediaServiceCore` and `SharedModules`; both were initialized successfully at the pinned gitlink SHAs above. `MediaServiceCore` itself references `SharedModules`, also checked out at `d4f09b11133036d90826baf012e120d06486ae73`. No submodule source is unavailable for this research.
- DeepEye auth class: current tree has `app/src/main/java/com/deepeye/musicpro/domain/auth/InnerTubeAuthManager.kt` and `YouTubeDeviceAuthManager.kt`; the existing auth audit identifies `YouTubeDeviceAuthManager` as RFC 8628 code/poll/refresh provider and `InnerTubeAuthManager` as session-facing coordinator. Current status was found by read-only existence checks plus `docs/youtube_personalization_auth_audit.md` (that existing doc references prior HEAD `baf23a...`; it is not treated as current-tree verification).
- AutoplayCoordinator: `app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoRadioCoordinator.kt` is **NOT FOUND**. Actual current-tree components named in the dirty baseline: `AutoplayRepository.kt`, `SmartTubeEngine.kt`, `ContentFetcher.kt`, `PlayerController.kt`, and `QueueManager.kt`.
- Pending HTTP 400 / same-track: current `docs/same_song_loop_root_cause.md` and `docs/same_song_loop_red_green.md` describe a prior reproduced HTTP 400 caused by synthetic `playlistId=RDAMVM<videoId>` and the single-item Repeat ALL loop. Current dirty diff removes the fabricated playlist field and handles one-item Repeat ALL; therefore these are currently being modified, not independently certified as closed. Exact 400 server reason remains unknown; the provided audit states H1/H2 status-only probe evidence is pending. No production files changed in this research.

## Account wire flow (source evidence)

1. **Sign-in UI:** `smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/signin/SignInFragment.java` obtains `SignInPresenter`, invokes `onViewInitialized()`, and offers external activation URL through QR/browser. `YTSignInPresenter.java` subscribes to `YouTubeServiceManager.instance().getSignInService().signInObserve()`.
2. **Device activation:** `MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/YouTubeSignInService.java` delegates `signInObserve()` to `YouTubeAccountManager.signInObserve()`. The exact polling/token persistence mechanics live in the manager/AuthService; they were not fully inspected in this pass, so detailed protocol claims are **PARTIAL**.
3. **Selected account / bearer:** `YouTubeSignInService.updateAuthHeaders()` reads the selected `Account`, extracts its refresh-token field, calls `AuthService.updateAccessToken(refreshToken)`, creates `tokenType + accessToken`, clears existing Retrofit auth headers, and installs `Authorization` only when both cached auth and selected account exist.
4. **Branded account context:** if selected account has a page-id token, `syncWithRetrofit()` adds `X-Goog-Pageid`. This is source-observed. It is not evidence that every InnerTube endpoint accepts/uses it.
5. **Refresh cadence:** `TOKEN_REFRESH_PERIOD_MS` is 60 minutes; `updateAuthHeadersIfNeeded()` skips refresh only when cached header matches current header and the cache age is below that period. The code comment says token max lifetime 60 minutes. This describes SmartTube’s code, not an official token guarantee.
6. **Account switch:** `AccountSelectionPresenter.selectAccount()` calls service `selectAccount(account)`, then updates channels and reapplies configured history state. Service-level selection is synchronized. Exact cache invalidation fanout and concurrent request fencing remain **PARTIAL**.
7. **Sign-out / account removal:** Account selector includes “none” (select null); `YouTubeSignInService.removeAccount()` delegates to manager. `GoogleSignInService.signOut()` is a TODO in its separate Google OAuth service. Do not generalize that TODO to the YouTube account path.

## Request contract / trust boundary

- Structural auth configuration in SmartTube: bearer authorization header assembled from the selected account’s refresh-token-derived access token; optional page ID header for branded accounts.
- Home/Music/Watch Next requests are private InnerTube endpoints and client configs, not established here as a documented public YouTube API contract.
- No credential values, account IDs, headers, cookies, tokens, or authenticated request/response bodies are reproduced in this document.
- The UI receives an activation code and URL by design; credentials are not scraped from browser/WebView state.

## Failure, cancellation, and cache findings

- `YouTubeContentService` calls `checkSigned()` before many account surfaces; `checkSigned()` invokes `YouTubeSignInService.checkAuth()`.
- Retrofit wrapper results can be null; Home returns null if result shelves absent, and observable emission helpers log/complete around missing groups. This can collapse provider failure to empty/null at higher layers; user-visible distinction depends on callers not yet exhaustively traced.
- Refresh is synchronized at the header-refresh method. Request-level simultaneous 401 recovery / cancellation/stale account result handling was **NOT fully traced**.
- `YouTubeServiceManager.invalidateCache()` clears Locale, auth header cache, app service and format caches. It proves a central invalidation hook exists, not that every account switch automatically invokes all steps.

## DeepEye account-wire review (current HEAD, line-anchored)

DeepEye source is at HEAD `6740b663b87dc6a0d948dedfabcd3e9e7e91151a`. The source files below are dirty: this section describes inspected working-tree behavior, not an immutable committed baseline.

| Step | DeepEye file / lines | Current-tree finding | Status |
|---|---|---|---|
| UI/device grant | `ui/screens/YouTubeLoginScreen.kt:32,53`; `domain/auth/YouTubeDeviceAuthManager.kt:40-60,107-167` | UI directly constructs device auth manager. Device-code request sends YouTube scope. Poll handles `authorization_pending` and RFC `slow_down` (+5 seconds); terminal errors stop. `IOException` is logged then polling continues without a shown retry bound. | `VERIFIED_IN_SOURCE` |
| OAuth client config | `YouTubeDeviceAuthManager.kt:42-48,52-60,114-125` | Client ID is reconstructed from an integer array; a private client-secret literal is present. Do not copy or reproduce either. This is not evidence of supported/official YouTube app identity. | `VERIFIED_IN_SOURCE`; provider support `UNKNOWN` |
| Token lifecycle | `YouTubeDeviceAuthManager.kt:25-30,138-145`; `InnerTubeAuthManager.kt:90-114,133-188` | Token response has `expiresIn`; coordinator reads `youtubeTokenExpiresAtEpochSec`, proactive refreshes near expiry, serializes via Mutex, times out after 25s, persists token pair and expiry. Legacy null expiry uses current token and relies on request-level 401 recovery. | `VERIFIED_IN_SOURCE` |
| Refresh errors | `InnerTubeAuthManager.kt:149-187` | Cancellation is rethrown; transient exception/null response preserves prior credentials; blank response tokens can lead to clearing credentials and cache. Exact `invalid_grant` classification is implemented indirectly from returned token fields in this code path; verify device auth manager result mapping before relying on it. | `VERIFIED_IN_SOURCE` / classification semantics `PARTIAL` |
| Cache invalidation | `AuthCacheManager.kt:17-31`; `InnerTubeAuthManager.kt:76-80,169-185,205-212`; `PersonalizationRepositoryImpl.kt:115-117` | Global `clearCache()` is called on token receipt, invalid-grant and logout. `invalidateAccountCache(accountKey)` is a no-op. Existing cache is global rather than demonstrably partitioned by account; check in-flight invalidation and all cache implementations next. | `VERIFIED_IN_SOURCE`; per-account cache isolation gap confirmed |
| Authenticated request client | `AuthenticatedYouTubeClient.kt:66-88,94-130` | `getValidAccessToken()` reads stored access token directly, not the expiry-aware `InnerTubeAuthManager.getAccessToken()`. It refreshes via a separate device-auth manager only after HTTP 401 and persists the pair without expiry via the 2-arg setter. Non-success browse reads/logs raw response body at 117-120; exception log includes exception object at 127-129. | `VERIFIED_IN_SOURCE`; privacy risk |
| Music browse | `InnerTubeRemoteClient.kt:488-560,565-...` | `browseMusic()` obtains token through manager; `postMusicBrowse()` adds Bearer if nonblank and retries once on 401. Other failures return blank body; parser path can return empty. Dirty shelf method separately blocks missing token and maps some failure types. | `VERIFIED_IN_SOURCE`, with dirty-worktree caveat |
| Home feed | `AuthenticatedYouTubeClient.kt:151-162`; `HomeFeedRepository.kt:50-74,122-...` | Auth state is access-token presence; authenticated fetch failures are caught to empty lists. If auth Home/Music is empty, other paths can serve public Home/trending depending on caller. Thus “signed-in” does not by itself prove each displayed item came from authenticated provider result. | `VERIFIED_IN_SOURCE`; exact current UI provenance `PARTIAL` |
| Account/session identity | `PersonalizationRepositoryImpl.kt:72-79,115-117` | Account session is observed with `collectLatest`; hidden-content manager switches account key. Feed state updates account session before rebuild. Cache invalidation hook is no-op; no generation ID/stale-response guard was established in inspected excerpt. | `VERIFIED_IN_SOURCE`; stale response guard `UNKNOWN` |

Historical DeepEye auth audits cite `baf23a435b737abb16b055e16082ea5ed1415812`; they are not used as current proof. No credential values or authenticated request payloads are reproduced. No authenticated request was made in Phase A.

## SmartTube token/session trace (pinned submodule)

- `YouTubeSignInService.updateAuthHeadersIfNeeded()` is `synchronized`; it reuses cached Authorization only if the cached value matches the Retrofit header and is younger than the declared 60-minute refresh period; otherwise it updates auth header (`MediaServiceCore/youtubeapi/.../YouTubeSignInService.java`, pinned `7c2451ea...`).
- `updateAuthHeaders()` reads selected account refresh token and asks `AuthService.updateAccessToken`; `syncWithRetrofit()` clears shared headers, sets bearer only if selected account and generated header are non-null, and adds page-ID header only if selected account is branded/page ID exists.
- `SignInService.selectAccount()` and `removeAccount()` delegate to `YouTubeAccountManager`; `AccountSelectionPresenter.selectAccount()` also updates channels and re-applies history preference. Exact account manager persistence/encryption, cache key strategy and concurrent request fencing were not read yet.
- `YouTubeServiceManager.invalidateCache()` invalidates Locale, YouTubeSignInService auth cache, AppService, and format cache. A global hook exists; automatic invocation on each account switch was not fully traced.
- SmartTube explicit sign-out UI action mapping to `removeAccount`/`selectAccount(null)` was not fully verified. Do not claim server token revocation or local secure deletion.

## DeepEye Auth Flow (read-only cross-check)

Current DeepEye HEAD `6740b663b87dc6a0d948dedfabcd3e9e7e91151a`:

- UI `YouTubeLoginScreen.kt:32,53` directly creates `YouTubeDeviceAuthManager`; device code/poll endpoints and token response parse are in `YouTubeDeviceAuthManager.kt:40-167`.
- `InnerTubeAuthManager.kt:90-114` is expiry-aware when its `getAccessToken()` is used. Its refresh outcome path `133-188` serializes refreshes, times out at 25s, persists `expiresAt`, and purges caches on success/invalid grant. `logout()` clears settings tokens and purges global cache at `205-212`.
- `AuthenticatedYouTubeClient.kt:66-88` bypasses `InnerTubeAuthManager.getAccessToken()`; reads `SettingsDataStore` access token itself and only refreshes on 401 through another path. This is a confirmed split lifecycle in the current dirty tree.
- `PersonalizationRepositoryImpl.kt:72-79` uses `collectLatest` for account session and switches hidden-content account scope; its account-cache invalidation method is no-op at `115-117`. Global `AuthCacheManager.clearCache()` is not evidence of account-scoped cache keys.
- No runtime auth, token, account-switch, or request test was performed.

## Confidence

- SmartTube sign-in UI/service/header mechanics: `VERIFIED_IN_SOURCE`.
- Low-level token persistence, refresh request, account-cache invalidation behavior: `PARTIAL` (manager/auth implementation needs line-by-line follow-through).
- Provider-supported official API contract: `UNKNOWN`; do not imply the private InnerTube contract is official.
- Runtime behavior: `NOT VERIFIED ON DEVICE`.
