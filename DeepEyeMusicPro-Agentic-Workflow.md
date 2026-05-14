# DeepEyeMusicPro — Agentic Development Workflow
### Lead Architect Blueprint · Full-Stack Android + DSP · v1.0

---

## Executive Summary

DeepEyeMusicPro is a premium Android music player featuring a 14-module Viper4Android-style DSP engine, Jetpack Compose glassmorphic UI, local library management, and streaming integration. This document defines the complete agentic development workflow: phased roadmap, atomic task breakdown, folder structure, coding standards, and stage-gate protocol. The build follows a strict **Frame → Layout → Orchestrate → World** (FLOW) execution model with self-correcting agent loops at every stage.

---

## Project Identity

| Attribute | Value |
|---|---|
| **App ID** | `com.deepeye.musicpro` |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 35 (Android 15) |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose 1.7+ |
| **Architecture** | Clean Architecture + MVI |
| **DI** | Hilt (Dagger) |
| **Database** | Room 2.6+ |
| **Audio** | ExoPlayer / Media3 + custom DSP |
| **Build** | Gradle 8.x + Version Catalogs |
| **CI/CD** | GitHub Actions |
| **Min RAM** | 3 GB recommended (DSP processing) |

---

## Folder Structure

```
DeepEyeMusicPro/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/deepeye/musicpro/
│       │   │   │
│       │   │   ├── core/                        # Pure Kotlin, zero Android deps
│       │   │   │   ├── extensions/              # Kotlin extension functions
│       │   │   │   ├── result/                  # Result<T, E> sealed class
│       │   │   │   └── utils/                   # TimeFormatter, SizeFormatter
│       │   │   │
│       │   │   ├── data/                        # Data layer
│       │   │   │   ├── db/                      # Room entities, DAOs, migrations
│       │   │   │   ├── prefs/                   # DataStore preferences
│       │   │   │   ├── repository/              # Repo implementations
│       │   │   │   └── source/
│       │   │   │       ├── local/               # MediaStore scanner
│       │   │   │       └── remote/              # Piped/YouTube API clients
│       │   │   │
│       │   │   ├── domain/                      # Business logic (no Android)
│       │   │   │   ├── model/                   # Song, Album, Artist, Playlist
│       │   │   │   ├── repository/              # Repository interfaces
│       │   │   │   └── usecase/                 # One class per use case
│       │   │   │
│       │   │   ├── dsp/                         # V4A DSP Engine
│       │   │   │   ├── engine/                  # V4AEngine, V4AViewModel
│       │   │   │   ├── model/                   # DspParams, GainBudget, EngineState
│       │   │   │   ├── data/                    # DspDatabase, DspPresetDao, PresetRepository
│       │   │   │   ├── session/                 # AudioSessionManager
│       │   │   │   └── di/                      # V4AModule (Hilt)
│       │   │   │
│       │   │   ├── player/                      # Playback engine
│       │   │   │   ├── service/                 # MusicPlayerService (Media3)
│       │   │   │   ├── controller/              # PlayerController
│       │   │   │   ├── queue/                   # QueueManager
│       │   │   │   ├── visualizer/              # AudioVisualizer bridge
│       │   │   │   └── notification/            # MediaNotificationManager
│       │   │   │
│       │   │   ├── ui/                          # Presentation layer
│       │   │   │   ├── theme/                   # DeepEyeTheme, Colors, Type, Shapes
│       │   │   │   ├── navigation/              # NavGraph, Routes, DeepLinks
│       │   │   │   ├── components/              # Reusable Compose components
│       │   │   │   │   ├── GlowCard.kt
│       │   │   │   │   ├── AudioVisualizer.kt
│       │   │   │   │   ├── MarqueeText.kt
│       │   │   │   │   └── ShimmerBox.kt
│       │   │   │   ├── home/                    # HomeScreen + HomeViewModel
│       │   │   │   ├── player/                  # NowPlayingScreen + PlayerViewModel
│       │   │   │   ├── library/                 # LibraryScreen, Albums, Artists
│       │   │   │   ├── search/                  # SearchScreen + SearchViewModel
│       │   │   │   ├── playlist/                # PlaylistScreen + PlaylistViewModel
│       │   │   │   ├── v4a/                     # V4AScreen + V4AComponents
│       │   │   │   └── settings/                # SettingsScreen + SettingsViewModel
│       │   │   │
│       │   │   ├── di/                          # App-level Hilt modules
│       │   │   │   ├── AppModule.kt
│       │   │   │   ├── DatabaseModule.kt
│       │   │   │   ├── PlayerModule.kt
│       │   │   │   └── NetworkModule.kt
│       │   │   │
│       │   │   ├── DeepEyeApp.kt                # @HiltAndroidApp
│       │   │   └── MainActivity.kt              # @AndroidEntryPoint
│       │   │
│       │   ├── res/
│       │   │   ├── drawable/                    # Vector icons, shapes
│       │   │   ├── raw/                         # Default IRS impulse responses
│       │   │   └── values/                      # strings.xml, themes.xml
│       │   └── AndroidManifest.xml
│       │
│       └── test/ + androidTest/                 # Unit + instrumented tests
│
├── docs/                                        # Architecture docs, ADRs
│   ├── ARCHITECTURE.md
│   ├── DSP_ENGINE.md
│   ├── API_CONTRACTS.md
│   └── PLAN.md                                  # This roadmap (persistent context)
│
├── gradle/
│   └── libs.versions.toml                       # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── .github/
    └── workflows/
        ├── ci.yml                               # PR checks
        └── release.yml                          # Signed APK + AAB

```

