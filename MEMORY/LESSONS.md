# LESSONS LEARNED
- Android 12+ strict foreground restrictions require `ContextCompat.startForegroundService` wrapped in a `try-catch` when triggered by background playback transitions (autoplay/skip).
- Imported but unused vector icons are strong clues for missing UI features (e.g., DSP GraphicEq icons).
