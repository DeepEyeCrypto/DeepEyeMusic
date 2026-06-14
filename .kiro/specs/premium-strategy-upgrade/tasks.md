# Implementation Plan: Premium Strategy Upgrade

## Overview

This implementation plan covers the Premium Strategy Upgrade across DeepEye Music Pro's 8 pillars. The work is organized into incremental tasks that build on the existing Jetpack Compose glassmorphic UI, Media3/ExoPlayer player, V4A DSP Engine, and NewPipe-based YouTube streaming infrastructure. Each task references specific requirements and builds on previous steps to ensure no orphaned code.

## Tasks

- [x] 1. Room Database Migration (Foundation)
  - [x] 1.1 Create Room migration adding `dsp_profiles` table with composite primary key (track_id, device_id) and all DSP parameter columns
    - Add migration to existing AppDatabase
    - _Requirements: 3.5, 13.1_
  - [x] 1.2 Create Room migration adding `play_events` table for listening history with trackId, title, artist, artworkUri, playedMs, durationMs, completedFully, timestamp
    - _Requirements: 20.1_
  - [x] 1.3 Create Room migration adding `onboarding_preferences` table with languages, genres, artists, and completed_at columns
    - _Requirements: 11.3_
  - [ ]* 1.4 Write migration tests verifying schema correctness and data preservation for all three new tables
    - _Requirements: 3.5, 20.1, 11.3_

- [x] 2. Color Theming System
  - [x] 2.1 Create `PaletteExtractor` class in `com.deepeye.musicpro.ui.theme.palette` with LRU cache (20 entries) that extracts dominant, vibrant, muted, darkVibrant, darkMuted, and lightVibrant colors from artwork URIs using AndroidX Palette
    - _Requirements: 1.1_
  - [x] 2.2 Create `PaletteConstrainer` utility that clamps palette luminance values to ensure WCAG AA contrast ratios (4.5:1 for text, 3:1 for UI) in Dark and AMOLED modes
    - _Requirements: 1.5, 21.1, 21.2, 21.3_
  - [x] 2.3 Create `PaletteResult` data class and `PaletteViewModel` that exposes animated palette state via `StateFlow<PaletteResult>` with 600ms cubic-bezier transitions
    - _Requirements: 1.2, 1.3_
  - [x] 2.4 Implement fallback logic: when artwork URI is null or extraction fails, return a default neutral dark palette without visual glitches
    - _Requirements: 1.4_
  - [ ]* 2.5 Write property test for palette contrast compliance
    - **Property 1: Palette Contrast Compliance**
    - **Validates: Requirements 1.5, 21.1, 21.2, 21.3**

- [ ] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Premium Sound Engine Exposure
  - [x] 4.1 Create `AudioQualityInfo` data class with fields: sampleRate, bitDepth, codec, isHiRes, isDvcActive in `com.deepeye.musicpro.ui.player.quality`
    - _Requirements: 3.1, 3.2, 3.6_
  - [x] 4.2 Create `AudioQualityBadge` composable that displays bit depth, sample rate, codec, Hi-Res badge, and DVC indicator in the Now Playing header
    - _Requirements: 3.1, 3.2, 3.6_
  - [x] 4.3 Implement audio metadata extraction from ExoPlayer Format object to populate `AudioQualityInfo` on track change
    - _Requirements: 3.1_
  - [ ]* 4.4 Write property test for Hi-Res badge correctness
    - **Property 2: Hi-Res Badge Correctness**
    - **Validates: Requirements 3.2**
  - [x] 4.5 Verify and enforce DSP chain order invariant: preamp → equalizer → bass boost → stereo widening → limiter → output in `DSPEngine.applyParams()`
    - _Requirements: 3.3_
  - [ ]* 4.6 Write property test for DSP chain order invariant
    - **Property 3: DSP Chain Order Invariant**
    - **Validates: Requirements 3.3**

