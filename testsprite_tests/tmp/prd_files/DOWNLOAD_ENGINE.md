# DeepEye Music Pro — Download Engine (STAGE 8)

This document specifies the background down loaders, storage structures, cache mappings, and metadata tags retention for DeepEye Music Pro.

---

## 1. Background Download Management

Downloads are executed asynchronously using Android **WorkManager** to ensure tasks complete even if the app process is killed or placed in background standby:

* **DownloadWorker**: Extends Hilt-injected `CoroutineWorker`.
* **Enqueue Pipeline**: Handles single track requests or batch playlist downloads using a sequential task queue.
* **State Updates**: Publishes progress updates (percentages, download speed, and current task state) using database tracking.
* **Network Constraints**: Work requests enforce connection constraints (`NetworkType.UNMETERED` or generic internet depending on settings).

---

## 2. Storage & Directory Structure

Downloaded files are stored in the app's isolated external media directory to ensure clean filesystem hygiene:

```
/Android/media/com.deepeye.musicpro/downloads/
  │
  ├─── .artwork/ (Cached album art image thumbnails)
  │
  ├─── [VideoID].mp3 (Remote resolved audio-only files)
  │
  └─── [VideoID].mp4 (Remote resolved video files)
```

### Cache Configuration:
* File names match the primary track/video ID to guarantee uniqueness.
* Artwork images are compressed to WebP format to minimize disk usage.

---

## 3. Metadata Preservation (ID3 Tags)

To keep files fully indexable by standard system players and local scanners, the download manager writes standard metadata tags before finishing tasks:
* **Audio Tags (MP3)**: Writes ID3v2 tags (Title, Artist, Album name, Genre, Year) and embeds the raw artwork byte array into the APIC (Attached Picture) tag frame.
* **Video Tags (MP4)**: Writes MP4 metadata atoms containing tracking details.
* Local Room indexes update `isDownloaded = true` status immediately upon completion.
