// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.util

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.deepeye.musicpro.ui.theme.TouchTargets
import androidx.compose.ui.unit.Dp

/**
 * Shared accessibility helpers.
 *
 * The app renders most of its chrome with bare `Modifier.clickable`, which
 * exposes no `Role` to TalkBack and imposes no minimum hit area. These helpers
 * close both gaps so every interactive surface can adopt them consistently.
 *
 * Apply [minTouchTarget] *before* `clickable` in the modifier chain.
 */

/**
 * Expands the hit area to at least the [TouchTargets.Min] (48dp) accessibility
 * floor without changing the visual bounds of the node.
 *
 * This does not distort layout: [sizeIn] constrains only the minimum, so a node
 * already larger than 48dp is untouched.
 */
fun Modifier.minTouchTarget(minSize: Dp = TouchTargets.Min): Modifier =
    this.sizeIn(minWidth = minSize, minHeight = minSize)

/**
 * Marks a node as a button for assistive tech.
 *
 * Compose infers `Role.Button` for [androidx.compose.material3.IconButton] and
 * [androidx.compose.material3.Button] automatically, but not for custom
 * glass/gradient cards built on raw `clickable`.
 */
fun Modifier.semanticsRoleButton(): Modifier = this.semantics { role = Role.Button }

/** Marks a node as a checkbox-style toggle and announces its on/off state. */
fun Modifier.semanticsRoleCheckbox(checked: Boolean): Modifier = this.semantics {
    role = Role.Checkbox
    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
}

/**
 * Marks a node as a navigation tab and announces which one is currently
 * selected, so TalkBack reads "Home, selected, tab 1 of 7" instead of silently
 * treating all destinations as equivalent buttons.
 */
fun Modifier.semanticsRoleTab(selected: Boolean): Modifier = this.semantics {
    role = Role.Tab
    this.selected = selected
}

/**
 * Attaches a human-readable label to a node that has no label of its own.
 *
 * Prefer this over `contentDescription = null` on any node that is clickable;
 * a null description on a *decorative* node is correct, but on an interactive
 * node it silently removes the control from the TalkBack focus order.
 */
fun Modifier.semanticsLabel(label: String): Modifier = this.semantics {
    contentDescription = label
}

/**
 * Announces a non-visual state (e.g. "Favourite", "Downloaded", "Locked") so it
 * is not conveyed by colour or icon alone.
 */
fun Modifier.semanticsState(state: String): Modifier = this.semantics {
    stateDescription = state
}

/**
 * Attaches an explicit accessibility action for nodes driven by raw
 * `pointerInput` gestures (e.g. the swipe-to-skip surface, gesture-locked
 * sliders) that otherwise expose no clickable semantics at all.
 *
 * @param action must return `true` when it consumed the action, per the
 *   `SemanticsPropertyReceiver.onClick` contract.
 */
fun Modifier.semanticsAction(label: String, action: () -> Boolean): Modifier = this.semantics {
    onClick(label = label, action = action)
}

/**
 * Builds a [MutableInteractionSource] for custom clickables that should still
 * show a ripple. Exposed so callers stop allocating one inline per call site.
 */
@Composable
fun rememberRippleSource(): MutableInteractionSource = remember { MutableInteractionSource() }