- [x] 5. Per-Device and Per-Track DSP Profiles
  - [x] 5.1 Create Room entity `DspProfileEntity` with composite primary key (trackId, deviceId) and DAO with save/load/delete operations
    - _Requirements: 3.5, 13.1_
  - [x] 5.2 Create `AudioRouteDetector` that listens for audio output device changes and emits device identifiers
    - _Requirements: 3.4_
  - [x] 5.3 Create `DspProfileManager` with resolution order: per-track → per-device → global, loading within 100ms
    - _Requirements: 13.2, 13.3, 3.4_
  - [x] 5.4 Integrate `DspProfileManager` with `PlayerController` to auto-load profiles on track change and device change
    - _Requirements: 13.2, 3.4_
  - [ ]* 5.5 Write property test for per-device EQ profile round-trip
    - **Property 4: Per-Device EQ Profile Round-Trip**
    - **Validates: Requirements 3.5**
  - [ ]* 5.6 Write property test for per-track DSP profile round-trip
    - **Property 10: Per-Track DSP Profile Round-Trip**
    - **Validates: Requirements 13.1, 13.2**

- [x] 6. Gapless Playback and Crossfade
  - [x] 6.1 Verify and enable gapless playback in ExoPlayer configuration (ensure concatenating media sources and audio becoming noisy handling)
    - _Requirements: 4.1_
  - [x] 6.2 Create `CrossfadeManager` with configurable duration (1000–12000ms) and equal-power curve using cos/sin functions
    - _Requirements: 4.2, 4.3_
  - [x] 6.3 Implement crossfade volume ducking using ExoPlayer volume control on current and next media items
    - _Requirements: 4.3_
  - [x] 6.4 Implement fallback: if next track buffer is not ready at crossfade start, fall back to gapless without crossfade
    - _Requirements: 4.4_
  - [ ]* 6.5 Write property test for crossfade duration validation
    - **Property 5: Crossfade Duration Validation**
    - **Validates: Requirements 4.2**
  - [ ]* 6.6 Write property test for equal-power crossfade energy conservation
    - **Property 6: Equal-Power Crossfade Energy Conservation**
    - **Validates: Requirements 4.3**

- [x] 7. Premium Bass Experience
  - [x] 7.1 Create `BassProcessor` with independent sub-bass (20–60Hz) and mid-bass (60–250Hz) gain controls using DynamicsProcessing multi-band EQ
    - _Requirements: 5.1_
  - [x] 7.2 Implement harmonic saturation processing (even-harmonic only) with configurable intensity 0.0–1.0
    - _Requirements: 5.2_
  - [x] 7.3 Implement bass limiter using DynamicsProcessing limiter stage with threshold preventing output > 0dBFS
    - _Requirements: 5.3_
  - [x] 7.4 Create default `BassPreset` configurations for HEADPHONE, SPEAKER, and CAR audio routes
    - _Requirements: 5.4_
  - [x] 7.5 Enhance existing `BassRingGlow` composable to pulse intensity proportional to bass FFT amplitude with color from palette dominant
    - _Requirements: 5.5, 5.6_
  - [ ]* 7.6 Write property test for bass limiter clipping prevention
    - **Property 7: Bass Limiter Clipping Prevention**
    - **Validates: Requirements 5.3**

- [ ] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Sleep Timer
  - [x] 9.1 Create `SleepTimerUseCase` with countdown state, start/cancel methods, and duration options (15, 30, 45, 60, 90 min, end of track)
    - _Requirements: 14.1_
  - [x] 9.2 Implement linear volume fade from current to zero over final 30 seconds
    - _Requirements: 14.2_
  - [x] 9.3 Implement pause and audio focus release when countdown reaches zero
    - _Requirements: 14.3_
  - [x] 9.4 Create glass-styled sleep timer indicator in Now Playing showing remaining time
    - _Requirements: 14.4_
  - [x] 9.5 Implement cancel: restore full volume immediately and dismiss countdown display
    - _Requirements: 14.5_
  - [ ]* 9.6 Write property test for volume fade linearity
    - **Property 11: Sleep Timer Volume Fade Linearity**
    - **Validates: Requirements 14.2**

