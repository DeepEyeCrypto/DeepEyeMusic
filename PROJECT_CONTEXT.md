# PROJECT CONTEXT
## Conversation Summary
App is an ultra-low latency, zero-lag media player with hybrid ExoPlayer (audio) and WebView (video) capabilities.
Recent updates: Fixed Android 12+ background service crash (ForegroundServiceStartNotAllowedException) and restored DSP to the MagicNavigationBar.
## User Goals
Editor-grade typing responsiveness. Premium Apple/Linear level UI aesthetics.
## Technical Constraints
Must use ExoPlayer for background audio. WebView used only for video rendering and must be muted when possible. Zero-latency UI.
