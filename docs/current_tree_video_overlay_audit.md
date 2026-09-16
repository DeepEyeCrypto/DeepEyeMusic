# DeepEye Music Pro — Current-Tree Video Player Overlay Audit

**Date:** September 15, 2026
**Target Architecture:** Android / Kotlin / Jetpack Compose / Media3 ExoPlayer
**Package:** `com.deepeye.musicpro`

---

## 1. Current Player Entry Point
- **Screen:** `com.deepeye.musicpro.ui.player.NowPlayingScreen`
- **Video Sub-Layout:** `VideoNowPlayingLayout` inside `NowPlayingScreen.kt` (lines 952–1440)
- **Container / Embedded Player Card:** `com.deepeye.musicpro.ui.components.HybridPlayerCard`
- **Native Video Surface:** `com.deepeye.musicpro.ui.components.VideoPlayerView` wrapping Media3 `PlayerView` (with `AspectRatioFrameLayout.RESIZE_MODE_FIT`)

---

## 2. Current Playback State Source
- **Primary State:** `PlayerState` (`com.deepeye.musicpro.domain.model.PlayerState`) emitted as `StateFlow<PlayerState>` from `PlayerController.playerState` and exposed via `PlayerViewModel.playerState`.
- **ExoPlayer Instance:** Injected singleton `androidx.media3.exoplayer.ExoPlayer` managed by `PlayerController` (`playerController.player` / `viewModel.player`).
- **Video Details:** `StateFlow<VideoDetails?>` from `PlayerViewModel.videoDetails` (fetched via `YoutubeRemoteDataSource.getVideoDetails()`).
- **Playback Diagnostics:** `StateFlow<PlaybackDiagnostics>` from `PlayerController.diagnostics` / `PlayerViewModel.diagnostics`.
- **Active Formats:** `playerState.availableVideoFormats`, `playerState.availableAudioFormats`, `playerState.selectedVideoFormat`, `playerState.selectedAudioFormat`.
- **Quality Preset:** `playerState.qualityPreset` (`QualityPreset.AUTO`, `MAX`, `P1080`, `P720`, `P480`, `P360`, `AUDIO_ONLY`).
- **Buffer Profile:** `playerState.bufferProfile` (`BALANCED`, `AGGRESSIVE`, `LOW_LATENCY`, `DATA_SAVER`).

---

## 3. Current Action Callbacks
- **Transport Controls:**
  - `viewModel.togglePlayPause()` / `playerController.togglePlayPause()`
  - `viewModel.seekTo(positionMs)` / `playerController.seekTo(positionMs)`
  - `viewModel.next()` / `playerController.next()`
  - `viewModel.previous()` / `playerController.previous()`
  - `viewModel.toggleRepeat()` / `playerController.toggleRepeat()`
  - `viewModel.toggleShuffle()` / `playerController.toggleShuffle()`
  - `viewModel.setPlaybackSpeed(speed)` / `playerController.setPlaybackSpeed(speed)`
- **Feedback / Taste:**
  - `viewModel.likeTrack(liked: Boolean)`
  - `viewModel.dislikeTrack()`
  - `viewModel.blockTrack()`
- **Format & Quality:**
  - `viewModel.setQualityPreset(preset)` / `playerController.setQualityPreset(preset)`
  - `viewModel.setVideoFormat(format)` / `playerController.setVideoFormat(format)`
  - `viewModel.setAudioFormat(format)` / `playerController.setAudioFormat(format)`
  - `viewModel.setBufferProfile(profile)` / `playerController.setBufferProfile(profile)`
- **Audio & Subtitle Enhancements:**
  - `viewModel.setAudioBoostLevel(level)` (`0`, `3`, `6`, `12` dB boost)
  - `viewModel.toggleSubtitles()` (`subtitlesEnabled: StateFlow<Boolean>`)
  - `viewModel.toggleStatsForNerds()` (`showStatsForNerds: StateFlow<Boolean>`)
  - `viewModel.startSleepTimer(minutes)` / `cancelSleepTimer()`
- **Fullscreen & Orientation:**
  - `LocalFullscreenMode.current.enter(forceLandscape = true/false)`
  - `LocalFullscreenMode.current.exit()`
  - `LocalFullscreenMode.current.toggle()`

---

## 4. Current PiP Implementation
- **Activity:** `MainActivity` with `android:supportsPictureInPicture="true"` and `PipEngine`.
- **CompositionLocal:** `LocalPipMode` (`com.deepeye.musicpro.ui.LocalPipMode`) providing `isInPipMode: Boolean`.
- **Behavior:** Overlays and controls automatically hide when `isInPipMode == true` to present clean video.

---

## 5. Current Queue Implementation
- **State:** `playerState.queue` (`ImmutableList<MediaItem>`), `playerState.currentIndex`.
- **Actions:**
  - `viewModel.setQueue(items, startIndex)`
  - `viewModel.moveMediaItem(fromIndex, toIndex)`
  - `viewModel.removeMediaItem(index)`
  - `viewModel.seekToMediaItem(index)`
- **Autoplay Recommendations:** `viewModel.autoplayState` (`AutoplayState`) with recommendations and upcoming video queue.

---

## 6. Current Subtitle / Audio / Quality Implementation
- **Quality Dialog:** `com.deepeye.musicpro.ui.player.quality.HqPlaybackSheet` with tabs: `VIDEO`, `AUDIO`, `BUFFER`, `STATS`.
- **Subtitles:** Tracked via `viewModel.subtitlesEnabled` and toggled on player track selection parameters.
- **Audio Boost:** Native multiplier on ExoPlayer volume (`setAudioBoostLevel`).
- **Stats for Nerds HUD:** `PlaybackDiagnostics` overlay rendering real-time codec, resolution, bitrate, dropped frames, buffer health, and network bandwidth.

---

## 7. Missing Actions / UI Enhancements for SmartTube-style TV OSD
- **Landscape/TV-optimized OSD Overlay:**
  - D-Pad / Remote control key navigation (`onKeyEvent`, directional DPAD focus, `FocusRequester`).
  - Sleek semi-transparent gradient overlays (top title bar with channel logo, bottom seekbar/scrubber with timestamps, buffer bar, sponsor segment visual markers).
  - Quick action bar: Play/Pause, Next/Previous, Seek -10s/+10s, Quality/HQ Settings, Audio Tracks/Boost, Subtitles CC, Like/Dislike, Queue Drawer, Speed Preset, Stats for Nerds HUD toggle, Repeat/Shuffle, Sleep Timer.
  - Auto-hide countdown timer with touch/remote reset.
  - Smooth animated transitions for OSD visibility.
  - Clean video player surface integration without altering backend audio/video pipeline.

---

## 8. Protected Contracts (DO NOT MODIFY)
- `PlayerController.kt`
- `VideoViewModel.kt` / `PlayerViewModel.kt` core business logic
- `MusicPlayerService.kt`
- `MediaSession` & `ForwardingPlayer`
- `QueueManager.kt`
- `PlaybackRecoveryCoordinator.kt` / `StreamRecoveryPolicy.kt`
- SmartTube / Innertube / NewPipe extraction & resolver layers
- `DSPEngine.kt` / `VisualizerEngine.kt`
- Room DB, DataStore preferences, PiP contracts

---

## 9. UI Files Allowed to Change / Add
- New overlay components in `app/src/main/java/com/deepeye/musicpro/ui/player/overlay/`
- Integration in `app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt` or `NowPlayingScreen.kt`
