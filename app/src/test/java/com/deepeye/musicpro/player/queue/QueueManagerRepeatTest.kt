// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.queue

import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.RepeatMode
import com.deepeye.musicpro.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * FEL: same-song loop regression matrix (RED-first).
 *
 * Case under test: single-item queue + Repeat ALL wraps modulo to index 0,
 * so natural completion / Next replays the SAME audio instead of triggering
 * autoplay. Privacy-safe: synthetic local items only, no IDs/URLs logged.
 *
 * Robolectric: real android.net.Uri is required by MediaItem.Local fixtures
 * (plain JUnit returns null from Uri.parse).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QueueManagerRepeatTest {

    private fun localItem(id: Long, title: String) = MediaItem.Local(
        song = Song(
            id = id, title = title, artist = "Synthetic",
            album = "", albumId = 0L, artistId = 0L,
            uri = android.net.Uri.parse("content://media/external/audio/media/$id"),
            duration = 180_000L, size = 4_000_000L,
            path = "/storage/emulated/0/Music/$title.mp3",
            artUri = null,
        ),
    )

    @Test
    fun `single-item Repeat ALL must NOT silently replay current (must return null so autoplay resolves next)`() {
        val qm = QueueManager()
        qm.setQueue(listOf(localItem(1L, "A")), startIndex = 0)
        // Cycle NONE -> ALL.
        qm.toggleRepeatMode()
        assertEquals(RepeatMode.ALL, qm.repeatMode.value)

        // RED (pre-fix): returns the SAME item (modulo wrap) -> same-song loop.
        // GREEN (post-fix): returns null for a single-item queue so the
        // controller falls through to autoplay resolution of a DIFFERENT track.
        val nxt = qm.next()
        assertNull(
            "single-item Repeat ALL replayed current instead of yielding to autoplay",
            nxt,
        )
    }

    @Test
    fun `multi-item Repeat ALL still wraps correctly`() {
        val qm = QueueManager()
        qm.setQueue(listOf(localItem(1L, "A"), localItem(2L, "B")), startIndex = 1)
        qm.toggleRepeatMode()
        val nxt = qm.next()
        assertEquals(localItem(1L, "A").id, nxt?.id)
    }

    @Test
    fun `intentional Repeat ONE still replays current`() {
        val qm = QueueManager()
        qm.setQueue(listOf(localItem(1L, "A")), startIndex = 0)
        qm.toggleRepeatMode() // NONE -> ALL
        qm.toggleRepeatMode() // ALL -> ONE
        assertEquals(RepeatMode.ONE, qm.repeatMode.value)
        val nxt = qm.next()
        assertEquals("A", nxt?.title)
    }

    @Test
    fun `Repeat NONE at end returns null`() {
        val qm = QueueManager()
        qm.setQueue(listOf(localItem(1L, "A")), startIndex = 0)
        assertNull(qm.next())
    }

    @Test
    fun `Repeat ALL wrap yields a different item when one exists`() {
        val qm = QueueManager()
        qm.setQueue(listOf(localItem(1L, "A"), localItem(2L, "B")), startIndex = 0)
        qm.toggleRepeatMode()
        val nxt = qm.next()
        assertNotEquals(localItem(1L, "A").id + "_same", nxt?.id)
        assertEquals(localItem(2L, "B").id, nxt?.id)
    }
}
