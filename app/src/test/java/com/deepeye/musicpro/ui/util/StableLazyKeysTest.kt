// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Covers the LazyList key strategy used by the drag-reorderable queue.
 *
 * Two defect classes are pinned here:
 *  * index-as-key, which rebinds a row's remembered state to the wrong track
 *    when the user reorders the queue,
 *  * raw-id-as-key, which throws "Key was already used" when a queue contains
 *    the same track twice.
 */
class StableLazyKeysTest {

    @Test
    fun `unique ids produce one key per item in order`() {
        val items = listOf("a", "b", "c")
        assertEquals(listOf("a", "b", "c"), stableLazyKeys(items) { it })
    }

    @Test
    fun `empty list produces empty keys`() {
        assertEquals(emptyList<String>(), stableLazyKeys(emptyList<String>()) { it })
    }

    @Test
    fun `single item produces the bare id`() {
        assertEquals(listOf("only"), stableLazyKeys(listOf("only")) { it })
    }

    @Test
    fun `duplicate ids are disambiguated with a stable suffix`() {
        val items = listOf("a", "b", "a")
        // "a" appears twice, so both occurrences need a suffix; "b" stays bare.
        assertEquals(listOf("a#1", "b", "a#2"), stableLazyKeys(items) { it })
    }

    @Test
    fun `keys are unique even when every id is identical`() {
        val items = List(5) { "same" }
        val keys = stableLazyKeys(items) { it }
        assertEquals(5, keys.size)
        assertEquals(5, keys.toSet().size)
    }

    @Test
    fun `reordering moves the key with its item, not with the index`() {
        // The invariant the index-as-key bug violates: a key belongs to the
        // track, so moving a track must carry its key along.
        val before = listOf("a", "b", "c")
        val after = listOf("c", "a", "b")

        val keysBefore = stableLazyKeys(before) { it }
        val keysAfter = stableLazyKeys(after) { it }

        val keyOfBefore = before.mapIndexed { i, id -> id to keysBefore[i] }.toMap()
        val keyOfAfter = after.mapIndexed { i, id -> id to keysAfter[i] }.toMap()

        assertEquals(keyOfBefore, keyOfAfter)
    }

    @Test
    fun `reordering duplicates keeps keys aligned per occurrence`() {
        val before = listOf("a", "b", "a")
        val after = listOf("a", "a", "b")

        val keysBefore = stableLazyKeys(before) { it }
        val keysAfter = stableLazyKeys(after) { it }

        // 1st "a" before -> 1st "a" after, 2nd "a" before -> 2nd "a" after.
        assertEquals(keysBefore[0], keysAfter[0])
        assertEquals(keysBefore[2], keysAfter[1])
        assertNotEquals(keysAfter[0], keysAfter[1])
    }

    @Test
    fun `adding a duplicate retroactively disambiguates rather than colliding`() {
        // Going from one "a" to two "a" changes the keys, which is the
        // intended trade-off: uniqueness is non-negotiable in a LazyList.
        val single = stableLazyKeys(listOf("a", "b")) { it }
        val duplicated = stableLazyKeys(listOf("a", "b", "a")) { it }

        assertEquals(listOf("a", "b"), single)
        assertEquals(3, duplicated.toSet().size)
    }
}
