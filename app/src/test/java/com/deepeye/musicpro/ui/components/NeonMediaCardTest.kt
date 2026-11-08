// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.deepeye.musicpro.data.db.PlaybackHistoryEntity
import com.deepeye.musicpro.data.db.VideoHistoryEntity
import com.deepeye.musicpro.domain.model.home.HomeMusicItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.library.LibraryItem
import com.deepeye.musicpro.domain.model.library.LibraryItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.recommendation.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the unified NeonMediaCard blank-data hiding rules and the
 * toNeonCardData() mapper family.
 *
 * Rules verified here:
 * - viewCount 0/missing -> hidden (never "0 views")
 * - uploadDate blank/"—" -> hidden
 * - duration unknown/0 -> hidden (never "0:00")
 * - quality badge: only real data; never id-hash derived
 * - avatar fallback: video-ID-derived ytimg thumbnail
 * - channel blank -> "DeepEye"
 */
@RunWith(RobolectricTestRunner::class)
class NeonMediaCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Mapper blank-data rules ─────────────────────────────────────────────

    @Test
    fun homeVideoItem_zeroViewCount_mapsToNull() {
        val item = HomeVideoItem(
            id = "abc123",
            title = "Title",
            channelName = "Ch",
            thumbnailUrl = "https://example.com/t.jpg",
            viewCount = 0,
            uploadDate = "2 days ago",
            duration = 125,
        )
        val data = item.toNeonCardData()
        assertNull("0 views must not leak through", data.viewCount)
        assertEquals("2 days ago", data.uploadDate)
        assertEquals(125_000L, data.durationMs)
    }

    @Test
    fun homeVideoItem_blankUploadDate_mapsToNull() {
        val item = HomeVideoItem(
            id = "abc",
            title = "T",
            channelName = "Ch",
            thumbnailUrl = "u",
            uploadDate = "",
        )
        assertNull(item.toNeonCardData().uploadDate)
    }

    @Test
    fun homeVideoItem_dashUploadDate_mapsToNull() {
        val item = HomeVideoItem(
            id = "abc",
            title = "T",
            channelName = "Ch",
            thumbnailUrl = "u",
            uploadDate = "—",
        )
        assertNull(item.toNeonCardData().uploadDate)
    }

    @Test
    fun homeVideoItem_zeroDuration_mapsToNull() {
        val item = HomeVideoItem(
            id = "abc",
            title = "T",
            channelName = "Ch",
            thumbnailUrl = "u",
            duration = 0,
        )
        assertNull("duration 0 must hide badge", item.toNeonCardData().durationMs)
    }

    @Test
    fun homeVideoItem_neverDerivesQualityFromIdHash() {
        val data = HomeVideoItem(
            id = "0123456789",
            title = "T",
            channelName = "Ch",
            thumbnailUrl = "u",
        ).toNeonCardData()
        assertNull("quality must come from real data, not id hash", data.qualityLabel)
    }

    @Test
    fun libraryItem_zeroPlayCount_mapsToNull() {
        val item = LibraryItem(
            id = "lib1",
            type = LibraryItemType.SONG,
            title = "Song",
            artist = "Artist",
            playCount = 0,
        )
        val data = item.toNeonCardData()
        assertNull(data.viewCount)
        assertEquals(NeonCardType.COMPACT, data.cardType)
    }

    @Test
    fun videoItem_parsesDurationStringToMs() {
        val data = VideoItem(
            videoId = "v1",
            title = "T",
            artist = "A",
            channelId = "c",
            duration = "1:05",
        ).toNeonCardData()
        assertEquals(65_000L, data.durationMs)
    }

    @Test
    fun videoItem_parsesHourDurationStringToMs() {
        val data = VideoItem(
            videoId = "v2",
            title = "T",
            artist = "A",
            channelId = "c",
            duration = "1:02:03",
        ).toNeonCardData()
        assertEquals(3723_000L, data.durationMs)
    }

    @Test
    fun videoItem_blankDuration_mapsToNull() {
        val data = VideoItem(
            videoId = "v3",
            title = "T",
            artist = "A",
            channelId = "c",
            duration = "",
        ).toNeonCardData()
        assertNull(data.durationMs)
    }

    @Test
    fun playbackHistory_zeroTotalDuration_mapsToNull() {
        val data = PlaybackHistoryEntity(
            mediaId = "m",
            title = "T",
            artist = "A",
            playDurationMs = 1000,
            totalDurationMs = 0,
            completionPercent = 0.5f,
            source = "youtube",
        ).toNeonCardData()
        assertNull(data.durationMs)
        assertEquals(NeonCardType.COMPACT, data.cardType)
    }

    @Test
    fun videoHistory_validDuration_mapsToMs() {
        val data = VideoHistoryEntity(
            videoId = "v",
            title = "T",
            durationMs = 200_000L,
            positionMs = 50_000L,
            completionPercent = 0.25f,
        ).toNeonCardData()
        assertEquals(200_000L, data.durationMs)
        assertEquals(NeonCardType.COMPACT, data.cardType)
    }

    // ── Composable blank-data hiding ────────────────────────────────────────

    @Test
    fun card_withZeroViewsAndNoDuration_hidesBoth() {
        composeTestRule.setContent {
            MaterialTheme {
                NeonMediaCard(
                    data = NeonCardData(
                        id = "abc",
                        title = "A Title",
                        channelName = "Some Channel",
                        thumbnailUrl = "https://i.ytimg.com/vi/abc/hqdefault.jpg",
                        viewCount = 0,
                        uploadDate = "—",
                        durationMs = 0,
                    ),
                    onClick = {},
                    modifier = Modifier.testTag("card"),
                )
            }
        }
        composeTestRule.waitForIdle()
        // Title still present
        composeTestRule.onNodeWithText("A Title").assertExists()
        // Channel present
        composeTestRule.onNodeWithText("Some Channel").assertExists()
        // No "0 views"
        composeTestRule.onNodeWithText("0 views", substring = true).assertDoesNotExist()
        // No "0:00"
        composeTestRule.onNodeWithText("0:00", substring = true).assertDoesNotExist()
    }

    @Test
    fun card_withRealData_showsViewsAndDuration() {
        composeTestRule.setContent {
            MaterialTheme {
                NeonMediaCard(
                    data = NeonCardData(
                        id = "abc",
                        title = "A Title",
                        channelName = "Ch",
                        thumbnailUrl = "https://i.ytimg.com/vi/abc/hqdefault.jpg",
                        viewCount = 5_000,
                        uploadDate = "3 days ago",
                        durationMs = 125_000,
                    ),
                    onClick = {},
                    modifier = Modifier.testTag("card"),
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("5K views", substring = true).assertExists()
        composeTestRule.onNodeWithText("3 days ago", substring = true).assertExists()
        composeTestRule.onNodeWithText("2:05").assertExists()
    }

    @Test
    fun card_blankChannel_fallsBackToDeepEye() {
        composeTestRule.setContent {
            MaterialTheme {
                NeonMediaCard(
                    data = NeonCardData(
                        id = "abc",
                        title = "T",
                        channelName = "",
                        thumbnailUrl = "u",
                    ),
                    onClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DeepEye").assertExists()
    }

    @Test
    fun card_avatarFallsBackToIdDerivedYtimg() {
        composeTestRule.setContent {
            MaterialTheme {
                NeonMediaCard(
                    data = NeonCardData(
                        id = "vid123",
                        title = "T",
                        channelName = "Ch",
                        thumbnailUrl = "https://i.ytimg.com/vi/vid123/hqdefault.jpg",
                        channelAvatarUrl = "",
                    ),
                    onClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        // The avatar's contentDescription is the channel name "Ch"; the fallback
        // URL itself is a coil model, not semantically assertable. We assert
        // the avatar node exists with the expected content description.
        composeTestRule.onNodeWithContentDescription("Ch").assertExists()
    }

    @Test
    fun card_liveBadge_takesPrecedenceOverQualityLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                NeonMediaCard(
                    data = NeonCardData(
                        id = "abc",
                        title = "T",
                        channelName = "Ch",
                        thumbnailUrl = "u",
                        qualityLabel = "1080P",
                        isLive = true,
                    ),
                    onClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("LIVE").assertExists()
        // 1080P suppressed when live
        composeTestRule.onNodeWithText("1080P", substring = true).assertDoesNotExist()
    }

    @Test
    fun card_noQualityLabel_hidesBadge() {
        composeTestRule.setContent {
            MaterialTheme {
                NeonMediaCard(
                    data = NeonCardData(
                        id = "abc",
                        title = "T",
                        channelName = "Ch",
                        thumbnailUrl = "u",
                    ),
                    onClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        // No quality badge at all
        composeTestRule.onNodeWithText("4K", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("HDR", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("1080P", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("LIVE", substring = true).assertDoesNotExist()
    }

    // ── SmartTubeVideoCard thin wrapper ──────────────────────────────────────

    @Test
    fun smartTubeVideoCard_wrapsNeonMediaCard() {
        val video = HomeVideoItem(
            id = "abc",
            title = "Wrapped Title",
            channelName = "WrappedCh",
            thumbnailUrl = "https://i.ytimg.com/vi/abc/hqdefault.jpg",
            viewCount = 10_000,
            uploadDate = "1 week ago",
            duration = 300,
        )
        composeTestRule.setContent {
            MaterialTheme {
                com.deepeye.musicpro.ui.youtube.SmartTubeVideoCard(
                    video = video,
                    onClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Wrapped Title").assertExists()
        composeTestRule.onNodeWithText("WrappedCh").assertExists()
        composeTestRule.onNodeWithText("5:00").assertExists()
    }
}
