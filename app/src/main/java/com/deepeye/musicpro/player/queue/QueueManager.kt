// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.queue

import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.ShuffleMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback queue including order, shuffle, and repeat modes.
 */
@Singleton
class QueueManager
@Inject
constructor() {
    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    private var originalQueue: List<MediaItem> = emptyList()

    val currentItem: MediaItem?
        get() {
            val idx = _currentIndex.value
            val q = _queue.value
            return if (idx in q.indices) q[idx] else null
        }

    /**
     * Sets a new queue and starts playing from the given index.
     */
    @Synchronized
    fun setQueue(
        items: List<MediaItem>,
        startIndex: Int = 0,
    ) {
        originalQueue = items
        if (_shuffleMode.value == ShuffleMode.ON) {
            val shuffled = items.toMutableList()
            val startItem = items.getOrNull(startIndex)
            shuffled.shuffle()
            // Move the start item to the front
            startItem?.let {
                shuffled.remove(it)
                shuffled.add(0, it)
            }
            _queue.value = shuffled
            _currentIndex.value = 0
        } else {
            _queue.value = items
            _currentIndex.value = startIndex.coerceIn(0, items.size - 1)
        }
    }

    /**
     * Moves to the next track in the queue.
     * Returns the next MediaItem, or null if at end of queue with repeat off.
     */
    @Synchronized
    fun next(): MediaItem? {
        val q = _queue.value
        if (q.isEmpty()) return null

        return when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Stay on same song
                currentItem
            }
            RepeatMode.ALL -> {
                val nextIndex = (_currentIndex.value + 1) % q.size
                _currentIndex.value = nextIndex
                q[nextIndex]
            }
            RepeatMode.NONE -> {
                val nextIndex = _currentIndex.value + 1
                if (nextIndex < q.size) {
                    _currentIndex.value = nextIndex
                    q[nextIndex]
                } else {
                    null // End of queue
                }
            }
        }
    }

    /**
     * Moves to the previous track in the queue.
     * Returns null if already at the beginning with no repeat, signaling the caller
     * should restart the current track instead.
     */
    @Synchronized
    fun previous(): MediaItem? {
        val q = _queue.value
        if (q.isEmpty()) return null

        return if (_currentIndex.value > 0) {
            val prevIndex = _currentIndex.value - 1
            _currentIndex.value = prevIndex
            q[prevIndex]
        } else if (_repeatMode.value == RepeatMode.ALL) {
            val prevIndex = q.size - 1
            _currentIndex.value = prevIndex
            q[prevIndex]
        } else {
            // Already at the beginning with no repeat — return null
            // so the caller can decide to restart the current track.
            null
        }
    }

    /**
     * Toggles repeat mode: OFF → ALL → ONE → OFF
     */
    @Synchronized
    fun toggleRepeatMode() {
        _repeatMode.value =
            when (_repeatMode.value) {
                RepeatMode.NONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.NONE
            }
    }

    /**
     * Toggles shuffle mode on/off.
     */
    @Synchronized
    fun toggleShuffleMode() {
        _shuffleMode.value =
            when (_shuffleMode.value) {
                ShuffleMode.OFF -> {
                    // Shuffle the queue, keeping current item at current position
                    val current = currentItem
                    val shuffled = _queue.value.toMutableList()
                    shuffled.shuffle()
                    current?.let {
                        shuffled.remove(it)
                        shuffled.add(0, it)
                        _currentIndex.value = 0
                    }
                    _queue.value = shuffled
                    ShuffleMode.ON
                }
                ShuffleMode.ON -> {
                    // Restore original order
                    val current = currentItem
                    _queue.value = originalQueue
                    current?.let {
                        val idx = originalQueue.indexOf(it)
                        if (idx >= 0) _currentIndex.value = idx
                    }
                    ShuffleMode.OFF
                }
            }
    }

    /**
     * Moves an item in the queue from one position to another (drag-to-reorder).
     */
    @Synchronized
    fun moveItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val mutable = _queue.value.toMutableList()
        if (fromIndex in mutable.indices && toIndex in mutable.indices) {
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            _queue.value = mutable

            // Adjust current index if needed
            val currentIdx = _currentIndex.value
            _currentIndex.value =
                when {
                    currentIdx == fromIndex -> toIndex
                    fromIndex < currentIdx && toIndex >= currentIdx -> currentIdx - 1
                    fromIndex > currentIdx && toIndex <= currentIdx -> currentIdx + 1
                    else -> currentIdx
                }
        }
    }

    /**
     * Removes an item from the queue.
     */
    @Synchronized
    fun removeItem(index: Int) {
        val mutable = _queue.value.toMutableList()
        if (index in mutable.indices) {
            mutable.removeAt(index)
            _queue.value = mutable

            val currentIdx = _currentIndex.value
            if (index < currentIdx) {
                // Item before current was removed, shift index back
                _currentIndex.value = currentIdx - 1
            } else if (index == currentIdx) {
                // Currently playing item was removed; keep index the same
                // so it now points to the next item (which shifted into this position).
                // If we removed the last item, clamp to the new last index.
                _currentIndex.value = currentIdx.coerceAtMost(mutable.size - 1).coerceAtLeast(-1)
            }
            // If index > currentIdx, no adjustment needed
        }
    }

    /**
     * Clears the queue.
     */
    @Synchronized
    fun clear() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentIndex.value = -1
    }

    /**
     * Jumps to a specific index in the queue.
     */
    @Synchronized
    fun jumpTo(index: Int): MediaItem? {
        val q = _queue.value
        return if (index in q.indices) {
            _currentIndex.value = index
            q[index]
        } else {
            null
        }
    }
}
