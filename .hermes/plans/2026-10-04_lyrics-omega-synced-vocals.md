# HERMES Lyrics-Omega Protocol (Synced Vocals Engine) Implementation Plan

## Goal
Implement a real-time, karaoke-synchronized lyrics engine for DeepEyeMusicPro by fetching InnerTube lyrics payloads (`/youtubei/v1/next` -> `browseId` -> `/youtubei/v1/browse`), parsing timed text into `LyricsLine(timestampMs, text)`, and rendering a kinetic auto-scrolling Compose UI synced to ExoPlayer's timeline.

---

## Current Context & Architecture
- **Existing Files**:
  - `InnerTubeRemoteClient.kt`: Handles `/youtubei/v1/next` and `/youtubei/v1/browse` requests with OAuth fallback.
  - `LyricsRepository.kt`: Currently provides stub/mock lyrics.
  - `PlayerViewModel.kt`: Observes `currentItem` and holds `_currentLyrics: MutableStateFlow<Lyrics?>`.
  - `LyricsBottomSheet.kt`: UI component for displaying lyrics.
- **Proposed Architecture**:
  1. **InnerTube Extraction Pipeline**:
     - Request `/youtubei/v1/next` -> Parse `tabs` array to extract the "Lyrics" `browseId` (`MPLY...`).
     - Request `/youtubei/v1/browse` with `browseId` -> Parse `musicTimedLyricsRenderer` (timed lyrics lines with millisecond timestamps) or `musicDescriptionShelfRenderer` (static plain text fallback).
     - Multi-tier Fallback: If InnerTube has no lyrics, query LRCLIB public API (`https://lrclib.net/api/get?artist_name=...&track_name=...&duration=...`) for synced LRC text.
  2. **ExoPlayer Sync State Flow**:
     - `PlayerViewModel` tracks `currentPlaybackPositionMs` and computes `activeLyricIndex` reactively.
     - Seek interactions on lyric lines trigger `playerController.seekTo(timestampMs)`.
  3. **Kinetic Typography & Auto-Scrolling UI**:
     - Active line: `scale(1.08f)`, full opacity (1.0f), prominent font weight, and Monet dynamic accent glow.
     - Inactive lines: `scale(0.94f)`, dimmed alpha (0.40f).
     - Smooth auto-scroll keeping the active line centered in viewport with user scroll override detection.

---

## Step-by-Step Implementation Tasks

### Task 1: InnerTube Lyrics Extraction (`InnerTubeRemoteClient.kt` & `LyricsRepository.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`
- Add `fetchLyrics(videoId: String): Lyrics?`:
  - Request `POST /youtubei/v1/next` with `ANDROID_MUSIC_CONTEXT` and `videoId`.
  - Traverse `tabs` -> locate `tabRenderer` where `title` equals "Lyrics" or endpoint contains `browseId` starting with `"MPLY"`.
  - If found, request `POST /youtubei/v1/browse` with `"browseId": browseId`.
  - Parse `musicTimedLyricsRenderer.timedLyricsData` -> extract timed lines:
    ```json
    { "lyricLine": "Hello darkness", "cueRange": { "startTimeMilliseconds": 12000, "endTimeMilliseconds": 15000 } }
    ```
  - Fallback to `musicDescriptionShelfRenderer.description.runs` if timed lyrics are absent.
- **File**: `app/src/main/java/com/deepeye/musicpro/domain/lyrics/LyricsRepository.kt`
  - Wire `getLyricsForTrack(videoId: String, title: String, artist: String, durationSec: Long)` to call `InnerTubeRemoteClient` and fallback to `LrcParser`.

### Task 2: LRCLIB Fallback & Universal LRC Parser Integration
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/lyrics/LrclibClient.kt`
  - Create OkHttp-based client fetching `https://lrclib.net/api/get?track_name={title}&artist_name={artist}&duration={duration}`.
  - Parse synced LRC string using `LrcParser.parseSyncedLyrics(syncedLyrics)`.

### Task 3: ViewModel & ExoPlayer Sync Engine (`PlayerViewModel.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/PlayerViewModel.kt`
  - When `currentItem` changes, invoke `lyricsRepository.getLyricsForTrack(...)` and emit to `_currentLyrics`.
  - Expose `activeLyricIndex: StateFlow<Int>` derived from `playerState.map { it.position }` matching the highest `timestampMs <= currentPosition`.
  - Expose `fun seekToLyric(timestampMs: Long) = playerController.seekTo(timestampMs)`.

### Task 4: Kinetic Typography & Auto-Scrolling UI (`LyricsBottomSheet.kt`)
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/LyricsBottomSheet.kt`
  - Update line item composable:
    - Apply `animateFloatAsState` on scale (`if (isActive) 1.08f else 0.94f`) and alpha (`if (isActive) 1f else 0.40f`).
    - Apply Monet `accentColor` / `labelColor` styling to active text.
  - In `LaunchedEffect(activeLyricIndex)`:
    - Auto-scroll `lazyListState.animateScrollToItem(index, scrollOffset = -viewportHeight / 3)` when user is not manually dragging.
  - Tap on any lyric line calls `onSeekTo(line.timestampMs)`.

---

## Verification & Validation Plan
1. **Compilation Check**:
   ```bash
   ./gradlew compileDebugKotlin -x test
   ```
2. **Unit Tests**:
   - Write `LrcParserTest.kt` & `InnerTubeLyricsParserTest.kt` validating JSON payload decoding and LRC timestamp calculations.
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. **On-Device Manual Test**:
   - Install APK on Realme RMX3945 (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
   - Play a song (e.g. YouTube Music track with synced lyrics) -> Open Lyrics Sheet -> Verify lyrics auto-scroll in sync with vocal audio.
   - Tap a line in verse 2 -> Verify ExoPlayer seeks instantly to that timestamp and lyrics highlight updates without stutter.

---

## Risks & Tradeoffs
- **Rate Limiting**: InnerTube browse endpoints are lightweight and use existing client context; LRCLIB is only queried as secondary fallback.
- **Scroll Contention**: Manual user touch gestures pause auto-scrolling for 3 seconds to allow reading before snapping back to the active line.
