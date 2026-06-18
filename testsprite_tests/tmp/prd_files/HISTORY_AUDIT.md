# Search & Playback History Audit Report (HISTORY_AUDIT.md)

This audit evaluates the database architecture, schema integrity, and backup/restore capabilities of the history subsystem.

---

## 1. Persistent Storage Schema (Room DB)

All history categories are stored under local Room database tables:

* **`search_history`**: Stores queries, timestamps, source type (local vs remote), and result type.
* **`playback_history`**: Stores tracks played, timestamps, play duration in milliseconds, total track duration, completion percentage, and audio source.
* **`video_history`**: Stores watch progress (position, completion percentage) for remote YouTube videos.
* **`queue_history`**: Persists the active queue as a JSON array (`QueueSnapshotEntity`) with the current track index, using a `1-second debounce` in `PlayerController` to avoid I/O bottlenecks.

---

## 2. Validation Checkpoints

* **Search History Persistence**: `PASS`. Search queries are stored instantly upon search trigger.
* **Playback History Log**: `PASS`. Playback events are recorded in `playback_history` once a song plays for more than `500 ms`.
* **Quick-Skip Analytics**: `PASS`. Tracks skipped in under `10 seconds` trigger a `recordQuickSkip` entry to feed the user taste profile recommendations.
* **Video Watch Progress**: `PASS`. Videos played store their exact positions, allowing resume-on-click functionality.
* **Export & Import Backup**: `PASS`. `HistoryRepository.kt` implements robust serialization/deserialization:
  - `exportToJson()` outputs full schemas into a single compact JSON file.
  - `importFromJson()` safely parses inputs and imports them using `insert` transactions on conflicting rows.