---

## Implementation Roadmap

---

### PHASE 0 — Project Bootstrap (Day 1)

**Goal:** Clean, compilable skeleton with all dependencies wired.

| # | Task | File(s) | Status |
|---|---|---|---|
| 0.1 | Initialize project with Kotlin 2.0, min SDK 26, target SDK 35 | `build.gradle.kts` | ⬜ |
| 0.2 | Configure `libs.versions.toml` version catalog | `gradle/libs.versions.toml` | ⬜ |
| 0.3 | Add all Gradle dependencies (Compose, Hilt, Room, Media3, Coroutines) | `app/build.gradle.kts` | ⬜ |
| 0.4 | Setup Hilt: `@HiltAndroidApp` on DeepEyeApp, `@AndroidEntryPoint` on MainActivity | `DeepEyeApp.kt` | ⬜ |
| 0.5 | Setup NavGraph scaffold with placeholder routes | `NavGraph.kt` | ⬜ |
| 0.6 | Create `DeepEyeTheme` with dark/light Color tokens, Typography, Shapes | `ui/theme/` | ⬜ |
| 0.7 | Implement base `AppModule`, `DatabaseModule` in Hilt | `di/` | ⬜ |
| 0.8 | Setup GitHub Actions CI (build + lint check on every PR) | `.github/workflows/ci.yml` | ⬜ |
| 0.9 | Write `PLAN.md` and `ARCHITECTURE.md` in `/docs` | `docs/` | ⬜ |

**Stage-gate:** `./gradlew assembleDebug` passes with zero errors.

---

### PHASE 1 — Domain & Data Layer (Days 2–4)

**Goal:** All data models, DAOs, and repository contracts defined before any UI.

#### 1A — Domain Models

| # | Task | File |
|---|---|---|
| 1.1 | Define `Song` domain model (id, title, artist, album, uri, duration, size, path, artUri) | `domain/model/Song.kt` |
| 1.2 | Define `Album`, `Artist`, `Playlist`, `Genre` models | `domain/model/` |
| 1.3 | Define `PlayerState` (queue, currentIndex, isPlaying, position, repeatMode, shuffleMode) | `domain/model/PlayerState.kt` |
| 1.4 | Define all Repository interfaces | `domain/repository/` |
| 1.5 | Define all UseCase classes (GetAllSongs, SearchSongs, GetAlbumSongs, CreatePlaylist, etc.) | `domain/usecase/` |

#### 1B — Room Database

| # | Task | File |
|---|---|---|
| 1.6 | Create `SongEntity` + `@Dao` with CRUD + Flow queries | `data/db/SongDao.kt` |
| 1.7 | Create `PlaylistEntity`, `PlaylistSongCrossRef` + DAO | `data/db/PlaylistDao.kt` |
| 1.8 | Create `AppDatabase` (version 1, exportSchema true, migration strategy) | `data/db/AppDatabase.kt` |
| 1.9 | Implement `DatabaseModule` Hilt provider | `di/DatabaseModule.kt` |

