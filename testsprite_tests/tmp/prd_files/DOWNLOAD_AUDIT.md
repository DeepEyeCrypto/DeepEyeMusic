# Download Subsystem Audit Report (DOWNLOAD_AUDIT.md)

This audit evaluates the reliability, file integrity, duplicate checks, and storage safety of the offline download system.

---

## 1. Subsystem Architecture

The app uses Media3's native caching and offline framework:
* **Engine**: Powered by `androidx.media3.exoplayer.offline.DownloadManager`.
* **Database**: `DownloadHistoryEntity` in Room logs all finished downloads.
* **Storage Provider**: Local scoped storage (`context.getExternalFilesDir(null)`), bypassing runtime legacy storage permission prompts on Android 13+ (API 33+).
* **Caching Layer**: `CacheDataSource` resolves local file streams instantly, providing zero-buffer local playback.

---

## 2. UI Status States & Actions

* **Download Button**: Outlined button on cards/Player screen.
* **Determinate Progress**: Circular determinate progress ring overlays during downloading.
* **Pause / Resume**: Supports pausing the DownloadManager download queue and resuming the active threads.
* **Deletion Action**: Deleting a download deletes the cache chunk using `DownloadService.sendRemoveDownload` and removes the database log.

---

## 3. Integrity & Safety Controls

| Checklist | Verdict | Validation Details |
| :--- | :---: | :--- |
| **File Integrity** | `PASS` | Media3 checks byte length and content range matching. Partial/corrupt files are automatically flagged as `FAILED` for retry. |
| **Duplicate Prevention** | `PASS` | `MusicDownloadManager` queries `downloadIndex` before queue insertion. Tapping download on an already downloaded track returns early. |
| **Storage Scoped Access** | `PASS` | Downloads are saved directly under the application sandbox directory, conforming to modern Android scoped storage constraints. |
