package com.deepeye.musicpro.domain.autoplay

import com.deepeye.musicpro.data.db.ListenEvent
import com.deepeye.musicpro.domain.recommendation.VideoItem
import java.util.Calendar

data class CandidateTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val channelId: String,
    val language: String? = null,
    val genre: String? = null,
    // Synthesized acoustic traits based on heuristics
    val energy: Float,
    val instrumentalScore: Float,
    val bassScore: Float,
    val chillScore: Float,
    val focusScore: Float,
    val upbeatScore: Float,
    val romanticScore: Float,
    val nightDriveScore: Float,
    // Engine scores
    val similarityToLastTrack: Float = 0.5f,
    val discoveryScore: Float = 0.5f,
    val freshnessScore: Float = 0.5f,
) {
    companion object {
        fun fromVideo(video: VideoItem): CandidateTrack {
            val lowerTitle = video.title.lowercase()

            // Very simple keyword heuristics to simulate audio analysis
            val energy = if (lowerTitle.contains("remix") || lowerTitle.contains("party") || lowerTitle.contains("workout")) 0.9f else 0.5f
            val instrumental = if (lowerTitle.contains("instrumental") || lowerTitle.contains("lofi") || lowerTitle.contains("beat")) 0.8f else 0.1f
            val bass = if (lowerTitle.contains("bass") || lowerTitle.contains("808") || lowerTitle.contains("trap")) 0.9f else 0.4f
            val chill = if (lowerTitle.contains("lofi") || lowerTitle.contains("chill") || lowerTitle.contains("relax")) 0.9f else 0.3f
            val focus = if (lowerTitle.contains("focus") || lowerTitle.contains("study") || instrumental > 0.5f) 0.8f else 0.2f
            val upbeat = if (lowerTitle.contains("upbeat") || lowerTitle.contains("pop") || lowerTitle.contains("happy")) 0.8f else 0.4f
            val romantic = if (lowerTitle.contains("love") || lowerTitle.contains("romantic") || lowerTitle.contains("sad")) 0.8f else 0.2f
            val nightDrive = if (lowerTitle.contains("synthwave") || lowerTitle.contains("midnight") || lowerTitle.contains("slowed")) 0.9f else 0.4f

            val language =
                when {
                    lowerTitle.contains("hindi") -> "Hindi"
                    lowerTitle.contains("punjabi") -> "Punjabi"
                    lowerTitle.contains("tamil") -> "Tamil"
                    lowerTitle.contains("telugu") -> "Telugu"
                    lowerTitle.contains("english") -> "English"
                    else -> null
                }

            return CandidateTrack(
                videoId = video.videoId,
                title = video.title,
                artist = video.artist,
                channelId = video.channelId,
                language = language,
                genre = video.genre.ifEmpty { null },
                energy = energy,
                instrumentalScore = instrumental,
                bassScore = bass,
                chillScore = chill,
                focusScore = focus,
                upbeatScore = upbeat,
                romanticScore = romantic,
                nightDriveScore = nightDrive,
                discoveryScore = Math.random().toFloat(),
                freshnessScore = Math.random().toFloat(),
            )
        }
    }
}

class AutoplayScorer {
    fun scoreCandidate(
        candidate: CandidateTrack,
        history: List<ListenEvent>,
        autoplayState: AutoplayState,
    ): Float {
        var score = 0f

        // 5. Recency penalty (avoid same song too soon)
        if (candidate.videoId in autoplayState.history.takeLast(20)) score -= 0.50f

        // 6. Blacklist penalty
        if (candidate.videoId in autoplayState.blacklist) score -= 1.0f

        // 7. Session similarity
        val recentHistory = history.takeLast(5)
        if (recentHistory.isNotEmpty()) {
            val validHistory = recentHistory.filter { !it.completionRatio.isNaN() }
            if (validHistory.isNotEmpty()) {
                val recentCompletion = validHistory.map { it.completionRatio }.average().toFloat()
                if (recentCompletion > 0.85f) {
                    // user is happy, keep it similar
                    score += candidate.similarityToLastTrack * 0.25f
                } else if (autoplayState.skipStreak >= 2) {
                    // user is bored, diversify
                    score += candidate.discoveryScore * 0.25f
                }
            } else {
                // fallback if all completion ratios are NaN
                score += candidate.discoveryScore * 0.10f
            }
        } else {
            score += candidate.discoveryScore * 0.10f
        }

        // 8. Time-of-day context
        score +=
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..8 -> candidate.chillScore * 0.10f
                in 9..12 -> candidate.focusScore * 0.10f
                in 13..17 -> candidate.upbeatScore * 0.10f
                in 18..22 -> candidate.romanticScore * 0.10f
                else -> candidate.nightDriveScore * 0.15f
            }

        // 9. Freshness bonus
        score += candidate.freshnessScore * 0.10f

        return score.coerceIn(-1f, 1f)
    }

    fun pickMode(
        state: AutoplayState,
        history: List<ListenEvent>,
    ): AutoplayMode {
        val recentHistory = history.takeLast(5)
        if (recentHistory.isEmpty()) return AutoplayMode.BALANCED

        val skipRate = recentHistory.count { it.wasSkipped } / recentHistory.size.toFloat()
        return when {
            state.skipStreak >= 2 || skipRate >= 0.6f -> AutoplayMode.DISCOVERY
            recentHistory.takeLast(3).all { !it.completionRatio.isNaN() && it.completionRatio > 0.8f } -> AutoplayMode.FAMILIAR
            state.likeStreak >= 3 -> AutoplayMode.FAMILIAR_PLUS
            else -> AutoplayMode.BALANCED
        }
    }
}
