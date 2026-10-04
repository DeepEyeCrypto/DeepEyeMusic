# Strict-Auth Isolation Protocol & Data Pipeline Audit Plan

## Goal
Enforce 100% strict authenticated data isolation in DeepEyeMusicPro, ensuring that when a YouTube account is connected, the app exclusively serves personalized recommendations (`FEmusic_home`, `FEmusic_liked`, `FEhistory`, `FEsubscriptions`), completely eradicating anonymous fallback bleed, stripping `visitorData` tracking tokens, and purging stale local caches on auth state change.

---

## Current Context & Root Causes

1. **Anonymous Fallback Bleed in ViewModels & Clients**:
   - In `YouTubeViewModel.kt`, when loading categories (`Home`, `Subscriptions`, `History`, `Liked`, `Music`), any empty response or network exception falls through to generic queries: `"trending music"`, `"trending videos"`, `"top hits songs"`.
   - In `AuthenticatedYouTubeClient.kt`, `getMusicFeed()`, `getLikedMusic()`, and `getMusicHistory()` fall back to `search("trending songs official audio video")` and `search("latest bollywood songs hindi hits")` when empty.
2. **Rogue Search Calls in Feed Repository**:
   - In `HomeFeedRepository.kt`, `authTrending` was fetching `authClient.getTrending()` (which executed `search("trending videos")`) rather than querying native InnerTube personalized home shelves.
3. **The VisitorData & Tracking Header Trap**:
   - InnerTube requests with guest cookies or `X-Goog-Visitor-Id` / `visitorData` cause Google's backend to prioritize anonymous guest session context over the Bearer token.
4. **Stale Ghost Cache Across Auth States**:
   - `YouTubeRepository` maintains an in-memory `railCache` (3-minute TTL) and `searchCache` (5-minute TTL) which retain guest data after the user logs in.
   - `RecommendationDao` and `SettingsDataStore` require explicit cache invalidation when authentication tokens are issued or cleared.

---

## Architecture & Proposed Approach

1. **Strict Auth Protocol Enforcement**:
   - When `hasAuth == true`, all feed methods must throw or return strictly isolated user data. Silent fallbacks to global search queries (`trending`, `top hits`) are disabled in authenticated paths.
2. **VisitorData Stripping**:
   - In `InnerTubeRemoteClient.kt`, ensure no `X-Goog-Visitor-Id` header is attached, and remove any `visitorData` property from the JSON context payload on authenticated requests.
3. **Dedicated Authenticated Endpoints**:
   - Home Feed: `browseMusic("FEmusic_home")`
   - Liked Songs: `browseMusic("FEmusic_liked")`
   - History: `browseHistory()` (`FEhistory` / `FEmusic_history`)
   - Subscriptions: `browseSubscriptions()` (`FEsubscriptions`)
4. **Auth-State Cache Invalidation Engine**:
   - Create `AuthCacheManager.kt` that observes `youtubeAccessToken` changes and invalidates `YouTubeRepository.railCache`, `YouTubeRepository.searchCache`, and in-memory recommendation caches.

---

## Step-by-Step Implementation Tasks

### Task 1: Enforce Strict Auth & Strip VisitorData in `InnerTubeRemoteClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`
- **Actions**:
  1. Audit request builder for `browseMusic`, `browseMain`, and `fetchNextAutoplay`.
  2. Ensure no `X-Goog-Visitor-Id` or `visitorData` fields exist in headers or context when `token != null`.
  3. If token exists and request returns 401/403 even after refresh, throw a structured `InnerTubeAuthException` instead of falling back to empty/guest results.
- **Verification**:
  - Run `./gradlew compileDebugKotlin -x test`

### Task 2: Remove Rogue Fallback Searches in `AuthenticatedYouTubeClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- **Actions**:
  1. In `getMusicFeed()`, if `token != null`, fetch strictly from `innerTubeClient.browseMusic("FEmusic_home")` and `innerTubeClient.browseLikedMusic()`. If both fail or are empty, return `emptyList()` or user liked tracks — DO NOT execute `search("latest bollywood songs")` or `search("trending songs")`.
  2. In `getLikedMusic()`, return only `innerTubeClient.browseLikedMusic()` or `innerTubeClient.browseLikedVideos()`. Remove `search("top hit songs official audio")` fallback.
  3. In `getMusicHistory()`, return only `innerTubeClient.browseHistory()`. Remove `search("latest hindi songs audio")` fallback.
