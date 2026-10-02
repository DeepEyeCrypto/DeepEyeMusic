# Plan: Complete YouTube Shorts Blocker Across Extraction, Feeds, Search, and UI

## Goal
Completely block and filter out YouTube Shorts from all app layers (extractors, authenticated InnerTube client, search results, home feed, recommendations, related streams, and UI rails) so no Shorts ever appear or play in DeepEyeMusicPro.

---

## Current Context & Root Cause Analysis
1. **Extractor Layer (`SmartTubeInnertubeExtractor`, `NewPipeExtractorBridge`):**
   - `SmartTubeInnertubeExtractor.parseSearchResponse` currently accepts `reelItemRenderer` (Shorts) and appends them to search results.
   - `getShorts()` explicitly queries `"trending shorts"`.
2. **Authenticated Client Layer (`AuthenticatedYouTubeClient`):**
   - `parseReelItemRenderer` parses Shorts reels into `HomeVideoItem(..., isShort = true)`.
   - Feed endpoints (`getHomeFeed()`, `getTrending()`, `search()`, etc.) return items without globally enforcing the removal of short-duration clips (< 60s) or shorts tags.
3. **Repository Layer (`HomeFeedRepository`, `YouTubeRepository`, `PersonalizationRepositoryImpl`):**
   - `HomeFeedRepository` maintains an active `shorts` list in `HomeFeedState` and queries `youtubeDs.getShorts()` / `searchVideos("... shorts")`.
4. **UI Layer (`HomeHubScreen`, `YouTubeScreen`, `YouTubeViewModel`):**
   - `HomeHubScreen` has a dedicated `ShortsRail` composable that renders a 2x2 grid of shorts.
   - `YouTubeScreen` contains an optional toggle (`hideShorts`) rather than a hard block.
5. **Filter Layer (`MusicFilter`):**
   - `MusicFilter.isMusicTrack` catches some shorts hashtags, but general video searches and feeds bypass `MusicFilter` if they aren't marked as music tracks.

---

## Architecture & Proposed Approach
- **Global Shorts Blocking Filter (`ShortsBlocker` / enhanced `MusicFilter`):**
  Create a single source of truth validator:
  ```kotlin
  fun isShort(title: String, durationSeconds: Long, isShortFlag: Boolean): Boolean {
      if (isShortFlag) return true
      if (durationSeconds in 1..59) return true
      val lower = title.lowercase()
      return lower.contains("#short") || lower.contains("#shorts") ||
             lower.contains("#ytshorts") || lower.contains("#shortsfeed") ||
             lower.contains("#shortvideo") || lower.contains("#youtubeshorts") ||
             lower.contains("(shorts)") || lower.contains("[shorts]") ||
             lower.contains("/shorts/")
  }
  ```
- **Extractor Level Blocker:**
  - Strip `reelItemRenderer` entirely from Innertube parsers.
  - Return `emptyList()` for `getShorts()`.
  - Filter all search results, related videos, and trending results through `!isShort(...)`.
- **Authenticated Client Blocker:**
  - `parseReelItemRenderer` returns `null`.
  - All public feed queries in `AuthenticatedYouTubeClient` filter out shorts.
- **Repository Level Cleanup:**
  - In `HomeFeedRepository`, remove the `shortsDeferred` job; `HomeFeedState.shorts` is permanently `emptyList()`.
- **UI Level Cleanup:**
  - Remove `ShortsRail` from `HomeHubScreen.kt`.
  - Ensure `YouTubeViewModel` and `VideoViewModel` strictly filter out any short-duration clips.

---

## Step-by-Step Implementation Tasks

### Task 1: Create Unit Tests for Shorts Blocking (`ShortsBlockerTest.kt`)
- **File:** `app/src/test/java/com/deepeye/musicpro/youtube/ShortsBlockerTest.kt`
- **Actions:**
  - Test duration threshold (< 60 seconds rejected).
  - Test hashtag patterns (`#shorts`, `#short`, `#ytshorts`, etc. rejected).
  - Test normal long-form songs & videos (3+ minutes accepted).
- **Command:** `./gradlew testDebugUnitTest --tests "com.deepeye.musicpro.youtube.ShortsBlockerTest"`

