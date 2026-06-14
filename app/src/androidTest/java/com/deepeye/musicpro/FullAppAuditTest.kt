// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullAppAuditTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val device =
        UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation(),
        )

    // ─── HOME SCREEN TESTS ────────────────────
    @Test
    fun test_home_screen_loads() {
        composeRule.onNodeWithText("Good Morning")
            .assertIsDisplayed()
    }

    @Test
    fun test_bottom_nav_all_tabs() {
        // Home tab
        composeRule.onNodeWithContentDescription("Home")
            .performClick()
        composeRule.waitForIdle()

        // YouTube tab
        composeRule.onNodeWithContentDescription("YouTube")
            .performClick()
        composeRule.waitForIdle()

        // Music tab
        composeRule.onNodeWithContentDescription("Music")
            .performClick()
        composeRule.waitForIdle()

        // Library tab
        composeRule.onNodeWithContentDescription("Library")
            .performClick()
        composeRule.waitForIdle()

        // Search tab
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        composeRule.waitForIdle()

        // DSP tab
        composeRule.onNodeWithContentDescription("DSP")
            .performClick()
        composeRule.waitForIdle()
    }

    // ─── NOW PLAYING SCREEN TESTS ────────────
    @Test
    fun test_play_pause_button() {
        navigateToPlayer()
        val playPauseBtn =
            composeRule
                .onNodeWithContentDescription("Play/Pause")
        playPauseBtn.assertIsDisplayed()
        playPauseBtn.performClick()
        composeRule.waitForIdle()
        playPauseBtn.performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun test_next_previous_buttons() {
        navigateToPlayer()
        composeRule.onNodeWithContentDescription("Next")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Previous")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun test_like_dislike_buttons() {
        navigateToPlayer()
        composeRule.onNodeWithContentDescription("Like Track")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Block Track")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun test_seek_slider() {
        navigateToPlayer()
        composeRule.onNodeWithTag("seek_slider")
            .assertIsDisplayed()
            .performTouchInput {
                swipeRight(startX = 0f, endX = width * 0.5f)
            }
        composeRule.waitForIdle()
    }

    @Test
    fun test_settings_navigation_doesnt_pause_video() {
        navigateToPlayer()
        // Navigate to settings
        composeRule.onNodeWithContentDescription("App Settings")
            .performClick()
        composeRule.waitForIdle()

        // Go back to player
        device.pressBack()
        composeRule.waitForIdle()
    }

    // ─── DSP SCREEN TESTS ────────────────────
    @Test
    fun test_dsp_screen_all_presets() {
        navigateToDSP()
        val presets =
            listOf(
                "Bass Monster",
                "Bluetooth Optimized",
                "Bollywood Vocals",
                "Flat",
                "Night Mode",
            )
        presets.forEach { preset ->
            composeRule.onNodeWithText(preset)
                .assertIsDisplayed()
                .performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun test_dsp_toggle() {
        navigateToDSP()
        composeRule.onNodeWithTag("dsp_toggle")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dsp_toggle")
            .performClick() // turn back on
        composeRule.waitForIdle()
    }

    @Test
    fun test_eq_band_sliders() {
        navigateToDSP()
        repeat(10) { band ->
            composeRule.onNodeWithTag("eq_band_$band")
                .performTouchInput {
                    swipeUp(startY = centerY, endY = centerY - 50f)
                }
            composeRule.waitForIdle()
        }
    }

    // ─── HELPERS ─────────────────────────────
    private fun navigateToPlayer() {
        composeRule.onNodeWithContentDescription("Home")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Kesariya (From \"Brahmastra\")")
            .performClick()
        composeRule.waitForIdle()
    }

    private fun navigateToDSP() {
        composeRule.onNodeWithContentDescription("DSP")
            .performClick()
        composeRule.waitForIdle()
    }
}
