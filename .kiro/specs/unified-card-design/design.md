# Unified Card Design Bugfix Design

## Overview

DeepEyeMusicPro renders media cards across at least nine distinct surfaces — For You, YouTube tab, Music tab, Video Hub, Quick Picks, New Releases, Supermix, Discover Mix, Because You Liked, Liked Songs, History Recently Played, and History Continue Watching — each using a different card composable with a different visual style, a different subset of metadata, and a different grid layout. The "For You" `VideoCard` is the declared reference standard: 16:9 thumbnail, quality badge, duration badge, channel avatar with fallback, channel name, title (2 lines), and a metadata row of views and upload date. The fix consolidates every card surface onto a single `UnifiedMediaCard` composable built from that reference, applies the blank-data hiding fix (hide "0 views"/"No views"/"—" instead of showing placeholders), and switches every section to a 2-column grid layout (`GridCells.Fixed(2)` for vertically-scrolling screens, chunked `Row` pairs for rails embedded in a parent `LazyColumn`, and `LazyRow` with 2-card width only for rails that must remain horizontal).

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug — a card or section deviates from the unified For You design, omits full metadata, does not use a 2-column grid, or displays a blank placeholder for missing data
- **Property (P)**: The desired behavior when the bug condition holds — the fixed card renders the unified design, shows available metadata, hides unavailable fields, and lays out in a 2-column grid
- **Preservation**: Existing For You card quality, thumbnail fallback, avatar fallback, tap navigation, quality/format badges, real-data display, loading skeletons, empty states, and watch-progress indicators that must remain unchanged
- **UnifiedMediaCard**: The single composable that replaces all existing card composables, based on the For You `VideoCard` reference standard with the blank-data hiding fix applied
- **UnifiedCardData**: A new data class that provides the fields `UnifiedMediaCard` needs; all existing domain models are mapped to it via extension functions
- **VideoCard**: The reference-standard composable in `ui/homehub/VideoCard.kt` — 16:9 thumbnail, quality badge, duration badge, avatar with fallback, channel name, title, metadata row
- **SmartTubeVideoCard**: The YouTube-tab card in `ui/youtube/YouTubeScreen.kt` — neon glow border, different badge set, gradient avatar — to be replaced
- **PersonalizedMusicCard**: The Music-tab card in `ui/music/MusicScreen.kt` — square 1:1 thumbnail, gradient background — to be replaced
- **ModernVideoCard**: The Video-Hub card in `ui/home/HomeScreen.kt` — no avatar/views/date — to be replaced
- **HomeMusicRail**: The inline card in `ui/homehub/HomeHubScreen.kt` — thumbnail + title + artist only — to be replaced
- **LibraryListItem**: The Liked-Songs row in `ui/library/LibraryListScreens.kt` — 1-column list, 56dp artwork — to be replaced
- **PlaybackHistoryRow**: The History-Recently-Played row in `ui/history/HistoryScreen.kt` — 1-column list, 64dp artwork — to be replaced
- **VideoHistoryCard**: The History-Continue-Watching card in `ui/history/HistoryScreen.kt` — wraps `SmartTubeVideoCard` with blank channel — to be replaced
- **HomeVideoItem**: Domain model in `domain/model/home/HomeFeedModels.kt` — has id, title, channelName, thumbnailUrl, channelAvatarUrl, viewCount, uploadDate, duration (Long seconds), isLive — the richest model, directly mappable
- **PersonalizedFeedItem**: Domain model in `domain/model/personalization/PersonalizedModels.kt` — has id, title, artist, artworkUrl, durationMs, playCount — missing viewCount, uploadDate, channelAvatarUrl
- **VideoItem**: Domain model in `domain/recommendation/RecommendationModels.kt` — has videoId, title, artist, channelId, duration (String) — missing viewCount, uploadDate, channelAvatarUrl, thumbnailUrl
- **HomeMusicItem**: Domain model in `domain/model/home/HomeFeedModels.kt` — has id, title, artist, thumbnailUrl, duration, playCount — missing viewCount, uploadDate, channelAvatarUrl
- **LibraryItem**: Domain model in `domain/model/library/LibraryItem.kt` — has id, title, subtitle, artworkUrl, artist, videoId, playCount — missing viewCount, uploadDate, channelAvatarUrl, duration
- **PlaybackHistoryEntity**: Room entity in `data/db/HistoryEntities.kt` — has mediaId, title, artist, artworkUri, totalDurationMs — missing viewCount, uploadDate, channelAvatarUrl
- **VideoHistoryEntity**: Room entity in `data/db/HistoryEntities.kt` — has videoId, title, thumbnailUri, durationMs, completionPercent — missing channelName, viewCount, uploadDate, channelAvatarUrl
- **CardGeometry**: Single source of truth for card geometry in `ui/theme/CardGeometry.kt` — provides aspect ratios, corner radii, min widths, spacing tokens
- **evenCarouselCardWidth**: Utility in `ui/util/ResponsiveUtils.kt` that sizes cards to fill a viewport edge-to-edge — used for horizontal rails

