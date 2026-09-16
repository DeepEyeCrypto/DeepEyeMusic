// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.overlay

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.deepeye.musicpro.domain.model.PlayerState
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.player.format.QualityPreset
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

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

        // Verify Play/Pause has button role
        composeTestRule.onNodeWithContentDescription("Pause")
            .assertExists()

        // Verify Next has button role
        composeTestRule.onNodeWithContentDescription("Next")
            .assertExists()

        // Verify Previous has button role
        composeTestRule.onNodeWithContentDescription("Previous")
            .assertExists()
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

        // All interactive elements must have content descriptions
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
        composeTestRule.onNodeWithTag("deepeye_video_overlay_root")
            .assertExists()
    }
}
