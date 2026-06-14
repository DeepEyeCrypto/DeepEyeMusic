# DOWNLOAD_SYSTEM

## Architecture
The download system will leverage `androidx.media3.exoplayer.offline.DownloadManager` to guarantee ExoPlayer compatibility and efficient background downloading.

## UI Integration
- **Download Action Button**: Located on the Now Playing screen and track lists.
- **States**:
  1. `NOT_DOWNLOADED`: Outlined icon.
  2. `DOWNLOADING`: Circular determinate progress ring overlay.
  3. `DOWNLOADED`: Solid filled accent icon.
  4. `FAILED`: Red retry icon.

## Backend Flow
1. User taps "Download".
2. `SourceResolver` fetches the highest quality M4A/WebM direct URL.
3. URL is passed to Media3 `DownloadRequest`.
4. `DownloadService` executes the download in a foreground service (ensuring it survives app death).
5. Upon completion, the local cache index is updated.
6. When the user plays the track later, `PlayerController` detects the cached copy and constructs a `CacheDataSource`, resulting in zero network usage and instant playback.
