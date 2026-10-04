# YouTube Cinema Home Feed Fix Plan

## Goal
Fix the `Error: No items found in your account for Home` failure in YouTube Cinema screen by adding recursive TVHTML5/InnerTube JSON parsing and robust feed resolution in `YouTubeViewModel`, `InnerTubeRemoteClient`, and `AuthenticatedYouTubeClient`.

## Current Context & Root Cause
- **Symptom**: User's screenshot shows YouTube Cinema displaying `Error: No items found in your account for Home` with empty video rails.
- **Root Cause**:
  1. `YouTubeViewModel.kt` treats `"Home"` as a strict-only account tab when `hasAuth == true`. If `authClient.getHomeFeed()` returns empty or encounters non-music TV format JSON, it terminates early with a hard error instead of resolving the main feed.
  2. `InnerTubeRemoteClient.kt`'s `parseBrowseSections` misses several TV client (`TVHTML5`) layouts like `tvBrowseRenderer`, `horizontalListRenderer`, and `tileRenderer`.
  3. `AuthenticatedYouTubeClient.kt`'s `getHomeFeed()` lacks multi-tier resolution if `browseMain("FEwhat_to_watch")` returns empty for a newly authorized device token.

---

## Proposed Architecture & Solution
1. **Multi-Layout Recursive Parser (`InnerTubeRemoteClient.kt`)**:
   - Traverse all JSON sections in `browseMain` and `browseMusic` handling `tvBrowseRenderer`, `horizontalListRenderer`, `shelfRenderer`, `compactVideoRenderer`, `tileRenderer`, `videoRenderer`, and `musicResponsiveListItemRenderer`.
2. **Multi-Tiered Feed Resolver (`AuthenticatedYouTubeClient.kt`)**:
   - `getHomeFeed()` resolves in cascading order:
     `browseMain("FEwhat_to_watch")` -> `browseMusic("FEmusic_home")` -> `browse("FEwhat_to_watch")` -> `browse("FEtrending")`.
3. **Graceful Home Resolution (`YouTubeViewModel.kt`)**:
   - Allow `"Home"` and `"Music"` categories to load global/trending video streams if personal TV shelves return zero items, while keeping `"History"`, `"Liked"`, `"Subscriptions"`, `"Watch Later"` isolated to account data.

---

## Step-by-Step Implementation Tasks

### Task 1: Enhance `InnerTubeRemoteClient.kt` Multi-Layout Parser
**File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`
- Add recursive item scanner for TV / Web renderers (`tvBrowseRenderer`, `horizontalListRenderer`, `gridVideoRenderer`, `tileRenderer`, `compactVideoRenderer`, `videoRenderer`).

### Task 2: Multi-Tiered Home Feed in `AuthenticatedYouTubeClient.kt`
**File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- Update `getHomeFeed()` to seamlessly cascade:
  ```kotlin
  suspend fun getHomeFeed(): List<HomeVideoItem> {
      val token = getValidAccessToken()
      if (token != null) {
          val authResult = innerTubeClient.browseMain("FEwhat_to_watch")
          if (authResult.isNotEmpty()) return authResult
          val musicHome = innerTubeClient.browseMusic("FEmusic_home")
          if (musicHome.isNotEmpty()) return musicHome
      }
      val publicHome = browse("FEwhat_to_watch")
      if (publicHome.isNotEmpty()) return publicHome
      return getTrending()
  }
  ```

### Task 3: Graceful Home Feed Handling in `YouTubeViewModel.kt`
**File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeViewModel.kt`
- In `loadCategory(category: String)`:
  - If `category == "Home"`, resolve `authClient.getHomeFeed()` and fall back to `baseQuery = "trending music"` / `fetchVideos(finalQuery)` if empty, so the Home screen is always populated with content.
  - Keep strict isolation (`error = "No items found in your account for $category"`) for strictly personal categories (`"Subscriptions"`, `"History"`, `"Liked"`, `"Watch Later"`).

### Task 4: Autonomous Build & On-Device Validation
- Run `./gradlew --no-daemon assembleDebug`
- Sideload onto Realme RMX3945: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Launch `MainActivity` and verify YouTube Cinema Home tab renders full video grid.

---

## Verification
- Compile check: `./gradlew --no-daemon compileDebugKotlin -x test` -> `BUILD SUCCESSFUL`
- Device test: `adb shell am start -n com.deepeye.musicpro/.MainActivity` -> check that "Home" loads video tiles without error.