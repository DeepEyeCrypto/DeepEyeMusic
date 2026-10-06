# 📐 ARCHITECTURE PLAN: COMPREHENSIVE LANDSCAPE & TV REDESIGN FOR ALL APP SECTIONS

**Document ID:** `2026-10-06_093000-landscape-tv-redesign-all-sections`  
**Status:** READY FOR IMPLEMENTATION  
**Target:** DeepEyeMusicPro (`com.deepeye.musicpro`)  
**Scope:** Home Section, YouTube Section, Video Hub, Music & NowPlaying Section, Library Section, and Global TVHTML5 Interceptor Validation.  
**Architect:** Hermes-Apex (Autonomous Principal UI/UX & API Architect)

---

## 🎯 Goal
Deeply audit, redesign, and debug the landscape and Android TV widescreen layouts across all five primary application sections (Home, YouTube, Video Hub, Music/NowPlaying, Library) to ensure responsive multi-column grids, master-detail viewports, focus-friendly D-pad navigation, and seamless integration with the global TVHTML5 Header Interceptor.

---

## 📌 Current Context & Architectural Baseline
1. **Device & Platform Realities**:
   - Physical Target: Realme RMX3945 (MT6835, Android 16) and Android TV / Landscape displays.
   - Screen Orientations: Portrait (Phone handheld) vs. Landscape (TV / Tablet / Foldable / Landscape Phone).
   - Navigation: Side `NavigationRail` (80dp) in landscape vs. Bottom `NavigationBar` in portrait.
2. **Global TVHTML5 Header Interceptor**:
   - Injected in `NetworkModule.kt`:
     - `User-Agent: Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)`
     - `X-YouTube-Client-Name: 85`
     - `X-YouTube-Client-Version: 7.20210614.03.00`
     - `Content-Type: application/json`
3. **Key Problem Areas in Current Landscape UI**:
   - **Home Hub (`HomeHubScreen.kt`)**: Vertical column cards waste widescreen space or stretch horizontally.
   - **YouTube Feed (`YouTubeScreen.kt`)**: Needs dynamic grid columns (3-4 columns in landscape) rather than single-list scrolling.
   - **Video Hub (`NetMirrorScreen.kt`)**: Movie & video cards need cinematic landscape grid geometry with hero backdrop.
   - **Music & Now Playing (`NowPlayingScreen.kt` & `MusicScreen.kt`)**: Needs true dual-pane master-detail (Left: Album Art + Controls; Right: Real-time kinetic lyrics & Up Next queue).
   - **Library (`LibraryScreen.kt`)**: 4-column adaptive grid for playlists, albums, and artists in landscape.

---

## 🏗️ Architecture & Proposed Redesign Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    LANDSCAPE VIEWPORT                                       │
├─────────┬───────────────────────────────────────────────────────────────────────────────────┤
│ NavRail │ SECTION VIEWPORT                                                                  │
│ (80dp)  │                                                                                   │
│         │ [1. Home Hub]   -> Split Hero Banner (3:2) + Multi-Row Carousels                 │
│  Home   │ [2. YouTube]    -> 3-4 Column LazyVerticalGrid + Channel Chips Header             │
│  Music  │ [3. Video Hub]  -> 16:9 Feature Hero Banner + 4-5 Column Cinematic Card Grid      │
│  Videos │ [4. Music / NP] -> Master-Detail Split (50% Artwork/Controls | 50% Synced Lyrics) │
│  Lib    │ [5. Library]    -> Filter Tabs + 4-Column Media Grid                              │
└─────────┴───────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Step-by-Step Implementation Tasks

### Task 1: Audit & Enforce Global TVHTML5 Interceptor in `NetworkModule.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/di/NetworkModule.kt`
- **Action**:
  - Verify that `provideOkHttpClient` attaches the `InnerTubeTvInterceptor` to all outgoing `/youtubei/v1` requests.
  - Verify zero leakage of legacy User-Agents or unauthorized headers.

---

