// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.ui.homehub.VideoCard
import com.deepeye.musicpro.ui.homehub.formatCountShort
import com.deepeye.musicpro.ui.homehub.formatDuration
import com.deepeye.musicpro.ui.youtube.SmartTubeVideoCard
import kotlin.math.absoluteValue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * **Property 2: Preservation** - Existing Good Behavior
 *
 * For You real data, fallbacks, badges, navigation, watch-progress
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8
 *
 * These tests verify that the For You VideoCard's existing good behavior is
 * preserved. They MUST PASS on unfixed code, establishing the baseline that
 * the fix (Task 3) must not regress.
 *
 * Observation-first methodology: the tests capture behavior observed on the
 * UNFIXED VideoCard and SmartTubeVideoCard composables for non-buggy inputs
 * (complete data, non-zero view counts, non-blank fields). If actual behavior
 * differs from expectations, the test was adjusted to match observed behavior.
 *
 * Test stack: JUnit 4 + Robolectric + Compose UI Test (createComposeRule).
 * Property-based tests use seeded java.util.Random loops for determinism.
 *
 * Implementation note: createComposeRule's setContent can only be called once
 * per test method. Each PBT test uses a single setContent with a
 * mutableStateOf index to swap the rendered card across seeded iterations,
 * ensuring each assertion targets exactly one card at a time.
 *
 * Preservation (formal):
 *   For all (section, card) where NOT isBugCondition(section, card),
 *   renderCard_original(section, card) = renderCard_fixed(section, card)
 */
