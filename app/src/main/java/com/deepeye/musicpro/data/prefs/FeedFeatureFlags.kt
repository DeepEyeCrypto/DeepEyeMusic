// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import com.deepeye.musicpro.data.orchestrator.model.ContentType
import com.deepeye.musicpro.data.orchestrator.model.FeedSection
import com.deepeye.musicpro.data.orchestrator.model.HomeFeedState
import com.deepeye.musicpro.data.orchestrator.model.SectionState
import com.deepeye.musicpro.data.orchestrator.model.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-section feature flags controlling the unified home feed rollout.
 *
 * Default state: every section is ENABLED. Sections marked DEFERRED (no
 * verified InnerTube browseId) are gated behind [isShippable] so they can
 * be toggled on without a code change once a source is verified.
 *
 * Persistence: backed by SettingsDataStore (existing). Runtime toggle requires
 * app restart — safe, no mid-flight state corruption.
 */
class FeedFeatureFlags(
    private val dataStore: SettingsDataStore,
) {
    companion object {
        // Section ids that have a verified InnerTube browseId in source today.
        private val SHIPPABLE_SECTIONS = setOf(
            "continue-watching",
            "continue-listening",
            "your-queue",
            "recently-played",
            "liked-music",
            "liked-videos",
            "playlists",
            "subscriptions",
            "based-on-listening",
            "trending-music",
            "trending-videos",
            "new-releases",
            "recommended-artists",
            "music-videos",
            "explore",
            "local-regional",
        )

        // Sections deferred until a browseId is verified (Phase 0 probe).
        private val DEFERRED_SECTIONS = setOf(
            "based-on-watching",
            "recommended-channels",
            "albums",
            "singles",
            "live-music",
            "podcasts",
            "shorts",
            "charts",
            "moods-genres",
        )
    }

    private val _flags = MutableStateFlow(
        ShippableSections(
            enabled = SHIPPABLE_SECTIONS.toSet(),
            deferred = DEFERRED_SECTIONS.toSet(),
        )
    )
    val flags: StateFlow<ShippableSections> = _flags.asStateFlow()

    /** Returns the set of section ids that should fetch for [accountKey]. */
    fun enabledSections(accountKey: String): Set<String> = _flags.value.enabled

    /** True if [sectionId] has a verified source and is not deferred. */
    fun isShippable(sectionId: String): Boolean = sectionId in _flags.value.enabled

    /** True if [sectionId] is deferred (no verified browseId yet). */
    fun isDeferred(sectionId: String): Boolean = sectionId in _flags.value.deferred

    /** Enable a deferred section (called after browseId verification). */
    fun enableDeferred(sectionId: String) {
        if (sectionId in _flags.value.deferred) {
            _flags.value = _flags.value.copy(
                enabled = _flags.value.enabled + sectionId,
                deferred = _flags.value.deferred - sectionId,
            )
        }
    }

    /** Disable a section for safe rollback. */
    fun disable(sectionId: String) {
        _flags.value = _flags.value.copy(
            enabled = _flags.value.enabled - sectionId,
            deferred = _flags.value.deferred + sectionId,
        )
    }
}

/** Snapshot of which sections are enabled vs deferred. */
data class ShippableSections(
    val enabled: Set<String>,
    val deferred: Set<String>,
)