## Bug Details

### Bug Condition

The bug manifests when any card surface deviates from the unified For You design, omits full metadata, does not render in a 2-column grid, or displays a blank placeholder for genuinely unavailable data. Each existing card composable violates at least one of these: `SmartTubeVideoCard` uses a neon glow border, a different badge set (4K/HDR/ATMOS), and a gradient avatar instead of the reference design. `PersonalizedMusicCard` uses a square 1:1 thumbnail, a gradient background, and omits views/date/avatar/duration. `ModernVideoCard` omits avatar/channel/views/date entirely. `HomeMusicRail` shows only thumbnail + title + artist. `LibraryListItem` uses a 1-column list with 56dp artwork. `PlaybackHistoryRow` uses a 1-column list with 64dp artwork. `VideoHistoryCard` passes blank channel data. `VideoCard` and `SmartTubeVideoCard` both display "0 views"/"No views" and "—" for missing data instead of hiding the field.

**Formal Specification:**
```
FUNCTION isBugCondition(section, card)
  INPUT: section of type Section, card of type Card
  OUTPUT: boolean

  // Returns true when the card or section deviates from the unified standard
  RETURN card.design != UNIFIED_FOR_YOU_DESIGN
      OR NOT card.showsFullMetadata
      OR NOT section.usesTwoColumnGrid
      OR card.showsBlankPlaceholderForMissingData
END FUNCTION
```

Where:
- `card.design != UNIFIED_FOR_YOU_DESIGN` is true for SmartTubeVideoCard (neon glow border, different badges), PersonalizedMusicCard (square thumbnail, gradient), ModernVideoCard (no avatar/metadata), HomeMusicRail (minimal card), LibraryListItem (list row), PlaybackHistoryRow (list row), VideoHistoryCard (blank channel)
- `NOT card.showsFullMetadata` is true for PersonalizedMusicCard, ModernVideoCard, HomeMusicRail, LibraryListItem, PlaybackHistoryRow — each omits views, upload date, channel avatar, or duration
- `NOT section.usesTwoColumnGrid` is true for the YouTube tab (Adaptive columns), Music tab (LazyRow), Liked (1-column LazyColumn), History Recently Played (1-column LazyColumn), and any rail using 4 columns in landscape
- `card.showsBlankPlaceholderForMissingData` is true for VideoCard ("0 views", "—") and SmartTubeVideoCard ("No views", "—")

### Examples

- **YouTube tab**: `SmartTubeVideoCard` renders a neon glow gradient border, 4K/HDR/ATMOS badge ribbons, and a gradient sweep-gradient avatar — visually inconsistent with the For You `VideoCard`. When `viewCount == 0`, it shows "No views". When `uploadDate` is null, it shows "—". The grid uses `GridCells.Adaptive(CardGeometry.Video.minWidth)` which can produce 3+ columns on wide screens instead of exactly 2.

- **Music tab**: `PersonalizedMusicCard` renders a 1:1 square thumbnail with a gradient background and no views, upload date, channel avatar, or duration badge. The section uses a `LazyRow` horizontal rail instead of a 2-column grid.

- **Video Hub (HomeScreen)**: `ModernVideoCard` renders a 16:9 thumbnail with a duration badge but no channel avatar, no channel name, no views, and no upload date. The rail uses `LazyRow` with `evenCarouselCardWidth` instead of a 2-column grid.

- **Quick Picks / New Releases / Supermix / Discover Mix / Because You Liked (HomeHubScreen)**: `HomeMusicRail` renders only a thumbnail, title, and artist — no views, upload date, channel avatar, or duration. The rail uses chunked rows with 2 columns in portrait but 4 in landscape.

- **Liked Songs (LibraryListScreens)**: `LibraryListItem` renders a 1-column `LazyColumn` with a 56dp square artwork, title, and subtitle — no card design, no grid, no full metadata.

- **History Recently Played (HistoryScreen)**: `PlaybackHistoryRow` renders a 1-column `LazyColumn` with a 64dp square artwork, title, artist, and a play button — no card design, no grid, no full metadata.

- **History Continue Watching (HistoryScreen)**: `VideoHistoryCard` wraps `SmartTubeVideoCard` with `channelName = ""`, producing a blank channel. It uses a `LazyRow` with 280dp-wide cards instead of a 2-column grid.

