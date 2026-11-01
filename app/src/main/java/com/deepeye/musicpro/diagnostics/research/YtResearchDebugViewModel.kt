// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug-only ViewModel for the InnerTube research probe trigger (plan Task 4).
 *
 * Emits a short, non-sensitive status string (counts + endpoint names + video
 * ids). No tokens, headers, or track titles are ever exposed — matching the
 * MASVS PII rule used across AuthenticatedYouTubeClient.
 */
@HiltViewModel
class YtResearchDebugViewModel
    @Inject
    constructor(
        private val probe: InnerTubeResearchProbe,
    ) : ViewModel() {
        private val _status = MutableStateFlow("Idle")
        val status: StateFlow<String> = _status

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        /**
         * Runs all three probes for [seedVideoId] and reports a summary.
         * @param seedVideoId bare videoId, no raw payload (FEL-400 contract).
         */
        fun triggerResearch(seedVideoId: String) {
            if (_running.value) return // no duplicate prefetch storm
            viewModelScope.launch {
                _running.value = true
                _status.value = "Running probes..."
                try {
                    val home = probe.probeMusicHome()
                    val graph = probe.probeAutoplayGraph(seedVideoId)
                    val related = probe.captureRelatedVideos(seedVideoId)
                    _status.value =
                        "OK: ${home.size} home shelves, ${graph.size} autoplay captures, $related"
                } catch (e: Exception) {
                    _status.value = "Error: ${e.message?.take(60) ?: "unknown"}"
                } finally {
                    _running.value = false
                }
            }
        }
    }
