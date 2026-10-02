package com.deepeye.musicpro.youtube

import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsBlockerTest {

    @Test
    fun testIsShort_flagExplicitlyTrue_returnsTrue() {
        assertTrue(MusicFilter.isShort("Awesome Track", durationSeconds = 180, isShortFlag = true))
    }

    @Test
    fun testIsShort_durationUnder60s_returnsTrue() {
        assertTrue(MusicFilter.isShort("Quick Clip", durationSeconds = 45, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Quick Clip 2", durationSeconds = 1, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Quick Clip 3", durationSeconds = 59, isShortFlag = false))
    }

    @Test
    fun testIsShort_hashtagsInTitle_returnsTrue() {
        assertTrue(MusicFilter.isShort("Viral Dance #shorts", durationSeconds = 120, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Funny Moment #short", durationSeconds = 90, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Epic Win #ytshorts", durationSeconds = 100, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Magic Trick #shortsfeed", durationSeconds = 150, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Daily Vlog [shorts]", durationSeconds = 80, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Guitar Solo (shorts)", durationSeconds = 75, isShortFlag = false))
        assertTrue(MusicFilter.isShort("https://youtube.com/shorts/xyz123", durationSeconds = 80, isShortFlag = false))
        assertTrue(MusicFilter.isShort("she is back with her character| Twinkle Oberoi| # #hellyshah #chumbak #funny #comedy #netflix", durationSeconds = 0, isShortFlag = false))
        assertTrue(MusicFilter.isShort("Expectation Se Jyada Mehnge Kapde Pasand Aa Gaye 😭 #neetubisht #lakhneet #funny #comedy", durationSeconds = 0, isShortFlag = false))
    }

    @Test
    fun testIsShort_normalLongTrack_returnsFalse() {
        assertFalse(MusicFilter.isShort("Arijit Singh - Kesariya (Official Audio)", durationSeconds = 240, isShortFlag = false))
        assertFalse(MusicFilter.isShort("Diljit Dosanjh - Lover | Official Music Video", durationSeconds = 185, isShortFlag = false))
        assertFalse(MusicFilter.isShort("Coldplay - Yellow", durationSeconds = 260, isShortFlag = false))
    }

    @Test
    fun testIsMusicTrack_rejectsShorts() {
        assertFalse(MusicFilter.isMusicTrack("Pop Song #shorts", "T-Series", durationSeconds = 180, isShort = false))
        assertFalse(MusicFilter.isMusicTrack("Best EDM Drop", "Sony Music", durationSeconds = 40, isShort = false))
        assertFalse(MusicFilter.isMusicTrack("Trending Music", "Zee Music", durationSeconds = 200, isShort = true))
    }
}
