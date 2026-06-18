# DeepEyeMusicPro — Agentic Workflow 2.0

### Premium Hybrid Media Blueprint · Android + DSP + YouTube Video + YouTube Music Surface

### Lead Architect Master Plan · FLOW Execution Model · v2.0

---

## Executive Summary

DeepEyeMusicPro is a premium Android hybrid media platform that combines:

- Local offline music playback
- 14-module V4A-style DSP engine
- Jetpack Compose premium glassmorphic UI
- YouTube video-first discovery
- YouTube Music-style listening rails
- Smart search, adaptive home personalization, and release-grade automation

This document upgrades the original architecture into a **HomeHub-first product strategy** where the app is not just a player, but a premium discovery + listening + DSP system. The core execution model remains:

**FLOW = Frame → Layout → Orchestrate → World**

This workflow enforces:

- strict clean architecture,
- stage-gated implementation,
- self-correcting agent loops,
- premium UX polish,
- and deployable Android production standards.

---

## Product Identity

| Attribute | Value |
|---|---|
| **App Name** | DeepEyeMusicPro |
| **Application ID** | `com.deepeye.musicpro` |
| **Min SDK** | API 26 |
| **Target SDK** | API 35 |
| **Language** | Kotlin 2.0+ |
| **UI** | Jetpack Compose 1.7+ |
| **Architecture** | Clean Architecture + MVI |
| **DI** | Hilt |
| **DB** | Room 2.6+ |
| **Playback** | Media3 / ExoPlayer |
| **DSP** | V4A-style engine, 14 modules |
| **Remote Feed Engine** | NewPipe Extractor-first strategy |
| **Settings** | DataStore |
| **Build** | Gradle 8.x + Version Catalog |
| **CI/CD** | GitHub Actions |
| **Distribution** | Open-source GitHub release |
| **Execution Framework** | FLOW |

---

## Core Product Evolution

### v1.0 Identity

- local playback
- premium DSP
- modern Compose player
- playlists, search, library, settings

### v2.0 Identity

- HomeHub becomes the product center
- YouTube video rail + YouTube Music-style rail
- DSP quick actions from home
- personalized and adaptive premium landing experience
- local + remote content coexist elegantly

---

## North-Star Experience

The user opens DeepEyeMusicPro and immediately sees:

1. a premium hero section with featured YouTube video and featured music content,
2. continue watching + continue listening,
3. local library resume,
4. DSP-aware quick actions,
5. premium animated rails,
6. one-tap transition into player, queue, V4A, or search.

The app must feel like:

- **YouTube + YouTube Music + Viper4Android + premium Android polish**
inside one consistent DeepEye product identity. [cite:541][cite:542]

---

## FLOW Execution Model

### 1. Frame

Define product role, domain boundaries, data contracts, APIs, and stage goal.

### 2. Layout

Build file structure, screen structure, models, composables, navigation, and state contracts.

### 3. Orchestrate

Connect repositories, use cases, playback engine, DSP engine, search, recommendation, and persistence.

### 4. World

Test on realistic devices, verify responsiveness, tune performance, validate CI/CD, and prepare release.

Each implementation output must pass:

- syntax sanity,
- dependency graph sanity,
- lifecycle sanity,
- UI resilience,
- and fallback behavior verification
before moving forward.

---

## Product Surfaces

### Primary Surfaces

- HomeHub
- YouTube
- Music
- Library
- Search
- Now Playing
- V4A DSP
- Playlists
- Settings

### Supporting Surfaces

- Queue sheet
- Device route sheet
- Preset manager
- Continue watching/listening cards
- Recent search/history sheet
- Notification controls
- Mini-player
- Inline media detail sheet

---

## Premium HomeHub

### Purpose

HomeHub replaces the old basic Home screen.

### HomeHub Composition

1. Greeting + adaptive recommendation banner
2. Split hero:
   - left = YouTube featured video
   - right = YouTube Music-style featured album/playlist
3. Continue Watching rail
4. Continue Listening rail
5. Shorts rail
6. Quick Picks rail
7. Trending Videos rail
8. New Music rail
9. Local Library Resume rail
10. DSP Quick Panel
11. Moods / activities chips
12. Recently searched / recent artists / recent channels
13. Retry / offline fallback cards if network unavailable

### Layout behavior

- **Phone:** vertically stacked hero cards
- **Tablet / foldable:** dual-pane split hero
- **Expanded width:** richer side-by-side dashboard using adaptive layout guidance from Jetpack Compose docs. [web:588][web:591]

---

## Navigation Upgrade

### Main Navigation

- YouTube icon replaces generic Home icon
- Music tab placed next to it
- Library
- Search
- V4A
- Settings

### Tab semantics

- **YouTube:** video-centric feed
- **Music:** listening-centric feed
- **Library:** local-first collection
- **V4A:** DSP dashboard
- **Search:** unified global search
- **Settings:** app, audio, personalization, diagnostics

Branding and icon usage should remain consistent with official YouTube brand guidance. [web:556]

---

## Remote Media Strategy

### Approved ingestion strategy

Use a **repository abstraction** where remote discovery and stream extraction are separated from UI.