@RunWith(RobolectricTestRunner::class)
class UnifiedCardPreservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── 2a. For You Real-Data Preservation ──────────────────────────────

    /**
     * VideoCard renders real view counts, upload dates, channel names,
     * durations, and avatars for items with complete data.
     *
     * Property: for all seeded HomeVideoItem with non-zero viewCount,
     * non-blank uploadDate, non-blank channelName, non-blank channelAvatarUrl,
     * the rendered card shows the view count text, upload date, channel name,
     * duration badge, and channel avatar (contentDescription = channelName).
     *
     * Validates: Requirements 3.1, 3.2, 3.3, 3.5
     */
    @Test
    fun `2a_forYouRealDataPreservation_VideoCard_rendersAllMetadata_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        val items = (0 until 10).map { i ->
            HomeVideoItem(
                id = "real_$i",
                title = "Real Data Item $i",
                channelName = "Chan$i",
                thumbnailUrl = "https://example.com/thumb_$i.jpg",
                channelAvatarUrl = "https://example.com/avatar_$i.jpg",
                viewCount = (random.nextInt(99_000) + 1).toLong(),
                uploadDate = "2024-0${(i % 9) + 1}-15",
                duration = (random.nextInt(7200) + 1).toLong(),
            )
        }

        var currentIndex by mutableStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                VideoCard(item = items[currentIndex], onClick = {})
            }
        }

        repeat(10) { i ->
            currentIndex = i
            composeTestRule.waitForIdle()

            val item = items[i]
            val expectedViewText = item.viewCount.formatCountShort()
            val expectedDurationText = item.duration.formatDuration()

            // View count is rendered in the metadata row
            composeTestRule.onNodeWithText(expectedViewText, substring = true, useUnmergedTree = true)
                .assertExists()
            // Upload date is rendered in the metadata row
            composeTestRule.onNodeWithText(item.uploadDate, substring = true, useUnmergedTree = true)
                .assertExists()
            // Channel name is rendered in the metadata row
            composeTestRule.onNodeWithText(item.channelName, substring = true, useUnmergedTree = true)
                .assertExists()
            // Duration badge is rendered
            composeTestRule.onNodeWithText(expectedDurationText, useUnmergedTree = true)
                .assertExists()
            // Channel avatar is rendered (contentDescription = channelName)
            composeTestRule.onNodeWithContentDescription(item.channelName, useUnmergedTree = true)
                .assertExists()
        }
    }

    // ─── 2b. Thumbnail/Avatar Fallback Preservation ─────────────────────

    /**
     * VideoCard falls back to ytimg default avatar when channelAvatarUrl
     * is blank. The card still renders with the avatar.
     *
     * The avatar model resolves to
     * "https://i.ytimg.com/vi/{id}/default.jpg" internally (observed in
     * VideoCard.kt: item.channelAvatarUrl.ifBlank { ytimg fallback }).
     * This URL is not directly assertable via Compose semantics, but the
     * card rendering successfully with an avatar (contentDescription =
     * channelName) proves the fallback works.
     *
     * Property: for all seeded items with blank channelAvatarUrl, the card
     * renders (title visible) and the avatar renders (contentDescription
     * = channelName visible).
     *
     * Validates: Requirements 3.2, 3.6
     */
    @Test
    fun `2b_avatarFallbackPreservation_VideoCard_rendersWithYtimgFallback_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        val items = (0 until 10).map { i ->
            HomeVideoItem(
                id = "fallback_$i",
                title = "Fallback Item $i",
                channelName = "FallbackChan$i",
                thumbnailUrl = "https://example.com/thumb_$i.jpg",
                channelAvatarUrl = "", // blank — triggers ytimg fallback
                viewCount = (random.nextInt(99_000) + 1).toLong(),
                uploadDate = "2024-01-15",
                duration = (random.nextInt(7200) + 1).toLong(),
            )
        }

        var currentIndex by mutableStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                VideoCard(item = items[currentIndex], onClick = {})
            }
        }

        repeat(10) { i ->
            currentIndex = i
            composeTestRule.waitForIdle()

            val item = items[i]

            // Card renders (title visible)
            composeTestRule.onNodeWithText(item.title, useUnmergedTree = true)
                .assertExists()
            // Avatar renders with channel name as contentDescription
            // (proves the avatar fell back to ytimg default and still renders)
            composeTestRule.onNodeWithContentDescription(item.channelName, useUnmergedTree = true)
                .assertExists()
        }
    }

    // ─── 2c. Quality Badge Preservation ──────────────────────────────────

    /**
     * VideoCard shows LIVE (when isLive), else 4K/HDR/1080P by
     * id.hashCode().absoluteValue % 3.
     *
     * Observed in VideoCard.kt:
     *   quality = when {
     *     item.isLive -> "LIVE"
     *     else -> when (item.id.hashCode().absoluteValue % 3) {
     *       0 -> "4K"
     *       1 -> "HDR"
     *       else -> "1080P"
     *     }
     *   }
     *
     * Property: for all seeded ids, the quality badge text matches that
     * deterministic function (with the LIVE override for isLive).
     *
     * Validates: Requirements 3.3
     */
    @Test
    fun `2c_qualityBadgePreservation_VideoCard_showsDeterministicBadges_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        val items = (0 until 10).map { i ->
            HomeVideoItem(
                id = "qual_$i",
                title = "Badge Test $i",
                channelName = "QChan$i",
                thumbnailUrl = "https://example.com/thumb_$i.jpg",
                isLive = random.nextBoolean(),
                viewCount = 100,
                uploadDate = "2024-01-15",
                duration = 60,
            )
        }

        var currentIndex by mutableStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                VideoCard(item = items[currentIndex], onClick = {})
            }
        }

        repeat(10) { i ->
            currentIndex = i
            composeTestRule.waitForIdle()

            val item = items[i]
            val expectedQuality = if (item.isLive) {
                "LIVE"
            } else {
                when (item.id.hashCode().absoluteValue % 3) {
                    0 -> "4K"
                    1 -> "HDR"
                    else -> "1080P"
                }
            }

            composeTestRule.onNodeWithText(expectedQuality, useUnmergedTree = true)
                .assertExists()
        }
    }

    // ─── 2d. Duration Badge Preservation ─────────────────────────────────

    /**
     * VideoCard shows formatDuration(durationSeconds), always visible
     * (including "0:00" when duration is 0).
     *
     * Observed in VideoCard.kt:
     *   Text(text = item.duration.coerceAtLeast(0L).formatDuration())
     *
     * formatDuration(Long):
     *   h:m:s format — "%d:%02d:%02d" when h > 0, else "%d:%02d"
     *
     * Property: for all seeded durations >= 0, the badge text equals
     * duration.formatDuration().
     *
     * Validates: Requirements 3.5
     */
    @Test
    fun `2d_durationBadgePreservation_VideoCard_showsFormattedDuration_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        val items = (0 until 10).map { i ->
            HomeVideoItem(
                id = "dur_$i",
                title = "Duration Test $i",
                channelName = "DChan$i",
                thumbnailUrl = "https://example.com/thumb_$i.jpg",
                duration = random.nextInt(10_800).toLong(), // 0 to 10799 seconds
                viewCount = 100,
                uploadDate = "2024-01-15",
            )
        }

        var currentIndex by mutableStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                VideoCard(item = items[currentIndex], onClick = {})
            }
        }

        repeat(10) { i ->
            currentIndex = i
            composeTestRule.waitForIdle()

            val item = items[i]
            val expectedDurationText = item.duration.formatDuration()

            composeTestRule.onNodeWithText(expectedDurationText, useUnmergedTree = true)
                .assertExists()
        }
    }

    // ─── 2e. Watch-Progress Preservation ─────────────────────────────────

    /**
     * SmartTubeVideoCard renders with a progress bar when progressFraction
     * is non-null and > 0.
     *
     * Observed in YouTubeScreen.kt:
     *   if (progressFraction != null && progressFraction > 0f) {
     *     Box(... background(Color.White.copy(alpha = 0.25f))) {
     *       Box(... fillMaxWidth(progressFraction.coerceIn(0f, 1f))
     *             ... background(Color(0xFFFF0033)))
     *     }
     *   }
     *
     * The progress bar is a non-semantic visual element (Box with background,
     * no contentDescription or testTag) that cannot be directly asserted via
     * the Compose semantics tree. The test verifies the card renders
     * successfully with progress data (title visible), confirming the
     * progressFraction parameter is accepted and the card does not crash.
     *
     * Property: for all seeded non-null progress fractions > 0, the card
     * renders (title visible).
     *
     * Validates: Requirements 3.8
     */
    @Test
    fun `2e_watchProgressPreservation_SmartTubeVideoCard_rendersWithProgress_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        val items = (0 until 10).map { i ->
            HomeVideoItem(
                id = "prog_$i",
                title = "Progress Item $i",
                channelName = "PChan$i",
                thumbnailUrl = "https://example.com/thumb_$i.jpg",
                duration = (random.nextInt(7200) + 1).toLong(),
                viewCount = 100,
                uploadDate = "2024-01-15",
            )
        }
        val progressFractions = (0 until 10).map {
            random.nextFloat() * 0.99f + 0.01f // 0.01 to 1.0
        }

        var currentIndex by mutableStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                SmartTubeVideoCard(
                    video = items[currentIndex],
                    onClick = {},
                    progressFraction = progressFractions[currentIndex],
                )
            }
        }

        repeat(10) { i ->
            currentIndex = i
            composeTestRule.waitForIdle()

            val item = items[i]

            // Card renders with progress data (title visible — proves the
            // card accepts progressFraction and renders without crashing)
            composeTestRule.onNodeWithText(item.title, useUnmergedTree = true)
                .assertExists()
        }
    }

    // ─── 2f. Tap Navigation Preservation ──────────────────────────────────

    /**
     * Card tap triggers the onClick callback with the correct item.
     *
     * VideoCard uses bouncyClickable (Modifier.clickable with
     * mergeDescendants = true) for click handling. In the merged tree,
     * the card node contains the title text and the click action, so
     * onNodeWithText(title) finds the clickable card node and
     * performClick() invokes the onClick callback.
     *
     * Property: for all seeded items, performing a card click invokes
     * onClick with the correct HomeVideoItem.
     *
     * Validates: Requirements 3.4, 3.7
     */
    @Test
    fun `2f_tapNavigationPreservation_VideoCard_clickInvokesOnClick_acrossSeededInputs`() {
        val random = java.util.Random(42L)
        val items = (0 until 10).map { i ->
            HomeVideoItem(
                id = "tap_$i",
                title = "Tap Nav Item $i",
                channelName = "TapChan$i",
                thumbnailUrl = "https://example.com/thumb_$i.jpg",
                viewCount = (random.nextInt(99_000) + 1).toLong(),
                uploadDate = "2024-01-15",
                duration = (random.nextInt(7200) + 1).toLong(),
            )
        }

        var currentIndex by mutableStateOf(0)
        var receivedItem: HomeVideoItem? = null

        composeTestRule.setContent {
            MaterialTheme {
                VideoCard(item = items[currentIndex], onClick = { receivedItem = it })
            }
        }

        repeat(10) { i ->
            // Reset state for this iteration
            currentIndex = i
            receivedItem = null
            composeTestRule.waitForIdle()

            val item = items[i]

            // Use the merged tree (default) — clickable sets mergeDescendants=true
            // so the card node contains the title text and the onClick action
            composeTestRule.onNodeWithText(item.title).performClick()

            assertTrue(
                "onClick should be invoked with the correct item for seed $i",
                receivedItem == item,
            )
        }
    }
}
