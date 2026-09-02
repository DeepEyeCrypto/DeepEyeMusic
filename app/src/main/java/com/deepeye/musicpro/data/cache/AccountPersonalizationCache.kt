// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache

import android.util.Log
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe cache for account-scoped personalization sections.
 *
 * Each cache entry is strictly partitioned by the SHA-256 [accountKey].
 * When an account is switched or signed out, calling [invalidateAccount] purges
 * all cached content for that namespace immediately.
 */
@Singleton
class AccountPersonalizationCache @Inject constructor() {

    private data class CacheEntry(
        val items: List<PersonalizedFeedItem>,
        val timestampMillis: Long = System.currentTimeMillis(),
    )

    private val lock = Mutex()
    // Map: accountKey -> (SectionType -> CacheEntry)
    private val accountCache = mutableMapOf<String, MutableMap<PersonalizedSectionType, CacheEntry>>()

    companion object {
        private const val TAG = "AccountPersonalizationCache"
        private const val DEFAULT_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    suspend fun get(
        accountKey: String,
        sectionType: PersonalizedSectionType,
        ttlMs: Long = DEFAULT_TTL_MS,
    ): List<PersonalizedFeedItem>? = lock.withLock {
        val accountMap = accountCache[accountKey] ?: return null
        val entry = accountMap[sectionType] ?: return null
        val age = System.currentTimeMillis() - entry.timestampMillis
        if (age <= ttlMs) {
            val safeKey = if (accountKey.length >= 8) accountKey.take(8) + "..." else "redacted"
            Log.d(TAG, "Cache HIT for account=$safeKey section=$sectionType items=${entry.items.size}")
            entry.items
        } else {
            accountMap.remove(sectionType)
            null
        }
    }

    suspend fun put(
        accountKey: String,
        sectionType: PersonalizedSectionType,
        items: List<PersonalizedFeedItem>,
    ) = lock.withLock {
        val accountMap = accountCache.getOrPut(accountKey) { mutableMapOf() }
        accountMap[sectionType] = CacheEntry(items = items, timestampMillis = System.currentTimeMillis())
        val safeKey = if (accountKey.length >= 8) accountKey.take(8) + "..." else "redacted"
        Log.d(TAG, "Cache PUT for account=$safeKey section=$sectionType items=${items.size}")
    }

    suspend fun invalidateAccount(accountKey: String) = lock.withLock {
        val removed = accountCache.remove(accountKey)
        val safeKey = if (accountKey.length >= 8) accountKey.take(8) + "..." else "redacted"
        Log.d(TAG, "Invalidated cache for account=$safeKey (sections removed=${removed?.size ?: 0})")
    }

    suspend fun clearAll() = lock.withLock {
        accountCache.clear()
        Log.d(TAG, "Cleared all account cache entries")
    }
}
