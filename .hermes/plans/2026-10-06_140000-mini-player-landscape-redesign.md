# 🎛️ PLAN: MINI PLAYER LANDSCAPE REDESIGN & HARDENING PROTOCOL

**Document ID:** `.hermes/plans/2026-10-06_140000-mini-player-landscape-redesign.md`  
**Status:** READY TO EXECUTE  

---

## 1. Goal
Redesign and debug the Mini Player specifically for Landscape and TV/Tablet displays, transforming it into an ultra-sleek, floating cyberpunk audio capsule (adaptive height 64.dp in landscape vs 88.dp in portrait, bounded max-width 640.dp, and refined tactile media controls) to prevent grid occlusion and UI stretching.

---

## 2. Current Context & Architectural Flaws
1. **Vertical Space Consumption**: In landscape orientation (360–420dp total height), the current 88.dp mini player occupies ~25% of the screen, obscuring content grids.
2. **Horizontal Over-Stretching**: Spanning `fillMaxWidth()` on wide landscape screens (900+ dp) creates excessive empty whitespace between the track title and playback controls.
3. **Control Ergonomics**: Landscape users need compact playback buttons (Previous, Play/Pause, Next, and Expand to Fullscreen) with dedicated tactile haptics.

---

## 3. Step-by-Step Implementation Tasks

### Task 1: Adaptive Landscape Tokens & Sizing
- **Files**:
  - `app/src/main/java/com/deepeye/musicpro/ui/player/MiniPlayerContent.kt`
  - `app/src/main/java/com/deepeye/musicpro/ui/player/AnchoredMiniPlayer.kt`
- **Design**:
  - Detect orientation via `val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE`.
  - Set collapsed height: `if (isLandscape) 64.dp else 88.dp`.
  - Max width constraint in landscape: `Modifier.widthIn(max = 680.dp).align(Alignment.BottomCenter)`.

### Task 2: Redesign `PlayerHeaderRow` for Landscape
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/MiniPlayerContent.kt`
- **Changes**:
  - Compact album thumbnail: 40.dp (with mini bass pulse ring).
  - Streamlined typography: Single-line title + artist with continuous horizontal marquee.
  - Controls: Tactile Prev (36.dp), Play/Pause (40.dp), Next (36.dp), and Fullscreen Expand button (36.dp).
  - Glowing bottom progress line with micro-height (2.dp) along the pill edge.

### Task 3: Sync `AnchoredMiniPlayer.kt` Draggable Anchors
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/player/AnchoredMiniPlayer.kt`
- **Changes**:
  - Update `collapsedAnchor` calculation using orientation-aware `miniPlayerHeight`:
    ```kotlin
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val miniPlayerHeight = if (isLandscape) 64.dp else 88.dp
    val collapsedAnchor = containerHeightPx - bottomBarHeightPx - with(density) { miniPlayerHeight.toPx() }
    ```

### Task 4: Synchronize Content Padding in `DeepEyeMusicApp.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/DeepEyeMusicApp.kt`
- **Changes**:
  - Update `miniPlayerPadding` animation target to match the landscape height (`64.dp` in landscape, `88.dp` in portrait).

### Task 5: Version Bump, Build & Hardware Verification
- **Files**: `app/build.gradle.kts`, `app/src/main/java/com/deepeye/musicpro/updates/AppChangelog.kt`
- **Actions**:
  - Bump version to `3.0.1.86` (`versionCode = 30096`).
  - Execute `./gradlew assembleDebug`.
  - Install APK on Realme RMX3945 via ADB.
  - Verify landscape rotation, swipe-to-expand, and playback controls.

---

## 4. Verification Commands
```bash
./gradlew compileDebugKotlin -x test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.deepeye.musicpro/com.deepeye.musicpro.MainActivity
```
