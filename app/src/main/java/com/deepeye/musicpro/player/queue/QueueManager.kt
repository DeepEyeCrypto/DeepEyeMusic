package com.deepeye.musicpro.player.queue

import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.ShuffleMode
import com.deepeye.musicpro.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback queue including order, shuffle, and repeat modes.
 */
@Singleton
class QueueManager @Inject constructor() {

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    private var originalQueue: List<Song> = emptyList()

    val currentSong: Song?
        get() {
            val idx = _currentIndex.value
            val q = _queue.value
            return if (idx in q.indices) q[idx] else null
        }

    /**
     * Sets a new queue and starts playing from the given index.
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        originalQueue = songs
        if (_shuffleMode.value == ShuffleMode.ON) {
            val shuffled = songs.toMutableList()
            val startSong = songs.getOrNull(startIndex)
            shuffled.shuffle()
            // Move the start song to the front
            startSong?.let {
                shuffled.remove(it)
                shuffled.add(0, it)
            }
            _queue.value = shuffled
            _currentIndex.value = 0
        } else {
            _queue.value = songs
            _currentIndex.value = startIndex.coerceIn(0, songs.size - 1)
        }
    }

    /**
     * Moves to the next track in the queue.
     * Returns the next Song, or null if at end of queue with repeat off.
     */
    fun next(): Song? {
        val q = _queue.value
        if (q.isEmpty()) return null

        return when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Stay on same song
                currentSong
            }
            RepeatMode.ALL -> {
                val nextIndex = (_currentIndex.value + 1) % q.size
                _currentIndex.value = nextIndex
                q[nextIndex]
            }
            RepeatMode.OFF -> {
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
     */
    fun previous(): Song? {
        val q = _queue.value
        if (q.isEmpty()) return null

        val prevIndex = if (_currentIndex.value > 0) {
            _currentIndex.value - 1
        } else if (_repeatMode.value == RepeatMode.ALL) {
            q.size - 1
        } else {
            0 // Stay at beginning
        }

        _currentIndex.value = prevIndex
        return q[prevIndex]
    }

    /**
     * Toggles repeat mode: OFF → ALL → ONE → OFF
     */
    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    /**
     * Toggles shuffle mode on/off.
     */
    fun toggleShuffleMode() {
        _shuffleMode.value = when (_shuffleMode.value) {
            ShuffleMode.OFF -> {
                // Shuffle the queue, keeping current song at current position
                val current = currentSong
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
                val current = currentSong
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
     * Moves a song in the queue from one position to another (drag-to-reorder).
     */
    fun moveSong(fromIndex: Int, toIndex: Int) {
        val mutable = _queue.value.toMutableList()
        if (fromIndex in mutable.indices && toIndex in mutable.indices) {
            val song = mutable.removeAt(fromIndex)
            mutable.add(toIndex, song)
            _queue.value = mutable

            // Adjust current index if needed
            val currentIdx = _currentIndex.value
            _currentIndex.value = when {
                currentIdx == fromIndex -> toIndex
                fromIndex < currentIdx && toIndex >= currentIdx -> currentIdx - 1
                fromIndex > currentIdx && toIndex <= currentIdx -> currentIdx + 1
                else -> currentIdx
            }
        }
    }

    /**
     * Removes a song from the queue.
     */
    fun removeSong(index: Int) {
        val mutable = _queue.value.toMutableList()
        if (index in mutable.indices) {
            mutable.removeAt(index)
            _queue.value = mutable

            val currentIdx = _currentIndex.value
            if (index < currentIdx) {
                _currentIndex.value = currentIdx - 1
            } else if (index == currentIdx && currentIdx >= mutable.size) {
                _currentIndex.value = (mutable.size - 1).coerceAtLeast(0)
            }
        }
    }

    /**
     * Clears the queue.
     */
    fun clear() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentIndex.value = -1
    }
}
