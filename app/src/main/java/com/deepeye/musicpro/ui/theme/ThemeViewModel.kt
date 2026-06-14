// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.prefs.AppSettings
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.util.ColorExtractor
import com.deepeye.musicpro.util.ExtractedColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel
@Inject
constructor(
    private val playerController: PlayerController,
    private val colorExtractor: ColorExtractor,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {
    private val _dynamicColors = MutableStateFlow<ExtractedColors?>(null)
    val dynamicColors: StateFlow<ExtractedColors?> = _dynamicColors.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    init {
        observeCurrentMedia()
    }

    private fun observeCurrentMedia() {
        viewModelScope.launch {
            playerController.playerState
                .map { it.currentItem }
                .distinctUntilChanged()
                .collectLatest { mediaItem ->
                    mediaItem?.artworkUri?.let { uri ->
                        val colors = colorExtractor.extractColors(uri)
                        _dynamicColors.value = colors
                    } ?: run {
                        _dynamicColors.value = null
                    }
                }
        }
    }
}
