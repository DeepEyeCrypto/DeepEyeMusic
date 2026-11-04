// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.deepeye.musicpro.data.db.PlaybackHistoryEntity
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.model.library.LibraryItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.recommendation.VideoItem
import com.deepeye.musicpro.ui.history.PlaybackHistoryRow
import com.deepeye.musicpro.ui.home.ModernVideoCard
import com.deepeye.musicpro.ui.homehub.VideoCard
import com.deepeye.musicpro.ui.library.LibraryListItem
import com.deepeye.musicpro.ui.music.PersonalizedMusicCard
import com.deepeye.musicpro.ui.youtube.SmartTubeVideoCard
import com.deepeye.musicpro.ui.youtube.VideoGridContent
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Bug Condition Exploration Test — Property 1
 *
 * Unified Card Design, Full Metadata, 2-Column Grid, Blank-Data Hiding
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10
 *
 * This test encodes the EXPECTED behavior (the fix): every card surface should
 * use the unified For You card design, show full metadata where available, lay
 * out in a 2-column grid, and hide blank-data placeholders.
 *
 * On UNFIXED code, these tests FAIL — the failure confirms the bug exists.
 * After the fix is implemented (Task 3), these tests PASS.
 *
 * Bug Condition (formal):
 *   isBugCondition(section, card) = card.design != UNIFIED_FOR_YOU_DESIGN
 *       OR NOT card.showsFullMetadata
 *       OR NOT section.usesTwoColumnGrid
 *       OR card.showsBlankPlaceholderForMissingData
 *
 * Test stack: JUnit 4 + Robolectric + Compose UI Test (createComposeRule).
 * Property-based tests use seeded java.util.Random loops for determinism.
 */