### Preferred stack

- **Primary extractor:** NewPipe Extractor for content extraction and stream metadata handling. [web:563][web:567][web:576]
- **Optional metadata layer:** official YouTube Data API for safe metadata-driven enhancements where appropriate. [web:547][web:548]

### Why this split

- NewPipe-style extraction fits the open-source, no-user-key-first workflow.
- Official YouTube Data API is useful for formally structured metadata access and future hardening. [web:547][web:548]
- The codebase should never tightly couple UI directly to extractor internals.

---

## Architecture 2.0 Folder Structure

```text
DeepEyeMusicPro/
├── app/
│   └── src/main/java/com/deepeye/musicpro/
│       ├── core/
│       │   ├── extensions/
│       │   ├── result/
│       │   ├── dispatcher/
│       │   ├── logger/
│       │   ├── network/
│       │   └── utils/
│       │
│       ├── data/
│       │   ├── db/
│       │   ├── prefs/
│       │   ├── repository/
│       │   └── source/
│       │       ├── local/
│       │       └── remote/
│       │           ├── youtube/
│       │           │   ├── NewPipeDownloader.kt
│       │           │   ├── YoutubeRemoteDataSource.kt
│       │           │   ├── YoutubeMusicRemoteDataSource.kt
│       │           │   ├── YoutubeMapper.kt
│       │           │   ├── YoutubePagingSource.kt
│       │           │   └── YoutubeSearchCache.kt
│       │           ├── feed/
│       │           │   ├── HomeFeedAggregator.kt
│       │           │   ├── RecommendationEngine.kt
│       │           │   └── MoodFeedBuilder.kt
│       │           └── analytics/
│       │               ├── PlaybackHistoryTracker.kt
│       │               └── HomeInteractionTracker.kt
│       │
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Song.kt
│       │   │   ├── Album.kt
│       │   │   ├── Artist.kt
│       │   │   ├── Playlist.kt
│       │   │   ├── Genre.kt
│       │   │   ├── PlayerState.kt
│       │   │   └── home/
│       │   │       ├── HomeFeedState.kt
│       │   │       ├── HomeVideoItem.kt
│       │   │       ├── HomeMusicItem.kt
│       │   │       ├── HomeRail.kt
│       │   │       ├── HomeSection.kt
│       │   │       └── RecommendationReason.kt
│       │   ├── repository/
│       │   └── usecase/
│       │       ├── home/
│       │       │   ├── GetHomeFeedUseCase.kt
│       │       │   ├── GetContinueWatchingUseCase.kt
│       │       │   ├── GetContinueListeningUseCase.kt
│       │       │   ├── GetQuickPicksUseCase.kt
│       │       │   ├── GetTrendingVideosUseCase.kt
│       │       │   ├── GetMoodMixesUseCase.kt
│       │       │   └── BuildHomeSectionsUseCase.kt
│       │       └── ...
│       │
│       ├── dsp/
│       │   ├── engine/
│       │   ├── model/
│       │   ├── data/
│       │   ├── session/
│       │   └── di/
│       │
│       ├── player/
│       │   ├── service/
│       │   ├── controller/
│       │   ├── queue/
│       │   ├── visualizer/
│       │   ├── history/
│       │   └── notification/
│       │
│       ├── ui/
│       │   ├── theme/
│       │   ├── navigation/
│       │   ├── components/
│       │   │   ├── premium/
│       │   │   │   ├── SplitMediaHero.kt
│       │   │   │   ├── PremiumHeroCard.kt
│       │   │   │   ├── GlassOmnibox.kt
│       │   │   │   ├── PremiumRail.kt
│       │   │   │   ├── GlowChip.kt
│       │   │   │   └── RecommendationBadge.kt
│       │   │   ├── GlowCard.kt
│       │   │   ├── AudioVisualizer.kt
│       │   │   ├── ShimmerBox.kt
│       │   │   ├── MarqueeText.kt
│       │   │   └── MiniPlayer.kt
│       │   ├── homehub/
│       │   │   ├── HomeHubScreen.kt
│       │   │   ├── HomeHubViewModel.kt
│       │   │   ├── HomeSections.kt
│       │   │   ├── ContinueWatchingRow.kt
│       │   │   ├── ContinueListeningRow.kt
│       │   │   ├── ShortsRail.kt
│       │   │   ├── QuickPicksRail.kt
│       │   │   ├── LocalResumeRail.kt
│       │   │   └── DspQuickPanel.kt
│       │   ├── youtube/
│       │   ├── music/
│       │   ├── library/
│       │   ├── player/
│       │   ├── search/
│       │   ├── playlist/
│       │   ├── v4a/
│       │   └── settings/
│       │
│       ├── di/
│       ├── DeepEyeApp.kt
│       └── MainActivity.kt
│
├── docs/
│   ├── PLAN.md
│   ├── ARCHITECTURE.md
│   ├── DSP_ENGINE.md
│   ├── HOMEHUB.md
│   ├── REMOTE_FEEDS.md
│   └── RELEASE.md
│
└── .github/workflows/
    ├── ci.yml
    └── release.yml
