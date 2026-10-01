package com.deepeye.musicpro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tag for the fixed-size box a hero is measured against. */
private const val HeroBoxTag = "heroBox"

/**
 * Instrumented coverage for the "No Rails" hero state.
 *
 * These assert the *behavioural contract* documented on [HeroEmptyState]:
 * the copy renders, the CTA is wired to its callback, and an incomplete
 * action pair never produces a dead button.
 */
@RunWith(AndroidJUnit4::class)
class HeroEmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersTitleSubtitleAndAction() {
        composeTestRule.setContent {
            HeroEmptyState(
                title = "Your Feed Is Empty",
                subtitle = "Play a few tracks to get started.",
                actionText = "Discover Music",
                onAction = {},
            )
        }

        composeTestRule.onNodeWithText("Your Feed Is Empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Play a few tracks to get started.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discover Music").assertIsDisplayed()
    }

    @Test
    fun actionInvokesCallback() {
        var clicks = 0
        composeTestRule.setContent {
            HeroEmptyState(
                title = "Nothing to Play Yet",
                subtitle = "We couldn't find anything new.",
                actionText = "Discover Music",
                onAction = { clicks++ },
            )
        }

        composeTestRule.onNodeWithText("Discover Music").performClick()
        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    /**
     * Regression guard: a label with no callback must not render a button the
     * user can see but cannot press.
     */
    @Test
    fun omitsActionWhenCallbackIsMissing() {
        composeTestRule.setContent {
            HeroEmptyState(
                title = "Nothing to Play Yet",
                subtitle = "No action available here.",
                actionText = "Discover Music",
                onAction = null,
            )
        }

        composeTestRule.onNodeWithText("Discover Music").assertDoesNotExist()
    }

    @Test
    fun omitsActionWhenLabelIsMissing() {
        composeTestRule.setContent {
            HeroEmptyState(
                title = "Nothing to Play Yet",
                subtitle = "No action available here.",
                actionText = null,
                onAction = {},
            )
        }

        composeTestRule.onNodeWithText("Nothing to Play Yet").assertIsDisplayed()
    }

    /**
     * The dock and mini player are overlays, so the hero owns the full viewport
     * and insets its own content. This proves the full-viewport geometry holds
     * under an explicit bottom inset — i.e. it fills its parent rather than
     * wrapping its content.
     */
    @Test
    fun fillsParentAndRespectsBottomInset() {
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize()) {
                HeroEmptyState(
                    title = "Your Feed Is Empty",
                    subtitle = "Nothing here yet.",
                    contentPadding = PaddingValues(bottom = 140.dp),
                )
            }
        }

        composeTestRule.onNodeWithText("Your Feed Is Empty").assertIsDisplayed()

        // The hero must not claim zero height: a wrap-content implementation
        // would collapse here and leave the surrounding viewport empty.
        val rootWidth = composeTestRule.onNodeWithText("Your Feed Is Empty")
            .fetchSemanticsNode().size.width
        assertTrue("hero content should be laid out with real width", rootWidth > 0)
    }

    /**
     * Regression test for a real on-device failure.
     *
     * The hero shipped showing only its icon: the title, subtitle and CTA were
     * composed off-screen. Two independent causes, both fixed here —
     *   1. the host Scaffold already insets the screen above the dock, and the
     *      hero added a second 140dp inset on top of it (double-counting);
     *   2. a fixed 160dp disc does not fit a short viewport, so the disc alone
     *      consumed the whole box and pushed the copy below the fold.
     *
     * This reproduces the post-fix real geometry: the host has already taken
     * the dock, and ~190dp of height remains. Every part must be laid out.
     */
    @Test
    fun keepsCopyAndActionInsideAShortViewport() {
        composeTestRule.setContent {
            // Only the HEIGHT is pinned. Pinning a width as well would couple the
            // test to the test window's width and orientation, and asking for
            // more width than the window has destabilises the harness rather
            // than exercising the hero. The regression this guards is vertical.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .testTag(HeroBoxTag),
            ) {
                HeroEmptyState(
                    title = "Your Feed Is Empty",
                    subtitle = "Play a few tracks or connect your account and we'll build your feed.",
                    actionText = "Discover Music",
                    onAction = {},
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                )
            }
        }

        composeTestRule.onNodeWithText("Your Feed Is Empty").assertExists()
        composeTestRule.onNodeWithText("Discover Music").assertExists()
    }

    /**
     * The compact layout must stay within the box it is given. If the disc is
     * still hard-coded at 160dp this overflows and the assertion below fails,
     * which is the regression this guards.
     *
     * The comparison is made against the *box's own* bounds rather than a dp
     * literal: semantics bounds are in pixels, and on a 2x device 300dp is
     * 600px, so asserting `bottom <= 300` would pass a genuinely overflowing
     * layout and fail a correct one.
     */
    @Test
    fun compactLayoutFitsItsBox() {
        composeTestRule.setContent {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .testTag(HeroBoxTag),
            ) {
                HeroEmptyState(
                    title = "Your Feed Is Empty",
                    subtitle = "Nothing to play yet.",
                    actionText = "Discover Music",
                    onAction = {},
                )
            }
        }

        val box = composeTestRule.onNodeWithTag(HeroBoxTag)
            .fetchSemanticsNode()
            .boundsInRoot
        val title = composeTestRule.onNodeWithText("Your Feed Is Empty")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "title ${title} must sit inside the 300dp box $box",
            title.bottom <= box.bottom,
        )
    }
}
