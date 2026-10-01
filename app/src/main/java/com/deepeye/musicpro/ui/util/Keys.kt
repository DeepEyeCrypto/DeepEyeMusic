// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.util

/**
 * Builds LazyList keys that are **both stable across reordering and unique**.
 *
 * Why this exists: the queue/now-playing lists are drag-reorderable. Keying them
 * by index (the naive `items(size, key = { it })`) makes Compose rebind the
 * remembered state of a row to whatever song now sits at that index, so the
 * drag highlight and elevation follow the *position* instead of the *track*.
 * Keying by a raw id is the usual fix, but a play queue can legitimately contain
 * the same track twice, and a duplicate key makes `LazyColumn` throw
 * "Key \"x\" was already used".
 *
 * `stableLazyKeys` satisfies both constraints: the first occurrence of an id
 * keeps the bare id (so nothing moves when no duplicate exists), and later
 * occurrences get a stable `#n` suffix.
 *
 * @param items the list being rendered, in display order
 * @param identity maps an item to its stable identity (e.g. `MediaItem::id`)
 * @return a key per item, positionally aligned with [items]
 */
fun <T> stableLazyKeys(items: List<T>, identity: (T) -> String): List<String> {
    val totalCounts = HashMap<String, Int>(items.size)
    for (item in items) {
        val id = identity(item)
        totalCounts[id] = (totalCounts[id] ?: 0) + 1
    }

    val seen = HashMap<String, Int>(items.size)
    return items.map { item ->
        val id = identity(item)
        if (totalCounts[id] == 1) {
            id
        } else {
            // nth occurrence (1-based) disambiguates duplicates deterministically.
            val occurrence = seen.merge(id, 1, Int::plus)!!
            "$id#$occurrence"
        }
    }
}
