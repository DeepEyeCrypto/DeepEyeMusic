# Codebase Scan: Personalization & Recommendation Architecture

## Goal
Provide a comprehensive inventory and audit of all personalization, recommendation, taste profile, and autoplay algorithmic code across the DeepEyeMusicPro codebase.

---

## 1. Remote Ingestion & Algorithmic Extractors (Network Layer)
- **`app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/SmartTubeEngine.kt`**
  - Spoofs `TVHTML5` client (`7.20210614.03.00` + Samsung Smart TV user-agent + `X-YouTube-Client-Name: 85`).
  - Calls `/youtubei/v1/browse` with `browseId = "FEwhat_to_watch"` & `"FEmusic_home"` for 1:1 authentic YouTube TV recommendations.
  - Calls `/youtubei/v1/next` for algorithmic next track extraction (`autoplayEndpointRenderer`, `maybeHistoryEndpointRenderer`).
- **`app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`**
  - InnerTube client for YouTube Music (`X-YouTube-Client-Name: 67`).
  - Implements `fetchPersonalizedFeed`, `fetchPersonalizedHome`, `fetchAutoplayNext`, and `fetchLyrics`.
- **`app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`**
  - Authenticated OAuth client fetching user subscriptions, watch history, liked tracks, and user playlists.
- **`app/src/main/java/com/deepeye/musicpro/domain/recommendation/ContentFetcher.kt`**
  - Aggregates multi-source tracks from YouTube, NewPipe, and local storage.

---

## 2. Core Algorithmic Engines & Scoring (Domain Layer)
- **`app/src/main/java/com/deepeye/musicpro/domain/recommendation/RecommendationEngine.kt`**
  - Hybrid recommendation engine balancing listening history, artist affinity, genre frequency, and freshness.
  - Receives listen events (`trackListenEvent`) to dynamically calibrate user taste.
- **`app/src/main/java/com/deepeye/musicpro/domain/recommendation/ScoringEngine.kt`**
  - Multi-factor mathematical scoring model (play count weight, completion rate, skip penalty, recency decay).
- **`app/src/main/java/com/deepeye/musicpro/domain/personalization/DiversityRanker.kt`**
  - Maximal Marginal Relevance (MMR) algorithm preventing repetitive tracks from the same artist/genre.
- **`app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoplayRepository.kt` & `AutoplayScorer.kt`**
  - Real-time continuous queue engine scoring next-track candidates with SmartTube `/next` results as priority #1.
- **`app/src/main/java/com/deepeye/musicpro/data/repository/PersonalizationRepositoryImpl.kt`**
  - Aggregates 8 distinct section types with multi-tier fallback (SmartTube TVHTML5 $\to$ InnerTube $\to$ Local Room DB $\to$ Trending).

---

## 3. Data Models & Entities
- **`app/src/main/java/com/deepeye/musicpro/domain/model/personalization/PersonalizedModels.kt`**
  - `PersonalizedFeedState`, `PersonalizedSection`, `PersonalizedFeedItem`, `PersonalizedSectionType` (`CONTINUE_LISTENING`, `YOUR_QUEUE`, `RECENTLY_PLAYED`, `LIKED_MUSIC`, `YOUR_PLAYLISTS`, `NEW_FROM_SUBSCRIPTIONS`, `BASED_ON_LISTENING`, `TRENDING_MUSIC`).
- **`app/src/main/java/com/deepeye/musicpro/domain/recommendation/RecommendationModels.kt`**
  - `VideoItem`, `ScoredVideo`, `RecommendationRow`, `RecommendationResult`.
- **`app/src/main/java/com/deepeye/musicpro/data/prefs/TasteProfile.kt`**
  - Vector storing genre affinities, top artists, explicit filter settings, and discovery sliders.
- **`app/src/main/java/com/deepeye/musicpro/domain/autoplay/AutoplayModels.kt`**
  - Data classes for candidate tracks, autoplay queues, and scoring weights.

---

## 4. Storage, Caching & User Control Layer
- **`app/src/main/java/com/deepeye/musicpro/data/cache/AccountPersonalizationCache.kt`**
  - Account-isolated caching preventing cross-profile recommendation leakage.
- **`app/src/main/java/com/deepeye/musicpro/data/cache/dao/PersonalizedSectionDao.kt` & `HiddenContentDao.kt`**
  - Room DAOs for offline feed caching and user-hidden song/artist filtering.
- **`app/src/main/java/com/deepeye/musicpro/data/cache/HiddenContentManager.kt`**
  - Dislike, hide track, and hide artist logic.
- **`app/src/main/java/com/deepeye/musicpro/data/prefs/TasteProfileDataStore.kt` & `PersonalizationPreferencesDataStore.kt`**
  - Preferences DataStore persisting taste profiles and privacy settings.

---

## 5. Background Jobs & Sync
- **`app/src/main/java/com/deepeye/musicpro/workers/RecommendationRefreshWorker.kt`**
  - Periodic WorkManager job calculating fresh recommendations in the background.
- **`app/src/main/java/com/deepeye/musicpro/workers/AutoplayPrefetchWorker.kt`**
  - Background audio stream and metadata prefetcher for zero-gap autoplay.
- **`app/src/main/java/com/deepeye/musicpro/domain/sync/CloudSyncManager.kt`**
  - Cloud taste profile synchronization.

---

## 6. UI & Presentation Layer
- **`app/src/main/java/com/deepeye/musicpro/ui/music/MusicScreen.kt` & `MusicViewModel.kt`**
  - Personalized Music feed screen with section carousels, pull-to-refresh, hide actions, and explanation triggers.
- **`app/src/main/java/com/deepeye/musicpro/ui/music/components/WhyThisBottomSheet.kt`**
  - Transparent user explainability modal ("Why did I get recommended this?").
- **`app/src/main/java/com/deepeye/musicpro/ui/homehub/HomeHubScreen.kt` & `HomeHubViewModel.kt`**
  - Main Home hub with dynamic recommendation rows, "Continue Listening", and video rails.
- **`app/src/main/java/com/deepeye/musicpro/ui/settings/SettingsScreen.kt` (Taste Profile Editor)**
  - Interactive UI for adjusting favorite genres, artists, and recommendation discovery aggressiveness.
