// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.source.remote.youtube

object MusicFilter {
    private val EXCLUDE_PATTERN = Regex(
        "\\b(news|bulletin|khabar|samachar|bjp|congress|modi|yogi|election|interview|debate|ground report|podcast|coding|javascript|frontend|backend|tutorial|vlog|vlogs|daily vlog|minivlog|prank|unboxing|review|reaction|reactions|gaming|gameplay|game|games|live stream|livestream|speech|press conference|flood|crime|court|police|upsc|ias|comedy|episode|trailer|teaser|full movie|full film|standup|cricket|ipl|scorecard|match highlights|highlights|satoshi|chutkule|kahani|kahaniya|lesson|course|lecture|guide|how to|diy|craft|crafts|experiment|promo|serial|drama|kids|cartoon|rhymes|rhyme|nursery|children|child|baby|balveer|pothole|detector|tech|android|pc|features|leaked|coloros|oppo|oneplus|smartphone|mobile|phone|update|bgmi|pubg|free fire|gadget|hack|science|geopolitics|documentary|shorts|short|ytshorts|shortsfeed|thelivetv|speedrun|scene|scenes|dialogue|action scene|movie scene|roast|meme|memes|status|ringtone|caller tune|reels|reel|tiktok|challenge|funny|cute|asmr|mukbang|cooking|recipe|fitness|gym|workout|yoga|trading|crypto|forex|stock market|finance|motivation|motivational|behind the scenes|making of|bts of)\\b",
        RegexOption.IGNORE_CASE
    )

    private val MUSIC_PATTERN = Regex(
        "\\b(song|songs|audio|music|lyrics|lyric|remix|feat|ft|official video|official audio|official music video|track|tracks|album|mashup|melody|lofi|lo-fi|beats|soundtrack|ost|acoustic|unplugged|jukebox|ghazal|qawwali|bhajan|aarti|slowed|reverb|karaoke|singer|singing|bollywood|punjabi|romance|romantic|edm|classical|sufi|harmonium|tabla|flute|guitar|piano|gaane|geet|chords|bass boosted|t-series|saregama|yrf|tips official|sony music|zee music|speed records|white hill music|geet mp3|deshi records|dm - desi melodies|tseries|vevo|records|records channel)\\b",
        RegexOption.IGNORE_CASE
    )

    private val SHORTS_HASHTAGS = listOf(
        "#short",
        "#shorts",
        "#ytshorts",
        "#shortsfeed",
        "#shortvideo",
        "#youtubeshorts",
        "(shorts)",
        "[shorts]",
    )

    fun isMusicTrack(
        title: String,
        channelName: String,
        durationSeconds: Long = 0,
        isShort: Boolean = false,
    ): Boolean {
        // 1. Explicit short flag rejection
        if (isShort) return false

        val lowerTitle = title.lowercase()
        val combined = "$title $channelName"

        // 2. Shorts hashtag / keyword rejection
        for (tag in SHORTS_HASHTAGS) {
            if (lowerTitle.contains(tag)) return false
        }

        // 3. Reject short-duration clips (< 60 seconds)
        if (durationSeconds in 1..59) {
            return false
        }

        // 4. Strict exclusion keyword check
        if (EXCLUDE_PATTERN.containsMatchIn(combined)) {
            return false
        }

        // 5. Excessively long videos (> 15 mins = 900s) without jukebox/mashup/playlist keyword are excluded
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

        // 6. Check positive music signals (Must match music pattern or legitimate music channel name)
        val channelLower = channelName.lowercase()
        val isMusicChannel = channelLower.contains("music") ||
            channelLower.contains("records") ||
            channelLower.contains("vevo") ||
            channelLower.contains("sound") ||
            channelLower.contains("audio") ||
            channelLower.contains("t-series") ||
            channelLower.contains("saregama") ||
            channelLower.contains("zee music") ||
            channelLower.contains("yrf")

        return isMusicChannel || MUSIC_PATTERN.containsMatchIn(combined)
    }
}