- **Edge case — For You with partial data**: `VideoCard` receives a `HomeVideoItem` where `viewCount == 0` and `uploadDate == ""`. The metadata row renders `"Unknown channel • 0 views • —"`. Expected behavior: hide the views and date, showing only the channel name.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- For You card quality with real data: real view counts, upload dates, channel names, durations, and channel avatars must continue to render correctly
- Thumbnail fallback: when a thumbnail URL fails to load, the card must fall back to the ytimg thumbnail derived from the video ID (`https://i.ytimg.com/vi/{id}/hqdefault.jpg`)
- Avatar fallback: when `channelAvatarUrl` is blank, the avatar must fall back to the ytimg default thumbnail derived from the video ID (`https://i.ytimg.com/vi/{id}/default.jpg`)
- Card tap navigation: tapping a card must open the corresponding content (video or music playback) without changing the existing navigation behavior
- Quality/format badges: LIVE, HDR, 4K, and other badges must continue to display when the information is available
- Real-data display: sections with partial data (some items have full metadata, some do not) must show real data where available and hide only the missing fields per item
- Loading skeletons and empty states: existing skeleton and empty-state composables must continue to render
- Watch-progress indicators: the Continue Watching section must continue to show progress bars where progress data exists

**Scope:**
All inputs that do NOT involve a card or section deviating from the unified standard should be completely unaffected by this fix. This includes:
- The video player and its controls (PiP, stats for nerds, sponsorblock)
- Search bar and search suggestion behavior
- Navigation between tabs and screens
- Mini-player and dock rendering
- DSP/V4A panels
- AI prompt bar
- BTC ticker and settings dock

## Hypothesized Root Cause

Based on the bug description, the most likely root causes are:

1. **Independent Card Composables Per Screen**: Each screen defines its own card composable independently — `VideoCard`, `SmartTubeVideoCard`, `PersonalizedMusicCard`, `ModernVideoCard`, `HomeMusicRail` inline card, `LibraryListItem`, `PlaybackHistoryRow`, `VideoHistoryCard`. There is no shared card composable, so each evolved its own visual style, badge set, avatar styling, and metadata subset. No single design authority forced them to converge.

2. **Incomplete Data Models**: Several domain models (`PersonalizedFeedItem`, `VideoItem`, `HomeMusicItem`, `LibraryItem`, `PlaybackHistoryEntity`, `VideoHistoryEntity`) lack the fields the unified card needs — `viewCount`, `uploadDate`, `channelAvatarUrl`, or even `channelName`. Because each card composable consumed its own model directly, there was no pressure to add the missing fields; instead, the card simply omitted them.

3. **Placeholder Text for Missing Data**: `VideoCard` unconditionally adds `item.viewCount?.formatCountShort() ?: "0 views"` and `item.uploadDate ?: "—"` to its metadata row. `SmartTubeVideoCard` calls `formatViews(video.viewCount)` which returns `"No views"` for 0, and `video.uploadDate ?: "—"`. Neither conditionally hides the field when data is genuinely unavailable.

4. **Inconsistent Grid Layouts**: Each screen chose its own grid strategy — `GridCells.Adaptive(minWidth)` for the YouTube tab (variable column count), `LazyRow` for Music tab and Video Hub, chunked `Row` pairs with 2/4 columns for HomeHub rails (orientation-dependent), and 1-column `LazyColumn` for Liked and History. No screen enforces exactly 2 columns as a hard constraint.

5. **Blank Channel Name Passed Through**: `VideoHistoryCard` constructs `HomeVideoItem(channelName = "")` from `VideoHistoryEntity` which has no channel field. The empty string is passed directly to `SmartTubeVideoCard`, which already has a `cleanChannel` fallback for "." / "·" / blank, but the underlying issue is that the model lacks the field entirely. `VideoCard` uses `item.channelName ?: "Unknown channel"` which shows "Unknown channel" rather than a clean "DeepEye" fallback.

## Correctness Properties

Property 1: Bug Condition - Unified Card Design, Full Metadata, 2-Column Grid

_For any_ card surface (section, card) where the bug condition holds (the card design differs from the For You reference, the card omits full metadata, the section does not use a 2-column grid, or the card shows a blank placeholder for missing data), the fixed code SHALL render every card using the unified `UnifiedMediaCard` design (16:9 thumbnail, duration badge, quality badges, channel avatar with fallback, channel name, title up to 2 lines, metadata row of views and upload date where available), SHALL lay out every section in a 2-column grid (`GridCells.Fixed(2)` for vertically-scrolling screens, chunked `Row` pairs for rails embedded in a parent `LazyColumn`), and SHALL hide any genuinely unavailable field (view count, upload date) rather than display "0 views", "No views", or "—".

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10**

Property 2: Preservation - Existing Good Behavior

_For any_ card surface (section, card) where the bug condition does NOT hold (the For You card already renders the unified design with real data), the fixed code SHALL produce exactly the same visual output and behavior as the original code, preserving For You card quality with real data, thumbnail fallback behavior, avatar fallback behavior, card tap navigation, quality/format badges, real-data display where available, loading skeletons, empty states, and watch-progress indicators.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8**

