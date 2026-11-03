# Plan: YouTube Account Data Persistence After OAuth Login

## Goal
After a user completes the YouTube OAuth device-flow login, persist the user's display name, avatar URL, and channel key to SettingsDataStore so that (a) the ProfileScreen shows the real user name instead of "DeepEye User" and (b) the music feed (`PersonalizationRepositoryImpl`) can render personalized content (liked music, subscriptions, account sections) instead of empty/anonymous states.

## Current Context / Assumptions
- The app uses a standard Google OAuth 2.0 Device Authorization Flow (RFC 8628) via `InnerTubeAuthManager` / `YouTubeDeviceAuthManager`.
- On token receipt, `InnerTubeAuthManager.refreshAccessTokenOutcome()` calls `settingsDataStore.setYouTubeTokens(accessToken, refreshToken, expiresAt)` — but **never** writes `youtubeUserName`, `youtubeUserAvatar`, or `youtubeUserEmail`.
- `SettingsDataStore` has fields: `youtubeAccessToken`, `youtubeRefreshToken`, `youtubeTokenExpiresAtEpochSec`, `youtubeUserName` (default null), `youtubeUserAvatar` (default null), `youtubeUserEmail` (default null).
- `AccountSessionManager.observeState()` emits `AccountSession.Connected(displayName, avatarUrl, channelKey, accountKey)` — currently `displayName` and `avatarUrl` are null because the data was never stored.
- The InnerTube API (`/youtubei/v1/browse`) returns user profile data in the response when authenticated with a Bearer token.
- The codebase already has `InnerTubeAuthManager` that handles the OAuth flow; we only need to add profile-persistence after the token is acquired.

