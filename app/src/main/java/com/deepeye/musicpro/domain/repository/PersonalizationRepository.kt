// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository providing account-aware, privacy-safe, lifecycle-safe music personalization feeds.
 *
 * Integrates local playback history, local queue, on-device affinity models, and authenticated
 * account feeds into structured, section-based feeds.
 */
interface PersonalizationRepository {

    /**
     * Observes the active personalized feed state, reacting automatically to account changes,
     * local queue mutations, and listening history updates.
     */
    fun observePersonalizedFeed(): StateFlow<PersonalizedFeedState>

    /**
     * Refreshes the full feed. If [forceRefresh] is true, remote and local caches are re-queried.
     */
    suspend fun refreshFeed(forceRefresh: Boolean = false)

    /**
     * Refreshes a single section, isolating errors to that specific section.
     */
    suspend fun refreshSection(sectionType: PersonalizedSectionType)

    /**
     * Clears all cached data for a specific account key.
     * Called during account switches or sign-outs.
     */
    suspend fun invalidateAccountCache(accountKey: String)
}