#### 1C — MediaStore Scanner

| # | Task | File |
|---|---|---|
| 1.10 | Build `MediaStoreScanner` — queries `MediaStore.Audio.Media` with all projections | `data/source/local/MediaStoreScanner.kt` |
| 1.11 | Handle Android 10/11/12/13/14 storage permission differences (READ_MEDIA_AUDIO) | `MediaStoreScanner.kt` |
| 1.12 | Build `LocalMusicRepository` implementation — maps `SongEntity` ↔ `Song` | `data/repository/LocalMusicRepositoryImpl.kt` |
| 1.13 | Write unit tests for scanner mapper functions | `test/` |

#### 1D — DSP Data Layer

| # | Task | File |
|---|---|---|
| 1.14 | Define `DspParams` data class with all 14 module fields | `dsp/model/DspModels.kt` |
| 1.15 | Define `GainBudget`, `EngineState`, `RiskLevel` | `dsp/model/DspModels.kt` |
| 1.16 | Create `DspPresetEntity`, `DspPresetDao`, `DspDatabase` | `dsp/data/` |
| 1.17 | Implement `PresetRepository` with 7 built-in presets seeder | `dsp/data/PresetRepository.kt` |
| 1.18 | Create `V4AModule` Hilt bindings | `dsp/di/V4AModule.kt` |

**Stage-gate:** All DAO unit tests pass; `AppDatabase` and `DspDatabase` compile and migrate cleanly.

---

### PHASE 2 — Audio Engine & DSP (Days 5–8)

**Goal:** Music plays end-to-end with full DSP processing attached.

#### 2A — Media3 Player Service

| # | Task | File |
|---|---|---|
| 2.1 | Build `MusicPlayerService` extending `MediaSessionService` | `player/service/MusicPlayerService.kt` |
| 2.2 | Configure `ExoPlayer` with `DefaultRenderersFactory`, audio offload disabled (DSP needs raw PCM) | `MusicPlayerService.kt` |
| 2.3 | Implement `PlayerController` — wrapper exposing play/pause/seek/queue operations | `player/controller/PlayerController.kt` |
| 2.4 | Build `QueueManager` — manages ordered + shuffled queue, repeat modes | `player/queue/QueueManager.kt` |
| 2.5 | Implement `MediaNotificationManager` with custom Compose-style notification layout | `player/notification/` |
| 2.6 | Declare service + permissions in `AndroidManifest.xml` | `AndroidManifest.xml` |

#### 2B — V4A DSP Engine

| # | Task | File |
|---|---|---|
| 2.7 | Build `V4AEngine` singleton — manages AudioEffect chain via `audioSessionId` | `dsp/engine/V4AEngine.kt` |
| 2.8 | Implement `AudioSessionManager` — attaches to ExoPlayer `AnalyticsListener` | `dsp/session/AudioSessionManager.kt` |
| 2.9 | Implement all 14 DSP modules as AudioEffect subclasses: Equalizer, BassBoost, Virtualizer, EnvironmentalReverb, PresetReverb, LoudnessEnhancer, DynamicsProcessing, Visualizer | `dsp/engine/` |
| 2.10 | Implement `GainBudget` calculator — real-time headroom risk scoring | `V4AEngine.kt` |
| 2.11 | Build `V4AViewModel` — state flows, preset apply/save, EQ hot-path | `dsp/engine/V4AViewModel.kt` |
| 2.12 | Wire `MusicPlayerService` ↔ `AudioSessionManager` ↔ `V4AEngine` | `MusicPlayerService.kt` |

#### 2C — Audio Visualizer

| # | Task | File |
|---|---|---|
| 2.13 | Build `VisualizerEngine` wrapping Android `Visualizer` API — FFT + waveform capture | `player/visualizer/VisualizerEngine.kt` |
| 2.14 | Create `AudioVisualizerState` — StateFlow of FFT float arrays | `player/visualizer/` |
| 2.15 | Wire visualizer to ExoPlayer session ID | `player/visualizer/` |

**Stage-gate:** Song plays from MediaStore → audio session attached → DSP applied → notification visible → visualizer emitting data.

---

### PHASE 3 — Core UI Screens (Days 9–14)

