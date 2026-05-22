package com.deepeye.musicpro.player.queue

import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.ShuffleMode
import com.deepeye.musicpro.domain.model.Song
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private lateinit var songs: List<MediaItem>

    @Before
    fun setUp() {
        queueManager = QueueManager()
        
        val song1 = mockk<Song>(relaxed = true) {
            every { id } returns 1L
            every { title } returns "Song 1"
            every { artist } returns "Artist A"
        }
        val song2 = mockk<Song>(relaxed = true) {
            every { id } returns 2L
            every { title } returns "Song 2"
            every { artist } returns "Artist B"
        }
        val song3 = mockk<Song>(relaxed = true) {
            every { id } returns 3L
            every { title } returns "Song 3"
            every { artist } returns "Artist C"
        }

        songs = listOf(
            MediaItem.Local(song = song1),
            MediaItem.Local(song = song2),
            MediaItem.Local(song = song3)
        )
    }

    @Test
    fun testSetQueue_setsInitialState() {
        queueManager.setQueue(songs, 1)
        assertEquals(3, queueManager.queue.value.size)
        assertEquals(1, queueManager.currentIndex.value)
        assertEquals(songs[1], queueManager.currentItem)
    }

    @Test
    fun testShuffleAndUnshuffle() {
        queueManager.setQueue(songs, 0)
        
        // Toggle shuffle ON
        queueManager.toggleShuffleMode()
        assertEquals(ShuffleMode.ON, queueManager.shuffleMode.value)
        // First item should stay at index 0 (as per current implementation)
        assertEquals(songs[0], queueManager.currentItem)
        assertEquals(0, queueManager.currentIndex.value)

        // Toggle shuffle OFF -> should restore original order
        queueManager.toggleShuffleMode()
        assertEquals(ShuffleMode.OFF, queueManager.shuffleMode.value)
        assertEquals(songs, queueManager.queue.value)
    }

    @Test
    fun testRepeatOneReplaysSameIndex() {
        queueManager.setQueue(songs, 1)
        queueManager.toggleRepeatMode() // NONE -> ALL
        queueManager.toggleRepeatMode() // ALL -> ONE
        assertEquals(RepeatMode.ONE, queueManager.repeatMode.value)

        val nextItem = queueManager.next()
        assertEquals(songs[1], nextItem)
        assertEquals(1, queueManager.currentIndex.value)
    }

    @Test
    fun testRepeatAllWrapsAround() {
        queueManager.setQueue(songs, 2) // Last item
        queueManager.toggleRepeatMode() // NONE -> ALL
        assertEquals(RepeatMode.ALL, queueManager.repeatMode.value)

        val nextItem = queueManager.next()
        assertEquals(songs[0], nextItem)
        assertEquals(0, queueManager.currentIndex.value)
    }

    @Test
    fun testRepeatNoneReturnsNullAtEnd() {
        queueManager.setQueue(songs, 2) // Last item
        assertEquals(RepeatMode.NONE, queueManager.repeatMode.value)

        val nextItem = queueManager.next()
        assertNull(nextItem)
        assertEquals(2, queueManager.currentIndex.value)
    }
}
