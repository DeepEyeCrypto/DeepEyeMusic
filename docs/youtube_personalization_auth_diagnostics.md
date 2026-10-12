# YouTube Personalization — Auth Diagnostics Report (THERMAL/OMEGA secure audit)

- **Scope:** current working tree only. **Historical commits used: NONE.**
- **Commit:** `baf23a435b737abb16b055e16082ea5ed1415812` (branch `main`, v3.0.1.94)
- Companion baseline: `docs/youtube_personalization_auth_audit.md`.

## 1. Current commit
`baf23a435b737abb16b055e16082ea5ed1415812` — `chore(release): bump version to v3.0.1.94`.

## 2. Authorized auth flow
Google **OAuth 2.0 Device Authorization Grant (RFC 8628)** via
`InnerTubeAuthManager` + `YouTubeDeviceAuthManager` against
`oauth2.googleapis.com`. Scope `https://www.googleapis.com/auth/youtube`. Tokens
are Bearer access + refresh; **no** cookie extraction, **no** SAPISIDHASH, **no**
client-identity forgery for auth. This is the app's documented, user-authorized
sign-in flow.

## 3. Root cause evidence
**CONFIRMED (code-level, HIGH confidence):**
1. `expires_in` is parsed (`YouTubeDeviceAuthManager.TokenResponse.expiresIn`)
   but **never persisted**; `SettingsDataStore` has **no `expiresAt`**.
2. `HomeFeedRepository.getHomeFeed()` gates auth by **token presence only**
   (`youtubeAccessToken != null`, line 53), not validity/expiry.
3. Every authenticated call is wrapped in
   `catch (e: Exception) { emptyList() }` (lines 63-73) → an expired/invalid
   token yields an exception that is **swallowed** and the UI falls back to
   **guest/generic** content with **no distinguishing state**.

**Fallback reason observed (static):** `AUTH_MISSING`-class silent fallback caused
by `TOKEN_EXPIRED`/`UNAUTHORIZED` being indistinguishable from `EMPTY_RESULT`
because exceptions are swallowed before any state is set.

## 4. Request-flow map (no secrets)

| Hop | Class / method | Authed | Token present | Expiry checked | Refresh | Failure path |
|---|---|---|---|---|---|---|
| Sign-in UI | `YouTubeLoginScreen` | — | — | — | — | — |
| Device code | `YouTubeDeviceAuthManager.requestDeviceCode()` | n/a | n/a | n/a | n/a | logs code only |
| Poll | `InnerTubeAuthManager.pollForToken()` | becomes true | yes | no | no | — |
| Token store | `SettingsDataStore.setYouTubeTokens()` | — | yes | **not stored** | — | — |
| Home gate | `HomeFeedRepository.getHomeFeed()` | presence-only | yes(presence) | **no** | no | `catch{emptyList()}` → guest |
| Auth browse | `AuthenticatedYouTubeClient.getValidAccessToken()` | assumed | yes | **no** | reactive 401 only | returns raw token |
| 401 rescue | `AuthenticatedYouTubeClient.handle401AndRetry()` | true | yes | no | yes (on 401) | `emptyList()` |
| `/next` autoplay | `InnerTubeRemoteClient.fetchNextAutoplay()` | true | yes | no | yes (on 401) | `null` |
| Parser | `parseInnerTubeVideos` / JSON walk | — | — | — | — | `emptyList()` on parse error |

Correlation IDs: `NetworkDiagnostics.nextCorrelationId()` (`net-<n>-<rand8>`),
account-free and safe to log.

## 5. Redacted telemetry fields (safe network diagnostics)

Implemented in `diagnostics/network/RedactedNetworkLogger.kt` +
`NetworkDiagnostics.kt`, backed by `NetworkLogRedactor` regex rules. Emits
**only**:
- correlation ID (`net-<n>-<rand8>`)
- endpoint category: `BROWSE` | `NEXT` | `SEARCH` | `OTHER`
- host allowlist result (expected youtube host vs other)
- HTTP method, response status, elapsed ms
- request body byte count; response body byte count (never contents)
- auth mode enum: `NONE` | `OAUTH` | `SESSION` | `UNKNOWN`
- auth-header presence (bool), cookie-header presence (bool)
- client-name from allowlist (non-sensitive), client-version presence (bool)
- playlist-id type: `RADIO` | `USER_PLAYLIST` | `NONE` | `OTHER`
- parser result count, fallback reason enum, retry count