- [x] 10. Listening History and Top Tracks
  - [x] 10.1 Create `PlayEventEntity` Room table and DAO with insert and query operations
    - _Requirements: 20.1_
  - [x] 10.2 Create `HistoryRepository` implementation that records play events with timestamp, duration, and completion status
    - _Requirements: 20.1_
  - [x] 10.3 Implement Top Tracks computation: aggregate by trackId, rank by total play time, filter by time period (week/month/all-time)
    - _Requirements: 20.2_
  - [ ] 10.4 Create listening history screen with chronological list (artwork thumbnail, title, artist, timestamp)
    - _Requirements: 20.3, 20.4_
  - [ ] 10.5 Create top tracks screen with ranked list and period selector (glass pills)
    - _Requirements: 20.2_
  - [ ]* 10.6 Write property test for play event recording round-trip
    - **Property 15: Play Event Recording Round-Trip**
    - **Validates: Requirements 20.1**
  - [ ]* 10.7 Write property test for top tracks sort order
    - **Property 16: Top Tracks Sort Order**
    - **Validates: Requirements 20.2**

- [x] 11. First-Launch Onboarding
  - [x] 11.1 Create `OnboardingViewModel` with multi-step state (language → genre → artist) and skip/complete actions
    - _Requirements: 11.1, 11.2_
  - [x] 11.2 Create glass-styled onboarding screen composables with page transitions using MotionTokens easing
    - _Requirements: 11.4_
  - [x] 11.3 Persist onboarding preferences to DataStore and seed TasteProfileRepository on completion
    - _Requirements: 11.3_
  - [x] 11.4 Add first-launch detection gate in MainActivity that routes to onboarding when preferences are empty
    - _Requirements: 11.1_
  - [ ]* 11.5 Write property test for onboarding preferences persistence round-trip
    - **Property 17: Onboarding Preferences Persistence Round-Trip**
    - **Validates: Requirements 11.3**

- [x] 12. Smart Queue and Autoplay Enhancement
  - [x] 12.1 Enhance existing autoplay logic to exclude blocked tracks from all recommendation candidates
    - _Requirements: 12.2_
  - [x] 12.2 Implement no-repeat window: prevent same track ID from appearing within last 10 autoplay selections
    - _Requirements: 12.3_
  - [x] 12.3 Implement skip-streak detection: after 3 consecutive skips, shift to Familiar mode (high-confidence matches)
    - _Requirements: 12.4_
  - [x] 12.4 Expose Familiar/Discovery mode toggle in the autoplay UI section of NowPlayingScreen
    - _Requirements: 12.5_
  - [ ]* 12.5 Write property test for autoplay exclusion of blocked tracks
    - **Property 8: Autoplay Exclusion of Blocked Tracks**
    - **Validates: Requirements 12.2**
  - [ ]* 12.6 Write property test for no-repeat within sliding window of 10 selections
    - **Property 9: Autoplay No-Repeat Window**
    - **Validates: Requirements 12.3**

- [ ] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Cinematic Now Playing Screen
  - [x] 14.1 Refactor `NowPlayingScreen` portrait layout to allocate minimum 55% viewport height to artwork, with blurred shadow layer at 18dp vertical offset
    - _Requirements: 2.1, 2.2_
  - [x] 14.2 Implement artwork crossfade animation using 400ms spring (stiffness 400, damping 0.8) when track changes
    - _Requirements: 2.3_
  - [x] 14.3 Enforce typography hierarchy: headline-medium (24sp bold) for title, title-medium (16sp medium) for artist, with marquee auto-scroll for overflow
    - _Requirements: 2.4, 22.4_
  - [x] 14.4 Ensure transport controls meet minimum sizes: 48dp for skip/previous, 72dp for play/pause button
    - _Requirements: 2.5_
  - [x] 14.5 Implement two-column responsive layout in landscape/tablet mode with artwork left and controls right
    - _Requirements: 2.6_

