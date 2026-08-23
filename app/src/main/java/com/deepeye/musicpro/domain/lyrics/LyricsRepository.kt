package com.deepeye.musicpro.domain.lyrics

import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.domain.model.LyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor() {

    suspend fun getLyricsForTrack(title: String, artist: String): Lyrics? = withContext(Dispatchers.IO) {
        // Mock LRC string
        val mockLrc = """
            [00:00.00] $title
            [00:05.00] $artist
            [00:10.00] (Instrumental intro)
            [00:15.00] Verse 1 starting...
            [00:20.00] This is the second line of the song.
            [00:25.00] And we are singing along.
            [00:30.00] Pre-chorus buildup.
            [00:35.00] Chorus! DeepEye Music Pro!
            [00:40.00] Glassmorphism looking so fresh.
            [00:45.00] Real-time lyrics in sync.
            [00:50.00] (Music fades)
        """.trimIndent()
        
        parseLrc(mockLrc)
    }

    private fun parseLrc(lrcText: String): Lyrics {
        val lines = mutableListOf<LyricsLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2}\\.\\d{2})](.*)")

        lrcText.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toFloat()
                val text = match.groupValues[3].trim()
                val timeMs = (minutes * 60 * 1000) + (seconds * 1000).toLong()
                lines.add(LyricsLine(timeMs, text))
            }
        }

        return Lyrics(
            lines = lines.sortedBy { it.timestampMs },
            isSynced = lines.isNotEmpty()
        )
    }
}