## Fix Implementation

### Architecture: UnifiedCardData and UnifiedMediaCard

The fix introduces three new artifacts and refactors every existing card surface to use them:

**1. `UnifiedCardData` — a new data class** in `ui/components/UnifiedCardData.kt` that provides every field the unified card needs:

```kotlin
data class UnifiedCardData(
    val id: String,
    val title: String,
    val channelName: String,          // blank if unknown; card applies "DeepEye" fallback
    val thumbnailUrl: String,         // blank if unknown; card applies ytimg fallback
    val channelAvatarUrl: String,     // blank if unknown; card applies ytimg fallback
    val viewCount: Long?,             // null = hide; 0 = hide (treat as unavailable)
    val uploadDate: String?,          // null or blank = hide
    val durationSeconds: Long,        // 0 = show "0:00" (preserving existing behavior)
    val isLive: Boolean,
    val progressFraction: Float? = null,  // watch-progress bar; null = no bar
)
```

The `viewCount` and `uploadDate` are nullable so the card can distinguish "data exists" from "data unavailable" and hide the field when unavailable. A `viewCount` of `0` is treated as unavailable (hidden) to satisfy requirement 2.7.

**2. Mapper extension functions** — one per domain model, in `ui/components/UnifiedCardMappers.kt`:

```kotlin
// HomeVideoItem — direct mapping (already has all fields)
fun HomeVideoItem.toUnifiedCardData(): UnifiedCardData

// PersonalizedFeedItem — artist → channelName, artworkUrl → thumbnailUrl,
//   durationMs/1000 → durationSeconds, playCount → viewCount
fun PersonalizedFeedItem.toUnifiedCardData(): UnifiedCardData

// VideoItem (recommendation) — videoId → id, artist → channelName,
//   duration string parsed to seconds, thumbnail from ytimg hqdefault,
//   viewCount = null, uploadDate = null, channelAvatarUrl = "" (ytimg fallback)
fun VideoItem.toUnifiedCardData(): UnifiedCardData

// HomeMusicItem — artist → channelName, playCount → viewCount,
//   viewCount hidden when 0, uploadDate = null, channelAvatarUrl = ""
fun HomeMusicItem.toUnifiedCardData(): UnifiedCardData

// LibraryItem — artist/subtitle → channelName, artworkUrl → thumbnailUrl,
//   playCount → viewCount, videoId → ytimg fallback for thumbnail/avatar
fun LibraryItem.toUnifiedCardData(): UnifiedCardData

// PlaybackHistoryEntity — artist → channelName, artworkUri → thumbnailUrl,
//   totalDurationMs/1000 → durationSeconds, viewCount = null, uploadDate = null
fun PlaybackHistoryEntity.toUnifiedCardData(): UnifiedCardData

// VideoHistoryEntity — videoId → id, thumbnailUri → thumbnailUrl,
//   durationMs/1000 → durationSeconds, channelName = "" (DeepEye fallback),
//   completionPercent → progressFraction
fun VideoHistoryEntity.toUnifiedCardData(): UnifiedCardData
```

Each mapper derives a ytimg fallback thumbnail and avatar from the video ID when the source model lacks them: thumbnail → `https://i.ytimg.com/vi/{id}/hqdefault.jpg`, avatar → `https://i.ytimg.com/vi/{id}/default.jpg`. For models that have no video ID (`PersonalizedFeedItem`, `PlaybackHistoryEntity`), the artwork URL is used directly and the avatar is left blank (the card renders a placeholder circle).

**3. `UnifiedMediaCard` — the single composable** in `ui/components/UnifiedMediaCard.kt`, based on the For You `VideoCard` reference standard with the blank-data hiding fix applied:

```
FUNCTION UnifiedMediaCard(data, onClick, modifier, progressFraction)
  // Thumbnail (16:9 aspect ratio, CardGeometry.Video)
  //   AsyncImage with thumbnailUrl (blank → ytimg fallback from id)
  //   Bottom vignette gradient
  //   Quality badge (top-left): LIVE if isLive, else 4K/HDR/1080P deterministic by id hash
  //   Duration badge (bottom-right): formatDuration(durationSeconds), always visible
  //   Watch-progress bar (bottom-full) if progressFraction != null and > 0
  //
  // Metadata Row:
  //   Avatar (36dp circle): channelAvatarUrl (blank → ytimg default from id)
  //   Column:
  //     Title (2 lines, 15sp bold)
  //     Metadata row — build list of available parts only:
  //       - channelName (blank/"."/"·" → "DeepEye")
  //       - viewCount (only if > 0: formatCountShort)
  //       - uploadDate (only if non-blank and non-"—")
  //     Join with " • ", single line, ellipsized
END FUNCTION
```

