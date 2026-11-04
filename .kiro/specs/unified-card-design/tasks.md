# Implementation Plan

## Overview

This plan implements the unified card design bugfix described in `bugfix.md` and specified in `design.md`. It consolidates every card surface onto a single `UnifiedMediaCard` composable built from the For You `VideoCard` reference standard, applies the blank-data hiding fix (hide "0 views"/"No views"/"—" instead of showing placeholders), and switches every section to a 2-column grid.

The tasks follow the bug condition methodology: surface counterexamples on unfixed code first, then implement the fix, then verify the fix resolves the bug and preserves existing good behavior.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Unified Card Design, Full Metadata, 2-Column Grid, Blank-Data Hiding
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists across every affected surface
  - **Scoped PBT Approach**: For deterministic bugs, scope the property to the concrete failing case(s) per surface to ensure reproducibility
  - Create `app/src/test/java/com/deepeye/musicpro/ui/components/UnifiedCardBugConditionTest.kt` (Robolectric + `createComposeRule`)
  - Test cases derived from the design's "Exploratory Bug Condition Checking" section, run on UNFIXED code:
    - 1a. YouTube Tab Design Mismatch: render `SmartTubeVideoCard` and `VideoCard` side-by-side with the same `HomeVideoItem`; assert they produce different visual output (border style, badge set, avatar styling). Confirm the mismatch.
    - 1b. Music Tab Missing Metadata: render `PersonalizedMusicCard` with a `PersonalizedFeedItem` (title, artist, artwork, duration); assert no views, upload date, channel avatar, or duration badge is shown. Confirm the omission.
    - 1c. Video Hub Missing Metadata: render `ModernVideoCard` with a `VideoItem`; assert no channel avatar, channel name, views, or upload date is shown. Confirm the omission.
    - 1d. HomeMusicRail Minimal Card: render the `HomeMusicRail` inline card with a `HomeMusicItem`; assert only thumbnail, title, and artist are shown. Confirm the omission.
    - 1e. Liked Songs 1-Column List: render `LibraryListItem`; assert it renders as a horizontal row (not a card) with 56dp artwork. Confirm the list layout.
    - 1f. History 1-Column List: render `PlaybackHistoryRow`; assert it renders as a horizontal row with 64dp artwork. Confirm the list layout.
    - 1g. Placeholder Text for Missing Data: render `VideoCard` with `viewCount = 0` and `uploadDate = ""`; assert the metadata row contains "0 views" and "—". Confirm the placeholder bug. Loop this over a seeded set of `HomeVideoItem` instances with zero/blank fields to confirm it holds for every blank-data combination.
    - 1h. YouTube Tab Adaptive Grid: render `VideoGridContent` at a wide viewport; assert the column count is greater than 2 (i.e., `GridCells.Adaptive` produces 3+ columns). Confirm the non-2-column grid.
    - 1i. HomeHub Landscape 4-Column: render `HomeVideoRail`/`HomeMusicRail` in landscape; assert the column count is 4. Confirm the orientation branch.
  - The test assertions should match the Expected Behavior Properties from design (unified design, 2-column grid, full metadata where available, no blank placeholders)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause (e.g., "SmartTubeVideoCard uses neon glow border + 4K/HDR/ATMOS badges; VideoCard uses 4K/HDR/1080P by id hash")
  - Mark task complete when test is written, run, and failure is documented
  - _Bug_Condition: isBugCondition(section, card) where card.design != UNIFIED_FOR_YOU_DESIGN OR NOT card.showsFullMetadata OR NOT section.usesTwoColumnGrid OR card.showsBlankPlaceholderForMissingData_
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Good Behavior (For You real data, fallbacks, badges, navigation, watch-progress)
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs (For You card with complete real data, the case where `isBugCondition` returns false)
  - Create `app/src/test/java/com/deepeye/musicpro/ui/components/UnifiedCardPreservationTest.kt` (Robolectric + `createComposeRule`)
  - Observe and record actual outputs on UNFIXED code, then write property-based tests (seeded-loop PBT) capturing observed behavior:
    - 2a. For You Real-Data Preservation: observe that `VideoCard` renders real view counts, upload dates, channel names, durations, and avatars on unfixed code. Property: for all seeded `HomeVideoItem` with non-zero `viewCount`, non-blank `uploadDate`, non-blank `channelName`, non-blank `channelAvatarUrl`, the rendered card shows all five. Verify test passes on UNFIXED code.
    - 2b. Thumbnail Fallback Preservation: observe that `VideoCard` falls back to `https://i.ytimg.com/vi/{id}/default.jpg`-style avatar when `channelAvatarUrl` is blank on unfixed code. Property: for all seeded items with blank `channelAvatarUrl`, the avatar model resolves to the ytimg default URL derived from the id. Verify test passes on UNFIXED code.
    - 2c. Quality Badge Preservation: observe that `VideoCard` shows LIVE (when `isLive`), else 4K/HDR/1080P by `id.hashCode().absoluteValue % 3` on unfixed code. Property: for all seeded ids, the badge matches that deterministic function (and the Live override). Verify test passes on UNFIXED code.
    - 2d. Duration Badge Preservation: observe that `VideoCard` shows `formatDuration(durationSeconds)` (e.g., "0:00" for 0, "1:30" for 90) always visible on unfixed code. Property: for all seeded durations >= 0, the badge text equals `Long.formatDuration()`. Verify test passes on UNFIXED code.
    - 2e. Watch-Progress Preservation: observe that the Continue Watching card shows a progress bar when progress data exists on unfixed code. Property: for all seeded non-null progress fractions > 0, a progress indicator is rendered. Verify test passes on UNFIXED code.
    - 2f. Tap Navigation Preservation: observe that card tap triggers the onClick callback on unfixed code. Property: for all seeded items, performing a card click invokes `onClick` with the correct item. Verify test passes on UNFIXED code.
  - Property-based testing (seeded loops) generates many test cases for stronger guarantees than a single example
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Preservation: For all (section, card) where NOT isBugCondition(section, card), renderCard_original(section, card) = renderCard_fixed(section, card)_
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [ ] 3. Fix for unified card design deviation across all surfaces

  - [ ] 3.1 Implement the unified card architecture and migrate all surfaces
    - _Bug_Condition: isBugCondition(section, card) where card.design != UNIFIED_FOR_YOU_DESIGN OR NOT card.showsFullMetadata OR NOT section.usesTwoColumnGrid OR card.showsBlankPlaceholderForMissingData_
    - _Expected_Behavior: renderCard_fixed produces UNIFIED_FOR_YOU_DESIGN, TWO_COLUMN_GRID, showsFullMetadata where available, and NOT showsBlankPlaceholderForMissingData_
    - _Preservation: For all (section, card) where NOT isBugCondition, renderCard_original = renderCard_fixed (For You real data, thumbnail/avatar fallback, tap navigation, badges, loading skeletons, empty states, watch-progress)_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

    - [x] 3.1.1 Create `ui/components/UnifiedCardData.kt` (NEW)
      - Define `data class UnifiedCardData` with fields: `id: String`, `title: String`, `channelName: String` (blank if unknown), `thumbnailUrl: String` (blank if unknown), `channelAvatarUrl: String` (blank if unknown), `viewCount: Long?` (null = hide; 0 = treat as unavailable), `uploadDate: String?` (null/blank = hide), `durationSeconds: Long` (0 = show "0:00"), `isLive: Boolean`, `progressFraction: Float? = null` (null = no progress bar)
      - Add `companion object` with `ytimgThumbnail(id): String` → `"https://i.ytimg.com/vi/$id/hqdefault.jpg"` and `ytimgAvatar(id): String` → `"https://i.ytimg.com/vi/$id/default.jpg"`
      - (depends on nothing; all later sub-tasks depend on this)
      - _Requirements: 2.7, 2.8, 2.9, 2.10, 3.2, 3.3_

    - [ ] 3.1.2 Create `ui/components/UnifiedCardMappers.kt` (NEW) (depends on 3.1.1)
      - Add `fun HomeVideoItem.toUnifiedCardData(): UnifiedCardData` — direct mapping (all fields present)
      - Add `fun PersonalizedFeedItem.toUnifiedCardData()` — `artist` → `channelName`, `artworkUrl` → `thumbnailUrl`, `durationMs/1000` → `durationSeconds`, `playCount` → `viewCount`; avatar left blank (no video id → placeholder circle)
      - Add `fun VideoItem.toUnifiedCardData()` — `videoId` → `id`, `artist` → `channelName`, parse duration string to seconds, thumbnail from `ytimgThumbnail(videoId)`, `channelAvatarUrl` from `ytimgAvatar(videoId)`, `viewCount = null`, `uploadDate = null`
      - Add `fun HomeMusicItem.toUnifiedCardData()` — `artist` → `channelName`, `playCount` → `viewCount` (hidden when 0), `uploadDate = null`, `channelAvatarUrl = ""` (uses ytimg fallback when a video id exists)
      - Add `fun LibraryItem.toUnifiedCardData()` — `artist`/`subtitle` → `channelName`, `artworkUrl` → `thumbnailUrl`, `playCount` → `viewCount`, `videoId` → ytimg fallback for thumbnail/avatar
      - Add `fun PlaybackHistoryEntity.toUnifiedCardData()` — `artist` → `channelName`, `artworkUri` → `thumbnailUrl`, `totalDurationMs/1000` → `durationSeconds`, `viewCount = null`, `uploadDate = null`
      - Add `fun VideoHistoryEntity.toUnifiedCardData()` — `videoId` → `id`, `thumbnailUri` → `thumbnailUrl`, `durationMs/1000` → `durationSeconds`, `channelName = ""` (DeepEye fallback), `completionPercent` → `progressFraction`
      - Add unit tests `app/src/test/java/com/deepeye/musicpro/ui/components/UnifiedCardMappersTest.kt` covering field mapping, ytimg fallback derivation, and null/blank handling for all 7 models
      - _Requirements: 2.3, 2.4, 2.5, 2.9, 2.10, 3.6_

    - [~] 3.1.3 Create `ui/components/UnifiedMediaCard.kt` (NEW) (depends on 3.1.1, 3.1.2)
      - Build `@Composable fun UnifiedMediaCard(data: UnifiedCardData, onClick: () -> Unit, modifier: Modifier = Modifier, progressFraction: Float? = data.progressFraction)` based on the For You `VideoCard` reference standard
      - Thumbnail: `Box` 16:9 aspect ratio with `AsyncImage`; `resolveThumbnail(data)` returns `data.thumbnailUrl` if non-blank else `UnifiedCardData.ytimgThumbnail(data.id)`; bottom vignette gradient preserved
      - Quality badge (top-left): `LIVE` if `data.isLive`, else `4K`/`HDR`/`1080P` by `data.id.hashCode().absoluteValue % 3`; colors: LIVE → red `0xFFFF0033`, HDR → green `0xFF00E676`, 4K/1080P → cyan `0xFF00E5FF`
      - Duration badge (bottom-right): `data.durationSeconds.coerceAtLeast(0L).formatDuration()`, always visible, monospace
      - Watch-progress bar (bottom-full) when `progressFraction != null && progressFraction > 0f`
      - Metadata row (avatar + column): avatar 36dp circle with `resolveAvatar(data)` (blank → ytimg default from id, else blank → placeholder circle); title 15sp bold 2 lines; `buildMetadataRow` joins available parts with " • "
      - `buildMetadataRow(channelName, viewCount, uploadDate)`: clean channel via `trim()` → `"DeepEye"` when `"."`/`"·"`/blank; add view count only when `viewCount != null && viewCount > 0`; add upload date only when non-null, non-blank, and not `"—"`; join with " • "
      - Reuse `formatDuration()` and `formatCountShort()` (keep in `VideoCard.kt` as public, import them — or relocate to a shared utils file; pick one and keep the import path stable)
      - Add unit tests `app/src/test/java/com/deepeye/musicpro/ui/components/UnifiedMediaCardTest.kt`: renders all fields when complete; hides view count when 0/null; hides upload date when blank/"—"; channel fallback to "DeepEye"; quality badge deterministic by id; progress bar shown only when fraction > 0
      - _Requirements: 2.7, 2.8, 2.9, 2.10, 3.1, 3.2, 3.3, 3.5, 3.8_

    - [~] 3.1.4 Refactor `ui/homehub/VideoCard.kt` (depends on 3.1.2, 3.1.3)
      - `VideoCard(item, onClick, modifier)` becomes a thin wrapper: `UnifiedMediaCard(item.toUnifiedCardData(), { onClick(item) }, modifier)`
      - Keep `formatDuration()`, `formatCount()`, `formatCountShort()` extensions public and importable (do not break existing call sites)
      - Remove the inline `metaParts` block that unconditionally added `"0 views"` / `"—"` (now handled inside `UnifiedMediaCard`)
      - _Requirements: 2.7, 2.8, 2.9, 3.1, 3.2, 3.3, 3.5_

    - [~] 3.1.5 Migrate `ui/youtube/YouTubeScreen.kt` (depends on 3.1.2, 3.1.3)
      - Replace `SmartTubeVideoCard(video, ...)` calls with `UnifiedMediaCard(video.toUnifiedCardData(), { onClick(video) })`
      - Change `VideoGridContent` grid from `GridCells.Adaptive(CardGeometry.Video.minWidth)` to `GridCells.Fixed(2)`
      - Keep `SmartTubeVideoCard` as a `@Deprecated` thin wrapper delegating to `UnifiedMediaCard` (SearchScreen still imports it until 3.1.12)
      - Remove/redirect the private `formatViews` function — blank-data hiding now lives in `UnifiedMediaCard`
      - Replace the SmartTubeVideoCard badge set (4K/HDR/ATMOS) with the unified VideoCard standard (4K/HDR/1080P by id hash)
      - _Requirements: 2.1, 2.6, 2.7, 2.8, 2.10, 3.2, 3.3, 3.5_

    - [~] 3.1.6 Migrate `ui/music/MusicScreen.kt` (depends on 3.1.2, 3.1.3)
      - Replace `PersonalizedMusicCard(item, ...)` calls with `UnifiedMediaCard(item.toUnifiedCardData(), { onClick(item) })`
      - Replace each section's `LazyRow` with chunked `Row` pairs (2 columns): `items.chunked(2).forEach { row -> Row { row.forEach { UnifiedMediaCard(..., Modifier.weight(1f)) } ; Spacer(Modifier.weight(1f)) for short rows } }` (sections live inside a parent `LazyColumn`, so a nested `LazyVerticalGrid` is forbidden)
      - Preserve the overflow menu (Play Next / Add to Queue / Why This): render it as an overlay on `UnifiedMediaCard` so the unified card design stays consistent (behavior layer on top), or add an optional actions slot
      - _Requirements: 2.2, 2.3, 2.6, 3.4_

    - [~] 3.1.7 Migrate `ui/home/HomeScreen.kt` (depends on 3.1.2, 3.1.3)
      - Replace `ModernVideoCard(video, ...)` calls with `UnifiedMediaCard(video.toUnifiedCardData(), { onClick(video) })`
      - Replace `ModernRecommendationRow`'s `LazyRow` with chunked `Row` pairs (2 columns) inside each section (sections are items in a parent `LazyColumn`)
      - Change `QuickGridItem` (quick-play dense grid) to use `UnifiedMediaCard` with 2-column chunked rows instead of `adaptiveColumnCount`
      - Remove any local `formatViews`-equivalent logic (handled by the unified card)
      - _Requirements: 2.3, 2.6, 3.4_

    - [~] 3.1.8 Migrate `ui/homehub/HomeHubScreen.kt` (depends on 3.1.2, 3.1.3)
      - Replace `SmartTubeVideoCard(video, ...)` in `HomeVideoRail` with `UnifiedMediaCard(video.toUnifiedCardData(), { onClick(video) })`
      - Replace `HomeMusicRail` inline card with `UnifiedMediaCard(music.toUnifiedCardData(), { onClick(music) })`
      - Force both rails to always 2 columns: `val columns = 2` and remove the `isLandscape` 2/4-column branch
      - _Requirements: 2.1, 2.3, 2.6, 3.5_

    - [~] 3.1.9 Migrate `ui/homehub/ContinueWatchingRow.kt` and `ui/homehub/ContinueListeningRow.kt` (depends on 3.1.2, 3.1.3)
      - `ContinueWatchingRow`: replace `ContinueWatchingCard` with `UnifiedMediaCard(video.toUnifiedCardData(), { onClick(video) }, progressFraction = video.progressPercent)`; keep the 2-column chunked `Row` layout (already 2 columns)
      - `ContinueListeningRow`: replace `ContinueListeningCard` with `UnifiedMediaCard(music.toUnifiedCardData(), { onClick(music) })`; keep the 2-column chunked `Row` layout; accept the intentional switch from horizontal artwork-beside-text to the unified vertical 16:9 card per requirement 2.2
      - _Requirements: 2.5, 2.6, 3.8_

    - [~] 3.1.10 Migrate `ui/library/LibraryListScreens.kt` (depends on 3.1.2, 3.1.3)
      - Replace `LibraryListItem` with `UnifiedMediaCard(track.toUnifiedCardData(), { onClick(track) })`
      - Convert `LikedSongsScreen` and `PlaylistsScreen` from 1-column `LazyColumn` to `LazyVerticalGrid(GridCells.Fixed(2))` (these are the sole scrolling containers, so a `LazyVerticalGrid` is correct and enables lazy recycling)
      - _Requirements: 2.4, 2.6_

    - [~] 3.1.11 Migrate `ui/history/HistoryScreen.kt` (depends on 3.1.2, 3.1.3)
      - Replace `PlaybackHistoryRow` with `UnifiedMediaCard(playback.toUnifiedCardData(), { onClick(playback) })`
      - Replace `VideoHistoryCard` with `UnifiedMediaCard(video.toUnifiedCardData(), { onClick(video) }, progressFraction = video.completionPercent)`
      - Replace the 1-column `LazyColumn` for Recently Played with chunked `Row` pairs (2 columns) inside the parent `LazyColumn` (History is a single `LazyColumn` with multiple sections, so chunked rows are required; do not nest a `LazyVerticalGrid`)
      - Replace the `LazyRow` (280dp cards) for Continue Watching with chunked `Row` pairs (2 columns)
      - Preserve watch-progress indicators where progress data exists
      - _Requirements: 2.5, 2.6, 3.8_

    - [~] 3.1.12 Migrate `ui/search/SearchScreen.kt` (depends on 3.1.2, 3.1.3, 3.1.5)
      - Replace the `SmartTubeVideoCard` call with `UnifiedMediaCard(video.toUnifiedCardData(), { onClick(video) })` (reuse the existing `SearchResultItem` → `HomeVideoItem` mapping, then `.toUnifiedCardData()`)
      - Once SearchScreen no longer imports `SmartTubeVideoCard`, confirm the deprecated wrapper in `YouTubeScreen.kt` is the only remaining reference
      - _Requirements: 2.1, 2.6_

    - [~] 3.1.13 Build and compile verification (depends on 3.1.4–3.1.12)
      - Run the project compile/build step (e.g., `./gradlew :app:compileDebugKotlin` or the configured build command) and fix any import or signature errors
      - Confirm no remaining call sites of the removed inline card composables other than the deprecated wrappers
      - _Requirements: 2.1–2.10, 3.1–3.8_

  - [~] 3.2 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Unified Card Design, Full Metadata, 2-Column Grid, Blank-Data Hiding
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior (unified design, 2-column grid, full metadata where available, no blank placeholders)
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms the bug is fixed for all buggy inputs)
    - If any assertion still fails, return to 3.1.x for the affected surface
    - _Expected_Behavior: renderCard_fixed produces UNIFIED_FOR_YOU_DESIGN, TWO_COLUMN_GRID, showsFullMetadata where data available, NOT showsBlankPlaceholderForMissingData_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10_

  - [~] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Good Behavior (For You real data, fallbacks, badges, navigation, watch-progress)
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions for non-buggy inputs)
    - Confirm all tests still pass after the fix (For You card output unchanged for complete-data inputs; thumbnail/avatar fallback, quality badges, duration badge, watch-progress, and tap navigation all preserved)
    - _Preservation: For all (section, card) where NOT isBugCondition, renderCard_original = renderCard_fixed_
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [~] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
  - Run the full test suite (unit tests + Robolectric Compose tests) for the `ui.components`, `ui.homehub`, `ui.youtube`, `ui.music`, `ui.home`, `ui.library`, `ui.history`, and `ui.search` packages
  - Confirm: every card surface renders `UnifiedMediaCard`; every section uses a 2-column grid (`Fixed(2)` for sole scrollers, chunked `Row` pairs for rails in a parent `LazyColumn`); blank data is hidden; For You real-data behavior, fallbacks, badges, navigation, and watch-progress are preserved
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

## Notes

### Task Dependencies

- Tasks 1 and 2 are standalone and MUST run before task 3 (exploration and preservation tests are written against the unfixed code).
- Within task 3, the sub-tasks form a dependency chain: `3.1.1` → `3.1.2` → `3.1.3` → the migration sub-tasks `3.1.4`–`3.1.12` (which depend on the three new files) → `3.1.13` (build verification).
- `3.2` and `3.3` re-run the tests written in tasks 1 and 2 and MUST run after all of `3.1.x` is complete.
- Task 4 is the final checkpoint.

### Property-Based Testing Note

The project test stack is JUnit 4 + Robolectric + Compose UI Test (`createComposeRule`), with MockK/Mockito. There is no dedicated property-based-testing framework on the classpath. Property-based tests in this plan are implemented as JUnit 4 tests that loop over a seeded pseudo-random generator (e.g., `java.util.Random(seed)` driving a small input domain) so they are deterministic and reproducible. The `**Property N:**` headers below are required for hover-status integration. A full PBT library (`io.kotest:kotest-property`) may be added later, but the seeded-loop approach is sufficient and matches the existing toolchain.
