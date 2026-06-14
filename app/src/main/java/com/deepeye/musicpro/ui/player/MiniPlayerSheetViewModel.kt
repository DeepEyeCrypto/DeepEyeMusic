// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player

import androidx.lifecycle.ViewModel
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MiniPlayerSheetViewModel
@Inject
constructor(
    private val playerController: PlayerController,
) : ViewModel() {
    private val _state = MutableStateFlow(MiniPlayerSheetState())
    val state = _state.asStateFlow()

    fun expand() {
        _state.update { it.copy(anchor = MiniSheetAnchor.EXPANDED) }
    }

    fun collapse() {
        _state.update { it.copy(anchor = MiniSheetAnchor.COLLAPSED) }
    }

    fun halfExpand() {
        _state.update { it.copy(anchor = MiniSheetAnchor.HALF_EXPANDED) }
    }

    fun nextTrack() = playerController.next()

    fun previousTrack() = playerController.previous()

    fun togglePlayPause() = playerController.togglePlayPause()

    fun setGestureLocked(locked: Boolean) {
        _state.update { it.copy(isGestureLocked = locked) }
    }
}
