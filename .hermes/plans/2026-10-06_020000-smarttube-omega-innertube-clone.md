# Plan: SmartTube-Omega (TVHTML5 InnerTube Reverse-Engineered Algorithmic Clone)

## Goal
Implement the SmartTube-Omega architecture by strictly spoofing the TVHTML5 InnerTube client (`youtubei/v1`) for 1:1 algorithmic YouTube recommendations (`/browse` with `FEwhat_to_watch`/`FEmusic_home`), seamless `/next` autoplay extraction (`autoplayEndpointRenderer`/`maybeHistoryEndpointRenderer`), and zero-latency ExoPlayer queue injection.

## Context & Key Insights (SmartTube Architecture Secrets)
1. **TVHTML5 Client Spoofing**:
   - Official YouTube TV endpoints accept OAuth Bearer tokens and return full personalized mixes, tailored watch-history recommendations, and high-quality stream metadata without strict web scraper bot blocks.
   - Payload context:
     ```json
     {
       "context": {
         "client": {
           "clientName": "TVHTML5",
           "clientVersion": "7.20210614.03.00",
           "userAgent": "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)"
         }
       }
     }
     ```
2. **Personalization Engine (`/browse`)**:
   - Endpoints: `https://www.youtube.com/youtubei/v1/browse` with `browseId`: `"FEwhat_to_watch"` (Main YouTube personalized home) & `"FEmusic_home"` (YouTube Music personalized home).
   - Injected with OAuth Bearer token header to load user's real subscribed/liked algorithm.
3. **Algorithmic Autoplay Engine (`/next`)**:
   - Endpoint: `https://www.youtube.com/youtubei/v1/next` with `videoId`.
   - Priority 1: `autoplayEndpointRenderer` / `nextVideoRenderer` for continuous smart mixes and radios.
   - Priority 2: `maybeHistoryEndpointRenderer` / `twoColumnWatchNextResults` for algorithmic single-track continuation.
4. **ExoPlayer Gapless Queue Injection**:
   - On playback start (`STATE_READY`), prefetch the next algorithmic track in background coroutine, resolve audio/video streams, and enqueue via `player.addMediaItem()` before the current track finishes.

---

## Step-by-Step Implementation Tasks

### Task 1: Create `SmartTubeInnerTubeClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/SmartTubeInnerTubeClient.kt`
- Implement TVHTML5 spoofing payload generator.
- Implement `/browse` (`FEwhat_to_watch`, `FEmusic_home`) with bearer token authentication.
- Implement `/next` extractor supporting `autoplayEndpointRenderer`, `maybeHistoryEndpointRenderer`, `playlistPanelVideoRenderer`, and `compactVideoRenderer`.

### Task 2: Implement `SmartTubeAutoPlayQueueBridge.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/player/controller/SmartTubeAutoPlayQueueBridge.kt`
- Attach ExoPlayer `Player.Listener`.
- When playback starts on a single track and queue is at the last item, query `SmartTubeInnerTubeClient.fetchNextAutoplay(currentVideoId)`.
- Preload stream and silently append to ExoPlayer queue with `addMediaItem()`.

### Task 3: Wire SmartTube Personalization into `PersonalizationRepositoryImpl.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/repository/PersonalizationRepositoryImpl.kt`
- Prioritize `SmartTubeInnerTubeClient` for loading personalized home shelves (`FEwhat_to_watch` & `FEmusic_home`).

### Task 4: Unit Testing & Verification
- Create `SmartTubeInnerTubeClientTest.kt` in `app/src/test/java/com/deepeye/musicpro/data/source/remote/youtube/SmartTubeInnerTubeClientTest.kt`.
- Test TVHTML5 JSON payload structure, `/browse` shelf parsing, and `/next` endpoint extraction.
- Run `./gradlew --no-daemon testDebugUnitTest`.

### Task 5: Build Debug APK & On-Device Validation (Realme RMX3945)
- Assemble debug APK: `./gradlew --no-daemon assembleDebug`.
- Sideload onto Realme device.
- Verify Home feed recommendations and background AutoPlay next-track queuing.
- Capture screen & logcat forensics.

### Task 6: Bump Version & Release `v3.0.1.67`
- Bump `versionCode = 30077`, `versionName = "3.0.1.67"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag `v3.0.1.67`, and push to GitHub.
