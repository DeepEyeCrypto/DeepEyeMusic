# Plan: Music Screen Redesign, Landscape Adaptive TopBar & Card Geometry Refinements

## Goal
Redesign and refine `MusicScreen.kt` and `CardGeometry.kt` with an adaptive ultra-compact landscape TopBar, enhanced card geometry (136dp+), glassmorphic styling, and seamless multi-rail viewport visibility.

## Current Context & Forensics
- On landscape mode (720x1604px / 360x802dp), the non-scrolling `Scaffold` `topBar` currently consumes ~180dp (50% of the screen height) with stacked Header, Search Bar, Account Banner, and Segmented Tabs.
- This leaves only ~150dp for the scrollable list, clipping carousels and showing only 1 chopped row.
- `CardGeometry.Music.minWidth` is set to `104.dp`, resulting in tiny, cramped album cards where song titles and artist names are severely truncated.

## Proposed Architecture
1. **Adaptive Compact Landscape TopBar**:
   - When in landscape (`orientation == Configuration.ORIENTATION_LANDSCAPE` or `maxHeight < 500.dp`), fold the Title, Search Pill, Segmented Tabs, and Refresh action into a single sleek horizontal Row (~46dp height).
   - In portrait mode, preserve the multi-row stack.
   - Frees up +130dp of vertical viewport on landscape devices, displaying 2-3 full carousel rows simultaneously.
2. **Enhanced Music Card Geometry**:
   - Update `CardGeometry.Music.minWidth` from `104.dp` to `136.dp` with `titleMaxLines = 2` on larger widths.
   - Polish `PersonalizedMusicCard` with dynamic neon glow borders, gradient overlays, and rounded micro-interaction play buttons.
3. **Typography & Badge Polish**:
   - Modernize section headers with semantic category icons (`Icons.Rounded.Whatshot`, `Icons.Rounded.Subscriptions`, `Icons.Rounded.History`).
   - Clean up duration pills and cache badges.

## Step-by-Step Implementation Tasks

### Task 1: Update Card Geometry in `CardGeometry.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/theme/CardGeometry.kt`
- Update `MusicCard` dimensions:
  - `minWidth = 136.dp`
  - `cornerRadius = 12.dp`
  - `contentPadding = 8.dp`
  - `titleMaxLines = 2`

### Task 2: Implement Adaptive Landscape TopBar in `MusicScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/music/MusicScreen.kt`
- Detect landscape mode: `val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE`
- In landscape:
  - Combine `"Music"` title + compact search bar + segmented pill tabs + refresh button into one unified 46dp top bar.
- In portrait:
  - Keep full-width search bar and standard tabs.

### Task 3: Redesign `PersonalizedMusicCard` & `PersonalizedSectionView` in `MusicScreen.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/music/MusicScreen.kt`
- Refine card background with multi-stop neon gradient border (`ElectricViolet` + `NeonCyan`).
- Add high-contrast typography hierarchy and smooth spring press feedback.
- Add category icons to section headers.

### Task 4: Run Unit Tests & Build Debug APK
- **Command**:
  ```bash
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon testDebugUnitTest
  cd /Users/enayat/Documents/DeepEyeMusicPro && ./gradlew --no-daemon assembleDebug
  ```

### Task 5: Sideload on Realme Device, Capture Screenshot & Verify
- Sideload APK:
  ```bash
  adb logcat -c && adb install -r /Users/enayat/Documents/DeepEyeMusicPro/app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.deepeye.musicpro/.MainActivity
  ```
- Capture live screenshot and verify zero logcat crashes.

### Task 6: Release & Push `v3.0.1.58`
- Bump `versionCode = 30068`, `versionName = "3.0.1.58"` in `app/build.gradle.kts` and `AppChangelog.kt`.
- Commit, tag `v3.0.1.58`, and push to GitHub.

## Risks & Mitigations
- **Risk**: Search bar click targets in compact mode.
  - **Mitigation**: Ensure search pill maintains a minimum 40dp height and 48dp touch target.
