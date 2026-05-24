package com.deepeye.musicpro.domain.recommendation

import com.deepeye.musicpro.data.db.ListenEvent
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoringEngine @Inject constructor() {

    // Score a single listen event (0.0 to 1.0)
    fun scoreListenEvent(event: ListenEvent): Float {
        var score = 0f

        // ── Completion ratio (most important signal) ──
        score += when {
            event.completionRatio >= 0.9f -> 0.40f  // loved it
            event.completionRatio >= 0.7f -> 0.30f  // liked it
            event.completionRatio >= 0.5f -> 0.15f  // ok
            event.completionRatio >= 0.3f -> 0.05f  // eh
            else -> -0.10f                          // didn't like
        }

        // ── Explicit signals ──
        if (event.wasLiked)              score += 0.25f
        if (event.wasDisliked)           score -= 0.40f
        if (event.wasSkipped)            score -= 0.15f
        if (event.wasReplayed)           score += 0.20f
        if (event.wasAddedToPlaylist)    score += 0.20f
        if (event.shareCount > 0)        score += 0.15f

        // ── Seek behavior ──
        score += when {
            event.seekCount == 0 -> 0.05f   // listened straight through
            event.seekCount > 5  -> -0.05f  // too much seeking
            else -> 0f
        }

        return score.coerceIn(-1f, 1f)
    }

    // Aggregate score for an artist from multiple listen events
    fun computeArtistScore(events: List<ListenEvent>): Float {
        if (events.isEmpty()) return 0f
        val rawScore = events.sumOf { scoreListenEvent(it).toDouble() } / events.size
        
        // Boost artists with high play count (familiarity)
        val playCountBoost = (events.size.toFloat() / 50f).coerceAtMost(0.2f)
        
        // Recency boost — recent listens matter more
        val recencyBoost = if (events.maxOf { it.timestamp } >
            System.currentTimeMillis() - 7 * 24 * 3600 * 1000L) 0.1f else 0f
            
        return (rawScore + playCountBoost + recencyBoost).toFloat().coerceIn(-1f, 1f)
    }

    // Time-of-day context weight
    fun getTimeContextWeight(): TimeContext {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..8   -> TimeContext.MORNING_CHILL
            in 9..12  -> TimeContext.FOCUS
            in 13..17 -> TimeContext.AFTERNOON
            in 18..22 -> TimeContext.EVENING
            else      -> TimeContext.NIGHT
        }
    }

    enum class TimeContext {
        MORNING_CHILL, FOCUS, AFTERNOON, EVENING, NIGHT
    }
}
