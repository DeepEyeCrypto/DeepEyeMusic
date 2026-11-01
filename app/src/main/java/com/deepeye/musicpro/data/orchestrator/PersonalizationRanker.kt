// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.orchestrator

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.data.orchestrator.model.ContentType
import com.deepeye.musicpro.data.orchestrator.model.FeedItem
import com.deepeye.musicpro.data.orchestrator.model.FeedSection
import com.deepeye.musicpro.data.orchestrator.SectionResult
import com.deepeye.musicpro.data.orchestrator.ThermalTier

/**
 * Ranking and diversity engine for the unified home feed.
 *
 * Takes raw [SectionResult]s from parallel fetchers and produces
 * [FeedSection]s ordered for the UI. Music and Video share one ranker so
 * cross-domain affinity (music listening → music-video recommendations)
 * is a first-class signal, not a bolt-on.
 *
 * Current behavior is a safe default (recency + source order). Phase 4
 * replaces this with the full affinity + MMR diversity pipeline.
 */
class PersonalizationRanker {

    /**
     * Rank a set of [SectionResult]s into ordered [FeedSection]s.
     *
     * @param results raw fetcher outputs (one per enabled section)
     * @param accountSession current account state (drives auth-gated ordering)
     * @param thermalTier current device thermal tier (drives prefetch ordering)
     * @return ordered, UI-ready sections
     */
    fun rank(
        results: List<SectionResult>,
        accountSession: AccountSession,
        thermalTier: ThermalTier,
    ): List<FeedSection> {
        return results
            .map { result ->
                val items = when (result) {
                    is SectionResult.Success -> result.items
                    is SectionResult.Offline -> result.cachedItems
                    else -> emptyList()
                }
                val state = SectionResult.toSectionState(result)
                FeedSection(
                    id = result.sectionId,
                    contentType = inferContentType(result.sectionId),
                    title = humanizeSectionId(result.sectionId),
                    subtitle = null,
                    sourceLabel = result.sectionId,
                    explanation = null,
                    items = items,
                    state = state,
                    error = when (result) {
                        is SectionResult.Error -> result.message
                        else -> null
                    },
                    canRetry = when (result) {
                        is SectionResult.Error -> result.retryable
                        is SectionResult.Offline -> true
                        is SectionResult.ThermalDeferred -> true
                        else -> false
                    },
                    isAccountRequired = requiresAccount(result.sectionId),
                    isFromCache = when (result) {
                        is SectionResult.Success -> result.fromCache
                        is SectionResult.Offline -> true
                        else -> false
                    },
                    lastUpdatedMillis = System.currentTimeMillis(),
                    failureReason = when (result) {
                        is SectionResult.Error -> "network"
                        is SectionResult.Unauthorized -> "auth"
                        is SectionResult.ThermalDeferred -> "thermal"
                        else -> null
                    },
                )
            }
            .sortedByDescending { section ->
                // Auth-gated sections sort after local-only sections so the UI
                // shows what's immediately available first.
                if (section.isAccountRequired && accountSession !is AccountSession.Connected) {
                    Int.MIN_VALUE
                } else {
                    section.items.size
                }
            }
    }

    private fun inferContentType(sectionId: String): ContentType =
        when {
            sectionId.contains("video", ignoreCase = true) ||
                sectionId.contains("watching", ignoreCase = true) ||
                sectionId.contains("shorts", ignoreCase = true) ||
                sectionId.contains("live", ignoreCase = true) ||
                sectionId.contains("podcast", ignoreCase = true) ||
                sectionId.contains("channel", ignoreCase = true) -> ContentType.VIDEO

            sectionId.contains("music", ignoreCase = true) ||
                sectionId.contains("album", ignoreCase = true) ||
                sectionId.contains("single", ignoreCase = true) ||
                sectionId.contains("playlist", ignoreCase = true) ||
                sectionId.contains("artist", ignoreCase = true) -> ContentType.MUSIC

            sectionId.contains("queue", ignoreCase = true) ||
                sectionId.contains("recently", ignoreCase = true) ||
                sectionId.contains("continue", ignoreCase = true) ||
                sectionId.contains("charts", ignoreCase = true) ||
                sectionId.contains("explore", ignoreCase = true) -> ContentType.MIXED

            else -> ContentType.MIXED
        }

    private fun humanizeSectionId(sectionId: String): String =
        sectionId.replaceFirstChar { it.uppercase() }
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                if (word.length <= 3) word else word.replaceFirstChar { it.uppercase() }
            }

    private fun requiresAccount(sectionId: String): Boolean =
        sectionId in setOf(
            "liked-music", "liked-videos", "subscriptions",
            "based-on-listening", "based-on-watching",
            "recommended-artists", "recommended-channels",
            "trending-music", "trending-videos", "new-releases",
            "albums", "singles", "live-music", "podcasts",
            "music-videos", "shorts", "charts", "moods-genres",
        )
}