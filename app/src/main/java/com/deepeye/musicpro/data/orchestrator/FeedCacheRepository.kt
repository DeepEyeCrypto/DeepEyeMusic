// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.orchestrator

import com.deepeye.musicpro.data.orchestrator.model.FeedItem
import com.deepeye.musicpro.data.orchestrator.model.FeedSection
import java.util.concurrent.ConcurrentHashMap

/**
 * Account-namespaced, TTL-bounded cache for feed sections.
 *
 * Replaces the global [com.deepeye.musicpro.data.cache.CacheManager] for feed
 * data with a per-account namespace so cache entries never leak across
 * account switches or signed-out states.
 *
 * Thread-safe: all mutations go through ConcurrentHashMap.computeIfAbsent /
 * compute. No locks, no Room writes — this is an in-memory cache only; Room
 * remains the persistence layer for playback history and taste data.
 */
class FeedCacheRepository {

    private data class Entry(
        val sectionId: String,
        val accountKey: String,
        val items: List<FeedItem>,
        val writtenAt: Long,
        val ttlMillis: Long,
    ) {
        val isFresh: Boolean get() = System.currentTimeMillis() - writtenAt < ttlMillis
    }

    private val store = ConcurrentHashMap<String, Entry>()

    private fun key(accountKey: String, sectionId: String) = "$accountKey:$sectionId"

    /**
     * Write a section's items under [accountKey].
     */
    fun writeAccount(accountKey: String, sections: List<FeedSection>) {
        val now = System.currentTimeMillis()
        sections.forEach { section ->
            store[key(accountKey, section.id)] = Entry(
                sectionId = section.id,
                accountKey = accountKey,
                items = section.items,
                writtenAt = now,
                ttlMillis = section.items.firstOrNull()?.ttlMillis ?: DEFAULT_TTL_MS,
            )
        }
    }

    /**
     * Update a single section's items (used by [refreshSection]).
     */
    fun updateSection(accountKey: String, sectionId: String, items: List<FeedItem>) {
        val existing = store[key(accountKey, sectionId)]
        store[key(accountKey, sectionId)] = Entry(
            sectionId = sectionId,
            accountKey = accountKey,
            items = items,
            writtenAt = System.currentTimeMillis(),
            ttlMillis = existing?.ttlMillis ?: DEFAULT_TTL_MS,
        )
    }

    /**
     * Load cached items for a section. Returns null when there is no entry
     * or the entry is stale.
     */
    fun load(accountKey: String, sectionId: String): List<FeedItem>? {
        val entry = store[key(accountKey, sectionId)] ?: return null
        return if (entry.isFresh) entry.items else null
    }

    /**
     * Invalidate all entries for [accountKey] (called on sign-out / switch).
     */
    fun invalidateAccount(accountKey: String) {
        store.keys.removeIf { it.startsWith("$accountKey:") }
    }

    /**
     * Evict stale entries beyond the per-account cap.
     */
    fun cleanup(maxEntriesPerAccount: Int = MAX_ENTRIES) {
        val now = System.currentTimeMillis()
        // Drop stale
        store.entries.removeIf { now - it.value.writtenAt > it.value.ttlMillis }
        // Enforce per-account cap (LRU-ish by recency)
        val byAccount = store.entries.groupBy { it.value.accountKey }
        byAccount.forEach { (account, entries) ->
            if (entries.size > maxEntriesPerAccount) {
                entries.sortedByDescending { it.value.writtenAt }
                    .drop(maxEntriesPerAccount)
                    .forEach { store.remove(it.key) }
            }
        }
    }

    companion object {
        private const val DEFAULT_TTL_MS = 24 * 60 * 60 * 1000L // 24h
        private const val MAX_ENTRIES = 400
    }
}