- [x] 15. Premium Motion System
  - [x] 15.1 Create `MotionTokens` object with standard cubic-bezier (0.4, 0.0, 0.2, 1.0), expand/collapse spring (stiffness 300, damping 0.7), and artwork spring (stiffness 400, damping 0.8)
    - _Requirements: 8.1_
  - [x] 15.2 Implement tab bar shrink/expand animation: 56dp → 0dp spring animation triggered at 100dp scroll offset
    - _Requirements: 8.2_
  - [x] 15.3 Implement album artwork track-change animation: scale-down-fade-out → scale-up-fade-in over 400ms total
    - _Requirements: 8.3_
  - [x] 15.4 Audit all existing animations to use MotionTokens constants and verify 16ms frame budget compliance
    - _Requirements: 8.4, 17.1_

- [x] 16. Premium Haptic Feedback
  - [x] 16.1 Create `HapticPatterns` object with distinct patterns: transportControl (Confirm), seekComplete (Tick), modeToggle (Toggle)
    - _Requirements: 9.1, 9.2, 9.3_
  - [x] 16.2 Integrate haptic feedback into all transport controls (play, pause, skip, previous) in NowPlayingScreen
    - _Requirements: 9.1_
  - [x] 16.3 Add tick haptic to seek slider release and toggle haptic to shuffle/repeat mode buttons
    - _Requirements: 9.2, 9.3_
  - [ ] 16.4 Implement accessibility check: suppress all haptics when system "disable haptics" setting is enabled
    - _Requirements: 9.4_

- [x] 17. Lock Screen Media Surface
  - [x] 17.1 Enhance MediaSession notification to include 512x512 artwork, 5 transport actions (previous, play/pause, next, like, close)
    - _Requirements: 10.1_
  - [x] 17.2 Ensure metadata updates within 100ms of track change by pre-loading artwork bitmap during track preparation
    - _Requirements: 10.2_
  - [x] 17.3 Implement notification persistence: maintain presence during 30-minute pause, restore state after service restart
    - _Requirements: 10.4, 10.5_
  - [ ]* 17.4 Write property test for null metadata handling in notification
    - **Property 12: Null Metadata Handling**
    - **Validates: Requirements 15.4**

- [x] 18. Premium Video Rail and Player
  - [x] 18.1 Create `VideoRailCard` composable with 16:9 thumbnail, 12dp corners, glass duration overlay, and metadata (title 2-line, channel, views)
    - _Requirements: 6.1, 6.4_
  - [x] 18.2 Implement progressive thumbnail loading: low-res placeholder → high-res transition within 300ms of visibility
    - _Requirements: 6.2_
  - [x] 18.3 Implement shared-element animation from video card tap to fullscreen Video_Player
    - _Requirements: 6.3_
  - [x] 18.4 Create glass-styled video transport overlay that auto-hides after 3 seconds with 200ms fade toggle on tap
    - _Requirements: 7.1, 7.2_
  - [x] 18.5 Implement seamless audio-to-video and video-to-audio mode transitions without audio interruption
    - _Requirements: 7.3_
  - [x] 18.6 Polish PiP mode: rounded corners, state continuity, glass progress bar visible during control auto-hide
    - _Requirements: 7.4, 7.5_

- [ ] 19. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 20. Per-Track DSP Profiles UI
  - [x] 20.1 Add "Save DSP for this track" action in the V4A settings screen when a track is playing
    - _Requirements: 13.1_
  - [x] 20.2 Add visual indicator in Now Playing when a per-track profile is active
    - _Requirements: 13.2_
  - [x] 20.3 Add "Delete per-track profile" option that reverts to device/global profile
    - _Requirements: 13.4_
  - [x] 20.4 Implement auto-load: when track with saved profile starts, apply within 100ms
    - _Requirements: 13.2_