@RunWith(RobolectricTestRunner::class)
class UnifiedCardBugConditionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── 1a. YouTube Tab Design Mismatch ─────────────────────────────────────

    /**
     * SmartTubeVideoCard uses a different badge set (4K/HDR/ATMOS) than the
     * unified VideoCard design (LIVE/4K/HDR/1080P).
     *
     * With id "d" (hashCode = 100, 100 % 4 = 0), SmartTubeVideoCard shows the
     * "ATMOS" badge. The unified design never shows "ATMOS".
     *
     * Expected: SmartTubeVideoCard does NOT show "ATMOS".
     * On unfixed: SmartTubeVideoCard shows "ATMOS" → FAILS.
     * After fix: SmartTubeVideoCard delegates to UnifiedMediaCard → no "ATMOS" → PASSES.
     *
     * Validates: Requirements 1.1
     */
    @Test
    fun `1a_youTubeTabDesignMismatch_SmartTubeVideoCard_usesUnifiedBadgeSet`() {
        // id "d": hashCode = 100, 100 % 4 = 0 → hasAtmos = true on unfixed code
        val item = HomeVideoItem(
            id = "d",
            title = "Design Mismatch Test",
            channelName = "Test Channel",
            thumbnailUrl = "https://example.com/thumb.jpg",
            viewCount = 1000,
            uploadDate = "2024-01-01",
        )
        composeTestRule.setContent {
            MaterialTheme {
                SmartTubeVideoCard(video = item, onClick = {})
            }
        }
        // The unified design uses LIVE/4K/HDR/1080P — never "ATMOS"
        composeTestRule.onNodeWithText("ATMOS", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // ─── 1b. Music Tab Missing Metadata ──────────────────────────────────────

    /**
     * PersonalizedMusicCard renders a 1:1 square thumbnail with a gradient
     * background and omits views, upload date, channel avatar, and duration badge.
     *
     * Expected: the card shows a view count in the metadata row.
     * On unfixed: no view count is shown → FAILS.
     * After fix: UnifiedMediaCard shows "1.5K views" → PASSES.
     *
     * Validates: Requirements 1.2, 1.3
     */
    @Test
    fun `1b_musicTabMissingMetadata_PersonalizedMusicCard_showsViewCount`() {
        val item = PersonalizedFeedItem(
            id = "music1",
            title = "Test Track",
            artist = "Test Artist",
            artworkUrl = "https://example.com/art.jpg",
            durationMs = 180000,
            playCount = 1500,
        )
        composeTestRule.setContent {
            MaterialTheme {
                PersonalizedMusicCard(
                    item = item,
                    onClick = {},
                    onPlayNext = {},
                    onAddToQueue = {},
                )
            }
        }
        // The unified card shows view count (playCount → viewCount → "1.5K views")
        composeTestRule.onNodeWithText("views", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    // ─── 1c. Video Hub Missing Metadata ─────────────────────────────────────

    /**
     * ModernVideoCard renders a 16:9 thumbnail with a duration badge but no
     * channel avatar, no channel name in a metadata row, no views, and no upload date.
     *
     * Expected: the card includes a channel avatar with contentDescription
     * matching the channel/artist name.
     * On unfixed: no avatar exists → FAILS.
     * After fix: UnifiedMediaCard includes a 36dp circular avatar → PASSES.
     *
     * Validates: Requirements 1.3
     */
    @Test
    fun `1c_videoHubMissingMetadata_ModernVideoCard_showsChannelAvatar`() {
        val video = VideoItem(
            videoId = "vid1",
            title = "Hub Video Title",
            artist = "Hub Artist Name",
            channelId = "ch1",
            duration = "3:30",
        )
        composeTestRule.setContent {
            MaterialTheme {
                ModernVideoCard(video = video, onClick = {})
            }
        }
        // The unified card has a channel avatar with contentDescription = channel name.
        // On unfixed code, the artist is shown as Text (which does not set
        // contentDescription in the unmerged tree), so this lookup finds nothing.
        composeTestRule
            .onNodeWithContentDescription("Hub Artist Name", useUnmergedTree = true)
            .assertExists()
    }

    // ─── 1d. HomeMusicRail Minimal Card — EVIDENCE GAP ──────────────────────

    /**
     * 1d. HomeMusicRail Minimal Card — EVIDENCE GAP
     *
     * The HomeMusicRail inline card is NOT a standalone composable: it is inline
     * code inside the private `HomeMusicRail` function in HomeHubScreen.kt.
     * It cannot be rendered in isolation because `HomeMusicRail` is private.
     *
     * Evidence from code reading (HomeHubScreen.kt lines ~588-680):
     *   - Renders a 1:1 square thumbnail (CardGeometry.Music.aspectRatio = 1f)
     *   - Shows only: thumbnail, title, artist
     *   - Omits: views, upload date, channel avatar, duration badge
     *   - Uses chunked Row pairs with 2/4 columns (orientation-dependent)
     *
     * This confirms the bug condition (card.design != UNIFIED, NOT showsFullMetadata).
     * After the fix (Task 3.1.8), HomeMusicRail renders UnifiedMediaCard.
     *
     * Validates: Requirements 1.3, 1.7
     */

    // ─── 1e. Liked Songs 1-Column List ──────────────────────────────────────

    /**
     * LibraryListItem renders a horizontal Row (not a card) with 56dp square
     * artwork, title, and subtitle — no card design, no grid, no full metadata.
     *
     * Expected: the card shows a view count in the metadata row.
     * On unfixed: LibraryListItem shows only title/subtitle → FAILS.
     * After fix: UnifiedMediaCard shows "500 views" → PASSES.
     *
     * Validates: Requirements 1.4, 1.3
     */
    @Test
    fun `1e_likedSongsList_LibraryListItem_rendersAsCardWithMetadata`() {
        val item = LibraryItem(
            id = "lib1",
            type = LibraryItemType.SONG,
            title = "Library Track",
            subtitle = "Library Subtitle",
            artworkUrl = "https://example.com/art.jpg",
            playCount = 500,
        )
        composeTestRule.setContent {
            MaterialTheme {
                LibraryListItem(item = item, onClick = {})
            }
        }
        // The unified card shows view count (playCount → viewCount → "500 views")
        composeTestRule.onNodeWithText("views", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    // ─── 1f. History 1-Column List ──────────────────────────────────────────

    /**
     * PlaybackHistoryRow renders a horizontal Row (not a card) with 64dp square
     * artwork, title, artist, and a play button — no card design, no duration badge.
     *
     * Expected: the card shows a duration badge (formatDuration(210) = "3:30").
     * On unfixed: PlaybackHistoryRow shows no duration → FAILS.
     * After fix: UnifiedMediaCard shows "3:30" duration badge → PASSES.
     *
     * Validates: Requirements 1.5
     */
    @Test
    fun `1f_historyList_PlaybackHistoryRow_rendersAsCardWithDurationBadge`() {
        val playback = PlaybackHistoryEntity(
            mediaId = "hist1",
            title = "History Track",
            artist = "History Artist",
            artworkUri = "https://example.com/art.jpg",
            playDurationMs = 60000,
            totalDurationMs = 210000,
            completionPercent = 50f,
            source = "youtube",
        )
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackHistoryRow(playback = playback, onClick = {})
            }
        }
        // The unified card shows a duration badge (totalDurationMs/1000 = 210 → "3:30")
        composeTestRule.onNodeWithText("3:30", useUnmergedTree = true)
            .assertExists()
    }

    // ─── 1g. Placeholder Text for Missing Data ───────────────────────────────

    /**
     * VideoCard unconditionally adds "0 views" (formatCountShort(0)) to the
     * metadata row when viewCount = 0.
     *
     * Expected: the card hides the view count when it is 0 (unavailable).
     * On unfixed: VideoCard shows "0 views" → FAILS.
     * After fix: UnifiedMediaCard hides viewCount when 0 → PASSES.
     *
     * Validates: Requirements 1.8
     */
    @Test
    fun `1g_placeholderText_VideoCard_hidesBlankDataPlaceholders`() {
        val item = HomeVideoItem(
            id = "blank1",
            title = "Blank Data Test",
            channelName = "Test Channel",
            thumbnailUrl = "https://example.com/thumb.jpg",
            viewCount = 0,
            uploadDate = "",
        )
        composeTestRule.setContent {
            MaterialTheme {
                VideoCard(item = item, onClick = {})
            }
        }
        // Expected: hide "0 views" when viewCount is 0
        composeTestRule.onNodeWithText("0 views", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    /**
     * PBT: loop over a seeded set of HomeVideoItem instances with zero/blank
     * fields to confirm the placeholder bug holds for every blank-data combination.
     *
     * Uses java.util.Random(42L) for deterministic reproducibility.
     *
     * Expected: "0 views" is never shown for any item with viewCount = 0.
     * On unfixed: "0 views" appears for every zero-view item → FAILS.
     * After fix: all iterations pass → PASSES.
     *
     * Validates: Requirements 1.8
     */
    @Test
    fun `1g_pbt_placeholderText_VideoCard_neverShowsBlankDataPlaceholders_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        repeat(5) { i ->
            val item = HomeVideoItem(
                id = "seed_$i",
                title = "Seeded Track $i",
                channelName = if (random.nextBoolean()) "" else "Channel $i",
                thumbnailUrl = "",
                duration = random.nextInt(301).toLong(),
                // always zero — triggers "0 views" placeholder on unfixed code
                viewCount = 0,
                uploadDate = if (random.nextBoolean()) "" else "2024-0${i + 1}-01",
            )
            composeTestRule.setContent {
                MaterialTheme {
                    VideoCard(item = item, onClick = {})
                }
            }
            composeTestRule.onNodeWithText("0 views", substring = true, useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }

    // ─── 1h. YouTube Tab Adaptive Grid ───────────────────────────────────────

    /**
     * VideoGridContent uses GridCells.Adaptive(CardGeometry.Video.minWidth = 140dp),
     * which produces 3+ columns at a wide viewport instead of exactly 2.
     *
     * Expected: exactly 2 columns (items 0 and 2 are in different rows).
     * On unfixed: Adaptive produces 6 columns at 1000dp width → items 0 and 2
     *   are in the same row → FAILS.
     * After fix: GridCells.Fixed(2) → items 0 and 2 in different rows → PASSES.
     *
     * Validates: Requirements 1.7
     */
    @Test
    @Config(qualifiers = "w1000dp-h1000dp")
    fun `1h_youTubeTabAdaptiveGrid_VideoGridContent_usesTwoColumnsAtWideViewport`() {
        val videos = (0..5).map { i ->
            HomeVideoItem(
                id = "grid$i",
                title = "GridItem$i",
                channelName = "Channel",
                thumbnailUrl = "https://example.com/thumb$i.jpg",
                viewCount = 100,
                uploadDate = "2024-01-01",
            )
        }
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.width(1000.dp).height(1000.dp)) {
                    VideoGridContent(
                        isLoading = false,
                        videos = videos,
                        isMoreLoading = false,
                        hasMore = false,
                        error = null,
                        isLandscape = false,
                        onVideoClick = {},
                        onLoadMore = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // With Fixed(2): items 0 and 2 are in different rows (y-positions differ).
        // With Adaptive(140dp) at 1000dp: 6 columns → items 0 and 2 in same row.
        val y0 = composeTestRule.onNodeWithText("GridItem0", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.top
        val y2 = composeTestRule.onNodeWithText("GridItem2", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.top

        assertTrue(
            "Expected 2-column grid: items 0 and 2 should be in different rows " +
                "(y0=$y0, y2=$y2), but they appear in the same row — " +
                "GridCells.Adaptive produced more than 2 columns",
            abs(y0 - y2) > 50f,
        )
    }

    // ─── 1i. HomeHub Landscape 4-Column — EVIDENCE GAP ──────────────────────

    /**
     * 1i. HomeHub Landscape 4-Column — EVIDENCE GAP
     *
     * HomeVideoRail and HomeMusicRail are private composables in HomeHubScreen.kt.
     * They cannot be rendered in isolation. The orientation-dependent column
     * count (4 in landscape, 2 in portrait) is inline logic inside these private
     * functions.
     *
     * Evidence from code reading (HomeHubScreen.kt lines ~542-680):
     *   HomeVideoRail:
     *     val isLandscape = configuration.orientation == LANDSCAPE
     *     val columns = if (isLandscape) 4 else 2
     *     val maxItems = if (isLandscape) 8 else 4
     *     Uses SmartTubeVideoCard (not unified design)
     *
     *   HomeMusicRail:
     *     val isLandscape = configuration.orientation == LANDSCAPE
     *     val columns = if (isLandscape) 4 else 2
     *     val maxItems = if (isLandscape) 8 else 4
     *     Uses inline minimal card (not unified design)
     *
     * This confirms the bug condition (NOT section.usesTwoColumnGrid in landscape).
     * After the fix (Task 3.1.8), both rails use `val columns = 2` (always 2).
     *
     * Validates: Requirements 1.7
     */
}
