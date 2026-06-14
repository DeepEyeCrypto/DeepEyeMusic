# DeepEye Music Pro — Version Changelog (STAGE 15)

This document tracks all features added, improvements implemented, and deprecated legacy code retired in DeepEye Music Pro.

---

## [Unreleased] — v2.0-Unified

### Added
* **Native Video Rendering**: Integrated `androidx.media3.ui.PlayerView` Compose wrapper for native video playback in the YouTube/Music tab.
* **Dynamic Resolution Capping**: Wired native track selection capping parameter settings in ExoPlayer for video quality selector (1080p, 720p, 480p).
* **Audiophile USB DAC Preset**: Added a new built-in preset styled for clean dynamics and headroom on external USB audio interfaces.
* **USB DAC Route Auto-detection**: Hardened `AudioRouteReceiver.kt` using `AudioManager.GET_DEVICES_OUTPUTS` checks to auto-apply the DAC preset when plugged in.
* **Defensive DSP Release**: Added try-catch auto-release handler in `DSPEngine.kt` to clean up partially allocated effects on session initialization failures.
* **History JSON Backup Restoring**: Implemented fully asynchronous JSON backup imports inside `HistoryRepository.kt`.

### Changed
* **Single Playback Engine**: Unified both video and audio streams directly under the single ExoPlayer instance, eliminating the legacy muted WebView and audio intercept proxy patterns.
* **Simplified MediaSession Service**: Removed the complex video mock overrides inside `ForwardingPlayer` wrapper, letting Media3 natively handle state synchronization.
* **Standardized Notification Controls**: Replaced custom manual notifications with native Media3 system media notifications for both audio and video modes.

### Removed
* **Retired YouTubeVideoPlayer WebView**: Deleted the legacy WebView-based player implementation entirely.
* **Retired StreamTeeProxy & TeeProxyInputStream**: Removed the unused local proxy server code.
* **Retired Unused Player Toggles**: Removed dead focus abandonment and player muting methods inside `PlayerController.kt`.