**Goal:** All primary screens functional with premium Compose UI.

#### 3A — Design System & Components

| # | Task | File |
|---|---|---|
| 3.1 | Finalize `DeepEyeTheme` — dark/light, dynamic color (Material You), custom color scheme | `ui/theme/Colors.kt` |
| 3.2 | Build `GlowCard` — glassmorphic card with configurable glow color, blur, border | `ui/components/GlowCard.kt` |
| 3.3 | Build `AudioVisualizer` Compose component — Canvas-based bar visualizer with beat pulse | `ui/components/AudioVisualizer.kt` |
| 3.4 | Build `MarqueeText` — smooth auto-scrolling for long song titles | `ui/components/MarqueeText.kt` |
| 3.5 | Build `ShimmerBox` — skeleton loading placeholder with shimmer animation | `ui/components/ShimmerBox.kt` |
| 3.6 | Build `MiniPlayer` — persistent bottom bar (thumbnail, title, play/pause, next) | `ui/components/MiniPlayer.kt` |

#### 3B — Home Screen

| # | Task | File |
|---|---|---|
| 3.7 | Build `HomeViewModel` — recently played, featured, quick categories | `ui/home/HomeViewModel.kt` |
| 3.8 | Build `HomeScreen` — Featured `GlowCard` row, category chips, recently played horizontal list | `ui/home/HomeScreen.kt` |
| 3.9 | Implement smooth scroll with `LazyRow` + snap behavior | `HomeScreen.kt` |

#### 3C — Now Playing Screen

| # | Task | File |
|---|---|---|
| 3.10 | Build `PlayerViewModel` — exposes `PlayerState` flow, wires to `PlayerController` | `ui/player/PlayerViewModel.kt` |
| 3.11 | Build `NowPlayingScreen` — album art (full-bleed), animated background blur, controls | `ui/player/NowPlayingScreen.kt` |
| 3.12 | Integrate `AudioVisualizer` component — pulsing bars synced to FFT data | `NowPlayingScreen.kt` |
| 3.13 | Implement swipe-to-dismiss (back), swipe left/right (prev/next track) | `NowPlayingScreen.kt` |
| 3.14 | Build queue bottom sheet — `LazyColumn` with drag-to-reorder | `ui/player/QueueSheet.kt` |

#### 3D — Library Screen

| # | Task | File |
|---|---|---|
| 3.15 | Build `LibraryViewModel` — all songs, albums, artists, genres with sort/filter | `ui/library/LibraryViewModel.kt` |
| 3.16 | Build `LibraryScreen` — tab row: Songs / Albums / Artists / Genres | `ui/library/LibraryScreen.kt` |
| 3.17 | Build `AlbumDetailScreen` — album art hero, track list | `ui/library/AlbumDetailScreen.kt` |
| 3.18 | Build `ArtistDetailScreen` — artist art, albums grid, top songs | `ui/library/ArtistDetailScreen.kt` |

#### 3E — Search Screen

| # | Task | File |
|---|---|---|
| 3.19 | Build `SearchViewModel` — debounced local search + remote suggestions | `ui/search/SearchViewModel.kt` |
| 3.20 | Build `SearchScreen` — search bar, recent searches, live results with skeleton loading | `ui/search/SearchScreen.kt` |

#### 3F — Playlist Screen

| # | Task | File |
|---|---|---|
| 3.21 | Build `PlaylistViewModel` — CRUD operations on playlists | `ui/playlist/PlaylistViewModel.kt` |
| 3.22 | Build `PlaylistScreen` — playlist grid, create/rename/delete dialogs | `ui/playlist/PlaylistScreen.kt` |
| 3.23 | Build `PlaylistDetailScreen` — song list with add/remove capability | `ui/playlist/PlaylistDetailScreen.kt` |

**Stage-gate:** All screens render correctly at 375dp and 1280dp. Navigation between all screens works. Mini player persists across screens.

---

### PHASE 4 — V4A DSP Screen (Days 15–17)

**Goal:** Complete Viper4Android-style DSP control screen.

