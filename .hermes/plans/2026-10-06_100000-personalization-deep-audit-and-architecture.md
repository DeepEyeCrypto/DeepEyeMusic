# 🧠 DEEPEYE PERSONALIZATION ARCHITECTURE: DEEP AUDIT & TECHNICAL BREAKDOWN

**Document ID:** `2026-10-06_100000-personalization-deep-audit-and-architecture`  
**Status:** COMPREHENSIVE AUDIT COMPLETE  
**Target:** DeepEyeMusicPro (`com.deepeye.musicpro`)  
**Scope:** HomeHub, YouTube, Feed Recommendation Engines, On-Device History, Gamification, and Monet Dynamic Theming.

---

## 🎯 Goal
Provide an exhaustive, architectural analysis of all active personalization engines, algorithmic sources, on-device heuristics, and dynamic contextual systems driving the DeepEyeMusicPro user interface.

---

## 🏗️ The 6-Pillar Personalization Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                          DEEPEYE MUSIC PRO PERSONALIZATION MATRIX                           │
├───────────────────────────────┬───────────────────────────────┬─────────────────────────────┤
│ 1. CLOUD INNERTUBE TVHTML5    │ 2. SMARTTUBE ALGORITHMIC NEXT  │ 3. ON-DEVICE RESUME ENGINE  │
│ - OAuth2 User Bearer Token    │ - Autoplay Endpoint Renderer  │ - Continue Watching (3-97%) │
│ - Cloud Liked Videos          │ - Dynamic Playlist Panel      │ - Continue Listening Resume │
│ - Subscriptions Feed          │ - Smart Home Topic Sections   │ - Local High-Res Library    │
├───────────────────────────────┼───────────────────────────────┼─────────────────────────────┤
│ 4. CONTEXT & DYNAMIC HEADER   │ 5. GAMIFICATION & ENGAGEMENT  │ 6. MONET & DSP AUDIO ENGINE │
│ - Time-of-day Greeting        │ - Daily Listening Streaks     │ - Palette Color Extraction  │
│ - Real-time BTC Price Ticker  │ - XP Reward Points & Badges   │ - Zero-Recomp VSYNC Engine  │
│ - Ambient Blur Backdrop       │ - Community Leaderboard (Top 3)│ - Viper4Android / DSP Master│
└───────────────────────────────┴───────────────────────────────┴─────────────────────────────┘
```

---

## 🔍 Detailed Breakdown of Active Personalization Systems

### 1. ☁️ InnerTube TVHTML5 Cloud Personalization (Authenticated Layer)
- **Source**: `AuthenticatedYouTubeClient.kt` & `HomeFeedRepository.kt`
- **Mechanism**:
  - Direct integration with Google/YouTube OAuth Bearer Token using the TVHTML5 client identity (`clientName: 85`, `version: 7.20210614.03.00`).
  - **Cloud Liked Tracks**: Direct extraction of the user's `LL` playlist ("Liked Videos").
  - **Subscriptions Feed**: Direct polling of new releases and video uploads from subscribed channels.
  - **Cloud Watch History**: Seamless synchronization with YouTube's cloud playback history.
  - **My Supermix & Daily Mixes**: Extraction of YouTube Music's algorithmic personalized mixes.

### 2. ⚡ SmartTube Algorithmic Discovery Engine
- **Source**: `SmartTubeEngine.kt` & `RecommendationViewModel.kt`
- **Mechanism**:
  - `getPersonalizedHome()`: Dynamically parses YouTube's TV browse response into personalized content rows (*"For You"*, *"Trending Hits"*, *"Genre Dives"*, *"Hidden Gems"*).
  - `getAlgorithmicNext(videoId)`: Continuous radio queue generation when playing any track, selecting optimal next songs using a 3-tier fallback parser (1: `autoplayEndpointRenderer`, 2: `playlistPanelRenderer`, 3: `compactVideoRenderer`).

### 3. 💾 On-Device State & Playback Resume Engine
- **Source**: `HistoryRepository.kt` & `MusicRepository.kt`
- **Mechanism**:
  - **Continue Watching Rail**: Automatically tracks video progress. Items with completion between `3%` and `97%` appear with an exact linear progress bar over the thumbnail.
  - **Continue Listening Rail**: Stores audio timestamp checkpoints for 1-tap instant resume.
  - **Local Audio Integration**: Scans internal/SD storage for local FLAC/Opus/MP3 tracks and injects recently added files directly into discovery rails.

### 4. 🕒 Dynamic Context & Time-of-Day System
- **Source**: `HomeGreetingHeader.kt` & `SplitMediaHero.kt`
- **Mechanism**:
  - **Dynamic Greeting**: Time-aware greeting (*"Good morning"*, *"Good afternoon"*, *"Good evening"*, *"Good night"*).
  - **Live Bitcoin (BTC) Price Pill**: Real-time cryptocurrency ticker.
  - **DotMatrix Clock**: Ambient live clock and network status display.

### 5. 🏆 Gamification & Streak Loyalty System
- **Source**: `GamificationEngine.kt` & `GamificationRepository.kt`
- **Mechanism**:
  - **Listening Streaks**: Calculates consecutive daily active listening sessions.
  - **Reward Points & XP**: Points awarded for completing full songs, creating playlists, and listening to high-res audio.
  - **Achievement Badges & Top 3 Leaderboard**: Unlocks visual badges with on-device celebration popups.

### 6. 🎨 Monet Dynamic Luminance & DSP Engine
- **Source**: `DSPEngine.kt`, `PlayerViewModel.kt`, & `ColorExtraction.kt`
- **Mechanism**:
  - **Palette Ambilight & Monet Theming**: Extracts dominant and vibrant colors from active album art in real-time. Dynamically computes WCAG contrast ratios to generate glowing text, borders, and ambient background blurs.
  - **DSP Master State**: Reflects hardware Viper4Android / 10-band equalizer profiles.

---

## 📋 Proposed Verification & Maintenance Tasks
1. **Task 1**: Verify OAuth token refresh loop in `AuthenticatedYouTubeClient.kt` to ensure zero session drops during cloud personalization fetching.
2. **Task 2**: Keep `SmartTubeEngine.kt` recursive JSON parser safe against InnerTube renderer schema updates.
3. **Task 3**: Ensure on-device `CachedRecommendationRow` maintains offline fallback capability when internet connection drops.
