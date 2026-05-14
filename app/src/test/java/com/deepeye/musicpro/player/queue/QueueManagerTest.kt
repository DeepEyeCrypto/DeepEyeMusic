package com.deepeye.musicpro.player.queue

import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.ShuffleMode
import com.deepeye.musicpro.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private val songs = listOf(
        Song(1, "A", "Artist", "Album", 1000, "uri1", "art1", 1),
        Song(2, "B", "Artist", "Album", 1000, "uri2", "art2", 2),
        Song(3, "C", "Artist", "Album", 1000, "uri3", "art3", 3),
        Song(4, "D", "Artist", "Album", 1000, "uri4", "art4", 4)
    )

    @Before
    fun setup() {
        queueManager = QueueManager()
        queueManager.setQueue(songs, 0)
    }

    @Test
    fun `test shuffle produces different order but keeps current song first`() {
        queueManager.toggleShuffleMode()
        
        assertEquals(ShuffleMode.ON, queueManager.shuffleMode.value)
        assertEquals(songs.size, queueManager.queue.value.size)
        // Current song must remain at index 0 after shuffle
        assertEquals(songs[0], queueManager.queue.value[0])
    }

    @Test
    fun `test unshuffle restores original order`() {
        queueManager.toggleShuffleMode() // ON
        queueManager.toggleShuffleMode() // OFF
        
        assertEquals(ShuffleMode.OFF, queueManager.shuffleMode.value)
        assertEquals(songs, queueManager.queue.value)
    }

    @Test
    fun `test REPEAT_ONE replays same index`() {
        queueManager.toggleRepeatMode() // ALL
        queueManager.toggleRepeatMode() // ONE
        assertEquals(RepeatMode.ONE, queueManager.repeatMode.value)

        val first = queueManager.currentSong
        val next = queueManager.next()
        
        assertEquals(first, next)
    }

    @Test
    fun `test REPEAT_ALL wraps from last to first`() {
        queueManager.toggleRepeatMode() // ALL
        assertEquals(RepeatMode.ALL, queueManager.repeatMode.value)

        queueManager.setQueue(songs, 3) // set to last song
        val next = queueManager.next()
        
        assertEquals(songs[0], next)
    }

    @Test
    fun `test next returns null when REPEAT_OFF at end of queue`() {
        // Default repeat is OFF
        queueManager.setQueue(songs, 3) // set to last song
        val next = queueManager.next()
        
        assertNull(next)
    }
}
