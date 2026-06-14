import os

specs = {
    "HISTORY_AUDIT.md": "# HISTORY AUDIT\n\n## Missing Persistence\n- Search History (Local/Remote)\n- Video Watch Progress\n- Queue State Persistence\n- Download History\n\n## Duplicate Storage\n- None identified.\n\n## Broken Resume Logic\n- ExoPlayer loses position across app restarts.",
    "SEARCH_HISTORY_SPEC.md": "# SEARCH HISTORY SPEC\n\n## Database\n- Table: `search_history`\n- Fields: query, timestamp, source, result_type\n\n## Features\n- Instant Save on Search\n- Auto Suggestions via Room DAO\n- Search Recall on Focus",
    "PLAYBACK_HISTORY_SPEC.md": "# PLAYBACK HISTORY SPEC\n\n## Database\n- Expand `PlayEvent` to `playback_history`\n- Track: title, artist, artwork, played_at, play_duration, completion_percent\n\n## Rules\n- Every playback over 5 seconds is recorded.",
    "VIDEO_HISTORY_SPEC.md": "# VIDEO HISTORY SPEC\n\n## Features\n- Continue Watching Carousel\n- Resume Playback exactly where stopped\n\n## Database\n- Table: `video_history`\n- Fields: video_id, position, duration, watched_at, completion_percent",
    "RECENTLY_PLAYED_SPEC.md": "# RECENTLY PLAYED SPEC\n\n## Sections\n- Today\n- Yesterday\n- This Week\n- This Month\n\n## Logic\n- Query `playback_history` with Date filtering.",
    "CONTINUE_LISTENING_SPEC.md": "# CONTINUE LISTENING SPEC\n\n## Restore Logic\n- Restore exact song, position, and queue snapshot.\n- Store in SharedPreferences or `session_state` table on `onPause`.",
    "CONTINUE_WATCHING_SPEC.md": "# CONTINUE WATCHING SPEC\n\n## Restore Logic\n- Same as Continue Listening, specialized for Video items.\n- Display in Home Screen as a horizontal carousel.",
    "DOWNLOAD_HISTORY_SPEC.md": "# DOWNLOAD HISTORY SPEC\n\n## Tracking\n- Store completed, failed, and deleted downloads.\n- Table: `download_history`",
    "FAVORITES_HISTORY_SPEC.md": "# FAVORITES HISTORY SPEC\n\n## Tracking\n- Maintain timestamp of when an item was liked.\n- Table: Expand `UserFeedback` to include timestamp.",
    "QUEUE_HISTORY_SPEC.md": "# QUEUE HISTORY SPEC\n\n## Snapshotting\n- Save exact state of `QueueManager` (items, index) to JSON in Room/DataStore on every change.\n- Allow one-tap restore.",
    "ANALYTICS_SPEC.md": "# ANALYTICS SPEC\n\n## Calculation Engine\n- Most Played Songs, Artists, Albums\n- Listening/Watch Time aggregator\n- Top Genres",
    "HISTORY_UI_SPEC.md": "# HISTORY UI SPEC\n\n## Design\n- Glassmorphism, Material 3, Dynamic Colors\n- Hub layout with sections: Searches, Playback, Videos, Favorites, Downloads",
    "DATABASE_SCHEMA.md": "# DATABASE SCHEMA\n\n## Entities\n- search_history\n- playback_history\n- video_history\n- favorites_history\n- download_history\n- queue_history\n- analytics_cache",
    "HISTORY_PERFORMANCE_PLAN.md": "# PERFORMANCE PLAN\n\n## Requirements\n- No UI Lag (Use Flow & Dispatchers.IO)\n- Pagination (Paging3)\n- Indexes on timestamps and IDs.",
    "BACKUP_RESTORE_SPEC.md": "# BACKUP & RESTORE SPEC\n\n## Formats\n- JSON export of Room DB.\n- AES Encryption option.",
    "PRIVACY_HISTORY_SPEC.md": "# PRIVACY CONTROLS SPEC\n\n## Features\n- Clear specific history domains (Search, Video, etc.)\n- 'Clear All' master switch.",
    "HISTORY_QA_REPORT.md": "# QA VALIDATION PLAN\n\n## Scenarios\n- Search > Close App > Reopen > Search Restored\n- Play > Kill App > Reopen > Continue Listening appears."
}

for filename, content in specs.items():
    with open(filename, 'w') as f:
        f.write(content)

print("Generated 17 specs.")
