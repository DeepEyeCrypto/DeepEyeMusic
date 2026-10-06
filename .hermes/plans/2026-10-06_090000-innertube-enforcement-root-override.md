# 🛡️ SYSTEM DIRECTIVE PLAN: HERMES INNER-TUBE ENFORCEMENT PROTOCOL (ROOT-OVERRIDE)

**Document ID:** `2026-10-06_090000-innertube-enforcement-root-override`  
**Status:** READY FOR EXECUTION  
**Target:** DeepEyeMusicPro (`com.deepeye.musicpro`)  
**Architect:** Hermes-Apex (Autonomous Principal API Architect)

---

## 🎯 Goal
Enforce a 100% pure TVHTML5 `/youtubei/v1` proxy architecture across DeepEyeMusicPro by eliminating all legacy YouTube Data API v3 (`googleapis.com/youtube/v3`) endpoints, removing unauthenticated scraping, and strictly centralizing all network calls through the TVHTML5 InnerTube engine.

---

## 📌 Current Context & Assumptions
1. **Scattered Legacy Calls**:
   - `ContentFetcher.kt` contains fallback / primary calls to `https://www.googleapis.com/youtube/v3/search` and `https://www.googleapis.com/youtube/v3/videos`.
   - `YouTubeDeviceAuthManager.kt` contains fallback calls to `https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true`.
   - `OAuthConfig.kt` declares `YOUTUBE_DATA_API_BASE = "https://www.googleapis.com/youtube/v3/"`.
2. **Target Engine**:
   - `SmartTubeEngine.kt` and `AuthenticatedYouTubeClient.kt` serve as the single source of truth for all YouTube operations (`/search`, `/browse`, `/next`, `/player`, `/like`, `/dislike`, `/browse MPLY`).
3. **Hardware / Runtime Constraints**:
   - Target Device: Realme RMX3945 (MT6835, Android 16).
   - Strict TVHTML5 headers: `X-YouTube-Client-Name: 85`, `X-YouTube-Client-Version: 7.20210614.03.00`, and `User-Agent: Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)`.

---

## 🏗️ Architecture & Proposed Approach
- **Global TVHTML5 Interceptor**: Build an `InnerTubeTvInterceptor` inside `NetworkModule.kt` that automatically attaches required TVHTML5 spoofing headers and bearer authorization on every request targeted at `youtubei/v1`.
- **Eradication of v3 Data API**: Completely strip all `googleapis.com/youtube/v3` URL construction and API key dependencies from `ContentFetcher.kt`, `YouTubeDeviceAuthManager.kt`, and `OAuthConfig.kt`.
- **SmartTubeEngine Universal Delegation**: Re-route `ContentFetcher.searchByArtist`, `searchByQuery`, and `getRelatedVideos` directly to `SmartTubeEngine.searchTracks` and `SmartTubeEngine.getAlgorithmicNext`.

---

## 📋 Step-by-Step Implementation Tasks

### Task 1: Centralize TVHTML5 Network Interceptor in `NetworkModule.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/di/NetworkModule.kt`
- **Action**: Add an `InnerTubeTvInterceptor` to `provideOkHttpClient` ensuring all `/youtubei/v1` outgoing calls automatically carry:
  - `X-YouTube-Client-Name: 85`
  - `X-YouTube-Client-Version: 7.20210614.03.00`
  - `User-Agent: Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)`
  - `Content-Type: application/json`

### Task 2: Purge Legacy Data API from `ContentFetcher.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/domain/recommendation/ContentFetcher.kt`
- **Action**:
  - Remove all references to `https://www.googleapis.com/youtube/v3/search` and `https://www.googleapis.com/youtube/v3/videos`.
  - Inject `SmartTubeEngine` into `ContentFetcher`.
  - Re-route `searchByArtist`, `searchByQuery`, and `searchByQueryFallback` to invoke `smartTubeEngine.searchTracks(query)`.
  - Re-route `getRelatedVideos` to invoke `smartTubeEngine.getAlgorithmicNext(videoId)`.

### Task 3: Strip Deprecated Channels API from `YouTubeDeviceAuthManager.kt` & `OAuthConfig.kt`
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/domain/auth/YouTubeDeviceAuthManager.kt`
  - `app/src/main/java/com/deepeye/musicpro/data/OAuthConfig.kt`
- **Action**:
  - In `OAuthConfig.kt`: Remove `YOUTUBE_DATA_API_BASE`.
  - In `YouTubeDeviceAuthManager.kt`: Remove `https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true` fallback and rely strictly on standard OAuth2 `https://www.googleapis.com/oauth2/v3/userinfo` or `/youtubei/v1/account/account_menu`.

### Task 4: Audit & Unify All InnerTube Context Payloads
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/SmartTubeEngine.kt`
  - `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- **Action**:
  - Audit all `postInnerTube` invocations to verify that the standard TVHTML5 context block is universally injected:
    ```json
    {
      "context": {
        "client": {
          "clientName": "TVHTML5",
          "clientVersion": "7.20210614.03.00",
          "userAgent": "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)",
          "hl": "en",
          "gl": "IN"
        },
        "user": {
          "enableSafetyMode": false,
          "lockedSafetyMode": false
        }
      }
    }
    ```

### Task 5: Compilation & Hardware Verification
- **Action**:
  - Run `./gradlew assembleDebug` in terminal to guarantee 0 compile/linking errors.
  - Install APK on Realme RMX3945 via `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
  - Verify device logcat for zero HTTP 400/403 errors and flawless discovery/playback.

---

## 🧪 Validation & Testing
- **Grep Audit**:
  ```bash
  grep -rnI "googleapis.com/youtube/v3" app/src/main/java
  # Expected: 0 matches
  ```
- **Compilation Check**:
  ```bash
  ./gradlew assembleDebug
  # Expected: BUILD SUCCESSFUL (Exit Code 0)
  ```
- **Logcat Audit**:
  ```bash
  adb logcat -d -t 150 | grep -i "SmartTubeEngine\|InnerTube"
  # Expected: Clean HTTP 200 responses with zero HTTP 400/403 errors
  ```

---

## ⚠️ Risks & Mitigation
- **Risk**: Missing metadata when replacing v3 search with TVHTML5 search.
  - **Mitigation**: `SmartTubeEngine` multi-renderer recursive parsing already supports `videoRenderer`, `compactVideoRenderer`, `musicResponsiveListItemRenderer`, and `musicTwoRowItemRenderer` with fallback thumbnails.
