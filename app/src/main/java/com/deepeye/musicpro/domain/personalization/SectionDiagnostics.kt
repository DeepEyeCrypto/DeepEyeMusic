// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.personalization

/**
 * Debug-only diagnostics snapshot attached to a [PersonalizedSection] at build time.
 * Accessible via the Diagnostics screen; not shown in release builds.
 */
data class SectionDiagnostics(
    val sectionId: String,
    val sectionTitle: String,
    val source: CacheSource,
    val accountScopeHash: String?,
    val cacheAgeMs: Long,
    val refreshState: RefreshState,
    val itemCount: Int,
    val reason: String?,
    val rebuiltByDiversityRanker: Boolean,
    val hiddenItemsPruned: Int,
) {
    enum class CacheSource { LIVE, MEMORY_CACHE, ROOM_CACHE, LOCAL_ONLY }
    enum class RefreshState { FRESH, STALE_WHILE_REVALIDATE, OFFLINE }

    fun formatCacheAge(): String {
        val secs = (cacheAgeMs / 1000).coerceAtLeast(0)
        return when {
            secs < 60 -> "${secs}s"
            secs < 3600 -> "${secs / 60}m ${secs % 60}s"
            else -> "${secs / 3600}h ${(secs % 3600) / 60}m"
        }
    }
}
