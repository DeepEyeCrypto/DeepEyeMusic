package com.deepeye.musicpro.domain.recommendation

import com.deepeye.musicpro.data.db.ListenEvent
import com.deepeye.musicpro.data.db.RecommendationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val dao: RecommendationDao,
    private val fetcher: ContentFetcher,
    private val scorer: ScoringEngine
) {

    private var currentSessionId: String = "session_${System.currentTimeMillis()}"

    // Main function — called on app open, returns all recommendation rows
    suspend fun buildRecommendations(): RecommendationResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val last30Days = now - 30L * 24 * 3600 * 1000

        // 1. Get user's listen history signals
        val topSongs = dao.getTopSongsSince(last30Days, 10)
        val topArtists = dao.getTopArtistsSince(last30Days, 5)
        val topGenres = dao.getTopGenres(last30Days)
        val blacklist = dao.getBlacklistedVideoIds().toSet()
        val timeCtx = scorer.getTimeContextWeight()

        // 2. Build seed video IDs for related fetching
        val seedVideoIds = topSongs.map { it.videoId }.take(5)
        val seedArtists = topArtists.map { it.artistName }.take(3)

        // 3. Fetch content in parallel
        val relatedLists = seedVideoIds.map { id ->
            async { fetcher.getRelatedVideos(id, 15) }
        }.awaitAll().flatten()

        val artistLists = seedArtists.map { artist ->
            async { fetcher.searchByArtist(artist, 10) }
        }.awaitAll().flatten()

        val trendingTask = async { fetcher.getTrendingMusic("IN", 20) }

        // Time-context based fetch
        val contextQuery = when (timeCtx) {
            ScoringEngine.TimeContext.MORNING_CHILL -> "lofi morning chill hindi"
            ScoringEngine.TimeContext.FOCUS        -> "instrumental focus music"
            ScoringEngine.TimeContext.AFTERNOON    -> "bollywood upbeat 2024"
            ScoringEngine.TimeContext.EVENING      -> "arijit singh romantic"
            ScoringEngine.TimeContext.NIGHT        -> "bass boosted night drive"
        }
        val contextualTask = async { fetcher.searchByQuery(contextQuery, 15) }

        val trending = trendingTask.await()
        val contextual = contextualTask.await()

        // 4. Deduplicate + remove blacklisted
        val allCandidates = (relatedLists + artistLists + contextual)
            .distinctBy { it.videoId }
            .filter { it.videoId !in blacklist }

        // 5. Score candidates
        val scored = allCandidates.map { video ->
            val artistBoost = topArtists
                .find { it.artistName.contains(video.artist, ignoreCase = true) }
                ?.avgCompletion ?: 0f
            val score = artistBoost * 0.4f + 0.6f
            ScoredVideo(video, score)
        }.sortedByDescending { it.score }

        // 6. Build recommendation rows for UI
        RecommendationResult(
            // Row 1: Because you listened to X
            becauseYouListened = topSongs.take(3).map { song ->
                RecommendationRow(
                    title = "Because you played \"${song.title}\"",
                    subtitle = song.artist,
                    items = relatedLists
                        .filter { it.videoId != song.videoId }
                        .take(10)
                )
            },
            // Row 2: Your favorite artists
            favoriteArtists = topArtists.take(3).map { artist ->
                RecommendationRow(
                    title = "More from ${artist.artistName}",
                    subtitle = "${artist.playCount} songs played",
                    items = artistLists.filter {
                        it.artist.contains(artist.artistName, ignoreCase = true)
                    }.take(10)
                )
            },
            // Row 3: Perfect for right now (time context)
            perfectForNow = RecommendationRow(
                title = when (timeCtx) {
                    ScoringEngine.TimeContext.MORNING_CHILL -> "☀️ Good Morning Vibes"
                    ScoringEngine.TimeContext.FOCUS         -> "🎯 Focus Mode"
                    ScoringEngine.TimeContext.AFTERNOON     -> "🌅 Afternoon Hits"
                    ScoringEngine.TimeContext.EVENING       -> "🌙 Evening Chill"
                    ScoringEngine.TimeContext.NIGHT         -> "🌃 Late Night Bangers"
                },
                subtitle = "Curated for this time",
                items = contextual.take(15)
            ),
            // Row 4: Trending in India
            trending = RecommendationRow(
                title = "🔥 Trending in India",
                subtitle = "Updated today",
                items = trending.take(20)
            ),
            // Row 5: Top genres deep dive
            genreDive = topGenres.take(2).map { genre ->
                RecommendationRow(
                    title = "More ${genre.genre}",
                    subtitle = "Based on your taste",
                    items = scored.filter { it.video.genre.contains(genre.genre, ignoreCase = true) }
                        .map { it.video }.take(10)
                )
            },
            // Row 6: Hidden gems (high score but low play count globally)
            hiddenGems = RecommendationRow(
                title = "💎 Hidden Gems",
                subtitle = "You might love these",
                items = scored
                    .filter { it.score > 0.7f }
                    .map { it.video }
                    .take(10)
            )
        )
    }

    // Called every time a song finishes playing
    suspend fun trackListenEvent(
        videoId: String,
        title: String,
        artist: String,
        channelId: String,
        listenDurationMs: Long,
        totalDurationMs: Long,
        wasSkipped: Boolean,
        wasLiked: Boolean,
        wasDisliked: Boolean,
        wasAddedToPlaylist: Boolean,
        wasReplayed: Boolean
    ) {
        val completionRatio = if (totalDurationMs > 0)
            (listenDurationMs.toFloat() / totalDurationMs) else 0f
            
        val event = ListenEvent(
            videoId = videoId,
            title = title,
            artist = artist,
            channelId = channelId,
            listenDurationMs = listenDurationMs,
            totalDurationMs = totalDurationMs,
            completionRatio = completionRatio,
            wasSkipped = wasSkipped,
            wasLiked = wasLiked,
            wasDisliked = wasDisliked,
            wasAddedToPlaylist = wasAddedToPlaylist,
            wasReplayed = wasReplayed,
            seekCount = 0,
            shareCount = 0,
            timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
            isHeadphonesOn = false,
            sessionId = currentSessionId
        )
        withContext(Dispatchers.IO) {
            dao.insertListenEvent(event)
        }
    }
}
