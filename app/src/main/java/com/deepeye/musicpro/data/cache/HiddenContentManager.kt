// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache

import android.util.Log
import com.deepeye.musicpro.data.cache.dao.HiddenContentDao
import com.deepeye.musicpro.data.cache.entities.HiddenContentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** A single "not interested" decision that can be undone shortly after acting. */
data class UndoableHide(
    val itemId: String,
    val label: String,
    /** Epoch millis until which the hide may be undone. Expiry keeps UI honest. */
    val expiresAt: Long,
)

/** Represents a hidden item entry displayed in the Hidden Content Management UI. */
data class HiddenItemEntry(
    val id: String,
    val title: String,
    val artist: String? = null,
    val hiddenAt: Long = System.currentTimeMillis()
)

private object NoOpHiddenContentDao : HiddenContentDao {
    override suspend fun insert(entity: HiddenContentEntity) {}
    override suspend fun insertAll(entities: List<HiddenContentEntity>) {}
    override fun observeHiddenContent(accountKey: String?): Flow<List<HiddenContentEntity>> = flowOf(emptyList())
    override suspend fun getAllHiddenContent(accountKey: String?): List<HiddenContentEntity> = emptyList()
    override suspend fun getHiddenContentByType(accountKey: String?, itemType: String): List<HiddenContentEntity> = emptyList()
    override suspend fun deleteByItemId(itemId: String, accountKey: String?) {}
    override suspend fun deleteByItemIds(itemIds: List<String>, accountKey: String?) {}
    override suspend fun deleteAllForAccount(accountKey: String?) {}
    override suspend fun clearAll() {}
    override suspend fun count(accountKey: String?): Int = 0
}

/**
 * Account-scoped, Room-backed and memory-cached manager for user "not interested" / hide controls.
 */