- [x] 21. Dark and AMOLED Mode
  - [x] 21.1 Implement AMOLED mode: true black (#000000) background, glass border alpha 0.20, glass tint alpha 0.04
    - _Requirements: 21.1, 21.2_
  - [x] 21.2 Implement Dark mode: elevated surface (#121212), glass tint minimum 0.08 alpha
    - _Requirements: 21.3_
  - [x] 21.3 Add Light/Dark/AMOLED mode selector in settings screen
    - _Requirements: 21.4_
  - [x] 21.4 Integrate mode selection with PaletteConstrainer to adjust luminance clamping per mode
    - _Requirements: 1.5, 21.1, 21.2, 21.3_

- [x] 22. Premium Typography
  - [x] 22.1 Define type scale constants: Display (34sp), Headline (24sp), Title (16sp), Body (14sp), Label (12sp), Caption (11sp)
    - _Requirements: 22.2_
  - [x] 22.2 Enforce three font weights: Regular (400), SemiBold (600), Bold (700) across all text styles
    - _Requirements: 22.1_
  - [x] 22.3 Apply 2sp letter-spacing to all uppercase label text (e.g., "NOW PLAYING", tab labels)
    - _Requirements: 22.3_
  - [ ] 22.4 Implement marquee auto-scrolling for track titles and artist names that exceed container width
    - _Requirements: 22.4_

- [x] 23. Zero-Crash Stability
  - [x] 23.1 Implement graceful error handling in PlayerController: on playback error, log, skip to next, show non-intrusive toast
    - _Requirements: 15.1_
  - [x] 23.2 Implement audio session recovery: re-attach DSPEngine to new session within 500ms when playback resumes after session loss
    - _Requirements: 15.2_
  - [x] 23.3 Verify configuration change handling: rotation, split-screen, fold preserve playback and UI state
    - _Requirements: 15.3_
  - [x] 23.4 Implement null metadata safety: all UI composables display placeholder values for null title/artist/artwork
    - _Requirements: 15.4_
  - [ ]* 23.5 Write property test for null metadata handling
    - **Property 12: Null Metadata Handling**
    - **Validates: Requirements 15.4**

- [x] 24. Zero-Flicker and Performance
  - [x] 24.1 Implement skeleton screens with shimmer animation for all loading states (home, search, history, video rail)
    - _Requirements: 16.1, 18.3_
  - [x] 24.2 Ensure palette transitions use 600ms interpolation and prevent recomposition of stable content using stable keys and remember blocks
    - _Requirements: 16.2, 16.3_
  - [x] 24.3 Implement shared-element or crossfade transitions for screen navigation to prevent blank frames
    - _Requirements: 16.4_
  - [x] 24.4 Add performance monitoring: log frame times, skip/reduce glass effects when budget exceeds 16ms
    - _Requirements: 17.1, 17.2, 17.3_
  - [x] 24.5 Implement FFT visualizer throttling to 30fps when total frame time exceeds 14ms
    - _Requirements: 17.4_

- [x] 25. Premium Empty, Error, and Loading States
  - [x] 25.1 Create reusable `GlassEmptyState` composable with icon, title, and action suggestion
    - _Requirements: 18.1_
  - [x] 25.2 Create reusable `GlassErrorState` composable with icon, message, and retry button
    - _Requirements: 18.2_
  - [x] 25.3 Create reusable `GlassSkeletonLoader` composable with shimmer matching content layout dimensions
    - _Requirements: 18.3_
  - [x] 25.4 Apply consistent glass-styled states across all screens: home, search, history, queue, video rail
    - _Requirements: 18.4_

- [x] 26. Accessibility Compliance
  - [x] 26.1 Audit and enforce 48dp minimum touch targets on all interactive elements across the app
    - _Requirements: 19.1_
  - [x] 26.2 Add contentDescription to all icon buttons and non-text interactive elements
    - _Requirements: 19.2_
  - [x] 26.3 Implement reduce-transparency fallback: replace glass blur with opaque backgrounds when system setting is enabled
    - _Requirements: 19.3_
  - [x] 26.4 Implement reduce-motion fallback: disable non-essential animations, use instant transitions when system setting is enabled
    - _Requirements: 19.4_
  - [x] 26.5 Verify WCAG AA contrast ratios for all text against computed backgrounds
    - _Requirements: 19.5_
  - [ ]* 26.6 Write property test for touch target minimum size
    - **Property 13: Touch Target Minimum Size**
    - **Validates: Requirements 19.1**
  - [ ]* 26.7 Write property test for content description completeness
    - **Property 14: Content Description Completeness**
    - **Validates: Requirements 19.2**
  - [ ]* 26.8 Write property test for WCAG text contrast compliance
    - **Property 18: WCAG Text Contrast Compliance**
    - **Validates: Requirements 19.5**

- [x] 27. Integration and Wiring
  - [x] 27.1 Integrate palette-driven theming into all screens (Now Playing, Video Rail, History, Onboarding, Settings)
    - _Requirements: 1.3_
  - [x] 27.2 Wire `DspProfileManager` auto-load with `CrossfadeManager` to ensure DSP profiles apply correctly during crossfade transitions
    - _Requirements: 4.3, 13.2_
  - [x] 27.3 Wire `HistoryRepository.recordPlayEvent()` into `PlayerController` track completion and skip callbacks
    - _Requirements: 20.1_
  - [x] 27.4 Wire autoplay engine with listening history data to improve recommendation quality
    - _Requirements: 12.1_

- [ ] 28. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Implementation uses Kotlin with Jetpack Compose, Room, Media3/ExoPlayer, and Kotest property testing
- The DSP chain extends the existing V4A engine — no new audio framework is introduced
- Database migrations are additive only (no breaking changes) per the design migration strategy

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "15.1", "22.1", "22.2"] },
    { "id": 1, "tasks": ["1.4", "2.1", "4.1", "8.1", "22.3", "22.4"] },
    { "id": 2, "tasks": ["2.2", "2.3", "4.2", "4.3", "5.1", "8.2", "15.2"] },
    { "id": 3, "tasks": ["2.4", "2.5", "4.4", "4.5", "4.6", "5.2", "5.3", "15.3", "15.4"] },
    { "id": 4, "tasks": ["5.4", "5.5", "5.6", "6.1", "7.1", "9.1", "16.1"] },
    { "id": 5, "tasks": ["6.2", "6.3", "7.2", "7.3", "9.2", "9.3", "10.1", "16.2", "16.3"] },
    { "id": 6, "tasks": ["6.4", "6.5", "6.6", "7.4", "7.5", "7.6", "9.4", "9.5", "9.6", "10.2", "16.4"] },
    { "id": 7, "tasks": ["10.3", "10.4", "10.5", "11.1", "12.1", "14.1"] },
    { "id": 8, "tasks": ["10.6", "10.7", "11.2", "11.3", "12.2", "12.3", "14.2", "14.3"] },
    { "id": 9, "tasks": ["11.4", "11.5", "12.4", "12.5", "12.6", "14.4", "14.5"] },
    { "id": 10, "tasks": ["17.1", "17.2", "18.1", "18.2", "20.1", "21.1", "21.2"] },
    { "id": 11, "tasks": ["17.3", "17.4", "18.3", "18.4", "18.5", "18.6", "20.2", "20.3", "20.4", "21.3", "21.4"] },
    { "id": 12, "tasks": ["23.1", "23.2", "23.3", "23.4", "24.1", "24.2", "25.1", "25.2", "25.3"] },
    { "id": 13, "tasks": ["23.5", "24.3", "24.4", "24.5", "25.4", "26.1", "26.2", "26.3", "26.4"] },
    { "id": 14, "tasks": ["26.5", "26.6", "26.7", "26.8", "27.1", "27.2", "27.3", "27.4"] }
  ]
}
```
