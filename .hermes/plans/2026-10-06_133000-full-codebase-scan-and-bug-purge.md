# 🛡️ PLAN: FULL CODEBASE SCAN & ZERO-BUG PURGE (MANTIS AUDIT)

**Document ID:** `.hermes/plans/2026-10-06_133000-full-codebase-scan-and-bug-purge.md`  
**Status:** READY TO EXECUTE  

---

## 1. Goal
Execute a comprehensive codebase-wide bug purge across all architectural layers (Remote Network Parsers, Stream Extractors, ExoPlayer Controllers, and Compose UI State Locals), eliminating all Kotlin compiler type mismatch warnings, unsafe forced unboxings (`!!`), deprecated APIs, and potential NullPointerException vectors.

---

## 2. Audit Findings & Root-Cause Matrix

| Category | File Path | Root Cause | Fix Strategy |
|---|---|---|---|
| **Type Mismatches (Warnings)** | `AuthenticatedYouTubeClient.kt` (10 instances) | `optString(key, null)` passes `Nothing?` to Java `optString(String, String)` | Use `optString(key, "").ifBlank { null }` |
| **Deprecated API** | `SettingsScreen.kt:841` | `Icons.Filled.Login` is deprecated | Replace with `Icons.AutoMirrored.Filled.Login` |
| **NPE Crash Risk** | `SmartTubeInnertubeExtractor.kt:654, 691, 728` | `val resolved = streamUrl(it)!!` forced unbox | Replace with safe let `streamUrl(it)?.takeIf { it.isNotBlank() } ?: return null` |
| **NPE Crash Risk** | `SmartTubeSourceRefreshUseCase.kt:88, 93` | `targetVFormat!!.streamUrl` forced unbox | Replace with safe `targetVFormat?.streamUrl ?: effectiveUrl` |
| **NPE Crash Risk** | `SearchScreen.kt:97` | `LocalHazeState.current!!` | Store in local val `val hazeState = LocalHazeState.current` and null-check |
| **NPE Crash Risk** | `DeepEyeVideoPlayerOverlay.kt:592, 595, 599` | `seekOsd!!`, `zoomOsd!!` | Bind to smart-castable local variables |
| **NPE Crash Risk** | `HybridPlayerCard.kt:947, 951` | `seekOsd!!` | Bind to smart-castable local variable |
| **NPE Crash Risk** | `RankingBottomSheet.kt:69` | `currentUserRank!!` | Bind `val rank = currentUserRank` with null-check |
| **NPE Crash Risk** | `ChatAuthScreen.kt:286` | `errorMessage!!` | Bind `val error = errorMessage` with null-check |

---

## 3. Step-by-Step Implementation Tasks

### Task 1: Eliminate Java Type Mismatch Warnings in `AuthenticatedYouTubeClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- **Changes**:
  - Lines 485-486, 501-502, 558-559, 588, 600-601, 668: Replace `optString(..., null)` with `optString(...).ifBlank { null }` or `optString(..., "")`.

### Task 2: Replace Deprecated Icon in `SettingsScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/settings/SettingsScreen.kt`
- **Changes**:
  - Replace `Icons.Default.Login` with `Icons.AutoMirrored.Filled.Login`.

### Task 3: Harden Stream Extractors Against NPEs
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/extractor/SmartTubeInnertubeExtractor.kt`
  - `app/src/main/java/com/deepeye/musicpro/player/recovery/SmartTubeSourceRefreshUseCase.kt`
- **Changes**:
  - Eliminate `!!` on `streamUrl(it)` and format objects.

### Task 4: Harden Compose UI State Locals & Overlay OSDs
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/search/SearchScreen.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/overlay/DeepEyeVideoPlayerOverlay.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/components/HybridPlayerCard.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/ranking/RankingBottomSheet.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/chat/ChatAuthScreen.kt`

### Task 5: Version Bump, Compilation & Hardware Verification
- **Files**: `app/build.gradle.kts`, `app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt`
- **Actions**:
  - Bump to version `3.0.1.85` (`versionCode = 30095`).
  - Execute `./gradlew assembleDebug`.
  - Install APK on Realme RMX3945 via ADB and verify zero warnings/crashes.

---

## 4. Verification Commands
```bash
./gradlew compileDebugKotlin -x test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.deepeye.musicpro/com.deepeye.musicpro.MainActivity
```
