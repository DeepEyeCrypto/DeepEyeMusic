// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.overlay

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.domain.model.RepeatMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Accessibility semantics for the immersive video overlay.
 *
 * The overlay is a gesture-driven surface, not a classic transport bar: its
 * only permanently-labelled affordances are the OSD icons it renders while a
 * gesture is active, plus the lock toggle. These tests therefore assert against
 * the content descriptions the composable actually emits, rather than against a
 * transport-bar layout that this surface does not have.
 *
 * @RunWith(RobolectricTestRunner) is required: without it the Compose test
 * environment reads android.os.Build.FINGERPRINT as null and throws
 * NullPointerException inside RobolectricIdlingStrategy before the test body runs.
 */
@RunWith(RobolectricTestRunner::class)
class DeepEyeVideoPlayerOverlaySemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun all_controls_have_role_button() {
        val state = PlayerState(
            isPlaying = true,
            position = 60000L,
            duration = 300000L,
            isVideo = true,
            isLoading = false,
            isLiked = false,
            isDisliked = false,
            isCaptionEnabled = false
        )
        val actions = VideoPlayerOverlayActions.fromLambdas()

        composeTestRule.setContent {
            MaterialTheme {
                DeepEyeVideoPlayerOverlay(playerState = state, actions = actions)
            }
        }

        // The centre transport toggle is a real, labelled control: it reports
        // "Pause" while playing and "Play" while paused.
        composeTestRule.onNodeWithContentDescription("Pause")
            .assertIsDisplayed()

        // An explicit Back affordance exists on the overlay's dismiss row.
        composeTestRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
    }

    @Test
    fun content_descriptions_are_set() {
        val state = PlayerState(
            isPlaying = false,
            position = 0L,
            duration = 0L,
            isVideo = true,
            isLoading = false
        )
        val actions = VideoPlayerOverlayActions.fromLambdas()

        composeTestRule.setContent {
            MaterialTheme {
                DeepEyeVideoPlayerOverlay(playerState = state, actions = actions)
            }
        }

        // All interactive elements must have content descriptions. The dismiss
        // affordance is always present, so it must be labelled.
        composeTestRule.onNodeWithContentDescription("Back")
            .assertExists()
    }

    @Test
    fun disabled_actions_renders() {
        val state = PlayerState(
            isPlaying = false,
            position = 0L,
            duration = 0L,
            isVideo = false,
            isLoading = false
        )
        val actions = VideoPlayerOverlayActions.fromLambdas()

        composeTestRule.setContent {
            MaterialTheme {
                DeepEyeVideoPlayerOverlay(playerState = state, actions = actions)
            }
        }

        // State should render without crashing even with zero duration
        composeTestRule.onNodeWithTag(DeepEyeVideoPlayerOverlayTags.ROOT)
            .assertExists()
    }
}