@Singleton
class HiddenContentManager @Inject constructor(
    private val hiddenContentDao: HiddenContentDao,
) {
    /** Secondary constructor for testing without Room. */
    constructor() : this(NoOpHiddenContentDao)

    private val mutex = Mutex()
    private var currentAccountKey: String? = null

    private val _hiddenItemIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenItemIds: StateFlow<Set<String>> = _hiddenItemIds.asStateFlow()

    private val _hiddenItems = MutableStateFlow<Map<String, HiddenItemEntry>>(emptyMap())
    val hiddenItems: StateFlow<Map<String, HiddenItemEntry>> = _hiddenItems.asStateFlow()

    private val _hiddenArtistNames = MutableStateFlow<Set<String>>(emptySet())
    val hiddenArtistNames: StateFlow<Set<String>> = _hiddenArtistNames.asStateFlow()

    private val _undoableHides = MutableStateFlow<List<UndoableHide>>(emptyList())
    val undoableHides: StateFlow<List<UndoableHide>> = _undoableHides.asStateFlow()

    private val _resetVersion = MutableStateFlow(0)
    /** Increments whenever all hidden content is reset, so the feed can rebuild once. */
    val resetVersion: StateFlow<Int> = _resetVersion.asStateFlow()

    companion object {
        private const val TAG = "HiddenContentManager"
        private const val UNDO_WINDOW_MS = 6_000L
    }

    /**
     * Switches the active account scope and loads persisted hidden items for that account.
     */
    suspend fun switchAccount(accountKey: String?) {
        mutex.withLock {
            currentAccountKey = accountKey
            loadFromPersistenceInternal(accountKey)
        }
    }

    /**
     * Loads persisted hidden items from Room into L1 memory cache.
     */
    suspend fun loadFromPersistence(accountKey: String? = currentAccountKey) {
        mutex.withLock {
            loadFromPersistenceInternal(accountKey)
        }
    }

    private suspend fun loadFromPersistenceInternal(accountKey: String?) {
        val dao = hiddenContentDao ?: return
        try {
            val entities = dao.getAllHiddenContent(accountKey)
            val songsMap = mutableMapOf<String, HiddenItemEntry>()
            val songIds = mutableSetOf<String>()
            val artists = mutableSetOf<String>()

            for (entity in entities) {
                if (entity.itemType == "ARTIST") {
                    artists.add(entity.itemId)
                } else {
                    songIds.add(entity.itemId)
                    songsMap[entity.itemId] = HiddenItemEntry(
                        id = entity.itemId,
                        title = entity.title,
                        artist = entity.artist,
                        hiddenAt = entity.hiddenAt,
                    )
                }
            }

            _hiddenItemIds.value = songIds
            _hiddenItems.value = songsMap
            _hiddenArtistNames.value = artists
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load hidden content from Room for account: $accountKey", e)
        }
    }

    /** Hides an item (and tracks an undo affordance). Returns true if newly hidden. */
    suspend fun hideItem(
        itemId: String,
        label: String,
        alsoHideArtist: String? = null,
        accountKey: String? = currentAccountKey
    ): Boolean = mutex.withLock {
        val wasAlreadyHidden = itemId in _hiddenItemIds.value
        _hiddenItemIds.update { current -> current + itemId }
        _hiddenItems.update { current ->
            current + (itemId to HiddenItemEntry(id = itemId, title = label, artist = alsoHideArtist))
        }
        if (alsoHideArtist != null && alsoHideArtist.isNotBlank()) {
            _hiddenArtistNames.update { it + alsoHideArtist }
        }
        _undoableHides.update { current ->
            val now = System.currentTimeMillis()
            val trimmed = current.filter { it.expiresAt > now }
            (trimmed + UndoableHide(itemId, label, now + UNDO_WINDOW_MS)).takeLast(5)
        }

        hiddenContentDao?.let { dao ->
            try {
                dao.insert(
                    HiddenContentEntity(
                        accountKey = accountKey,
                        itemId = itemId,
                        title = label,
                        artist = alsoHideArtist,
                        itemType = "SONG",
                        hiddenAt = System.currentTimeMillis()
                    )
                )
                if (alsoHideArtist != null && alsoHideArtist.isNotBlank()) {
                    dao.insert(
                        HiddenContentEntity(
                            accountKey = accountKey,
                            itemId = alsoHideArtist,
                            title = alsoHideArtist,
                            itemType = "ARTIST",
                            hiddenAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist hidden item to Room: $itemId", e)
            }
        }

        return@withLock !wasAlreadyHidden
    }

    /** Reverses the most recent hide that still matches [itemId]. */
    suspend fun undoHide(itemId: String, accountKey: String? = currentAccountKey) = mutex.withLock {
        _undoableHides.update { current ->
            val now = System.currentTimeMillis()
            current.filterNot { it.itemId == itemId && it.expiresAt > now }
        }
        _hiddenItemIds.update { current -> current - itemId }
        _hiddenItems.update { current -> current - itemId }

        hiddenContentDao?.let { dao ->
            try {
                dao.deleteByItemId(itemId, accountKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete hidden item from Room: $itemId", e)
            }
        }
    }

    /** Permanently removes a hide decision. Called from a "Manage hidden content" UI. */
    suspend fun unhideItem(itemId: String, accountKey: String? = currentAccountKey) = mutex.withLock {
        _hiddenItemIds.update { current -> current - itemId }
        _hiddenItems.update { current -> current - itemId }
        _undoableHides.update { current -> current.filterNot { it.itemId == itemId } }

        hiddenContentDao?.let { dao ->
            try {
                dao.deleteByItemId(itemId, accountKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unhide item in Room: $itemId", e)
            }
        }
    }

    suspend fun unhideArtist(artistName: String, accountKey: String? = currentAccountKey) = mutex.withLock {
        _hiddenArtistNames.update { current -> current - artistName }

        hiddenContentDao?.let { dao ->
            try {
                dao.deleteByItemId(artistName, accountKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unhide artist in Room: $artistName", e)
            }
        }
    }

    fun isItemHidden(itemId: String): Boolean = itemId in _hiddenItemIds.value
    fun isArtistHidden(artistName: String): Boolean =
        artistName.isNotBlank() && artistName in _hiddenArtistNames.value

    /** Completely resets hidden items and artists for current account. */
    suspend fun resetAll(accountKey: String? = currentAccountKey) = mutex.withLock {
        _hiddenItemIds.value = emptySet()
        _hiddenItems.value = emptyMap()
        _hiddenArtistNames.value = emptySet()
        _undoableHides.value = emptyList()
        _resetVersion.update { it + 1 }

        hiddenContentDao?.let { dao ->
            try {
                dao.deleteAllForAccount(accountKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset all hidden content in Room for account: $accountKey", e)
            }
        }
    }

    /** Drops undo affordances whose window has passed. */
    fun pruneExpiredUndo(now: Long = System.currentTimeMillis()) {
        _undoableHides.update { current -> current.filter { it.expiresAt > now } }
    }
}