- **Verification**:
  - Code audit: verify zero hardcoded search queries inside `getMusicFeed()`, `getLikedMusic()`, `getMusicHistory()`.

### Task 3: Isolate `HomeFeedRepository.kt` Data Pipeline
- **File**: `app/src/main/java/com/deepeye/musicpro/data/repository/HomeFeedRepository.kt`
- **Actions**:
  1. When `hasAuth == true`:
     - Replace `authTrendingDeferred = async { authClient.getTrending() }` with `authHomeDeferred = async { innerTubeClient.browseMusic("FEmusic_home") }`.
     - Derive all music rails (`music`, `discoverMix`, `supermix`, `becauseYouLikedMix`, `newReleases`) strictly from `authHome` (`FEmusic_home`) and `authLiked` (`FEmusic_liked`).
     - Do not blend non-authenticated `searchVideos` or `searchMusic` into the authenticated state.
- **Verification**:
  - Code audit: check that no `youtubeDs.searchMusic()` runs when `hasAuth == true`.

### Task 4: Patch `YouTubeViewModel.kt` Fallback Swallowing
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeViewModel.kt`
- **Actions**:
  1. In `loadCategory(category: String)`:
     - When `hasAuth == true`: if `authItems` is empty or an error occurs, display an authenticated error state or empty state (`_uiState.update { it.copy(videos = emptyList(), isLoading = false, error = "Unable to load personalized feed") }`).
     - Prevent fallthrough to `baseQuery = "trending music"` / `fetchVideos(finalQuery)` for logged-in accounts.
- **Verification**:
  - Run `./gradlew compileDebugKotlin -x test`

### Task 5: Implement `AuthCacheManager` & Auto-Purge on Auth State Change
- **File 1**: `app/src/main/java/com/deepeye/musicpro/domain/auth/AuthCacheManager.kt`
- **File 2**: `app/src/main/java/com/deepeye/musicpro/domain/auth/InnerTubeAuthManager.kt`
- **Actions**:
  1. Create `@Singleton class AuthCacheManager` injecting `YouTubeRepository`, `SettingsDataStore`.
  2. Implement `purgeAllCaches()`:
     - Clears `YouTubeRepository.clearCache()`.
     - Emits cache invalidation signal.
  3. In `InnerTubeAuthManager.kt`, call `authCacheManager.purgeAllCaches()` upon receiving new tokens from `pollForToken` or `refreshToken`, as well as on `logout()`.
- **Verification**:
  - Verify compile passes and cache is cleared on token update.

### Task 6: Compilation & Device Telemetry Audit (Mantis Reflect)
- **Actions**:
  1. Run `./gradlew --no-daemon assembleDebug`.
  2. Install on Realme device (`adb install -r app-debug.apk`).
  3. Launch app and filter logcat for `[InnerTube-Personalized]`.
  4. Verify that the first 3 track titles returned from `FEmusic_home` match the authenticated user's YouTube account history / mixes and not generic "Global Top 100".

---

## Risks, Tradeoffs & Edge Cases

| Risk / Edge Case | Mitigation |
| :--- | :--- |
| **New YouTube Account with 0 Likes/History** | `FEmusic_home` returns onboarding quick picks from YouTube Music directly; the app renders these without injecting local artificial search tracks. |
| **Token Expiration during Browse** | `InnerTubeRemoteClient` catches 401 and calls `authManager.refreshAccessToken()` once before throwing `InnerTubeAuthException`. |
| **Network Disconnection / Offline** | UI receives `isOffline = true` error state instead of deceiving the user with stale anonymous trending songs. |

---

## Verification Commands
```bash
# 1. Fast compile verification
./gradlew compileDebugKotlin -x test

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Sideload to Realme device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Check InnerTube Auth Telemetry & Shelves in Logcat
adb logcat -d | grep -E "InnerTubeAuth|InnerTube-Personalized"
```
