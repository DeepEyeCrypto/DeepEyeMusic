// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single Source of Truth for *content bounds* — the maximum width a vertically
 * scrolling column, a dialog, or a piece of navigation chrome may occupy.
 *
 * ## Why this object exists
 *
 * [CardGeometry] answers "how big is one card". It cannot answer "how wide is
 * the column of cards", which is a different question and turned out to be the
 * dominant one in landscape.
 *
 * On an ~891dp-wide landscape viewport a `fillMaxWidth()` row and a
 * `weight(1f)` button both resolve to roughly 800dp. A settings row that reads
 * well at 360dp in portrait is then a 1400%-wide strip of dead glass with a
 * single line of text pinned to its left edge. Nothing about the *scale* is
 * wrong — the type and the chrome are already resolving through
 * [ViewportScaler]. The content is simply allowed to stretch without limit,
 * which is the "comically oversized, mostly empty" symptom.
 *
 * `GridCells.Fixed(n)` is the same defect expressed as a column count: it picks
 * how many *columns* fit rather than how *wide* they may get, so on a wide
 * canvas it produces two enormous cards instead of six normal ones.
 *
 * The cure for both is the same: bound the width, then centre the result.
 *
 * ## Contract for consumers
 *
 * - Scrolling lists, settings panes and dialogs: [readingColumn].
 * - Grids: use `GridCells.Adaptive` with a floor from [CardGeometry] or
 *   [dspModuleMinSize]. Never `GridCells.Fixed`.
 * - Navigation chrome: [navigationDock].
 *
 * ## Why these are plain `.dp`
 *
 * Authored sizes, resolved once globally. The root zoom in
 * `DeepEyeMusicTheme` scales every dp uniformly — including the hardcoded
 * internals of Material3 components — so there is no per-call-site scaling
 * factor here and none is needed.
 *
 * These are *chrome and content* dimensions, so they follow the viewport. They
 * are deliberately NOT [TouchTargets]: a touch target is a platform
 * accessibility guarantee, and `TouchTargets` compensates for the zoom itself.
 *
 * Exposed as `get()` accessors for the same reason as [CardGeometry]: a `val`
 * would capture the value once at class-load time and keep reporting it after a
 * rotation.
 */
object ContentBounds {

    /**
     * Widest a vertically scrolling list of settings may become.
     *
     * 600dp is the conventional measure for body copy — Twitter/X, Gmail and
     * GitHub all cap their reading columns in this neighbourhood — and it is
     * the point at which a two-line setting description stops needing to be
     * shortened for its own sake.
     *
     * On a landscape phone this resolves to roughly 400dp, which still fits
     * three settings rows side by side with room for their icons, while
     * leaving balanced margins rather than one item marooned at each edge.
     */
    val readingColumn: Dp get() = 600.dp

    /**
     * Widest a modal dialog may become.
     *
     * Narrower than [readingColumn] because a dialog additionally carries a
     * header and an action row, and because it must never touch the screen
     * edges — a floating card that fills its window stops reading as floating.
     */
    val dialog: Dp get() = 560.dp

    /**
     * Widest the bottom navigation dock may become.
     *
     * The dock's tab cells were `weight(1f)` inside a `fillMaxWidth()` capsule,
     * so on a wide viewport each tab became ~120dp wide to hold a 20dp glyph —
     * a row of seven tiny icons adrift in a stadium of empty glass. Bounding the
     * dock keeps the capsule tight around its contents while leaving the
     * background art visible at the sides, which is the intended composition.
     */
    val navigationDock: Dp get() = 560.dp

    /**
     * Adaptive-grid floor for the DSP module cards (Viper Bass, Pre-Gain, EQ…).
     *
     * These are control surfaces, not thumbnails: each carries a switch, a
     * title and a slider or curve, so it needs more room than a cover-art tile.
     * Fixed(2) or 260dp packs two spacious cards across on landscape viewports.
     */
    val dspModuleMinSize: Dp get() = 260.dp

    /**
     * Adaptive-grid floor for square content tiles (badges, artist pickers).
     *
     * Matches the density the music grid already uses, so badge and album tiles
     * keep a consistent column rhythm across the app.
     */
    val tileMinSize: Dp get() = 120.dp
}

/**
 * Bounds this node to [maxWidth] and centres it within its parent.
 *
 * ## Modifier order is load-bearing
 *
 * `fillMaxWidth()` runs *outside* `widthIn(max = ...)`, which coerces the
 * incoming constraints to `max = min(parentMax, maxWidth)` before the child is
 * measured. The child therefore reports exactly [maxWidth] on a viewport wider
 * than that, and exactly the parent's width on a narrower one — so the same
 * call site behaves correctly on a 360dp split-screen pane and a 1200dp
 * desktop with no `if` at the call site.
 *
 * Centring itself is the *caller's* job, because only the caller knows whether
 * it is in a `ColumnScope` (`.align(Alignment.CenterHorizontally)`) or a
 * `BoxScope` (`contentAlignment = Alignment.TopCenter`). Baking one of those in
 * here would break the other.
 *
 * Deliberately applies no vertical constraint, so it composes with
 * `Modifier.weight(1f)` in a scrolling `Column` without changing the height
 * budget.
 */
fun Modifier.boundedContent(maxWidth: Dp = ContentBounds.readingColumn): Modifier =
    fillMaxWidth().widthIn(max = maxWidth)