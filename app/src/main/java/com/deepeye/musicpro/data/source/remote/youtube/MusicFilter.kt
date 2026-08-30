// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

object MusicFilter {
    private val EXCLUDE_PATTERN = Regex(
        "\\b(news|bulletin|khabar|samachar|bjp|congress|modi|yogi|election|interview|debate|ground report|podcast|coding|javascript|frontend|backend|tutorial|vlog|prank|unboxing|review|gaming|gameplay|live stream|speech|press conference|flood|crime|court|police|upsc|ias|comedy|episode|trailer|teaser|full movie|standup|cricket|ipl|scorecard|satoshi|chutkule|kahani|kahaniya|lesson|course|lecture|guide|how to|diy|promo|serial|drama|kids|cartoon|rhymes|rhyme|nursery|children|child|baby|balveer|pothole|detector|tech|android|pc|features|leaked|coloros|oppo|oneplus|smartphone|mobile|phone|update|bgmi|pubg|free fire|gadget|hack|science|geopolitics|documentary|shorts|thelivetv|speedrun)\\b",
        RegexOption.IGNORE_CASE
    )

    private val MUSIC_PATTERN = Regex(
        "\\b(song|songs|audio|music|lyrics|lyric|remix|feat|ft|official video|official audio|track|tracks|album|mashup|melody|lofi|lo-fi|beats|soundtrack|acoustic|unplugged|jukebox|ghazal|qawwali|bhajan|aarti|slowed|reverb|karaoke|singer|singing|bollywood|punjabi|romance|romantic|edm|classical|sufi|harmonium|tabla|flute|guitar|piano|gaane|geet|chords|bass boosted|t-series|saregama|yrf|tips official|sony music|zee music)\\b",
        RegexOption.IGNORE_CASE
    )

    fun isMusicTrack(title: String, channelName: String, durationSeconds: Long = 0): Boolean {
        val combined = "$title $channelName"

        // 1. Strict exclusion check
        if (EXCLUDE_PATTERN.containsMatchIn(combined)) {
            return false
        }

        // 2. Excessively long videos (> 15 mins = 900s) without jukebox/mashup/playlist keyword are excluded
        val lowerCombined = combined.lowercase()
        if (durationSeconds > 900 &&
            !lowerCombined.contains("jukebox") &&
            !lowerCombined.contains("mashup") &&
            !lowerCombined.contains("playlist") &&
            !lowerCombined.contains("non stop") &&
            !lowerCombined.contains("nonstop")
        ) {
            return false
        }

        // 3. Check positive music signals (Must match at least one strong music keyword or music channel)
        return MUSIC_PATTERN.containsMatchIn(combined)
    }
}