The key fix vs. the existing `VideoCard`: the metadata row is built conditionally. Instead of `add(item.viewCount?.formatCountShort() ?: "0 views")`, it uses `if (viewCount != null && viewCount > 0) add(viewCount.formatCountShort())`. Instead of `add(item.uploadDate ?: "—")`, it uses `if (!uploadDate.isNullOrBlank() && uploadDate != "—") add(uploadDate)`. The channel name fallback uses the same logic as `SmartTubeVideoCard`: `trim().let { if (it == "." || it == "·" || it.isBlank()) "DeepEye" else it }`.

### Changes Required

Assuming our root cause analysis is correct, the following specific changes are required:

**File**: `ui/components/UnifiedCardData.kt` (NEW)
- Create the `UnifiedCardData` data class with all fields the unified card needs
- Include a companion factory for ytimg URL derivation: `ytimgThumbnail(id)` → `hqdefault`, `ytimgAvatar(id)` → `default`

**File**: `ui/components/UnifiedCardMappers.kt` (NEW)
- Create extension functions `toUnifiedCardData()` for each of the 7 domain model types
- Each mapper extracts the available fields and derives fallbacks from the video ID where possible

**File**: `ui/components/UnifiedMediaCard.kt` (NEW)
- Create the `UnifiedMediaCard` composable based on `VideoCard` with the blank-data hiding fix
- Reuse the existing `formatDuration()` and `formatCountShort()` extensions from `VideoCard.kt` (either move them to a shared utils file or keep them and import)
- Accept `progressFraction: Float?` parameter for watch-progress bars (used by Continue Watching and History)

**File**: `ui/homehub/VideoCard.kt`
- `VideoCard` itself becomes a thin wrapper calling `UnifiedMediaCard(item.toUnifiedCardData(), onClick)`
- The `formatDuration()` and `formatCountShort()` extensions remain available (move to a shared location or keep public)
- The For You rail in `HomeHubScreen` that currently calls `SmartTubeVideoCard` is switched to call `UnifiedMediaCard` via the `HomeVideoItem.toUnifiedCardData()` mapper

**File**: `ui/youtube/YouTubeScreen.kt`
- Replace `SmartTubeVideoCard` calls with `UnifiedMediaCard(video.toUnifiedCardData(), onClick)`
- Change `VideoGridContent` grid from `GridCells.Adaptive(CardGeometry.Video.minWidth)` to `GridCells.Fixed(2)`
- Keep the `SmartTubeVideoCard` function as a deprecated wrapper calling `UnifiedMediaCard` for backward compatibility (SearchScreen imports it)
- Remove or redirect the `formatViews` private function (no longer needed — the unified card handles blank-data hiding internally)
- Change 2: grid columns from Adaptive to Fixed(2)

**File**: `ui/music/MusicScreen.kt`
- Replace `PersonalizedMusicCard` calls with `UnifiedMediaCard(item.toUnifiedCardData(), onClick)`
- Replace `LazyRow` horizontal rail with chunked `Row` pairs (2 columns) inside the existing section `Column`, since the section is embedded in a parent `LazyColumn` (cannot nest `LazyVerticalGrid`)
- Each section renders its header, then `items.chunked(2).forEach { row -> Row { row.forEach { UnifiedMediaCard(...) } } }` with `Modifier.weight(1f)` per card and spacer padding for short rows
- Keep the overflow menu (Play Next, Add to Queue, Why This) by either adding an optional `actions` slot to `UnifiedMediaCard` or rendering it as an overlay — the unified card design stays consistent; the menu is a behavior layer on top

**File**: `ui/home/HomeScreen.kt`
- Replace `ModernVideoCard` calls with `UnifiedMediaCard(video.toUnifiedCardData(), onClick)`
- Replace `ModernRecommendationRow`'s `LazyRow` with chunked `Row` pairs (2 columns) inside each section, since sections are items in a parent `LazyColumn`
- Change `QuickGridItem` (the quick-play dense grid) to also use `UnifiedMediaCard` with 2-column chunked rows instead of the current `adaptiveColumnCount`
- Remove or redirect the `formatViews`-equivalent logic (the unified card handles it)

**File**: `ui/homehub/HomeHubScreen.kt`
- Replace `SmartTubeVideoCard` calls in `HomeVideoRail` with `UnifiedMediaCard(video.toUnifiedCardData(), onClick)`
- Replace `HomeMusicRail` inline card with `UnifiedMediaCard(music.toUnifiedCardData(), onClick)`
- Change both rails from orientation-dependent 2/4 columns to always 2 columns (chunked `Row` pairs)
- `HomeVideoRail`: `val columns = 2` (remove the `isLandscape` branch)
- `HomeMusicRail`: `val columns = 2` (remove the `isLandscape` branch)

