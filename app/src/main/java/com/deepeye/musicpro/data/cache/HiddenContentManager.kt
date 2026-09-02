// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** A single "not interested" decision that can be undone shortly after acting. */
data class UndoableHide(
    val itemId: String,
    val label: String,
    /** Epoch millis until which the hide may be undone. Expiry keeps UI honest. */
    val expiresAt: Long,
)

/**
 * In-memory, privacy-safe manager for user "not interested" / hide controls.
 *
 * Hides are intentionally kept in memory (not persisted to any server) and scoped to the
 * current process. This guarantees no remote account data is ever mutated by a hide action.
 */
@Singleton
class HiddenContentManager @Inject constructor() {

    private val _hiddenItemIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenItemIds: StateFlow<Set<String>> = _hiddenItemIds.asStateFlow()

    private val _hiddenArtistNames = MutableStateFlow<Set<String>>(emptySet())
    val hiddenArtistNames: StateFlow<Set<String>> = _hiddenArtistNames.asStateFlow()

    private val _undoableHides = MutableStateFlow<List<UndoableHide>>(emptyList())
    val undoableHides: StateFlow<List<UndoableHide>> = _undoableHides.asStateFlow()

    private val _resetVersion = MutableStateFlow(0)
    /** Increments whenever all hidden content is reset, so the feed can rebuild once. */
    val resetVersion: StateFlow<Int> = _resetVersion.asStateFlow()

    companion object {
        private const val UNDO_WINDOW_MS = 6_000L
    }

    /** Hides an item (and tracks an undo affordance). Returns true if newly hidden. */
    suspend fun hideItem(itemId: String, label: String, alsoHideArtist: String? = null): Boolean {
        val wasAlreadyHidden = itemId in _hiddenItemIds.value
        _hiddenItemIds.update { current -> current + itemId }
        if (alsoHideArtist != null && alsoHideArtist.isNotBlank()) {
            _hiddenArtistNames.update { it + alsoHideArtist }
        }
        _undoableHides.update { current ->
            val now = System.currentTimeMillis()
            val trimmed = current.filter { it.expiresAt > now }
            (trimmed + UndoableHide(itemId, label, now + UNDO_WINDOW_MS)).takeLast(5)
        }
        return !wasAlreadyHidden
    }

    /** Reverses the most recent hide that still matches [itemId]. */
    suspend fun undoHide(itemId: String) {
        _undoableHides.update { current ->
            val now = System.currentTimeMillis()
            current.filterNot { it.itemId == itemId && it.expiresAt > now }
        }
        _hiddenItemIds.update { current -> current - itemId }
    }

    /** Permanently removes a hide decision. Called from a "Manage hidden content" UI. */
    suspend fun unhideItem(itemId: String) {
        _hiddenItemIds.update { current -> current - itemId }
        _undoableHides.update { current -> current.filterNot { it.itemId == itemId } }
    }

    suspend fun unhideArtist(artistName: String) {
        _hiddenArtistNames.update { current -> current - artistName }
    }

    fun isItemHidden(itemId: String): Boolean = itemId in _hiddenItemIds.value
    fun isArtistHidden(artistName: String): Boolean =
        artistName.isNotBlank() && artistName in _hiddenArtistNames.value

    /** Completely resets hidden items and artists. */
    suspend fun resetAll() {
        _hiddenItemIds.value = emptySet()
        _hiddenArtistNames.value = emptySet()
        _undoableHides.value = emptyList()
        _resetVersion.update { it + 1 }
    }

    /** Drops undo affordances whose window has passed. */
    fun pruneExpiredUndo(now: Long = System.currentTimeMillis()) {
        _undoableHides.update { current -> current.filter { it.expiresAt > now } }
    }
}