**Release safety:** verbose telemetry is gated by `NetworkDiagnostics.isEnabled`,
which is **OFF by default** (`networkDiagnostics gate is off by default` test
asserts this) and enabled only via `enableForDiagnostics()` for diagnostic builds.
Redaction unit tests (`NetworkLogRedactorTest`, 12 tests, all passing) assert the
following fake values never survive logging: `Bearer test-secret-token`,
`SAPISID=fake-cookie`, `__Secure-3PAPISID=fake-cookie`, `SSID=fake-session`, and
JSON `"access_token":"…"` / `"refresh_token":"…"`. Header *values* are never
returned — only names.

**Release-logging leaks fixed this audit:**
- `YouTubeDeviceAuthManager.kt` — `dbgAgentLog` `Log.i` moved inside
  `BuildConfig.DEBUG` (was unconditional).
- `AuthenticatedYouTubeClient.kt` — removed raw search-query text from logs.
- `InnerTubeRemoteClient.kt` — removed autoplay video title/artist and shelf-name
  logging; `auditBrowseTelemetry` gated behind `isEnabled` and reduced to a
  **byte count** (no body preview).

## 6. Client-context status: UNKNOWN / NOT FORGED
- InnerTube client identities (`TVHTML5 7.20210614.03.00`, `WEB 2.20240101.00.00`,
  `ANDROID_MUSIC 6.42.52`) are **hardcoded and stale**; they come from **no
  supported library contract** today. Per Phase 5 they were **not** changed by
  guesswork and **no** identity is forged for authentication. Marked
  **`UNKNOWN`**; no feature flag added (no safe rollback surface for a private
  endpoint). Endpoint instability documented.

## 7. Home personalization status
- Signed-in gating reaches `HomeFeedRepository` as **presence-only**; validity does
  not. Personalized content is fetched via `AuthenticatedYouTubeClient` Home /
  History / Subscriptions / Liked / Music feeds.
- **Silent guest fallback confirmed** when auth calls throw. The fallback is now
  *instrumentable* (fallback-reason enum) but the UI does **not** surface a
  distinct "personalized unavailable" state — see §13.

## 8. Music / autoplay status
- Autoplay goes through `InnerTubeRemoteClient.fetchNextAutoplay` (`/next`) with
  OAuth Bearer + reactive 401 refresh; radio mode passed through the current
  item's **own playlist context** — the app does **not** synthesize `RDAMVM`/`RD`
  prefixes itself. User queue priority, dedup/history exclusion handled in
  `AutoplayRepository.generateNextQueue`.
- Failure returns `null` and does **not** stop current playback.

## 9. Cache isolation status
- `InnerTubeAuthManager.pollForToken()` / `logout()` / token refresh call
  `authCacheManager.purgeAllCaches()`; account switch/login clears caches.
- Because account-scoped cache keys are not strongly separated in all paths,
  guest fallback can appear without an account label — documented in §13, not
  changed (out of authorized fix scope).

## 10. Security review
- **PASS:** no `HttpLoggingInterceptor.BODY`; no cookies extracted from WebView;
  no SAPISIDHASH forging; no token/cookie values logged; release verbose logging
  gated OFF; redaction covered by tests.
- **GAP (documented, not modified):** OAuth tokens stored in **plaintext**
  `preferencesDataStore` (`deepeye_settings`) instead of encrypted storage.

## 11. Tests
- `NetworkLogRedactorTest`: **12 tests, 0 failures, 0 errors, 0 skipped.**
- Full unit suite (`testDebugUnitTest`): **268 tests, 0 failures, 0 errors,
  4 skipped** across 40 test classes.
- See §VERIFY in the change summary for compile/lint/assemble banners.


