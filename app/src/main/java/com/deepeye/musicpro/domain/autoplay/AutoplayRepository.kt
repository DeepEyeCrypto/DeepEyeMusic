package com.deepeye.musicpro.domain.autoplay

import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoplayRepository
@Inject
constructor(

    private val dao: RecommendationDao,
    private val contentFetcher: ContentFetcher,
) {
    private val scorer = AutoplayScorer()

    suspend fun generateNextQueue(
        currentTrack: MediaItem?,
        autoplayState: AutoplayState,
    ): List<QueueItem> =
        withContext(Dispatchers.IO) {
            val since = System.currentTimeMillis() - 30L * 86400_000

            // 1. Seed candidates from recent listens and preferences
            val topSongs = dao.getTopSongsSince(since, 15)
            val topArtists = dao.getTopArtistsSince(since, 10)

            // Try getting history
            // Wait, dao.getSongsForTimeContext isn't exactly all history.
            // Let's just use the currentTrack to seed Related.

            // 2. Build candidate pool in parallel
            val candidateLists =
                coroutineScope {
                    val fromHistory =
                        async {
                            if (currentTrack != null) {
                                if (currentTrack is MediaItem.Local) {
                                    // Local file IDs aren't valid YouTube IDs, so we search YouTube
                                    // for the song's title and artist to get relevant recommendations.
                                    val query = "${currentTrack.title} ${currentTrack.artist} song audio"
                                    contentFetcher.searchByQuery(query, 20)
                                } else {
                                    contentFetcher.getRelatedVideos(currentTrack.id, 20)
                                }
                            } else {
                                emptyList()
                            }
                        }

                    val trending =
                        async {
                            contentFetcher.getTrendingMusic("IN", 15)
                        }

                    val allCandidates = mutableListOf<com.deepeye.musicpro.domain.recommendation.VideoItem>()
                    allCandidates.addAll(fromHistory.await())
                    allCandidates.addAll(trending.await())
                    allCandidates
                }

            // 3. Deduplicate and remove blacklist/history
            val blacklist = dao.getBlacklistedVideoIds().toSet() + autoplayState.blacklist
            val recentHistory = autoplayState.history.takeLast(30).toSet()

            val candidates =
                candidateLists
                    .distinctBy { it.videoId }
                    .filter { it.videoId !in blacklist }
                    .filter { it.videoId !in recentHistory }

            // 4. Score each candidate
            val scored =
                candidates.map { video ->
                    val c = CandidateTrack.fromVideo(video)
                    val score =
                        scorer.scoreCandidate(
                            candidate = c,
                            history = emptyList(), // We could fetch recent ListenEvents, but leaving empty for now
                            autoplayState = autoplayState,
                        )
                    QueueItem(
                        videoId = video.videoId,
                        title = video.title,
                        artist = video.artist,
                        channelId = video.channelId,
                        reason = buildReason(c, autoplayState),
                        rank = 0,
                        score = score,
                    )
                }.sortedByDescending { it.score }
                    .take(20)
                    .mapIndexed { index, item -> item.copy(rank = index + 1) }

            scored
        }

    private fun buildReason(
        candidate: CandidateTrack,
        state: AutoplayState,
    ): String {
        return when {
            state.discoveryMode -> "New discovery for you"
            state.familiarMode -> "Similar to what you played"
            else -> "Picked for you"
        }
    }
}
