// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.autoplay

import com.deepeye.musicpro.data.cache.CacheManager
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.recommendation.ContentFetcher
import com.deepeye.musicpro.domain.recommendation.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoplayRepository @Inject constructor(
    private val contentFetcher: ContentFetcher,
    private val smartTubeEngine: SmartTubeEngine,
    private val innerTubeClient: InnerTubeRemoteClient,
    private val cacheManager: CacheManager,
    private val networkConditionChecker: com.deepeye.musicpro.util.NetworkConditionChecker,
    private val settingsDataStore: com.deepeye.musicpro.data.prefs.SettingsDataStore,
) {
    companion object {
        /**
         * AUTOPLAY-OMEGA: how many recently-played tracks to exclude from the next queue.
         * InnerTube up-next graphs frequently contain short cycles (A→B→A); excluding the
         * recent session window breaks those loops without re-introducing taste/blacklist
         * interference — InnerTube still dictates *what* plays.
         */
        private const val RECENT_HISTORY_GUARD_WINDOW = 15
    }

    suspend fun generateNextQueue(
        currentTrack: MediaItem?,
        autoplayState: AutoplayState,
        activeQueueArtists: List<String> = emptyList(),
    ): List<QueueItem> = withContext(Dispatchers.IO) {
        if (currentTrack == null) return@withContext emptyList()

        android.util.Log.i(
            "AUTOPLAY_DEBUG",
            "event=generate_next_queue_enter videoId=${currentTrack.id} " +
                "type=${currentTrack::class.simpleName}"
        )

        // Change 3 — Cellular / metered network guard.
        // Pre-fetch only fires on unmetered networks (Wi-Fi/ethernet) OR when the user
        // has explicitly opted in via the "Autoplay on Cellular" setting. This prevents
        // the 15s pre-fetch from silently draining the user's mobile data.
        val isMetered = networkConditionChecker.isNetworkMetered()
        val autoplayOnCellular = settingsDataStore.settings.first().autoplayOnCellular
        if (isMetered && !autoplayOnCellular) {
            android.util.Log.w(
                "AUTOPLAY_DEBUG",
                "event=generate_next_queue_skip reason=metered_network videoId=${currentTrack.id} " +
                    "metered=$isMetered cellularOptIn=$autoplayOnCellular"
            )
            android.util.Log.i(
                "AutoplayRepository",
                "event=autoplay_prefetch_skipped reason=metered_network cellular_opt_in=false"
            )
            return@withContext emptyList()
        }

        // ISOLATION-OMEGA (non-destructive bypass): return cached InnerTube queue as-is.
        // No local blacklist/sessionHistory filtering — InnerTube is the sole authority.
        if (currentTrack !is MediaItem.Local) {
            val cachedQueue = cacheManager.loadAutoplayQueue(currentTrack.id)
            if (!cachedQueue.isNullOrEmpty()) {
                return@withContext cachedQueue
            }
        }

        val candidates = coroutineScope {
            val smartTubeDeferred = async {
                if (currentTrack !is MediaItem.Local) {
                    try {
                        // Change 4 — AUTOPLAY-OMEGA wiring: SmartTubeEngine.fetchUpNext() is the
                        // primary Up-Next extraction (TVHTML5 /next with the real
                        // singleColumnWatchNextResults.autoplay sets parsing). The ANDROID_MUSIC
                        // client (401-retry) remains the fallback. Both run on Dispatchers.IO.
                        val upNextItem = smartTubeEngine.fetchUpNext(currentTrack.id) as? MediaItem.Remote
                        val primary = if (upNextItem != null && upNextItem.id.isNotBlank()) {
                            VideoItem(
                                videoId = upNextItem.id,
                                title = upNextItem.title,
                                artist = upNextItem.artist,
                                channelId = "",
                                duration = (upNextItem.duration / 1000L).toString(),
                                genre = "SmartTube-Algorithmic"
                            )
                        } else {
                            innerTubeClient.fetchNextAutoplay(currentTrack.id)?.let { track ->
                                if (track.videoId.isNotBlank()) {
                                    VideoItem(
                                        videoId = track.videoId,
                                        title = track.title,
                                        artist = track.artist,
                                        channelId = "",
                                        duration = track.durationSeconds.toString(),
                                        genre = "SmartTube-Algorithmic"
                                    )
                                } else null
                            }
                        }
                        if (primary != null) listOf(primary) else emptyList()
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "AutoplayRepository",
                            "event=autoplay_up_next_fetch_failed videoId=${currentTrack.id} reason=\"${e.message}\"",
                            e
                        )
                        emptyList()
                    }
                } else emptyList()
            }

            val relatedDeferred = async {
                try {
                    if (currentTrack is MediaItem.Local) {
                        val query = "${currentTrack.title} ${currentTrack.artist} song audio"
                        contentFetcher.searchByQuery(query, 20)
                    } else {
                        val isVideo = (currentTrack as? MediaItem.Remote)?.isVideo ?: false
                        contentFetcher.getRelatedVideos(currentTrack.id, 20, isVideo)
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val smartList = smartTubeDeferred.await()
            val relatedList = relatedDeferred.await()

            // AUTOPLAY-OMEGA loop guard. The Isolation refactor removed the local
            // blacklist/sessionHistory filtering, leaving only a same-track guard at the
            // call sites. That alone lets InnerTube's short up-next cycles (A→B→A) loop
            // Infinite Radio forever. Here we exclude the recent session-history window
            // (already maintained in AutoplayState.history) plus the current track. This is
            // NOT taste/blacklist interference — it only prevents immediate repeats; if the
            // guard would empty the pool we fall back to a same-track-only filter so the
            // stream never halts.
            val recentHistory = autoplayState.history.takeLast(RECENT_HISTORY_GUARD_WINDOW).toSet()
            val merged = (smartList + relatedList).distinctBy { it.videoId }
            val guarded = merged.filter { it.videoId != currentTrack.id && it.videoId !in recentHistory }
            val effective = guarded.ifEmpty {
                merged.filter { it.videoId != currentTrack.id }
            }
            android.util.Log.i(
                "AutoplayRepository",
                "event=autoplay_loop_guard videoId=${currentTrack.id} " +
                    "merged=${merged.size} recentHistory=${recentHistory.size} " +
                    "guarded=${guarded.size} effective=${effective.size}"
            )
            effective
        }

        candidates.take(20).mapIndexed { index, video ->
            QueueItem(
                videoId = video.videoId,
                title = video.title,
                artist = video.artist,
                channelId = video.channelId,
                reason = if (video.genre == "SmartTube-Algorithmic") "Up Next on YouTube" else "Similar Track",
                rank = index + 1,
                score = 1.0f - (index * 0.04f),
            )
        }
    }
}