### Task 2: Redesign Home Section for Landscape (`HomeHubScreen.kt` & `HomeScreen.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/homehub/HomeHubScreen.kt`
- **Actions**:
  - Detect orientation via `val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE`.
  - In Landscape:
    - Top: Dual-pane `SplitMediaHero` (Left: Featured algorithmic recommendation; Right: `DotMatrixClock` + Quick Actions).
    - Rows: Wrap category items into horizontal scrolling carousels with fixed-width cards (`240.dp` for videos, `180.dp` for music albums).
    - Grid adaptation: When switching to full feed, use `LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 220.dp))`.
    - Focus support: Attach `.hoverable()` and focus outline borders for Android TV D-Pad controllers.

---

### Task 3: Redesign YouTube Section for Landscape (`YouTubeScreen.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeScreen.kt`
- **Actions**:
  - In Landscape:
    - Transform main list into a 3 or 4 column `LazyVerticalGrid(columns = GridCells.Fixed(if (isWideLandscape) 4 else 3))`.
    - Render `SmartTubeVideoCard` with 16:9 thumbnail ratio, high-contrast dynamic text badges, and channel avatar overlays.
    - Category Filter Row: Sticky top pill bar with horizontal scroll and D-pad focus retention.
    - Video Details / Inline Preview: Expandable side-drawer or dialog for video comments and related items without navigating away.

---

### Task 4: Redesign Video Hub Section for Landscape (`NetMirrorScreen.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/screens/NetMirrorScreen.kt`
- **Actions**:
  - In Landscape:
    - Top Hero: Cinematic 21:9 or 16:9 full-width backdrop banner featuring the top trending video with instant play button.
    - Grid layout: 4-column adaptive movie/video cards (`GridCells.Adaptive(180.dp)`) with glassmorphism meta chips.
    - Filtering: Horizontal chip selector (Movies, Trailers, Music Videos, Live Streams) aligned neatly under the hero banner.

---

### Task 5: Redesign Music & Now Playing for Landscape (`MusicScreen.kt` & `NowPlayingScreen.kt`)
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/music/MusicScreen.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/NowPlayingScreen.kt`
- **Actions**:
  - In `MusicScreen.kt`:
    - Landscape: 2-column or 3-column split view (Left: Personalized Radio / Quick Mixes; Right: Top Tracks & Playlists).
  - In `NowPlayingScreen.kt`:
    - Landscape Master-Detail:
      - **Left Pane (45% width)**: Vinyl/Album Art + Track Metadata + Player Controls (Play/Pause, Skip, Shuffle, Repeat, Volume Boost, DSP shortcut).
      - **Right Pane (55% width)**: Real-time kinetic `SyncedLyricsScreen` with auto-scroll centering and seamless tab switch to Up Next Queue (`PlaylistBottomSheet` content).

---

### Task 6: Redesign Library Section for Landscape (`LibraryScreen.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/library/LibraryScreen.kt`
- **Actions**:
  - In Landscape:
    - Top: Segmented control tabs (Playlists, Liked Songs, Downloaded, Artists, Albums).
    - Body: 4-column adaptive grid (`GridCells.Adaptive(160.dp)`) for playlist/album tiles.
    - Quick Play Floating Action Button / Header Action for instant shuffle.

---

### Task 7: Compilation, Debugging & Hardware Verification
- **Actions**:
  - Compile with `./gradlew assembleDebug`.
  - Install APK on Realme RMX3945 via ADB.
  - Test screen rotation (Portrait <-> Landscape) across all 5 sections.
  - Verify smooth UI rendering, 60fps scrolling, zero layout clipping, and zero network errors.

---

## 🧪 Validation Matrix
- **Orientation Stress Test**: Rotate device while playing media; verify playback does not stutter and UI instantly adapts to dual-pane/grid.
- **TV D-pad Test**: Ensure focus moves predictably across grid items and carousels.
- **Network Validation**: Verify `/youtubei/v1` calls return HTTP 200 with proper TVHTML5 user-agent.

---

## ⚠️ Risks & Tradeoffs
- **State Preservation on Rotation**: Ensure ViewModels and rememberSaveable states retain scroll position and active lyric sync during orientation changes.
- **Aspect Ratio Differences**: Tablets (4:3 / 16:10) vs Phones in Landscape (19.5:9) require `GridCells.Adaptive` instead of hardcoded fixed column counts.
