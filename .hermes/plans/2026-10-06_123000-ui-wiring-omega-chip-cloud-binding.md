# 📺 PLAN: HERMES UI-WIRING-OMEGA PROTOCOL (CHIP TO CLOUD BINDING)

**Document ID:** `.hermes/plans/2026-10-06_123000-ui-wiring-omega-chip-cloud-binding.md`  
**Status:** READY TO EXECUTE  

---

## 1. Goal
Wire all horizontal category chips ("Subscriptions", "Liked", "History", "Music", "Movies", "Gaming", "News") in YouTube Cinema / Landscape Video Hub directly to live TVHTML5 authenticated cloud endpoints (`FEsubscriptions`, `VLLL`, `FEhistory`, `FEmusic_home`), ensuring immediate StateFlow reactivity, active cyan glow states, and 1:1 LazyVerticalGrid rendering.

---

## 2. Current Context & Architectural Design
1. **SSOT ViewModel State (`YouTubeViewModel.kt`)**:
   - `uiState: StateFlow<YouTubeUiState>` tracks `selectedCategory`, `videos`, `isLoading`, `error`, and `hasAuth`.
   - Category selection clears the stale grid immediately and triggers targeted InnerTube TVHTML5 fetches with OAuth Bearer tokens.
2. **Cyberpunk UI Ribbon (`YouTubeScreen.kt`)**:
   - `LazyRow` renders category chips with adaptive highlights (`Color(0xFF00E5FF)` neon cyan border, frosted glass surface, and bold typography).
3. **Zero-Mock Policy**:
   - Grid renders real cloud data. Empty states indicate genuine account state rather than fabricating synthetic fallback tracks.

---

## 3. Step-by-Step Implementation Tasks

### Task 1: ViewModel Intent & Direct TVHTML5 Cloud Mapping
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeViewModel.kt`
- **Actions**:
  1. Verify `selectCategory(category: String)` updates `selectedCategory` and immediately clears old videos:
     ```kotlin
     fun selectCategory(category: String) {
         _uiState.update { it.copy(selectedCategory = category, videos = emptyList(), isLoading = true, error = null, hasMore = false) }
         loadCategory(category)
     }
     ```
  2. Ensure `loadCategory` maps categories 1:1 to Authenticated TVHTML5 methods:
     - `"Liked"` -> `authClient.getLikedVideos()` (LL Playlist)
     - `"History"` -> `authClient.getHistory()` (`FEhistory`)
     - `"Subscriptions"` -> `authClient.getSubscriptionsFeed()` (`FEsubscriptions`)
     - `"Music"` -> `authClient.fetchSupermix()` (`FEmusic_home`)
     - `"Movies"` -> `authClient.getMoviesFeed()`
     - `"Gaming"` -> `authClient.getGamingFeed()`
     - `"News"` -> `authClient.getNewsFeed()`

### Task 2: Compose Chip Wiring & Active Visual Feedback
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeScreen.kt`
- **Actions**:
  1. Ensure `LazyRow` items bind `onClick = { viewModel.selectCategory(tab) }`.
  2. Apply dynamic glowing borders (`BorderStroke(1.5.dp, Color(0xFF00E5FF))` when selected vs `1.dp, Color(0x22FFFFFF)` when idle).
  3. Ensure active chip text uses `FontWeight.Bold` with neon cyan tint.

### Task 3: LazyVerticalGrid Reactivity & Skeleton Shimmers
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeScreen.kt`
- **Actions**:
  1. `VideoGridContent` renders `VideoCardSkeleton` items while `isLoading == true`.
  2. When loaded, renders `LazyVerticalGrid(columns = GridCells.Adaptive(CardGeometry.Video.minWidth))` iterating directly over `uiState.videos`.
  3. Genuine error/empty states if account category has no records.

### Task 4: Compilation, Deployment & Hardware Logcat Verification
- **Actions**:
  1. Bump version in `app/build.gradle.kts` and `AppChangelog.kt`.
  2. Execute `./gradlew assembleDebug`.
  3. Install APK on physical Realme RMX3945 via ADB.
  4. Verify 0 logcat errors during chip clicks and smooth grid updates.

---

## 4. Verification Commands
```bash
./gradlew compileDebugKotlin -x test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.deepeye.musicpro/com.deepeye.musicpro.MainActivity
```
