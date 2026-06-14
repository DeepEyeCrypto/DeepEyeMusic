package com.deepeye.musicpro.domain.model

data class LyricsLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val lines: List<LyricsLine>,
    val isSynced: Boolean
)
