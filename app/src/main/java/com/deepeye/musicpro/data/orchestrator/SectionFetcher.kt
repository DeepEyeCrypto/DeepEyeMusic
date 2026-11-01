// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.orchestrator

import com.deepeye.musicpro.data.orchestrator.model.ContentType
import com.deepeye.musicpro.data.orchestrator.model.FeedItem
import com.deepeye.musicpro.data.orchestrator.SectionResult
import kotlinx.coroutines.Job

/**
 * Input contract for every [SectionFetcher].
 *
 * [generationToken] is the orchestrator's current generation counter. A fetcher
 * MUST drop any response whose token is stale (see [HomeFeedOrchestrator]).
 */
data class FetchInput(
    val generationToken: Long,
    val accountKey: String,
    val thermalTier: ThermalTier,
    val force: Boolean = false,
    val cancellationJob: Job? = null,
)

/**
 * Per-section fetcher plugin.
 *
 * Implementations are one-per-section (one fetcher per rail in the feed).
 * They own NO dispatcher, NO semaphore, NO coroutine scope — the orchestrator
 * provides bounded concurrency via [FetchInput.cancellationJob] and a shared
 * [kotlinx.coroutines.Dispatchers.IO.limitedParallelism(4)].
 *
 * Fetchers are thin: they call ONE data source (auth client, SmartTube, Room,
 * or search) and return a normalized [SectionResult]. Normalization, ranking,
 * caching, and thermal gating are orchestrator concerns.
 */
interface SectionFetcher {
    /** Stable section id, e.g. "continue-watching", "liked-music". */
    val sectionId: String

    /** Content type this section always produces. */
    val contentType: ContentType

    /** Time-to-live for cached entries of this section (see TTL matrix). */
    val ttlMillis: Long

    /** True when the section requires an authenticated account to render. */
    val isAccountRequired: Boolean

    /**
     * Perform one fetch attempt for this section.
     *
     * Implementations MUST:
     *  - Check [input.cancellationJob]?.isActive before and during network I/O.
     *  - Never throw [kotlinx.coroutines.CancellationException] (propagate it).
     *  - Return a [SectionResult] for every outcome, including failure.
     *  - Never write to global state or the shared cache directly.
     */
    suspend fun fetch(input: FetchInput): SectionResult

    /**
     * Load cached items for this section when the live fetch would be a cache
     * hit. Returns null when there is no usable cache entry.
     */
    suspend fun loadCached(input: FetchInput): List<FeedItem>? = null
}

/**
 * Thermal tiers used by the orchestrator to gate concurrency and prefetch.
 *
 * Mirrors the existing [com.deepeye.musicpro.ui.player.visualizer.agsl.ThermalGovernor]
 * enum so the same readings feed both the visualizer and the feed pipeline.
 */
enum class ThermalTier {
    COOL,
    WARM,
    HOT,
    CRITICAL,
}