| # | Task | File |
|---|---|---|
| 4.1 | Build `V4AScreen` — scrollable column with all 14 module cards | `ui/v4a/V4AScreen.kt` |
| 4.2 | Build 10-band `FireEqualizerCard` with Canvas EQ curve preview | `ui/v4a/V4AComponents.kt` |
| 4.3 | Build `GainBudgetCard` — real-time headroom meter with risk color coding | `ui/v4a/V4AComponents.kt` |
| 4.4 | Build `PresetBar` — horizontal chip row, load/save/delete presets | `ui/v4a/V4AComponents.kt` |
| 4.5 | Build individual module cards: PGC, Convolver, Reverb, Tube, Bass, Clarity, Dynamic, HRTF, Surround, Protection, Gate | `ui/v4a/V4AComponents.kt` |
| 4.6 | Add phase conflict auto-detection warning (Field Surround + Convolver combo) | `V4AScreen.kt` |
| 4.7 | Add IRS picker bottom sheet for Convolver module | `V4AScreen.kt` |
| 4.8 | Wire V4AScreen → V4AViewModel → V4AEngine live updates | `V4AScreen.kt` |

**Stage-gate:** All 14 modules render, sliders update DSP state in real-time, presets load/save correctly.

---

### PHASE 5 — Settings & Configuration (Day 18)

| # | Task | File |
|---|---|---|
| 5.1 | Build `SettingsViewModel` using `DataStore<Preferences>` | `ui/settings/SettingsViewModel.kt` |
| 5.2 | Build `SettingsScreen` — grouped sections: Appearance, Audio, Library, About | `ui/settings/SettingsScreen.kt` |
| 5.3 | Implement theme picker (Dark / Light / System) | `SettingsScreen.kt` |
| 5.4 | Implement audio quality settings (sample rate, bit depth, crossfade duration) | `SettingsScreen.kt` |
| 5.5 | Implement library rescan with progress dialog | `SettingsScreen.kt` |
| 5.6 | Add "About DeepEye" section with version, build info, licenses | `SettingsScreen.kt` |

---

### PHASE 6 — Polish, Testing & Release (Days 19–22)

#### 6A — Quality & Testing

| # | Task |
|---|---|
| 6.1 | Unit tests: all UseCases, Repository implementations, GainBudget calculator |
| 6.2 | UI tests: NavGraph routing, screen transitions, bottom sheet behavior |
| 6.3 | Integration tests: MediaStore scan → Room insert → Library display |
| 6.4 | DSP integration test: session attach → preset apply → gain budget verify |
| 6.5 | Performance: profile with Android Profiler — target <16ms frame time on NowPlaying |
| 6.6 | Memory leak check: LeakCanary integration, verify no Activity/Service leaks |

#### 6B — Release Pipeline

| # | Task | File |
|---|---|---|
| 6.7 | Configure release signing in `build.gradle.kts` with keystore via GitHub Secrets | `build.gradle.kts` |
| 6.8 | Write GitHub Actions release workflow — trigger on `v*` tag push | `.github/workflows/release.yml` |
| 6.9 | Generate signed APK + AAB artifacts | `release.yml` |
| 6.10 | Move all planning docs to `/docs` folder | `docs/` |
| 6.11 | Write final `README.md` with build instructions, features, screenshots | `README.md` |

**Stage-gate:** Signed release APK installs and runs on Android 8.0–15. All P0 features verified.

---

## Key Dependencies (`libs.versions.toml`)

```toml
[versions]
kotlin            = "2.0.21"
compose-bom       = "2024.12.01"
hilt              = "2.52"
room              = "2.6.1"
media3            = "1.5.1"
coroutines        = "1.9.0"
datastore         = "1.1.1"
gson              = "2.10.1"
coil              = "2.7.0"
leakcanary        = "2.14"

[libraries]
compose-bom              = { group = "androidx.compose", name = "compose-bom",          version.ref = "compose-bom" }
compose-ui               = { group = "androidx.compose.ui", name = "ui" }
compose-material3        = { group = "androidx.compose.material3", name = "material3" }
compose-navigation       = { group = "androidx.navigation", name = "navigation-compose", version = "2.8.4" }
hilt-android             = { group = "com.google.dagger", name = "hilt-android",         version.ref = "hilt" }
hilt-compiler            = { group = "com.google.dagger", name = "hilt-android-compiler",version.ref = "hilt" }
hilt-navigation-compose  = { group = "androidx.hilt", name = "hilt-navigation-compose",  version = "1.2.0" }
room-runtime             = { group = "androidx.room", name = "room-runtime",             version.ref = "room" }
room-compiler            = { group = "androidx.room", name = "room-compiler",            version.ref = "room" }
room-ktx                 = { group = "androidx.room", name = "room-ktx",                 version.ref = "room" }
media3-exoplayer         = { group = "androidx.media3", name = "media3-exoplayer",        version.ref = "media3" }
media3-session           = { group = "androidx.media3", name = "media3-session",          version.ref = "media3" }
media3-ui                = { group = "androidx.media3", name = "media3-ui",               version.ref = "media3" }
coroutines-android       = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
datastore-prefs          = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
gson                     = { group = "com.google.code.gson", name = "gson",               version.ref = "gson" }
coil-compose             = { group = "io.coil-kt", name = "coil-compose",                version.ref = "coil" }
leakcanary               = { group = "com.squareup.leakcanary", name = "leakcanary-android", version.ref = "leakcanary" }
```

