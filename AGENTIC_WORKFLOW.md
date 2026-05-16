# DeepEyeMusicPro: Agentic Workflow Document

## 1. System Overview & Architecture

DeepEyeMusicPro is a premium, high-fidelity media hub designed for the Android ecosystem. It converges local music playback with YouTube discovery and a pro-grade Digital Signal Processing (DSP) engine.

### High-Level Architecture (MVVM + Clean Architecture)
- **UI Layer**: Jetpack Compose based, utilizing a "Single Activity" pattern. Highly dynamic and theme-responsive.
- **Domain Layer**: Pure business logic (Models, Use Cases) ensuring platform-independent rules.
- **Data Layer**: 
    - **Local**: Room DB for library management, ContentResolver for local media.
    - **Remote**: NewPipe-powered YouTube extractor for stream fetching.
- **Player Layer**: Centralized `PlayerController` orchestrating Media3/ExoPlayer and the `AudioSessionManager`.
- **DSP Layer (V4A)**: Custom engine wrapping Android's `AudioEffect` API with a focus on low-latency, high-priority processing and a "Viper-style" control system.

---

## 2. Core Constraints & Coding Standards

- **State Management**: Absolute reliance on `StateFlow` and `collectAsStateWithLifecycle`. All state updates must be atomic using `.update { }`.
- **Concurrency**: Mandatory use of `Dispatchers.Main` for UI and `Dispatchers.IO` for HAL/Network/DB.
- **Error Handling**: Loud logging at the `Error` level for all playback and HAL-related signals to facilitate remote debugging.
- **DSP Integrity**: Audio effects must be attached to the specific `audioSessionId` provided by the active player instance. Effects must be released and re-initialized on session changes to prevent HAL leaks.
- **Aesthetics**: Premium visual excellence is non-negotiable. Use custom HSL color palettes, glassmorphism, and hardware-accelerated animations.

---

## 3. Phased Implementation Roadmap

### Phase 1: Foundation & Discovery Integration [COMPLETED]
- [x] Initialize Project Structure (Compose + Hilt + Media3).
- [x] Implement YouTube Remote Data Source (NewPipe Integration).
- [x] Design Home Hub discovery rails (Trending, Music, Shorts).
- [x] Implement `PlayerController` singleton.

### Phase 2: Playback Pipeline & YouTube Stream Fetching [IN PROGRESS]
- [x] Implement YouTube ID extraction and Stream URL fetching.
- [x] Map Remote/Local domain models to Media3 `MediaItem`.
- [x] Connect Discovery Rail clicks to `PlayerController`.
- [ ] Implement "Now Playing" playback controls (Play/Pause, Seek, Skip).
- [ ] Add Loading states and Stream Error recovery logic.

### Phase 3: DSP Engine (V4A) & Visualizer [REFINING]
- [x] Implement `V4AEngine` with Equalizer, Bass Boost, and Virtualizer.
- [x] Create `AudioSessionManager` for dynamic effect attachment.
- [x] Implement Real-time FFT Visualizer.
- [x] Fix DSP state management and HAL debouncing.
- [ ] Implement Advanced V4A Modules (Convolver, Dynamics, Tube Simulator).
- [ ] Add Gain Budget Meter with auto-clipping protection.

### Phase 4: Local Media & Library Management [PENDING]
- [ ] Implement MediaStore scanner for local storage.
- [ ] Create Local Library UI (Songs, Albums, Artists).
- [ ] Implement Search functionality (Unified Local + YouTube).
- [ ] Add Playlist management (Add to Queue, Create Playlist).

### Phase 5: Advanced UI/UX & High-Fidelity Polish [PENDING]
- [ ] Implement Dynamic Themeing based on Album Art colors.
- [ ] Add Premium Transitions between Discovery and Now Playing.
- [ ] Optimize Visualizer performance using Canvas/DrawScope.
- [ ] Implement Background Playback and Media Notifications.

---

## 4. Agent Context State (Task Tracking)

| Module | Last Action | Current Status | Next Step |
| :--- | :--- | :--- | :--- |
| **Playback** | Fixed `updateState` atomicity | STABLE | Implement Queue controls |
| **V4A Engine** | Added HAL debouncing & Master Gain | STABLE | Implement Convolver/IRS |
| **UI** | Wired Playback from Rails | FUNCTIONAL | Add Loading Indicators |
| **Discovery** | Fixed YouTube ID extraction | STABLE | Implement Pagination/Infinite Scroll |

---

## 5. Architectural Decisions Records (ADR)
- **ADR-001**: Used `LoudnessEnhancer` as a proxy for PGC/Master Gain because standard `Equalizer` lacks a global preamp.
- **ADR-002**: Implemented 100ms debounce in `V4AViewModel` to prevent JNI/HAL saturation during rapid slider interaction.
- **ADR-003**: Opted for `NewPipe` over official YouTube Data API to bypass API quota limits and ensure direct stream access.