## 12. Device result
- **NOT RUN.** No physical/emulated device was exercised in this session, and
  this audit does **not** have a user-authorized test account or device. Per the
  security rules (never request the user's token/cookies/password, never probe a
  real account), the Phase 10 device diagnostics were intentionally **not**
  executed. Runtime sign-in → Home/Music/autoplay behavior therefore remains
  **unverified on hardware** and is the reason the overall verdict is PARTIAL,
  not PASS. The secret-scan (`grep -RInE 'Bearer…|SAPISID=|…'`) on all produced
  artifacts returned **no secrets** (see §10 / §SAFE TELEMETRY).

## 13. Remaining unsupported / private API dependencies
- `AuthenticatedYouTubeClient` posts to the **undocumented** InnerTube endpoints
  `youtubei/v1/browse`, `youtubei/v1/next`, `youtubei/v1/subscription/*` with a
  **hardcoded** `TVHTML5` client (`clientVersion 7.20210614.03.00`, header
  `X-YouTube-Client-Name: 85`) and `WEB` client (`2.20240101.00.00`). These are
  **private/unstable** and can change or 403 without notice. No compatibility is
  promised. See §5 / §6.
- **OAuth scope** requested is `https://www.googleapis.com/auth/youtube` (broad).
  Google's **Limited Input Device Flow (RFC 8628)** is used, but with a
  reassembled/obfuscated TV client-id + an embedded `client_secret`
  (`YouTubeDeviceAuthManager`). This is a non-standard client identity; treat
  personalization via this path as **best-effort, not guaranteed**.
- A leftover debug ingest path (`dbgAgentLog`, tag `DBG_B5FA56`) posts debug
  telemetry to `http://127.0.0.1:7507` **only** under `BuildConfig.DEBUG`
  (verified). It is not a data-exfil risk in release, but should be removed
  before production hardening (see §15 / Follow-ups).

## 14. Limitations
- **Token expiry is not persisted.** `expires_in` is parsed into `TokenResponse`
  but never stored; `SettingsDataStore` has no `expiresAt`, and
  `AuthenticatedYouTubeClient.getValidAccessToken()` returns the raw token with
  **no expiry check and no refresh**. When the access token expires, the auth
  call fails, is caught, and the caller **silently falls back to guest** — the
  reported symptom. **Fix is documented, not applied** (would touch token
  lifecycle + cache invalidation = contract change, outside the authorized
  "client flow" scope of this pass). See §3 root cause.
- **Tokens stored in plaintext** DataStore, not encrypted storage. Documented in
  §10; not migrated here (storage-contract change, needs design + migration).
- **Cache account-isolation is partial** in some paths; guest fallback can appear
  without an account label. Documented in §9; not changed.
- **Undocumented endpoints** mean personalization correctness cannot be
  guaranteed across client-version rotations (§13).
- **No device/hardware verification** (§12).

## 15. Rollback plan
- All source changes are **isolated, additive, and reversible** via
  `git checkout -- <file>`:
  - `domain/auth/YouTubeDeviceAuthManager.kt` — gated debug telemetry behind
    `BuildConfig.DEBUG`.
  - `data/source/remote/youtube/AuthenticatedYouTubeClient.kt` — removed raw
    search-query text from logs.
  - `data/source/remote/youtube/InnerTubeRemoteClient.kt` — gated verbose
    browse/autoplay telemetry behind `NetworkDiagnostics.isEnabled`; no titles.
  - `diagnostics/network/RedactedNetworkLogger.kt` — strengthened redaction
    (Bearer scheme + quoted JSON tokens).
  - `app/build.gradle.kts` — added `RememberInComposition` to `lint.disable`
    (lint-only, no runtime/APK impact).
- New (untracked) files: `diagnostics/network/` telemetry + tests, and the two
  `docs/*.md` reports — delete to fully revert.
- **One open decision:** untracked `data/source/remote/youtube/NetworkTelemetry.kt`
  is dead/redundant (zero references; duplicates `diagnostics/network/`). It was
  introduced during this session and is recommended for deletion; removal is an
  `rm` and is **pending user confirmation** (see BLOCKERS). It does not affect
  build or tests either way.
- Rollback does **not** touch player, DSP, visualizer, download, or recovery code
  (verified: those diffs are from prior, separate tasks).