---

## AndroidManifest.xml — Required Permissions

```xml
<!-- Audio playback -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Media access -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />               <!-- API 33+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />                                                    <!-- API ≤32 -->

<!-- DSP / AudioEffect -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />                  <!-- Visualizer -->

<!-- Network (streaming) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Post-notification (API 33+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## Coding Standards & Agent Rules

### Architecture Rules
- **Clean Architecture strictly enforced**: UI → Domain → Data (no reverse dependencies)
- **Domain layer**: zero Android imports (pure Kotlin only)
- **Data layer**: implements domain interfaces, maps entities ↔ domain models
- **UI layer**: only imports from domain models and ViewModels

### Compose Rules
- Every screen has a dedicated `ViewModel` with `@HiltViewModel`
- State: `StateFlow<UiState>` in ViewModel, collected via `collectAsStateWithLifecycle()`
- Side effects: handled in `LaunchedEffect`, not in composable body
- Previews: every component has `@Preview` annotation with mock data
- No business logic in composable functions

### DSP Rules
- `V4AEngine` is `@Singleton` — only one instance per app lifetime
- Session attach/detach MUST be symmetric — never leak an AudioEffect
- All DSP operations run on `Dispatchers.IO` except UI state updates
- `GainBudget` must be recalculated on every `DspParams` change

### Self-Correcting Agent Loop
Before presenting any code output:
1. **Syntax check**: Valid Kotlin syntax, no missing imports
2. **Dependency check**: All injected classes exist in DI graph
3. **API check**: No deprecated MediaStore/AudioEffect APIs without fallback
4. **Null safety**: No force-unwrap `!!` operators
5. **Coroutine check**: No blocking calls on Main thread
6. **Lifecycle check**: No Activity/Fragment context leaks in ViewModels

---

## Stage-Gate Protocol

```
Phase 0 → ✅ gradlew assembleDebug passes
Phase 1 → ✅ All Room DAOs compile + unit tests pass
Phase 2 → ✅ Audio plays end-to-end + DSP session attached
Phase 3 → ✅ All screens render at 375dp + 1280dp, navigation works
Phase 4 → ✅ All 14 DSP modules functional, presets persist
Phase 5 → ✅ Settings persist across app restarts
Phase 6 → ✅ Signed release APK runs on Android 8.0–15
```

**Each phase requires explicit "GO" before proceeding to the next.**

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Android AudioEffect API deprecated/broken on custom ROMs | High | High | Test on stock + MIUI + OneUI; fallback to software DSP |
| ExoPlayer audio session ID changes on format switch | High | Medium | `AudioSessionManager` re-attaches on every `onAudioSessionIdChanged` |
| MediaStore permission denied on Android 13+ | Medium | High | Runtime permission request with rationale dialog; graceful empty state |
| Room migration failure on DB schema change | Medium | High | `fallbackToDestructiveMigration()` in debug; proper migrations in release |
| Memory pressure with high-res album art | Medium | Medium | Coil with bitmap pool, downsample to 512×512px max |
| DSP gain clipping causing audio distortion | Low | High | `GainBudget` calculator + PGC module auto-correction |

---

*Document maintained in `/docs/PLAN.md` — update after each phase completion*
*DeepEyeMusicPro · deepeye.tech · Build with precision, ship with confidence*
