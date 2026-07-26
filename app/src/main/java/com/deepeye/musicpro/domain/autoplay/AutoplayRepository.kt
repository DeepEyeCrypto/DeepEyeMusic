package com.deepeye.musicpro.domain.autoplay

import com.deepeye.musicpro.data.db.RecommendationDao
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

import com.deepeye.musicpro.domain.repository.TasteProfileRepository

@Singleton
class AutoplayRepository
@Inject
constructor(
    private val dao: RecommendationDao,
    private val contentFetcher: ContentFetcher,
    private val tasteProfileRepository: TasteProfileRepository,
) {
    private val scorer = AutoplayScorer()

    suspend fun generateNextQueue(
        currentTrack: MediaItem?,
        autoplayState: AutoplayState,
        activeQueueArtists: List<String> = emptyList(),
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
            val candidatesAndScores =
                coroutineScope {
                    val fromHistory =
                        async {
                            if (currentTrack != null) {
                                val isVideo = (currentTrack as? MediaItem.Remote)?.isVideo ?: false
                                
                                val relatedTask = async {
                                    if (currentTrack is MediaItem.Local) {
                                        val query = "${currentTrack.title} ${currentTrack.artist} song audio"
                                        contentFetcher.searchByQuery(query, 20)
                                    } else {
                                        contentFetcher.getRelatedVideos(currentTrack.id, 20, isVideo)
                                    }
                                }
                                
                                // YouTube style: Explicitly fetch more from the same artist to ensure continuity
                                val artistTask = async {
                                    if (currentTrack.artist.isNotBlank() && currentTrack.artist != "Unknown") {
                                        contentFetcher.searchByQuery("${currentTrack.artist} songs", 15)
                                    } else {
                                        emptyList()
                                    }
                                }
                                
                                relatedTask.await() + artistTask.await()
                            } else {
                                emptyList()
                            }
                        }

                    val trending =
                        async {
                            val profile = tasteProfileRepository.getTasteProfile().firstOrNull()
                            if (profile?.preferredLanguages.isNullOrEmpty()) {
                                contentFetcher.getTrendingMusic("IN", 15)
                            } else {
                                emptyList()
                            }
                        }

                    val languageQuery =
                        async {
                            val profile = tasteProfileRepository.getTasteProfile().firstOrNull()
                            val langs = profile?.preferredLanguages?.takeIf { it.isNotEmpty() }?.joinToString(" ")
                            if (langs != null) {
                                contentFetcher.searchByQuery("$langs latest songs", 15).map {
                                    it.copy(genre = langs)
                                }
                            } else {
                                emptyList()
                            }
                        }

                    val related = fromHistory.await()
                    val lang = languageQuery.await()
                    val trend = trending.await()
                    
                    val allCandidates = mutableListOf<com.deepeye.musicpro.domain.recommendation.VideoItem>()
                    allCandidates.addAll(related)
                    allCandidates.addAll(lang)
                    allCandidates.addAll(trend)
                    
                    val relevanceMap = mutableMapOf<String, Float>()
                    related.forEachIndexed { i, v -> relevanceMap[v.videoId] = 1.0f - (i / 40f) }
                    lang.forEach { v -> if (!relevanceMap.containsKey(v.videoId)) relevanceMap[v.videoId] = 0.3f }
                    trend.forEach { v -> if (!relevanceMap.containsKey(v.videoId)) relevanceMap[v.videoId] = 0.1f }

                    android.util.Log.d("AutoplayEngine", "Seed track: ${currentTrack?.title} by ${currentTrack?.artist}")
                    android.util.Log.d("AutoplayEngine", "Candidate pool raw size: ${allCandidates.size}")
                    
                    Pair(allCandidates, relevanceMap)
                }

            val candidateLists = candidatesAndScores.first
            val relevanceMap = candidatesAndScores.second

            // 3. Deduplicate and remove blacklist/history
            val blacklist = dao.getBlacklistedVideoIds().toSet() + autoplayState.blacklist
            // Use sessionHistory which accurately contains ALL previously queued/played items
            val recentHistory = autoplayState.sessionHistory

            val candidates =
                candidateLists
                    .distinctBy { it.videoId }
                    .filter { it.videoId !in blacklist }
                    .filter { it.videoId !in recentHistory }
                    
            android.util.Log.d("AutoplayEngine", "Candidates after deduplication & history filter (Dropped ${candidateLists.size - candidates.size}): ${candidates.size}")

            // 4. Score each candidate
            val profile = tasteProfileRepository.getTasteProfile().firstOrNull()
            val preferredLanguages = profile?.preferredLanguages?.toList() ?: emptyList()

            val scored =
                candidates.map { video ->
                    val c = CandidateTrack.fromVideo(video, relevanceMap[video.videoId] ?: 0.5f)
                    val score =
                        scorer.scoreCandidate(
                            candidate = c,
                            seedTrack = currentTrack,
                            activeQueueArtists = activeQueueArtists,
                            history = emptyList(), // We could fetch recent ListenEvents, but leaving empty for now
                            autoplayState = autoplayState,
                            preferredLanguages = preferredLanguages
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
                }.filter { it.score > -0.5f }
                    .sortedByDescending { it.score }
                    .take(20)
                    .mapIndexed { index, item -> item.copy(rank = index + 1) }

            android.util.Log.d("AutoplayEngine", "Final Autoplay Queue Generated: ${scored.joinToString { "${it.title} (${it.score})" }}")

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
