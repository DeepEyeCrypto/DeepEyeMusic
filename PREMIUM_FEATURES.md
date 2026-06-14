# PREMIUM_FEATURES

## Roadmap
These are the flagship features that elevate the app from a basic player to an ultra-premium product.

1. **Synchronized Lyrics**: Fetch LRC data from public APIs (e.g., Lrclib) and sync it to ExoPlayer's `currentPosition`. UI should blur the background and auto-scroll text.
2. **Sleep Timer**: A simple bottom sheet allowing users to set a countdown or "Stop after this track" flag, which pauses ExoPlayer and kills the service.
3. **Audio Quality Selector**: Give users choice (Low, Normal, High) which maps to YouTube itag resolutions (e.g., itag 140 for 128kbps AAC, itag 251 for Opus).
4. **Smart Shuffle**: Implement an algorithm that doesn't just randomize, but prevents same-artist clustering.
5. **Play Statistics**: Local Room database to track play counts, generating a "Recently Played" and "Most Played" dynamic playlist.
