# PLAYBACK HISTORY SPEC

## Database
- Expand `PlayEvent` to `playback_history`
- Track: title, artist, artwork, played_at, play_duration, completion_percent

## Rules
- Every playback over 5 seconds is recorded.