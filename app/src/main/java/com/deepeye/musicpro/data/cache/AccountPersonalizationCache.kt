// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache

import android.net.Uri
import android.util.Log
import com.deepeye.musicpro.data.cache.dao.PersonalizedSectionDao
import com.deepeye.musicpro.data.cache.entities.CachedPersonalizedItemEntity
import com.deepeye.musicpro.data.cache.entities.CachedPersonalizedSectionEntity
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class CachedFeedSectionResult(
    val items: List<PersonalizedFeedItem>,
    val title: String? = null,
    val subtitle: String? = null,
    val sourceLabel: String? = null,
    val explanation: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
    val isStale: Boolean = false,
)

@Singleton
class AccountPersonalizationCache @Inject constructor() {

    /**
     * Injected by Hilt in production to enable Room SQLite persistence.
     * Nullable so the cache also operates purely in-memory during unit tests.
     */
    @Inject
    @JvmField
    var personalizedSectionDao: PersonalizedSectionDao? = null

    private data class MemoryEntry(
        val items: List<PersonalizedFeedItem>,
        val title: String? = null,
        val subtitle: String? = null,
        val sourceLabel: String? = null,
        val explanation: String? = null,
        val timestampMillis: Long = System.currentTimeMillis(),
    )

    private val lock = Mutex()
    private val memoryCache = mutableMapOf<String, MutableMap<PersonalizedSectionType, MemoryEntry>>()

    companion object {
        private const val TAG = "AccountPersonalizationCache"
        private const val DEFAULT_TTL_MS = 10 * 60 * 1000L
        private const val PERSISTENT_TTL_MS = 24 * 3600 * 1000L
    }

    suspend fun get(
        accountKey: String,
        sectionType: PersonalizedSectionType,
        allowStale: Boolean = false,
        ttlMs: Long = DEFAULT_TTL_MS,
    ): List<PersonalizedFeedItem>? {
        return getFullSection(accountKey, sectionType, allowStale, ttlMs)?.items
    }
    suspend fun getFullSection(
        accountKey: String,
        sectionType: PersonalizedSectionType,
        allowStale: Boolean = false,
        ttlMs: Long = DEFAULT_TTL_MS,
    ): CachedFeedSectionResult? = lock.withLock {
        val now = System.currentTimeMillis()
        val memEntry = memoryCache[accountKey]?.get(sectionType)
        if (memEntry != null) {
            val age = now - memEntry.timestampMillis
            val isExpired = age > ttlMs
            if (!isExpired || allowStale) {
                return@withLock CachedFeedSectionResult(
                    items = memEntry.items,
                    title = memEntry.title,
                    subtitle = memEntry.subtitle,
                    sourceLabel = memEntry.sourceLabel,
                    explanation = memEntry.explanation,
                    cachedAt = memEntry.timestampMillis,
                    isStale = isExpired,
                )
            } else {
                memoryCache[accountKey]?.remove(sectionType)
            }
        }

        val dao = personalizedSectionDao ?: return@withLock null
        try {
            val sectionEntity = dao.getSection(accountKey, sectionType.name) ?: return@withLock null
            val isExpired = now > sectionEntity.expiresAt
            if (isExpired && !allowStale) return@withLock null

            val itemEntities = dao.getItemsForSection(accountKey, sectionType.name)
            if (itemEntities.isEmpty()) return@withLock null

            val feedItems = itemEntities.map { entity ->
                val type = try { PersonalizedItemType.valueOf(entity.itemType) } catch (e: Exception) { PersonalizedItemType.SONG }
                PersonalizedFeedItem(
                    id = entity.itemId,
                    title = entity.title,
                    artist = entity.artist,
                    channelId = entity.channelId,
                    artworkUrl = entity.artworkUrl,
                    durationMs = entity.durationMs,
                    itemType = type,
                    sourceBadge = entity.sourceBadge,
                    explanation = entity.explanation,
                    mediaItem = MediaItem.Remote(
                        id = entity.itemId,
                        title = entity.title,
                        artist = entity.artist,
                        artworkUri = entity.artworkUrl?.let { Uri.parse(it) },
                        duration = entity.durationMs,
                    ),
                )
            }

            memoryCache.getOrPut(accountKey) { mutableMapOf() }[sectionType] = MemoryEntry(
                items = feedItems,
                title = sectionEntity.title,
                subtitle = sectionEntity.subtitle,
                sourceLabel = sectionEntity.sourceLabel,
                explanation = sectionEntity.explanation,
                timestampMillis = sectionEntity.cachedAt,
            )

            return@withLock CachedFeedSectionResult(
                items = feedItems,
                title = sectionEntity.title,
                subtitle = sectionEntity.subtitle,
                sourceLabel = sectionEntity.sourceLabel,
                explanation = sectionEntity.explanation,
                cachedAt = sectionEntity.cachedAt,
                isStale = isExpired,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Room cache account=$accountKey section=$sectionType", e)
            return@withLock null
        }
    }


    suspend fun put(
        accountKey: String,
        sectionType: PersonalizedSectionType,
        items: List<PersonalizedFeedItem>,
        title: String? = null,
        subtitle: String? = null,
        sourceLabel: String? = null,
        explanation: String? = null,
        ttlMs: Long = DEFAULT_TTL_MS,
    ) = lock.withLock {
        val now = System.currentTimeMillis()
        memoryCache.getOrPut(accountKey) { mutableMapOf() }[sectionType] = MemoryEntry(
            items = items,
            title = title,
            subtitle = subtitle,
            sourceLabel = sourceLabel,
            explanation = explanation,
            timestampMillis = now,
        )

        val dao = personalizedSectionDao
        if (dao != null) {
            try {
                val sectionEntity = CachedPersonalizedSectionEntity(
                    accountKey = accountKey,
                    sectionType = sectionType.name,
                    title = title ?: sectionType.name,
                    subtitle = subtitle,
                    sourceLabel = sourceLabel ?: "Personalized",
                    explanation = explanation,
                    cachedAt = now,
                    expiresAt = now + PERSISTENT_TTL_MS,
                )
                val itemEntities = items.mapIndexed { index, item ->
                    CachedPersonalizedItemEntity(
                        accountKey = accountKey,
                        sectionType = sectionType.name,
                        itemId = item.id,
                        title = item.title,
                        artist = item.artist,
                        channelId = item.channelId,
                        artworkUrl = item.artworkUrl,
                        durationMs = item.durationMs,
                        itemType = item.itemType.name,
                        sourceBadge = item.sourceBadge,
                        explanation = item.explanation,
                        rank = index,
                        cachedAt = now,
                    )
                }
                dao.saveSectionWithItems(sectionEntity, itemEntities)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to Room cache account=$accountKey section=$sectionType", e)
            }
        }
        val safeKey = if (accountKey.length >= 8) accountKey.take(8) + "..." else accountKey
        Log.d(TAG, "Cache PUT for account=$safeKey section=$sectionType items=${items.size}")
    }

    suspend fun invalidateAccount(accountKey: String) = lock.withLock {
        val removed = memoryCache.remove(accountKey)
        try {
            personalizedSectionDao?.invalidateAccountCache(accountKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account cache from Room key=$accountKey", e)
        }
        val safeKey = if (accountKey.length >= 8) accountKey.take(8) + "..." else accountKey
        Log.d(TAG, "Invalidated cache for account=$safeKey (memory sections removed=${removed?.size ?: 0})")
    }

    suspend fun clearAll() = lock.withLock {
        memoryCache.clear()
        try {
            personalizedSectionDao?.clearAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Room cache", e)
        }
        Log.d(TAG, "Cleared all account cache entries")
    }
}
