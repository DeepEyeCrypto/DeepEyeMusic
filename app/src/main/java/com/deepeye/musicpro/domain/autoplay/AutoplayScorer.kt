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
        fun fromVideo(video: VideoItem, sourceRelevance: Float = 0.5f): CandidateTrack {
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
                    video.genre.isNotEmpty() -> video.genre
                    lowerTitle.contains("hindi") -> "Hindi"
                    lowerTitle.contains("punjabi") -> "Punjabi"
                    lowerTitle.contains("haryanvi") -> "Haryanvi"
                    lowerTitle.contains("bhojpuri") -> "Bhojpuri"
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
                similarityToLastTrack = sourceRelevance,
                discoveryScore = 1.0f - sourceRelevance,
                freshnessScore = Math.random().toFloat(),
            )
        }
    }
}

class AutoplayScorer {
    fun scoreCandidate(
        candidate: CandidateTrack,
        seedTrack: com.deepeye.musicpro.domain.model.MediaItem?,
        activeQueueArtists: List<String> = emptyList(),
        history: List<ListenEvent>,
        autoplayState: AutoplayState,
        preferredLanguages: List<String> = emptyList(),
    ): Float {
        var score = 0f

        // Language matching
        if (preferredLanguages.isNotEmpty()) {
            if (com.deepeye.musicpro.domain.util.LanguageUtils.isLanguageBlocked(candidate.language, preferredLanguages.toSet())) {
                score -= 100f // HARD penalty to completely remove it from the pool
            } else if (candidate.language != null) {
                val isLanguagePreferred = preferredLanguages.any { candidate.language.contains(it, ignoreCase = true) }
                if (isLanguagePreferred) {
                    score += 0.8f // Big boost for preferred languages
                }
            } else {
                // Slight penalty for unknown languages to prioritize explicit language matches
                score -= 0.3f
            }
        }

        // 5. Calculate Affinity & Completion Boost
        val candidateHistory = history.filter { it.videoId == candidate.videoId }
        if (candidateHistory.isNotEmpty()) {
            val plays = candidateHistory.size
            val avgCompletion = candidateHistory.map { it.completionRatio }.average().toFloat()
            val likes = candidateHistory.count { it.wasLiked }
            
            var trackAffinity = (plays * 0.1f) + (likes * 2.0f)
            
            // Completion Boost
            trackAffinity += when {
                avgCompletion > 0.90f -> 1.0f
                avgCompletion > 0.50f -> 0.5f
                else -> 0f
            }
            score += trackAffinity
        }

        // Artist Affinity
        val artistHistory = history.filter { it.channelId == candidate.channelId }
        if (artistHistory.isNotEmpty()) {
            val plays = artistHistory.size
            val avgCompletion = artistHistory.map { it.completionRatio }.average().toFloat()
            val artistAffinity = (plays * 0.05f) + (avgCompletion * 0.5f)
            score += artistAffinity
        }

        // 6. Skip Penalty (Heavy penalty if skipped quickly)
        val recentSkips = candidateHistory.filter { it.wasSkipped && it.timestamp > System.currentTimeMillis() - 90L * 86400_000 }
        for (skip in recentSkips) {
            if (skip.listenDurationMs < 5000L) {
                score -= 2.0f // Hard skip
            } else if (skip.listenDurationMs < 30000L) {
                score -= 1.0f // Soft skip
            }
        }

        // 7. Strict Recency/Deduplication penalty (avoid same song in current session)
        if (candidate.videoId in autoplayState.sessionHistory) score -= 100f // Hard block
        else if (candidate.videoId in autoplayState.history.takeLast(50)) score -= 0.80f

        // 8. Blacklist penalty
        if (candidate.videoId in autoplayState.blacklist) score -= 100f // Hard block

        // 8.5 Artist Continuity and Diversity Score
        var artistMatchLog = ""
        if (seedTrack != null) {
            // Boost if it's the exact same artist (Continuity)
            if (candidate.artist.isNotBlank() && seedTrack.artist.contains(candidate.artist, ignoreCase = true) ||
                candidate.artist.contains(seedTrack.artist, ignoreCase = true)) {
                score += 0.4f
                artistMatchLog = "(Artist Continuity Boost +0.4) "
            }
        }
        
        // Diversity: Penalize if we already generated this artist too many times in the active queue
        val artistCountInQueue = activeQueueArtists.count { it.equals(candidate.artist, ignoreCase = true) }
        var diversityLog = ""
        if (artistCountInQueue > 0) {
            val penalty = 0.15f * artistCountInQueue
            score -= penalty
            diversityLog = "(Diversity Penalty -$penalty for $artistCountInQueue matching queued tracks) "
        }

        // 9. Session / Context Match similarity
        if (seedTrack != null) {
            score += candidate.similarityToLastTrack * 1.5f
            
            val recentHistory = history.takeLast(5)
            if (recentHistory.isNotEmpty()) {
                val validHistory = recentHistory.filter { !it.completionRatio.isNaN() }
                if (validHistory.isNotEmpty()) {
                    val recentCompletion = validHistory.map { it.completionRatio }.average().toFloat()
                    if (recentCompletion < 0.5f || autoplayState.skipStreak >= 2) {
                        score += candidate.discoveryScore * 0.5f
                    }
                }
            }
        } else {
            score += candidate.discoveryScore * 1.0f
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

        if (score > -0.5f) {
            android.util.Log.d("AutoplayEngine", "Scored [${candidate.title}]: $score $artistMatchLog$diversityLog")
        }

        return score
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
