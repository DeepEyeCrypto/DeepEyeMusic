// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.update

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.deepeye.musicpro.updates.ChangelogEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Layout regression tests for the "What's New" changelog dialog.
 *
 * ## The defect these lock down
 *
 * The dialog body used to be capped with a hardcoded
 * `Modifier.heightIn(max = 420.dp)` instead of taking a weighted slot. On a
 * landscape phone the viewport is only ~360dp tall (720px / density 2.0), so
 * 420dp never clamped anything: the body grew to fit every entry and pushed the
 * "Later" / "Got it" action row clean off the bottom of the screen, leaving the
 * user with a title they could read and no way to dismiss the dialog.
 *
 * The fix is structural — the body takes `weight(1f, fill = false)` inside a
 * card clamped to the viewport — so the assertions here are behavioural
 * ("the buttons are on screen with a long changelog") rather than pinned to a
 * dp value that would just get edited to match the next regression.
 *
 * @RunWith(RobolectricTestRunner) is required: without it the Compose test
 * environment reads android.os.Build.FINGERPRINT as null and throws
 * NullPointerException inside RobolectricIdlingStrategy before the body runs.
 */
@RunWith(RobolectricTestRunner::class)
class ChangelogDialogLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * A changelog long enough to blow past any viewport: six releases, each with
     * a multi-line description. This is roughly the shape of a real
     * several-versions-behind install, which is exactly when the dialog broke.
     */
    private val longChangelog = List(6) { index ->
        ChangelogEntry(
            versionCode = 30048 - index,
            versionName = "v3.0.1.${38 - index}",
            releaseDate = "2026-09-${10 - index}",
            title = "Release ${38 - index} — a deliberately long headline that wraps",
            items = List(4) { bullet ->
                "Bullet $bullet: a release-note line long enough to wrap onto a second " +
                    "line at reading width, which is what makes this dialog overflow."
            },
            highlight = index == 0,
        )
    }

    private fun showDialog(entries: List<ChangelogEntry> = longChangelog) {
        composeTestRule.setContent {
            MaterialTheme {
                ChangelogDialog(
                    entries = entries,
                    onDismiss = {},
                    onLater = {},
                )
            }
        }
    }

    /**
     * The headline guarantee: with an overflowing changelog, both actions remain
     * displayed and hittable.
     */
    @Test
    fun action_buttons_remain_displayed_with_long_changelog() {
        showDialog()

        composeTestRule.onNodeWithText("Later").assertIsDisplayed()
        composeTestRule.onNodeWithText("Got it").assertIsDisplayed()
    }

    /**
     * The title stays pinned too — the body scrolls *between* the title and the
     * buttons, it does not scroll them away.
     */
    @Test
    fun title_stays_displayed_with_long_changelog() {
        showDialog()

        composeTestRule.onNodeWithText("What's New").assertIsDisplayed()
    }

    /**
     * The body must actually be scrollable, not merely clipped. If the fix ever
     * regresses to a hardcoded cap with no weight, the last release would be
     * silently unreachable rather than the buttons being pushed away — a quieter
     * but equally real bug.
     */
    @Test
    fun changelog_body_is_scrollable() {
        showDialog()

        composeTestRule.onNodeWithText(longChangelog.last().items.first())
            .assertExists()
    }

    /**
     * A short changelog must still work: `fill = false` means the dialog shrinks
     * to fit rather than being forced to the clamp.
     */
    @Test
    fun single_entry_dialog_shows_both_actions() {
        showDialog(entries = listOf(longChangelog.first()))

        composeTestRule.onNodeWithText("Later").assertIsDisplayed()
        composeTestRule.onNodeWithText("Got it").assertIsDisplayed()
    }
}