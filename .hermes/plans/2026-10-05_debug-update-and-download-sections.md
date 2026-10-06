# Plan: Comprehensive Forensics, Debugging & Enhancement for Update and Download Sections

## Goal
Diagnose and resolve all critical bugs, progress desynchronizations, offline playback failures, and missing UX controls in the **Update Section** (OTA GitHub release engine, progress bar scaling, error feedback, package installer permissions) and **Download Section** (real-time progress tracking, offline local URI playback, download deletion, storage statistics).

---

## Current Context & Forensics
1. **Update Section**:
   - **Progress Desync**: `AutoUpdateManager` emits progress as a normalized float `0.0f..1.0f` (`bytesCopied / contentLength`), but `UpdateDialog.kt` multiplies it inappropriately (`${state.progress}%` and `state.progress / 100f`), causing the progress percentage to display as `0.45%` instead of `45%` and rendering the progress bar frozen.
   - **Silent Error Failures**: `SettingsScreen.kt` and `UpdateDialog.kt` do not present descriptive error states when GitHub API rate-limits (HTTP 403) or times out.
   - **Hardcoded Version Toast**: `SettingsScreen.kt` displays a hardcoded version string `"DeepEye Music Pro is up to date (v3.0.1.35)"` instead of evaluating `BuildConfig.VERSION_NAME`.
   - **Android 14–16 Package Installer Permissions**: When `canRequestPackageInstalls()` returns false, opening settings leaves the dialog in an unrecoverable state without an auto-retry or re-install action upon return.

2. **Download Section**:
   - **Offline Playback Failure**: `DownloadsScreen.kt` dispatches `MediaItem.Remote` without supplying the persisted `localPath` (`content://...` or `file://...`). In offline mode, ExoPlayer fails to play downloaded media because it attempts an unnecessary network request.
   - **Missing Download Progress & Size Metrics**: `MusicDownloadManager.kt` maintains a binary state without reporting byte transfer progress or total content lengths. `DownloadsScreen.kt` only displays a generic indeterminate spinner.
   - **Missing Delete / Storage Management**: Users cannot delete downloaded tracks from `DownloadsScreen.kt` to reclaim storage.

---

## Architecture & Proposed Approach
1. **Update Engine Refactoring**:
   - Standardize `UpdateState.Downloading(val progress: Float, val bytesDownloaded: Long = 0L, val totalBytes: Long = 0L)` across `AutoUpdateManager` and `UpdateDialog.kt` where `progress` represents `0.0f..1.0f` and UI computes `(progress * 100).roundToInt()`.
   - Add proper `UpdateState.Error` visual feedback with retry buttons and dynamic `BuildConfig.VERSION_NAME` reporting in `SettingsScreen.kt`.
   - Add fallback release asset lookup (matching both `app-release.apk` and `DeepEyeMusicPro-*.apk`).

2. **Download Engine Refactoring**:
   - Introduce `DownloadProgressState(val item: MediaItem, val progress: Float, val bytesDownloaded: Long, val totalBytes: Long)` in `MusicDownloadManager.kt`.
   - Thread progress callbacks through `DownloaderHelper.downloadWithResume`.
   - Map `localPath` from `DownloadEntity` into `LibraryItem.localPath` and instantiate `MediaItem.Remote(..., streamUri = Uri.parse(item.localPath))` so downloaded tracks play 100% offline.
   - Add delete download actions in `DownloadsViewModel`, `LibraryRepository`, and `DownloadsScreen.kt` with swipe/trash controls.

---

## Step-by-Step Tasks

### Task 1: Fix Update Progress Bar & Percentage in `UpdateDialog.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/update/UpdateDialog.kt`
- **Action**: Fix progress formatting from `"${state.progress}%"` to `"${(state.progress * 100).roundToInt()}%"` and `LinearProgressIndicator(progress = { state.progress.coerceIn(0f, 1f) })`.
- **Add**: File size transfer readout `${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)}`.

### Task 2: Enhance `AutoUpdateManager.kt` Error Handling, Asset Parsing & State Emits
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/update/AutoUpdateManager.kt`
- **Action**:
  - Update `UpdateState.Downloading` to include `bytesDownloaded: Long` and `totalBytes: Long`.
  - Fix asset matching regex to prioritize `app-release.apk` or `DeepEyeMusicPro-*.apk`.
  - Enhance `installApk` error recovery and handle permission re-checks.

### Task 3: Fix `SettingsScreen.kt` Version Reporting & Update Error Dialog
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/settings/SettingsScreen.kt`
- **Action**:
  - Replace hardcoded toast string with `"DeepEye Music Pro is up to date (v${BuildConfig.VERSION_NAME})"`.
  - Show error toast or banner when `uiState.updateState is UpdateState.Error`.

### Task 4: Upgrade `DownloaderHelper.kt` & `MusicDownloadManager.kt` with Progress Streaming
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/player/download/DownloaderHelper.kt`
  - `app/src/main/java/com/deepeye/musicpro/player/download/MusicDownloadManager.kt`
- **Action**:
  - Add `onProgress: (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit` callback to `downloadWithResume`.
  - Update `_activeDownloads` in `MusicDownloadManager` to expose progress states.

### Task 5: Add Local Path Mapping to `LibraryItem` & Enable 100% Offline Playback
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/domain/model/library/LibraryItem.kt`
  - `app/src/main/java/com/deepeye/musicpro/domain/repository/library/LibraryRepository.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/downloads/DownloadsScreen.kt`
- **Action**:
  - Add `val localPath: String? = null` to `LibraryItem`.
  - In `LibraryRepository.kt`, map `localPath = localPath` inside `DownloadEntity.toLibraryItem()`.
  - In `DownloadsScreen.kt`, instantiate `MediaItem.Remote(..., streamUri = item.localPath?.let { Uri.parse(it) })` for instant offline playback.

### Task 6: Add Delete Download & Storage Footprint Management in `DownloadsScreen.kt`
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/downloads/DownloadsScreen.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/downloads/DownloadsViewModel.kt`
  - `app/src/main/java/com/deepeye/musicpro/domain/repository/library/LibraryRepository.kt`
- **Action**:
  - Add `deleteDownload(videoId: String)` in `DownloadsViewModel`.
  - Add trash/delete action button on completed download cards in `DownloadsScreen.kt`.

### Task 7: Unit Tests & Build Verification
- **Files**:
  - `app/src/test/java/com/deepeye/musicpro/data/source/remote/update/AutoUpdateManagerTest.kt`
  - `app/src/test/java/com/deepeye/musicpro/player/download/DownloaderHelperTest.kt`
- **Action**: Verify version comparisons, progress math, and unit tests with `./gradlew --no-daemon testDebugUnitTest`.
- **On-Device Run**: Compile debug APK, install on Realme RMX3945, and verify zero crashes with ADB logcat.

---

## Risks, Tradeoffs & Open Questions
- **Scoped Storage URI Persistence**: MediaStore URIs (`content://media/external/audio/media/...`) on Android 10+ must be checked for existence (`resolver.openInputStream`) in case files were deleted externally by the user.
- **GitHub API Rate Limits**: GitHub unauthenticated API allows 60 requests/hr per IP. The app should gracefully display remaining rate-limit retry cooldowns rather than generic errors.