### Task 2: Enhance `MusicFilter.kt` with Robust `isShort` Validation
- **File:** `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/MusicFilter.kt`
- **Code:**
  ```kotlin
  fun isShort(title: String, durationSeconds: Long = 0, isShortFlag: Boolean = false): Boolean {
      if (isShortFlag) return true
      if (durationSeconds in 1..59) return true
      val lower = title.lowercase()
      return lower.contains("#short") || lower.contains("#shorts") ||
             lower.contains("#ytshorts") || lower.contains("#shortsfeed") ||
             lower.contains("#shortvideo") || lower.contains("#youtubeshorts") ||
             lower.contains("(shorts)") || lower.contains("[shorts]") ||
             lower.contains("/shorts/")
  }
  ```

### Task 3: Block Shorts in `SmartTubeInnertubeExtractor.kt`
- **File:** `app/src/main/java/com/deepeye/musicpro/extractor/SmartTubeInnertubeExtractor.kt`
- **Actions:**
  1. In `parseSearchResponse`: Drop `reelItemRenderer` parsing (`// Drop shorts reels`).
  2. In `searchVideosFirstPage` / `searchVideosNextPage`:
     Filter items: `.filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }`.
  3. In `getTrending()`:
     Filter items: `.filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }`.
  4. In `getShorts()`: Return `emptyList()`.
  5. In `getRelatedVideos()`:
     Filter items: `.filterNot { MusicFilter.isShort(it.title, it.duration, it.isShort) }`.

### Task 4: Block Shorts in `AuthenticatedYouTubeClient.kt`
- **File:** `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- **Actions:**
  1. `parseReelItemRenderer`: Return `null`.
  2. In `parseVideoRenderer`, `parseCompactVideoRenderer`, `parseGridVideoRenderer`, `parseTileRenderer`:
     Mark `isShort = MusicFilter.isShort(title, durationSeconds, false)`.
  3. In all feed methods (`getHomeFeed()`, `getTrending()`, `search()`, `getSubscriptionsFeed()`, `getLikedVideos()`, `getHistory()`, `getWatchLater()`, `getMusicFeed()`, `getMoviesFeed()`, `getGamingFeed()`, `getNewsFeed()`):
     Filter items: `.filterNot { it.isShort || it.duration in 1..59 || MusicFilter.isShort(it.title, it.duration, it.isShort) }`.

### Task 5: Clean Up `HomeFeedRepository.kt` & `HomeFeedModels.kt`
- **Files:**
  - `app/src/main/java/com/deepeye/musicpro/data/repository/HomeFeedRepository.kt`
  - `app/src/main/java/com/deepeye/musicpro/domain/model/home/HomeFeedModels.kt`
- **Actions:**
  1. In `HomeFeedRepository.kt`: Remove `shortsDeferred` and `shorts = ...` computation. Always provide `shorts = emptyList()`.
  2. Filter all feeds with `!MusicFilter.isShort(it.title, it.duration, it.isShort)`.

### Task 6: Remove `ShortsRail` from `HomeHubScreen.kt`
- **File:** `app/src/main/java/com/deepeye/musicpro/ui/homehub/HomeHubScreen.kt`
- **Actions:**
  1. Remove `if (feedState.shorts.isNotEmpty()) { item { ShortsRail(...) } }`.
  2. Delete the private `@Composable fun ShortsRail(...)` function.

### Task 7: Update `YouTubeViewModel.kt` & `YoutubeRemoteDataSource.kt`
- **Files:**
  - `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeViewModel.kt`
  - `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/YoutubeRemoteDataSource.kt`
- **Actions:**
  1. `YoutubeRemoteDataSource.getShorts()`: Return `emptyList()`.
  2. `YoutubeRemoteDataSource.searchVideos()`: Strip shorts from results.
  3. `YouTubeViewModel`: Enforce global shorts exclusion on `videos` state.

### Task 8: Verification & Compilation
- **Commands:**
  1. Run unit tests: `./gradlew testDebugUnitTest --tests "com.deepeye.musicpro.youtube.ShortsBlockerTest"`
  2. Kotlin compile check: `./gradlew compileDebugKotlin -x kspDebugKotlin`
  3. Run deployment script: `./run_deepeye.sh`
  4. Capture device screenshot to verify HomeHub and YouTube screen have zero shorts rails:
     `adb -s LZN7EERSZPS4VSUG exec-out screencap -p > /Users/enayat/.hermes/cache/scratch/shorts_blocked_verified.png`

---

## Risks & Tradeoffs
- **Edge Case: Very short real songs (e.g. 50s intro track):**
  - Standard music tracks with `duration in 1..59` could get caught if purely based on duration.
  - *Mitigation:* In `MusicFilter.isMusicTrack`, if channel name is an official music partner (e.g. `t-series`, `vevo`, `saregama`) AND title does NOT contain any `#short` hashtag, it can be preserved as music while blocking all generic video shorts.
