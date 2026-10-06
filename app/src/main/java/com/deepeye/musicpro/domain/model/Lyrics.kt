package com.deepeye.musicpro.domain.model

data class LyricsLine(
    val timestampMs: Long,
    val text: String
) {
    val startTimeMs: Long get() = timestampMs
}

typealias LyricLine = LyricsLine

data class Lyrics(
    val lines: List<LyricsLine>,
    val isSynced: Boolean
)
