# OAuth Expiry Fix Plan — Current-Tree Baseline (baf23a43, main)

## 1. Confirmed cause
- `TokenResponse.expiresIn` is parsed (`YouTubeDeviceAuthManager` poll/refresh, 3 sites)
  but never persisted. `SettingsDataStore` holds only
  `youtubeAccessToken` / `youtubeRefreshToken` — no expiry column.
- `InnerTubeAuthManager.getAccessToken()` returns any non-blank token with no
  expiry check; it only refreshes when the access token is missing/blank.
- Result: an expired token is sent until a 401 arrives. The 401→refresh→retry-once
  rescue already exists in `InnerTubeRemoteClient` (`postMusicBrowse`, `browseMain`,
  `fetchNextAutoplay`) and `AuthenticatedYouTubeClient.handle401AndRetry`, so the
  residual bugs are: (a) no proactive refresh, (b) concurrent browse/music calls can
  stampede N parallel refreshes, (c) `AuthenticatedYouTubeClient` refreshes via
  `YouTubeDeviceAuthManager` directly, bypassing expiry persistence + cache purge,
  (d) auth failures are swallowed to `emptyList()` and rendered as guest content
  with `hasAuth=true` — guest masquerading as personalized.

## 2. Scope contract (what changes / what does not)
- Change: `SettingsDataStore` (+1 nullable expiry column, defaulted param),
  `TokenExpiry` (new pure helper), `InnerTubeAuthManager` (proactive + single-flight
  + typed outcome), `AuthenticatedYouTubeClient` (delegate refresh to the manager),
  login save path (persist expiry), `HomeFeedState` (+3 defaulted honest-label fields),
  `HomeFeedRepository` (set labels, never throw), `NetworkEvent` (+4 defaulted
  redacted fields), tests, docs.
- No change: InnerTube client names/versions, OAuth endpoints/client IDs/scopes,
  playback, DSP, visualizer, downloads, recovery, queue, Smart Mix, Room schema.
- No raw BODY logging. No token/cookie/query/title logging. No cookie extraction,
  no SAPISIDHASH synthesis, no client-identity spoofing.
- InnerTube `youtubei/v1` + TVHTML5/ANDROID_MUSIC contexts are private/undocumented
  and stay classified UNSTABLE.

## 3. Expiry model
- `youtubeTokenExpiresAtEpochSec: Long?` (absolute epoch seconds, null = unknown/legacy).
- `TokenExpiry.computeExpiresAt(nowEpochSec, expiresInSec)`: null when provider omitted
  (`expiresIn <= 0` or null); clamps absurd values to `MAX_EXPIRES_IN_SEC = 90 days`;
  pure + injectable-clock friendly (caller passes `now`).
- `TokenExpiry.isExpiredOrNear(expiryEpochSec, nowEpochSec, safetyMarginSec = 300)`:
  null expiry → false (legacy path relies on 401 rescue; documented below).
- Migration: additive nullable column, no DataStore/Room migration needed; legacy users
  with null expiry keep current behavior (token used as-is, 401 rescue still applies).
  No wipe of unrelated prefs. `exportToJson()` redacts tokens before serialization
  (backup must never carry secrets).

## 4. Refresh flow (authorized device-flow only)
- `InnerTubeAuthManager.getAccessToken()`: return token when present and not
  expired/near-expiry; else single-flight refresh via `refreshMutex`; re-read store
  inside the lock so a second waiter reuses the winner's token (no stampede).
- Bounded: `withTimeout(REFRESH_TIMEOUT_MS = 25s)`; cancellation propagates;
  no infinite retry; exactly one network refresh per trigger.
- Outcome mapping (`TokenRefreshOutcome`): `Success`, `NoRefreshToken`,
  `TransientFailure` (IO/timeout/5xx — old token kept, recoverable),
  `InvalidGrant` (HTTP 400 `invalid_grant` — credentials cleared, re-auth required).
  `refreshAccessToken(): String?` kept for backward compat (returns token or null).
- Sign-out/account-switch clears tokens + expiry + `authCacheManager.purgeAllCaches()`.

## 5. 401 recovery (already present — keep, unify)
- `InnerTubeRemoteClient` 401→`authManager.refreshAccessToken()`→retry-once→throw
  `InnerTubeAuthException` on second 401: unchanged semantics.
- `AuthenticatedYouTubeClient.handle401AndRetry`: route through
  `InnerTubeAuthManager` (single-flight + expiry persist + cache purge) instead of
  direct `YouTubeDeviceAuthManager` call. One retry per request; second 401 →
  empty + surfaced re-auth flag (no silent guest).

## 6. Personalization UX (honest labels)
- New `AuthenticatedContentState`/`GuestFallbackReason`/`PersonalizationStatus` in
  `domain/auth/` (no existing equivalent; `HomeFeedState.hasAuth: Boolean` is not a state machine).
- `HomeFeedState` += `personalization`, `guestReason`, `reauthRequired` (all defaulted).
- `HomeFeedRepository`: catches `InnerTubeAuthException` per auth call; sets
  `reauthRequired=true`, `guestReason=UNAUTHORIZED`, `personalization=UNAVAILABLE`
  when authed calls were rejected; `GUEST/SIGNED_OUT` when signed out; `PERSONALIZED`
  only when an authed path returned items. Never labels guest rows "personalized".

## 7. Token storage review
- Tokens live in plaintext Preferences DataStore (`yt_access_token`,
  `yt_refresh_token`, + new `yt_token_expires_at`). `TokenStorage`
  (EncryptedSharedPreferences) exists but is unwired — switching providers mid-patch
  risks token loss, so migration is deferred as a separately scoped plan
  (`docs/token_storage_migration_plan.md`). Not claimed as encrypted.

## 8. Test plan
- `TokenExpiryTest`: expiry math, missing/invalid/negative/clamped expiry, skew margin,
  legacy-null path. `InnerTubeAuthManagerRefreshTest`: valid avoids refresh, near-expiry
  refreshes, concurrent refresh = 1 network call, transient vs invalid-grant mapping,
  unsupported (no refresh token). `HomeFeedLabelingTest`: signed-out/personalized/
  reauth mapping. Redaction test: new `NetworkEvent` fields contain no secrets.
- Commands: `compileDebugKotlin`, `testDebugUnitTest`, `lint`, `assembleDebug` (offline OK).

## 9. Rollback
- Revert commits touching `SettingsDataStore`, `InnerTubeAuthManager`,
  `AuthenticatedYouTubeClient`, `HomeFeed*` to `baf23a43`. New expiry key is
  nullable/ignored by old code, so rollback is data-safe.
