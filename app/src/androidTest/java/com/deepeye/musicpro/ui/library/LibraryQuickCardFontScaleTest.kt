// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.library

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Font-scale behaviour of the Library quick-access tiles.
 *
 * The regression these guard: a fixed `160.dp` card left the label only a 66dp
 * text column, which "Downloads" only just filled at 1.0x (measured 65px against
 * 66px available on a real device). That is a hair from breaking across two
 * lines, and certain to break as the user's font scale rises.
 *
 * Why this must be an instrumented test:
 *  - MainActivity pins `LocalDensity.fontScale` to `coerceAtMost(1.0f)`, so no app
 *    content ever renders above 1.0x on a handset. A manual device pass at 2.0x is
 *    a no-op -- verified: the Library renders byte-identically at 1.0x and 2.0x.
 *    Overriding the CompositionLocal is the only way to drive scale.
 *  - A Robolectric (src/test) version was written first and discarded: Robolectric
 *    measures "Downloads" as a constant 10px that ignores fontScale, so nothing
 *    ever wraps and the assertion passed even against the buggy `.width(160.dp)`.
 *    Only the real runtime measures text faithfully, which is what makes a wrap
 *    detectable at all.
 */
@RunWith(AndroidJUnit4::class)
class LibraryQuickCardFontScaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Rendered at the label's own text style and short enough that it can never
     * wrap, so its height is exactly one line. Comparing against it detects a
     * wrap without hard-coding device font metrics.
     */
    private val singleLineProbe = "Mg"

    // setContent() may only be called once per test, so every scenario drives one
    // content tree and mutates these to force recomposition.
    private var fontScale by mutableStateOf(1.0f)
    private var label by mutableStateOf("Downloads")
    private var count by mutableStateOf(12)

    private fun setState(scale: Float, labelText: String = label, itemCount: Int = count) {
        fontScale = scale
        label = labelText
        count = itemCount
    }

    private fun mount() {
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale)
            ) {
                MaterialTheme {
                    Row {
                        LibraryQuickCard(
                            icon = Icons.Filled.Download,
                            label = label,
                            count = count,
                            accentColor = Color.Cyan,
                            onClick = {},
                        )
                        Text(
                            text = singleLineProbe,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    private fun boundsOf(text: String): Rect =
        composeTestRule.onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    private fun assertLabelStaysOnOneLine(target: String, scale: Float) {
        val labelHeight = boundsOf(target).height
        val lineHeight = boundsOf(singleLineProbe).height
        assertTrue(
            "label \"$target\" wrapped at fontScale=$scale: height $labelHeight px " +
                "against a $lineHeight px single line",
            labelHeight <= lineHeight,
        )
    }

    @Test
    fun label_stays_on_one_line_at_default_font_scale() {
        setState(1.0f)
        mount()
        assertLabelStaysOnOneLine("Downloads", 1.0f)
    }

    @Test
    fun label_stays_on_one_line_at_large_font_scale() {
        setState(2.0f)
        mount()
        assertLabelStaysOnOneLine("Downloads", 2.0f)
    }

    @Test
    fun every_quick_access_label_stays_on_one_line_at_large_font_scale() {
        // "Downloads" is the widest label; assert the whole set anyway so a future
        // label addition cannot quietly reintroduce the wrap.
        setState(2.0f)
        mount()
        listOf("Liked", "Saved", "Playlists", "Downloads", "Recent", "Offline").forEach {
            setState(2.0f, labelText = it)
            assertLabelStaysOnOneLine(it, 2.0f)
        }
    }

    @Test
    fun card_grows_with_the_font_scale_instead_of_overflowing() {
        // The adaptive half of the fix: widthIn(min, max) lets the tile widen as
        // text grows. A fixed 160.dp card would keep its width and wrap instead.
        setState(1.0f)
        mount()
        val narrow = boundsOf("Downloads").width

        setState(2.0f)
        val wide = boundsOf("Downloads").width

        assertTrue(
            "label should occupy more width at 2.0x than at 1.0x ($narrow -> $wide) " +
                "if the card is genuinely adaptive",
            wide > narrow,
        )
    }

    // Deliberately one state per test: mutating the snapshot state after mount()
    // tears the composition down and every later query fails with "No compose
    // hierarchies found". Mounting once per scenario avoids that entirely.

    @Test
    fun count_label_singular_at_large_font_scale() {
        setState(2.0f, itemCount = 1)
        mount()
        composeTestRule.onNodeWithText("1 item", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("1 items", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun count_label_plural_for_many_at_large_font_scale() {
        setState(2.0f, itemCount = 12)
        mount()
        composeTestRule.onNodeWithText("12 items", useUnmergedTree = true).assertExists()
    }

    @Test
    fun count_label_plural_for_zero() {
        setState(1.0f, itemCount = 0)
        mount()
        composeTestRule.onNodeWithText("0 items", useUnmergedTree = true).assertExists()
    }
}
