# 🎵 PLAN: DEBUG YOUTUBE MUSIC & DIRECT AUTHENTICATED TVHTML5 SUBSCRIPTIONS MAPPING

**Document ID:** `.hermes/plans/2026-10-06_130000-debug-youtube-music-and-subscriptions-tvhtml5.md`  
**Status:** READY TO EXECUTE  

---

## 1. Goal
Debug the "Music" section in YouTube Cinema and enforce strict, direct Authenticated TVHTML5 mapping for **Subscriptions** (`authClient.getSubscriptionsFeed()`) and **Music** (`authClient.fetchSupermix()` / `authClient.getMusicFeed()`), aligning TVHTML5 client versions (`7.20210614.03.00`) and headers across all InnerTube remote layers.

---

## 2. Current Context & Root-Cause Audit
1. **Version Mismatch**: `InnerTubeRemoteClient.kt` used `TVHTML5_CLIENT_VERSION = "7.20230412.08.00"` while the global `InnerTubeTvInterceptor` and `NetworkModule.kt` enforce `7.20210614.03.00`. Aligning to the canonical `7.20210614.03.00` ensures zero HTTP 400 bad requests from InnerTube.
2. **Direct Subscriptions Mapping**:
   - The "Subscriptions" category must call `authClient.getSubscriptionsFeed()` (which targets `FEsubscriptions` on `https://www.youtube.com/youtubei/v1/browse`).
   - If authenticated, it returns the real-time uploads of channels and artists the user is subscribed to on YouTube.
3. **Music Category Engine Debug**:
   - The "Music" category in `YouTubeViewModel.kt` must fetch both YouTube Music's `FEmusic_home` personalized mixes (`fetchSupermix()`) and user's `browseLikedMusic()` vault.

---

## 3. Step-by-Step Implementation Tasks

### Task 1: Align TVHTML5 Fingerprint in `InnerTubeRemoteClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`
- **Changes**:
  1. Set `const val TVHTML5_CLIENT_VERSION = "7.20210614.03.00"`.
  2. Set `User-Agent: Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)`.

### Task 2: Direct Subscriptions & Music Mapping in `YouTubeViewModel.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeViewModel.kt`
- **Changes**:
  1. In `loadCategory(category: String)`:
     - `"Subscriptions"` ➡️ `if (hasAuth) authClient.getSubscriptionsFeed() else emptyList()`
     - `"Music"` ➡️ `if (hasAuth) authClient.getMusicFeed() else youtubeRemoteDataSource.getMusicFeed()`
  2. Ensure immediate reactive propagation to `_uiState` with zero race conditions.

### Task 3: Version Bump & Release Tracking
- **Files**: `app/build.gradle.kts`, `app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt`
- **Changes**: Bump to `3.0.1.83` (`versionCode = 30093`).

### Task 4: Compilation, Deployment & Hardware Logcat Verification
- **Actions**:
  1. Execute `./gradlew assembleDebug`.
  2. Install on Realme RMX3945 via ADB.
  3. Verify logcat for successful InnerTube TVHTML5 responses on Subscriptions and Music tabs.
  4. Git commit and tag `v3.0.1.83`.

---

## 4. Verification Commands
```bash
./gradlew compileDebugKotlin -x test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.deepeye.musicpro/com.deepeye.musicpro.MainActivity
```