**File**: `ui/homehub/ContinueWatchingRow.kt`
- Replace `ContinueWatchingCard` with `UnifiedMediaCard(video.toUnifiedCardData(), onClick, progressFraction = video.progressPercent)`
- Keep the 2-column chunked `Row` layout (already 2 columns)

**File**: `ui/homehub/ContinueListeningRow.kt`
- Replace `ContinueListeningCard` with `UnifiedMediaCard(music.toUnifiedCardData(), onClick)`
- Keep the 2-column chunked `Row` layout (already 2 columns)
- Note: this card uses a horizontal layout (artwork beside text) currently; switching to the unified vertical card (artwork above text) changes the visual layout. The 16:9 thumbnail replaces the square artwork. This is intentional per requirement 2.2 (Music tab unified design with 16:9 thumbnail)

**File**: `ui/library/LibraryListScreens.kt`
- Replace `LibraryListItem` with `UnifiedMediaCard(track.toUnifiedCardData(), onClick)`
- Replace the 1-column `LazyColumn` with a 2-column grid using chunked `Row` pairs inside `LazyColumn`, or convert to `LazyVerticalGrid(GridCells.Fixed(2))` if the screen is the sole scrolling container
- Both `LikedSongsScreen` and `PlaylistsScreen` adopt the 2-column grid

**File**: `ui/history/HistoryScreen.kt`
- Replace `PlaybackHistoryRow` with `UnifiedMediaCard(playback.toUnifiedCardData(), onClick)`
- Replace `VideoHistoryCard` with `UnifiedMediaCard(video.toUnifiedCardData(), onClick, progressFraction = video.completionPercent)`
- Replace the 1-column `LazyColumn` for Recently Played with chunked `Row` pairs (2 columns) inside `LazyColumn`, or convert to `LazyVerticalGrid(GridCells.Fixed(2))`
- Replace the `LazyRow` for Continue Watching with chunked `Row` pairs (2 columns)
- Both sections render in the 2-column grid per requirement 2.5

**File**: `ui/search/SearchScreen.kt`
- Replace `SmartTubeVideoCard` call with `UnifiedMediaCard(video.toUnifiedCardData(), onClick)`
- The `SearchResultItem` → `HomeVideoItem` mapping already exists; add `.toUnifiedCardData()` or map directly

### 2-Column Grid Layout Specification

| Section | Current Layout | Fixed Layout | Container Type |
|---------|---------------|-------------|----------------|
| For You (HomeHub `HomeVideoRail`) | Chunked 2/4 cols (orientation) | Chunked 2 cols (always) | `Row` pairs in parent `LazyColumn` |
| YouTube tab (`VideoGridContent`) | `LazyVerticalGrid(Adaptive)` | `LazyVerticalGrid(Fixed(2))` | `LazyVerticalGrid` (sole scroller) |
| Music tab (sections) | `LazyRow` per section | Chunked 2 cols per section | `Row` pairs in parent `LazyColumn` |
| Video Hub (HomeScreen rails) | `LazyRow` | Chunked 2 cols | `Row` pairs in parent `LazyColumn` |
| Quick Picks (HomeScreen grid) | `adaptiveColumnCount` chunked | Chunked 2 cols | `Row` pairs in parent `LazyColumn` |
| Quick Picks (HomeHub `HomeMusicRail`) | Chunked 2/4 cols | Chunked 2 cols | `Row` pairs in parent `LazyColumn` |
| New Releases / Supermix / Discover Mix / Because You Liked | Chunked 2/4 cols | Chunked 2 cols | `Row` pairs in parent `LazyColumn` |
| Continue Watching (HomeHub) | Chunked 2 cols | Chunked 2 cols (unchanged) | `Row` pairs in parent `LazyColumn` |
| Continue Listening (HomeHub) | Chunked 2 cols | Chunked 2 cols (unchanged) | `Row` pairs in parent `LazyColumn` |
| Liked Songs | 1-column `LazyColumn` | `LazyVerticalGrid(Fixed(2))` | `LazyVerticalGrid` (sole scroller) |
| Playlists | 1-column `LazyColumn` | `LazyVerticalGrid(Fixed(2))` | `LazyVerticalGrid` (sole scroller) |
| History Recently Played | 1-column `LazyColumn` | Chunked 2 cols | `Row` pairs in parent `LazyColumn` |
| History Continue Watching | `LazyRow` 280dp | Chunked 2 cols | `Row` pairs in parent `LazyColumn` |

Rationale for `Row` pairs vs. `LazyVerticalGrid`: sections embedded inside a parent `LazyColumn` (HomeHub, Music tab, HomeScreen, History) cannot nest a `LazyVerticalGrid` (Compose forbids nesting scrollable containers of the same axis). These use chunked `Row` pairs with `Modifier.weight(1f)` per card and a trailing `Spacer(Modifier.weight(1f))` per short row. Sections that ARE the sole scrolling container (YouTube tab, Liked Songs, Playlists) use `LazyVerticalGrid(GridCells.Fixed(2))` for proper lazy recycling.

