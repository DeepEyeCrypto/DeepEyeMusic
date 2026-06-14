package com.deepeye.musicpro.core.utils

import com.deepeye.musicpro.domain.model.Lyrics
import com.deepeye.musicpro.domain.model.LyricsLine
import java.util.regex.Pattern

object LrcParser {
    // Matches [mm:ss.xx] or [mm:ss:xx] or [mm:ss]
    private val timeRegex = Pattern.compile("\\[(\\d{2,}):(\\d{2})(?:[.:](\\d{2,3}))?\\]")

    fun parseSyncedLyrics(lrcString: String): Lyrics {
        val lines = mutableListOf<LyricsLine>()
        val lrcLines = lrcString.split("\n")

        for (line in lrcLines) {
            val matcher = timeRegex.matcher(line)
            var lastEnd = 0
            val timestamps = mutableListOf<Long>()

            while (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3)

                var ms = 0L
                if (msStr != null) {
                    ms = if (msStr.length == 2) {
                        msStr.toLongOrNull()?.times(10) ?: 0L
                    } else {
                        msStr.toLongOrNull() ?: 0L
                    }
                }

                val totalMs = min * 60000 + sec * 1000 + ms
                timestamps.add(totalMs)
                lastEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = line.substring(lastEnd).trim()
                for (ts in timestamps) {
                    lines.add(LyricsLine(ts, text))
                }
            }
        }

        // Sort by timestamp just in case
        lines.sortBy { it.timestampMs }

        return Lyrics(lines = lines, isSynced = true)
    }

    fun parsePlainLyrics(plainText: String): Lyrics {
        val lines = plainText.split("\n").map { line ->
            LyricsLine(0L, line.trim())
        }
        return Lyrics(lines = lines, isSynced = false)
    }
}
