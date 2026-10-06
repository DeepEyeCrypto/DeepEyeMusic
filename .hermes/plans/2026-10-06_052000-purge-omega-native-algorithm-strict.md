# Plan: Purge-Omega Protocol (Native YouTube Algorithm Strict & Local Bloat Deletion)

## Goal
Permanently delete all redundant local recommendation math, manual scoring engines, MMR diversity rankers, custom predictive background workers, and obsolete explainability dialogs, transitioning `PersonalizationRepository` and `AutoplayRepository` into 100% pure proxies delegating strictly to native InnerTube & SmartTube TVHTML5 endpoints (`/youtubei/v1/browse` and `/youtubei/v1/next`).

## Current Context / Assumptions
- The app currently maintains both local predictive engines (`RecommendationEngine`, `ScoringEngine`, `DiversityRanker`, `AutoplayScorer`) and direct TVHTML5 InnerTube extractors (`SmartTubeEngine`, `InnerTubeRemoteClient`, `AuthenticatedYouTubeClient`).
- Local scoring engines duplicate YouTube's sophisticated server-side neural network models and introduce unnecessary CPU/battery overhead on mobile hardware.
- The connected YouTube account already provides authentic, personalized recommendations and mixes via OAuth Bearer token + TVHTML5 client context.

## Step-by-Step Implementation Plan

### Step 1: Remove References from Consumers & Decouple DI
1. **`app/src/main/java/com/deepeye/musicpro/player/controller/PlayerController.kt`**:
   - Remove `RecommendationEngine` and `recommendationEngine.trackListenEvent(...)`.
2. **`app/src/main/java/com/deepeye/musicpro/ui/player/PlayerViewModel.kt`**:
   - Remove `RecommendationEngine` parameter and import.
3. **`app/src/main/java/com/deepeye/musicpro/MainActivity.kt`**:
   - Remove `RecommendationRefreshWorker` enqueue calls if present.
4. **`app/src/main/java/com/deepeye/musicpro/DeepEyeApp.kt`**:
   - Remove `PeriodicWorkRequestBuilder<RecommendationRefreshWorker>` background registration.
5. **`app/src/main/java/com/deepeye/musicpro/ui/music/MusicScreen.kt` & `MusicViewModel.kt`**:
   - Remove `WhyThisBottomSheet` references.

### Step 2: Delete Obsolete Files
Permanently remove:
- `app/src/main/java/com/deepeye/musicpro/domain/recommendation/RecommendationEngine.kt`
- `app/src/main/java/com/deepeye/musicpro/domain/recommendation/ScoringEngine.kt`
- `app/src/main/java/com/deepeye/musicpro/domain/personalization/DiversityRanker.kt`
- `app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoplayScorer.kt`
- `app/src/main/java/com/deepeye/musicpro/workers/RecommendationRefreshWorker.kt`
- `app/src/main/java/com/deepeye/musicpro/ui/music/components/WhyThisBottomSheet.kt`

### Step 3: Streamline Repositories into Pure Proxies
1. **`app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoplayRepository.kt`**:
   - Strip `AutoplayScorer` and local fallback queues.
   - Pure delegation to `SmartTubeEngine.getAlgorithmicNext(videoId)` with `InnerTubeRemoteClient` fallback.
2. **`app/src/main/java/com/deepeye/musicpro/data/repository/PersonalizationRepositoryImpl.kt`**:
   - Refactor `getPersonalizedFeed` to delegate directly to `SmartTubeEngine.getPersonalizedHome()` and `InnerTubeRemoteClient.fetchPersonalizedFeed()`.

### Step 4: Verification, Test Suite & Release
1. Run `./gradlew --no-daemon testDebugUnitTest`.
2. Bump version to `versionCode = 30080`, `versionName = "3.0.1.70"`.
3. Assemble debug APK with `./gradlew --no-daemon assembleDebug`.
4. Sideload onto Realme RMX3945 and capture screenshot to verify clean, live Home/Music feeds.
5. Commit, tag `v3.0.1.70`, and push to GitHub.

## Risks & Tradeoffs
- **Zero Local Offline Fallback**: Feed requires network connectivity to load personalized mixes, but cached feeds in memory / Room handle intermittent network drops gracefully.