### Blank-Data Hiding Logic

The unified card's metadata row is built conditionally:

```
FUNCTION buildMetadataRow(channelName, viewCount, uploadDate)
  parts = emptyList
  cleanChannel = channelName.trim()
  IF cleanChannel == "." OR cleanChannel == "·" OR cleanChannel.isBlank()
    cleanChannel = "DeepEye"
  END IF
  add(cleanChannel) to parts

  IF viewCount != null AND viewCount > 0
    add(viewCount.formatCountShort()) to parts
  END IF

  IF uploadDate != null AND uploadDate.isNotBlank() AND uploadDate != "—"
    add(uploadDate) to parts
  END IF

  RETURN parts.joinToString(" • ")
END FUNCTION
```

This replaces both `VideoCard`'s unconditional `add(item.viewCount?.formatCountShort() ?: "0 views")` and `SmartTubeVideoCard`'s `formatViews(video.viewCount)` which returns "No views" for 0.

### Thumbnail and Avatar Fallback Logic

```
FUNCTION resolveThumbnail(data)
  IF data.thumbnailUrl.isNotBlank()
    RETURN data.thumbnailUrl
  ELSE IF data.id is a valid YouTube video ID
    RETURN "https://i.ytimg.com/vi/{data.id}/hqdefault.jpg"
  ELSE
    RETURN data.thumbnailUrl  // let AsyncImage show error/placeholder
  END IF
END FUNCTION

FUNCTION resolveAvatar(data)
  IF data.channelAvatarUrl.isNotBlank()
    RETURN data.channelAvatarUrl
  ELSE IF data.id is a valid YouTube video ID
    RETURN "https://i.ytimg.com/vi/{data.id}/default.jpg"
  ELSE
    RETURN ""  // card renders placeholder circle background
  END IF
END FUNCTION
```

This preserves the existing `VideoCard` avatar fallback (`item.channelAvatarUrl.ifBlank { "https://i.ytimg.com/vi/${item.id}/default.jpg" }`) and the `SmartTubeVideoCard` avatar fallback (same logic), now unified in a single place.

### Quality Badge Logic

Preserved from `VideoCard` unchanged:
```
quality = when {
    isLive -> "LIVE"
    else -> when (id.hashCode().absoluteValue % 3) {
        0 -> "4K"
        1 -> "HDR"
        else -> "1080P"
    }
}
```
Colors: LIVE → red (0xFFFF0033), HDR → green (0xFF00E676), 4K/1080P → cyan (0xFF00E5FF). The `SmartTubeVideoCard` badge set (4K/HDR/ATMOS with separate hash checks) is replaced by the `VideoCard` standard to unify the design.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Write tests that render each card surface with representative data and assert visual inconsistencies, missing metadata, wrong grid column count, and placeholder text. Run these tests on the UNFIXED code to observe failures and understand the root cause.

**Test Cases**:
1. **YouTube Tab Design Mismatch**: Render `SmartTubeVideoCard` and `VideoCard` side-by-side with the same `HomeVideoItem` and assert they produce different visual output (border style, badge set, avatar styling). Will fail to match on unfixed code.
2. **Music Tab Missing Metadata**: Render `PersonalizedMusicCard` with a `PersonalizedFeedItem` that has title, artist, and artwork, and assert that no views, upload date, channel avatar, or duration badge is shown. Will confirm the omission on unfixed code.
3. **Video Hub Missing Metadata**: Render `ModernVideoCard` with a `VideoItem` and assert that no channel avatar, channel name, views, or upload date is shown. Will confirm the omission on unfixed code.
4. **HomeMusicRail Minimal Card**: Render the `HomeMusicRail` inline card with a `HomeMusicItem` and assert only thumbnail, title, and artist are shown. Will confirm the omission on unfixed code.
5. **Liked Songs 1-Column List**: Render `LibraryListItem` and assert it renders as a horizontal row (not a card) with 56dp artwork. Will confirm the list layout on unfixed code.
6. **History 1-Column List**: Render `PlaybackHistoryRow` and assert it renders as a horizontal row with 64dp artwork. Will confirm the list layout on unfixed code.
7. **Placeholder Text for Missing Data**: Render `VideoCard` with `viewCount = 0` and `uploadDate = ""`, assert the metadata row contains "0 views" and "—". Will confirm the placeholder bug on unfixed code.
8. **YouTube Tab Adaptive Grid**: Render `VideoGridContent` on a wide screen and assert the column count is greater than 2. Will confirm the non-2-column grid on unfixed code.

