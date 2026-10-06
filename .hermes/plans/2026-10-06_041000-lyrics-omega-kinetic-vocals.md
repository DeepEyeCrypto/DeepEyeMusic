# Plan: Lyrics-Omega Protocol (Kinetic Vocals & Synchronized Karaoke Engine)

## Goal
Elevate the synchronized lyrics engine with InnerTube timed lyrics extraction, 100ms ultra-smooth playback ticker synchronization, and a kinetic typography Compose UI with animated vertical auto-centering, scale transitions, active line highlighting, and ambient blur backdrop.

## Current Context / Assumptions
- `InnerTubeRemoteClient` already implements `fetchLyrics` using `/youtubei/v1/next` $\to$ `extractLyricsBrowseId` (tabs with `MPLY...`) $\to$ `/youtubei/v1/browse` (`musicTimedLyricsRenderer` / `musicDescriptionShelfRenderer`).
- `LyricsRepository` implements dual-engine fetching (InnerTube timed lyrics primary + LRCLIB secondary fallback).
- `PlayerController` currently ticks at 250ms. Updating to 100ms ensures fluid, zero-stutter karaoke line progression.
- `LyricsBottomSheet` currently implements smooth auto-scroll, scale/alpha animations, and tap-to-seek.

## Proposed Architecture & Enhancements
1. **100ms Ultra-Smooth Karaoke Playback Ticker**:
   - Update `PlayerController.kt` playback position polling loop to 100ms while active.
2. **InnerTube & SmartTube Timed Lyrics Resolution**:
   - Ensure `SmartTubeEngine.kt` also wires `fetchLyrics` or routes to `LyricsRepository` so TVHTML5 / Android Music clients seamlessly fetch timed karaoke timestamps.
3. **Kinetic Typography Lyrics UI**:
   - In `LyricsBottomSheet.kt`:
     - Active line: `scale(1.10f)`, `alpha = 1.0f`, bold kinetic typography with progressive ambient glow.
     - Inactive lines: `scale(0.92f)`, `alpha = 0.38f`.
     - Centered Auto-Scroll: `lazyListState.animateScrollToItem(index = activeIndex, scrollOffset = -viewportHeight / 3)` ensuring the singing vocal stays vertically centered.
     - Instant interactive seek: Tapping any lyric line instantly seeks ExoPlayer timeline to that exact millisecond.

## Step-by-Step Implementation Plan

### Step 1: Polish 100ms Ticker in `PlayerController.kt`
- File: `app/src/main/java/com/deepeye/musicpro/player/controller/PlayerController.kt`
- Optimize position update interval to 100ms during active playback.

### Step 2: Refine Kinetic Typography & Center Scroll in `LyricsBottomSheet.kt`
- File: `app/src/main/java/com/deepeye/musicpro/ui/player/LyricsBottomSheet.kt`
- Polish scale (`1.10f` vs `0.92f`), alpha (`1.0f` vs `0.38f`), and center viewport auto-scroll offset.

### Step 3: Add Unit Tests for Lyrics Parsing & Timing Resolution
- File: `app/src/test/java/com/deepeye/musicpro/domain/lyrics/LyricsRepositoryTest.kt`
- Test synchronized lyric parsing, line searching, and fallback handling.

### Step 4: Version Bump, Build, Device Test & Release
- Bump `versionCode = 30079`, `versionName = "3.0.1.69"`.
- Run `./gradlew --no-daemon testDebugUnitTest`.
- Run `./gradlew --no-daemon assembleDebug`.
- Sideload onto Realme RMX3945, test live lyrics sheet, and capture screenshot.
- Commit, tag `v3.0.1.69`, and push to GitHub.

## Risks & Tradeoffs
- **100ms Coroutine CPU Usage**: Negligible on modern multi-core devices (MT6835); coroutine only polls during active playback (`isPlaying == true`) and pauses when paused/stopped.
