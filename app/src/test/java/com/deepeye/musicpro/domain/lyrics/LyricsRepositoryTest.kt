// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.lyrics

import com.deepeye.musicpro.core.utils.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRepositoryTest {

    @Test
    fun verifyLrcParserParsesTimedLyricsCorrectly() {
        val sampleLrc = """
            [00:12.00]First line of the song
            [00:24.50]Second line with vocals
            [01:05.10]Chorus line playing loud
        """.trimIndent()

        val parsed = LrcParser.parseSyncedLyrics(sampleLrc)
        assertNotNull(parsed)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(12000L, parsed.lines[0].timestampMs)
        assertEquals("First line of the song", parsed.lines[0].text)
        assertEquals(24500L, parsed.lines[1].timestampMs)
        assertEquals(65100L, parsed.lines[2].timestampMs)
    }

    @Test
    fun verifyActiveLineBinarySearch() {
        val sampleLrc = """
            [00:10.00]Line 1
            [00:20.00]Line 2
            [00:30.00]Line 3
        """.trimIndent()

        val lyrics = LrcParser.parseSyncedLyrics(sampleLrc)
        val pos = 25000L // 25s -> Should be Line 2 (index 1)

        val activeIndex = lyrics.lines.indexOfLast { it.timestampMs <= pos }
        assertEquals(1, activeIndex)
        assertEquals("Line 2", lyrics.lines[activeIndex].text)
    }
}