## Architecture / Proposed Approach
1. **Modify `InnerTubeAuthManager`**: After `refreshAccessTokenOutcome()` successfully refreshes the token, additionally fetch the authenticated user's profile from YouTube's `/youtubei/v1/browse` endpoint with a minimal query (just `user` fields). Then persist `youtubeUserName`, `youtubeUserAvatar`, `youtubeUserEmail` to `SettingsDataStore` via a new method `setYouTubeUserUserInfo(name, avatarUrl, channelKey)`.
2. **Or (simpler, no new network call)**: After `setYouTubeTokens()` succeeds, also extract the `displayName` and `avatarUrl` from the `TokenResponse` object (Google's token endpoint sometimes returns `id_token` claims, or we can use the already-authenticated `InnerTubeRemoteClient` to fetch `/youtubei/v1/user`) and persist them. If the token response doesn't contain profile data, add a lightweight fetch step.
3. Wire the persisted values: `AccountSessionManager.observeState()` already reads from `SettingsDataStore` via `SettingsAccountDataSourceImpl` — ensure it maps `youtubeUserName` → `displayName`, `youtubeUserAvatar` → `avatarUrl`, `youtubeUserEmail` → `channelKey` so the ProfileScreen and feed sections receive real data.

## Step-by-Step Tasks

### Task 1 — Add `setYouTubeUserInfo` to `SettingsDataStore`
**File:** `data/prefs/SettingsDataStore.kt`
**What to do:** Add three new optional fields and a method to write them atomically.
**Copy-paste addition** (after line 95 where `youtubeUserAvatar` is set):
```kotlin
suspend fun setYouTubeUserInfo(name: String?, avatarUrl: String?, channelKey: String?) {
    // Atomically write all three so they always appear together
    edit {
        this[youtubeUserNameKey] = name
        this[youtubeUserAvatarKey] = avatarUrl
        this[youtubeUserEmailKey] = channelKey
    }
}
```
**Verification command:**
```bash
cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew :app:compileDebugKotlin -x test --console=plain 2>&1 | grep -E "e:|SUCCESS"
```
**Expected output:** `SUCCESS`.

### Task 2 — Persist user info after OAuth login in `InnerTubeAuthManager`
**File:** `domain/auth/InnerTubeAuthManager.kt`
**What to do:** In `refreshAccessTokenOutcome()` success branch (line 192), after `settingsDataStore.setYouTubeTokens(...)`, add a call to persist user info. If the `TokenResponse` has `extra` claims with `name`, `picture`, `email`, use those; otherwise, make a lightweight InnerTube `/youtubei/v1/user` request (already have the authenticated client) and extract the profile, then call `settingsDataStore.setYouTubeUserInfo(name, avatarUrl, channelKey)`.
**Code addition** (after line 191 `authCacheManager.purgeAllCaches()`):
```kotlin
// Persist user profile so ProfileScreen and feed show real names
val userName = /* extract from tokenResponse or fetch via InnerTube */ null
val avatarUrl = /* extract from tokenResponse or fetch via InnerTube */ null
val channelKey = /* extract from tokenResponse or fetch via InnerTube */ null
settingsDataStore.setYouTubeUserInfo(userName, avatarUrl, channelKey)
```
**Verification command:**
```bash
cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew :app:compileDebugKotlin -x test --console=plain 2>&1 | grep -E "e:|SUCCESS"
```
**Expected output:** `SUCCESS`.

### Task 3 — Wire user info into `AccountSessionManager` / `AccountDataSource`
**File:** `account/SettingsAccountDataSourceImpl.kt` and `account/AccountSessionManager.kt`
**What to do:** Ensure `connectedStream` maps `youtubeUserName` → `displayName`, `youtubeUserAvatar` → `avatarUrl`, `youtubeUserEmail` → `channelKey`. The mapping already exists in `SettingsAccountDataSourceImpl.lines 27-40` — verify it reads the three new fields.
**Verification command:**
```bash
cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew :app:compileDebugKotlin -x test --console=plain 2>&1 | grep -E "e:|SUCCESS"
```
**Expected output:** `SUCCESS`.

### Task 4 — Device validation
**What to do:** Install debug APK, complete YouTube OAuth login flow, verify:
- ProfileScreen shows the real user name (not "DeepEye User")
- Avatar circle shows the real avatar (not the default silhouette)
- Music feed shows personalized sections (Liked, Subscriptions, etc.) instead of "Connect YouTube Account"
**Verification command:** Manual on device; no automated unit test.

## Tests / Validation
After each task, run the compile task and verify **SUCCESS** with zero new `e:` lines. Then:

**Unit test Task 2:** Mock `TokenResponse` with `extra` containing `displayName`, `picture`, `email`. Verify `settingsDataStore.setYouTubeUserInfo` is called with the correct values.

**Unit test Task 3:** Verify `AccountSession.Connected` has non-null `displayName` and `avatarUrl` after the data-store change.

**Manual test:** Complete OAuth login on device; ProfileScreen and Home feed should show real user data.

## Risks, Tradeoffs, and Open Questions
- **Risk:** Adding a network call (`/youtubei/v1/user` fetch) after every token refresh adds latency and could fail. Mitigation: make it optional — only fetch if the token response doesn't already contain profile claims; cache the profile for 24h to amortize the cost.
- **Tradeoff:** Storing `youtubeUserEmail` as `channelKey` is a schema choice — email is not a stable channel key, but it was the existing field name. Better to derive a stable `accountKey` from the refresh token hash (already done in `deriveStableAccountKey`) and store display name/avatar separately.
- **Open question:** Should the profile fetch be part of the initial OAuth flow (one-time on first login) or on every token refresh? First-login only is simpler; every refresh is more keeping-stale-data fresh but adds overhead.
- **No new dependencies** — all changes use existing `OkHttpClient`, `SettingsDataStore`, and `InnerTubeRemoteClient` which are already injected.

## Commit Format (per task)
```bash
git add -A && git commit -q -m "feat: persist YouTube user profile after OAuth login — <one-line description>"
# then:
git push origin HEAD
```
