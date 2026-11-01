// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.orchestrator

import com.deepeye.musicpro.data.orchestrator.model.FeedItem
import com.deepeye.musicpro.data.orchestrator.model.FeedSection
import com.deepeye.musicpro.data.orchestrator.model.SectionState

/**
 * Result contract for a single [SectionFetcher] invocation.
 *
 * A failed section MUST NEVER abort sibling sections or the orchestrator's
 * overall emission. Each variant carries enough information for the UI to
 * render a rail-level state (ready / empty / error / offline / deferred).
 */
sealed interface SectionResult {
    val sectionId: String
    val latencyMs: Long

    /** Fetch succeeded; [items] may be empty (legitimate empty state). */
    data class Success(
        override val sectionId: String,
        val items: List<FeedItem>,
        val fromCache: Boolean,
        override val latencyMs: Long,
    ) : SectionResult

    /** Source returned zero items legitimately (e.g. no listening history yet). */
    data class Empty(
        override val sectionId: String,
        val reason: String,
        override val latencyMs: Long,
    ) : SectionResult

    /** Network, parse, or timeout failure. [retryable] governs auto-retry. */
    data class Error(
        override val sectionId: String,
        val message: String,
        val retryable: Boolean,
        override val latencyMs: Long,
        val attemptCount: Int = 1,
    ) : SectionResult

    /** No connectivity and cache is stale or absent. */
    data class Offline(
        override val sectionId: String,
        val cachedItems: List<FeedItem> = emptyList(),
        override val latencyMs: Long = 0L,
    ) : SectionResult

    /** Auth client returned 401/403; section hidden until re-auth. */
    data class Unauthorized(
        override val sectionId: String,
        override val latencyMs: Long = 0L,
    ) : SectionResult

    /** Thermal tier is HOT/CRITICAL; fetch deferred to next thermal window. */
    data class ThermalDeferred(
        override val sectionId: String,
        override val latencyMs: Long = 0L,
    ) : SectionResult

    companion object {
        fun toSectionState(result: SectionResult): SectionState = when (result) {
            is SectionResult.Success -> if (result.items.isEmpty()) SectionState.Empty else SectionState.Ready
            is SectionResult.Empty -> SectionState.Empty
            is SectionResult.Error -> SectionState.Error
            is SectionResult.Offline -> SectionState.Offline
            is SectionResult.Unauthorized -> SectionState.Unauthorized
            is SectionResult.ThermalDeferred -> SectionState.ThermalDeferred
        }
    }
}