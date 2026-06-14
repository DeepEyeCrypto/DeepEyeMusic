// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

enum class MiniSheetAnchor {
    COLLAPSED,
    HALF_EXPANDED,
    EXPANDED,
}

data class MiniPlayerSheetState(
    val anchor: MiniSheetAnchor = MiniSheetAnchor.COLLAPSED,
    val offsetY: Float = 0f,
    val velocityY: Float = 0f,
    val dragEnabled: Boolean = true,
    val isGestureLocked: Boolean = false,
)
