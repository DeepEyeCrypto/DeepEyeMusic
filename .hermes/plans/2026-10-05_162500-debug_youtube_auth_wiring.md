# Plan: Make YouTube Connected Account Wiring 100% Authentic

## Goal
Eliminate silent fallbacks to generic "trending" searches for authenticated users, bypass overly aggressive regex filters that drop 85% of legitimate personalized music tracks, and accurately display the user's authentic personalized Home, Music, and Category feeds directly from the InnerTube API.

---

## Current Context & Forensics
The user noticed that the "Music" and other category tabs on the YouTube/NetMirror screen feel disconnected from their actual Google/YouTube account ("10-15% wired").
- **Root Cause 1 (The Trapdoor Fallback):** When `loadCategory("Music")` calls `authClient.getMusicFeed()`, it fetches authentic `FEmusic_home`, `liked`, and `history` from the InnerTube API. However, it filters ALL results through `MusicFilter.isMusicTrack()`. 
- **Root Cause 2 (The Aggressive Filter):** `MusicFilter.isMusicTrack()` requires either the channel name to contain words like "music", "vevo", "records" OR the title to contain "song", "audio", etc. A legitimate song like "Starboy" by "The Weeknd" fails this check and is entirely dropped.
- **Root Cause 3 (The Dummy Search):** Because 90% of the authentic feed is dropped by the filter, `accountMusic` returns empty. `YouTubeViewModel.kt` sees an empty list, skips the `return@launch` block (since "Music" is not in the explicit strict set of account-only tabs), and silently falls through to `fetchVideos("official music video songs hits")`. The user ends up seeing generic 90s/Bollywood hits instead of their personal taste profile, making the integration feel fake.

---

## Architecture & Proposed Approach
1. **Trust InnerTube Endpoints:** Tracks retrieved from YouTube Music endpoints (`FEmusic_home` and `browseLikedMusic`) are guaranteed to be music. We must bypass `MusicFilter.isMusicTrack()` for these endpoints.
2. **Patch `MusicFilter.kt`:** Soften the `isMusicTrack` requirement so it doesn't aggressively drop track names and artist names that lack explicit "music" keywords.
3. **Hard-Wire Authentic Feeds (`YouTubeViewModel.kt`):** If a user is authenticated (`hasAuth == true`), we must explicitly prevent falling back to generic searches for the "Music", "Home", "Movies", and "Gaming" tabs. If the authentic fetch returns empty, we display an empty state or gracefully append the TasteProfile, ensuring the user is never stealthily redirected to a generic logged-out search.

---

## Step-by-Step Tasks

### Task 1: Trust Music Endpoints in `AuthenticatedYouTubeClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/AuthenticatedYouTubeClient.kt`
- **Action**: In `getMusicFeed()`, pass the `musicHome` and `liked` lists directly into `accountMusic` without running them through `MusicFilter.isMusicTrack()`. Only apply `isMusicTrack()` to the generic `history` list (since regular history contains non-music videos).
- **Detail**:
  ```kotlin
  val seen = mutableSetOf<String>()
  for (item in musicHome + liked) {
      if (!item.isShort && seen.add(item.id)) accountMusic.add(item)
  }
  for (item in history) {
      if (!item.isShort && MusicFilter.isMusicTrack(item.title, item.channelName, item.duration, item.isShort) && seen.add(item.id)) {
          accountMusic.add(item)
      }
  }
  ```

### Task 2: Relax Aggressive Regex in `MusicFilter.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/MusicFilter.kt`
- **Action**: Fix `isMusicTrack()` so it acts primarily as an exclusion filter. If `EXCLUDE_PATTERN` (e.g., news, gaming, vlog) is hit, return `false`. If the channel is a known music channel or ends with " - Topic", return `true`. Otherwise, default to `true` if it's within standard music durations (60s to 12 minutes).

### Task 3: Enforce 100% Authentic Feeds in `YouTubeViewModel.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/ui/youtube/YouTubeViewModel.kt`
- **Action**: Modify `loadCategory()` to strictly respect `hasAuth`:
  - Enhance the `if (hasAuth && ...)` condition after `authItems` parsing to include `"Music"`, `"Home"`, `"Movies"`, and `"Gaming"`.
  - If the auth fetch returns empty, set the `error` state to `"Your $category feed is empty or failed to load."` rather than falling through to `baseQuery` searches. (Or keep a transparent fallback that explicitly tells the user `"Showing recommendations because your $category feed is empty"`).

### Task 4: Fix Auth Retry Resiliency in `InnerTubeRemoteClient.kt`
- **File**: `app/src/main/java/com/deepeye/musicpro/data/source/remote/youtube/InnerTubeRemoteClient.kt`
- **Action**: Ensure `browseMusic()` and `browseMain()` efficiently map unauthorized 401 exceptions without bubbling up crashes that wipe the feed out entirely.

---

## Tests / Validation
- Run unit tests with `./gradlew --no-daemon testDebugUnitTest`.
- Execute `./gradlew --no-daemon assembleDebug`.
- **On Device**: Ensure `Music` tab immediately loads the actual YouTube Music home feed of the connected account instead of falling back to "official music video songs hits" searches.

## Risks, Tradeoffs, and Open Questions
- If a user just created their Google Account and has literally zero history or liked music, `FEmusic_home` might return sparse or empty results natively. Showing an "Empty Feed" error might feel like a bug to them. To mitigate: if the authenticated feed is genuinely empty, populate it with Taste Profile recommendations using the `HomeFeedRepository`.