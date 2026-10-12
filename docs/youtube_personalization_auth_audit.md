# YouTube Personalization — Authentication Audit (Current-Tree Baseline)

- **Scope:** current working tree only. **Historical commits used: NONE.**
- **Commit:** `baf23a435b737abb16b055e16082ea5ed1415812`
- **Branch:** `main`
- **Version:** `chore(release): bump version to v3.0.1.94`
- **Audit mode:** AUDIT / DIAGNOSE (read-only baseline; authorized redaction/telemetry fixes applied separately).

## 1. Current authentication mechanism

| Aspect | Finding |
|---|---|
| Flow | **Google OAuth 2.0 Device Authorization Grant (RFC 8628)** — "Limited Input Device" flow |
| Sign-in entry | `ui/screens/YouTubeLoginScreen.kt` → `domain/auth/InnerTubeAuthManager.kt` |
| Token provider | `domain/auth/YouTubeDeviceAuthManager.kt` (calls `oauth2.googleapis.com`) |
| Credential type | OAuth **Bearer** access token + refresh token (no cookies, no SAPISIDHASH) |
| Scope | `https://www.googleapis.com/auth/youtube` (single, declared at device-code request) |
| Request signing | `Authorization: Bearer <accessToken>` header on InnerTube `youtubei/v1/*` calls |

**Authorized sign-in entry point:** `YouTubeLoginScreen` triggers
`InnerTubeAuthManager.requestDeviceCode()` → user visits `youtube.com/activate` →
`InnerTubeAuthManager.pollForToken()` receives `TokenResponse`.

## 2. Token lifecycle

| Stage | Class / method | Notes |
|---|---|---|
| Device code | `YouTubeDeviceAuthManager.requestDeviceCode()` | RFC 8628 |
| Poll | `YouTubeDeviceAuthManager.pollForToken()` | until authorized/expired |
| Store | `SettingsDataStore.setYouTubeTokens(access, refresh)` | plaintext DataStore |
| Read | `InnerTubeAuthManager.getAccessToken()` | proactive refresh only if access token **blank** |
| Refresh | `YouTubeDeviceAuthManager.refreshToken()` → `oauth2.googleapis.com/token` | reactive + on blank access token |
| Revoke/logout | `InnerTubeAuthManager.logout()` | clears tokens + `purgeAllCaches()` |

**Confirmed gap (root-cause candidate):** `expires_in` is parsed into
`TokenResponse.expiresIn` (YouTubeDeviceAuthManager.kt:26-30) but is **never
persisted**. `SettingsDataStore` has **no `expiresAt` field** (only
`youtubeAccessToken`, `youtubeRefreshToken`). Consequently there is **no expiry
check** and **no proactive refresh-on-expiry**; the app only discovers expiry
reactively via an HTTP 401.

## 3. Request builders / client library

- **Client:** hand-rolled OkHttp + `org.json` InnerTube client (**no** official
  YouTube/InnerTube SDK). Endpoints are undocumented private `youtubei/v1/*`.
- `data/source/remote/youtube/AuthenticatedYouTubeClient.kt` — `browseInternal`,
  `getHomeFeed`, `getMusicFeed`, `getHistory`, `getLikedVideos`, `performAction`
  (like/subscribe), `getVideoInteractionStatus`.
- `data/source/remote/youtube/InnerTubeRemoteClient.kt` — `fetchNextAutoplay`
  (`/next`), `browseMain`, autoplay "auth-rescue".
- `data/source/remote/youtube/SmartTubeEngine.kt` — `getAlgorithmicNext`.
- Repositories: `data/repository/HomeFeedRepository.kt`,
  `data/repository/PersonalizationRepositoryImpl.kt`,
  `domain/autoplay/AutoplayRepository.kt`.

## 4. Client context (identity)

Hardcoded InnerTube client identities are **client-spoofed** (Phase 5):

| clientName | clientVersion | Location |
|---|---|---|
| `TVHTML5` | `7.20210614.03.00` (stale, 2021) | AuthenticatedYouTubeClient (auth context) |
| `WEB` | `2.20240101.00.00` | AuthenticatedYouTubeClient (public) |
| `ANDROID_MUSIC` | `6.42.52` | InnerTubeRemoteClient (`/next`, music) |

Header `X-YouTube-Client-Name: 85` accompanies TVHTML5 requests. **No** supported
library defines these versions; they are guessed/stale. See
`docs/youtube_personalization_auth_diagnostics.md` §6 for compatibility status.

## 5. Authenticated vs guest state flow

- `HomeFeedRepository.getHomeFeed()` decides state by **token presence**:
  `hasAuth = authSettings?.youtubeAccessToken != null` (line 53) — **not** by token
  validity or expiry.
- If `hasAuth`, every `authClient.*` call is wrapped in
  `try { … } catch (e: Exception) { emptyList() }` (lines 63-73).
- When auth calls throw (e.g. expired token → 401/403), the result is silently
  `emptyList()` and the repository renders **guest/generic** content with **no
  distinct state** — the reported symptom.
- `InnerTubeRemoteClient.fetchNextAutoplay` and `AuthenticatedYouTubeClient`
  each contain a **reactive 401 refresh-and-retry** (`handle401AndRetry` /
  "Auth-Rescue"), but there is no expiry-aware proactive refresh.

## 6. Logging configuration (pre-fix)

Found and fixed during this audit (see diagnostics report §5):
- `Log.d("AuthYTClient", "hasAuth: $hasAuth, tokenPresent: …")` — boolean only
  (acceptable), but ran in release.
- `Log.i("DBG_B5FA56", payload)` debug-session telemetry — ran **unconditionally**
  (outside `BuildConfig.DEBUG`); now gated.
- Raw search-query text and personalized shelf titles logged; body previews
  emitted. All now diagnostic-gated or removed.
- No `HttpLoggingInterceptor.Level.BODY` is used anywhere (verified).

## 7. Privacy risks

| Risk | Severity | Status |
|---|---|---|
| Tokens stored in **plaintext** `preferencesDataStore` (not encrypted) | High | Documented; not changed (out of authorized fix scope) |
| `expires_in` not persisted → no expiry enforcement | Medium | Documented |
| Guest data silently presented as personalized (no state label) | High (trust) | Addressed via telemetry fallback-reason enum; UI state enum documented for follow-up |
| Release verbose network logging | High | Fixed (gated behind `NetworkDiagnostics.isEnabled`, default OFF) |

## 8. Findings & confidence

- **Confidence: HIGH** that the personalization fallback stems from the
  **missing token-expiry handling** combined with **token-presence-only** auth
  gating and **swallowed exceptions** → silent guest fallback.
- **Confidence: HIGH** that the auth flow itself is Google's documented RFC 8628
  device flow (authorized), but the InnerTube *content* endpoints are
  **undocumented private interfaces** and therefore unstable/unsupported for
  guaranteed personalization.

## 9. Files inspected

- `domain/auth/InnerTubeAuthManager.kt`, `domain/auth/YouTubeDeviceAuthManager.kt`
- `data/source/remote/youtube/AuthenticatedYouTubeClient.kt`,
  `InnerTubeRemoteClient.kt`, `SmartTubeEngine.kt`, `NetworkTelemetry.kt`
- `data/prefs/SettingsDataStore.kt`
- `data/repository/HomeFeedRepository.kt`,
  `data/repository/PersonalizationRepositoryImpl.kt`,
  `domain/autoplay/AutoplayRepository.kt`
- `di/NetworkModule.kt`, `ui/screens/YouTubeLoginScreen.kt`