**Expected Counterexamples**:
- Card designs differ across surfaces (different borders, badges, avatar styles)
- Metadata fields missing (no views, no date, no avatar, no duration)
- Placeholder text shown for missing data ("0 views", "No views", "—")
- Grid layouts use 1 column (Liked, History), adaptive columns (YouTube tab), or 4 columns (HomeHub landscape)
- Possible causes: independent composables, incomplete data models, unconditional placeholder strings, inconsistent grid strategies

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL (section, card) WHERE isBugCondition(section, card) DO
  result := renderCard_fixed(section, card)
  ASSERT result.design = UNIFIED_FOR_YOU_DESIGN
  ASSERT result.layout = TWO_COLUMN_GRID
  ASSERT result.showsFullMetadata WHERE data available
  ASSERT NOT result.showsBlankPlaceholderForMissingData
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL (section, card) WHERE NOT isBugCondition(section, card) DO
  ASSERT renderCard_original(section, card) = renderCard_fixed(section, card)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first for the For You card with real data (non-zero view count, non-blank upload date, non-blank channel name, non-blank avatar URL), then write property-based tests capturing that behavior.

**Test Cases**:
1. **For You Real-Data Preservation**: Observe that `VideoCard` renders real view counts, upload dates, channel names, durations, and avatars on unfixed code, then verify `UnifiedMediaCard` produces the same output for the same `HomeVideoItem`
2. **Thumbnail Fallback Preservation**: Observe that `VideoCard` falls back to ytimg when `thumbnailUrl` is blank on unfixed code, then verify `UnifiedMediaCard` does the same
3. **Avatar Fallback Preservation**: Observe that `VideoCard` falls back to ytimg default when `channelAvatarUrl` is blank on unfixed code, then verify `UnifiedMediaCard` does the same
4. **Quality Badge Preservation**: Observe that `VideoCard` shows LIVE/4K/HDR/1080P badges based on `isLive` and id hash on unfixed code, then verify `UnifiedMediaCard` produces the same badges for the same inputs
5. **Watch-Progress Preservation**: Observe that `SmartTubeVideoCard` shows a progress bar when `progressFraction` is non-null on unfixed code, then verify `UnifiedMediaCard` does the same
6. **Tap Navigation Preservation**: Observe that card tap triggers the onClick callback on unfixed code, then verify `UnifiedMediaCard` does the same

### Unit Tests

- Test `UnifiedCardData` mappers for each domain model type: verify field mapping, ytimg fallback derivation, and null/blank handling
- Test `buildMetadataRow` logic: verify that view count is hidden when 0 or null, upload date is hidden when blank or "—", channel name falls back to "DeepEye" when blank/"."/"·"
- Test `resolveThumbnail` and `resolveAvatar` fallback logic: verify ytimg URLs are derived from video ID when source URL is blank
- Test `formatDuration` and `formatCountShort` extensions: verify correct formatting for 0, short durations, long durations, and large view counts
- Test `UnifiedMediaCard` composable: verify it renders all visible fields when data is complete, hides missing fields, and shows the correct quality badge
- Test each grid layout change: verify `GridCells.Fixed(2)` produces exactly 2 columns, and chunked `Row` pairs produce exactly 2 cards per row with correct spacing

### Property-Based Tests

- Generate random `HomeVideoItem` instances with varying field availability (full data, partial data, blank fields, zero view count) and verify `UnifiedMediaCard` shows available fields and hides unavailable ones
- Generate random `PersonalizedFeedItem`, `VideoItem`, `HomeMusicItem`, `LibraryItem`, `PlaybackHistoryEntity`, `VideoHistoryEntity` instances and verify their `toUnifiedCardData()` mappers produce correct `UnifiedCardData` with correct fallbacks
- Generate random grid layouts and verify every section produces exactly 2 columns regardless of screen width
- Test that For You card output is identical before and after the fix for all inputs where the bug condition does not hold (same `HomeVideoItem` → same visual output)
- Test that the metadata row never contains "0 views", "No views", or "—" for any input

### Integration Tests

- Test full HomeHub flow: verify For You, Quick Picks, New Releases, Supermix, Discover Mix, Because You Liked all render `UnifiedMediaCard` in 2-column grids with full metadata where available
- Test full YouTube tab flow: verify the grid uses `Fixed(2)` columns and each card renders the unified design
- Test full Music tab flow: verify each section renders `UnifiedMediaCard` in 2-column chunked rows with 16:9 thumbnails and available metadata
- Test full Video Hub flow: verify recommendation rows render `UnifiedMediaCard` in 2-column chunked rows
- Test Liked Songs screen: verify it renders `UnifiedMediaCard` in a 2-column grid with complete metadata
- Test History screen: verify both Recently Played and Continue Watching render `UnifiedMediaCard` in 2-column grids with complete metadata and watch-progress indicators
- Test navigation: verify tapping any `UnifiedMediaCard` triggers the correct navigation callback (video or music playback)
- Test loading and empty states: verify skeletons and empty states still render correctly after the card replacement
