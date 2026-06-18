# DeepEye Music Pro — History Engine (STAGE 7)

This document specifies the database models, query behaviors, backup/restore serializers, and transaction strategies of the History persistence layer in DeepEye Music Pro.

---

## 1. Database Entities & Relationships

The history subsystem is backed by Room (SQLite) storing five key entities to record interactions:

### Search History (`search_history`)
Stores raw user queries to auto-populate instant suggestions during searches:
* `query` (Primary Key): The query string.
* `timestamp`: Creation time.
* `source`: `local` or `youtube`.
* `result_type`: `song` or `video`.

### Playback History (`playback_history`)
Tracks listening history, allowing calculation of completion rates:
* `id` (Auto-increment PK)
* `media_id`: Track identifier.
* `title`, `artist`, `album`, `artwork_uri`
* `played_at`: Timestamp.
* `play_duration_ms`: Total active play time in milliseconds (excluding pauses).
* `total_duration_ms`: Length of the track.
* `completion_percent`: Derived ratio (`play_duration_ms / total_duration_ms`).
* `source`: Playback track origin (`local` or `youtube`).

### Video History (`video_history`)
Tracks watch progress for resume hooks:
* `video_id` (Primary Key)
* `title`, `thumbnail_uri`
* `position_ms`: Paused seek position.
* `duration_ms`: Length of the video.
* `watched_at`: Timestamp.
* `completion_percent`: Derived ratio (`position_ms / duration_ms`).

### Download History (`download_history`)
Logs download transitions to track asset availability:
* `download_id` (Primary Key)
* `media_id`, `title`
* `status`: `COMPLETED`, `FAILED`, `DELETED`.
* `timestamp`

### Queue History (`queue_history`)
Saves a snapshot of the active playlist queue for clean session restores on startup:
* `id` (Primary Key, hardcoded to 1 for singleton storage)
* `queue_json`: Gson-serialized queue array.
* `current_index`: Active queue cursor.
* `timestamp`

---

## 2. Backup & Restore Specifications

### JSON Schema Output:
```json
{
  "searches": [
    {
      "query": "Lofi hip hop",
      "timestamp": 1780695029412,
      "source": "youtube",
      "result_type": "video"
    }
  ],
  "playbacks": [
    {
      "id": 1,
      "media_id": "youtube_track_id",
      "title": "Chilled Beats",
      "artist": "Lofi Artist",
      "album": "",
      "played_at": 1780695123902,
      "play_duration_ms": 120000,
      "total_duration_ms": 180000,
      "completion_percent": 0.66667,
      "source": "youtube"
    }
  ],
  "videos": [],
  "downloads": []
}
```

### Import Restore Strategy:
* Backup imports use a JsonParser to split tables asynchronously.
* Each entity insertion is wrapped in individual `try-catch` blocks inside the iterator loop, guaranteeing that single entity parsing errors do not halt the entire restoration process.
