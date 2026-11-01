// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.diagnostics.research

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One structured, PII-free observation of a YouTube personalization/autoplay
 * surface (plan Task 1). Persisted as a single JSON line so the on-device
 * captures can be pulled off the device and diffed against the real YouTube
 * app's behaviour.
 *
 * NEVER store track titles, account ids, or tokens — only video ids and
 * counts (Phase 3 PII rule, same as AuthenticatedYouTubeClient).
 */
data class ResearchCapture(
    val endpoint: String,
    val videoId: String,
    val ts: Long,
    val resultSummary: Map<String, String>,
    val items: List<String>,
) {
    fun toJsonLine(): String =
        JSONObject()
            .put("endpoint", endpoint)
            .put("videoId", videoId)
            .put("ts", ts)
            .put("summary", JSONObject(resultSummary))
            .put("items", org.json.JSONArray().also { j -> items.forEach { j.put(it) } })
            .toString()
}

/**
 * On-device InnerTube research probe.
 *
 * Mirrors exactly what the app's personalization + autoplay consume (music-home
 * shelves, the `/next` autoplay graph, related videos) and appends one
 * JSON line per call to `<filesDir>/research/captures.log` so the user's
 * operator workflow (`adb shell run-as com.deepeye.musicpro cat ...`) gets
 * real evidence of what YouTube's algorithm returned for this account.
 */
@Singleton
class InnerTubeResearchProbe
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authClient: com.deepeye.musicpro.data.source.remote.youtube.AuthenticatedYouTubeClient,
        private val smartTubeEngine: com.deepeye.musicpro.data.source.remote.youtube.SmartTubeEngine,
        private val innerTubeClient: com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient,
        private val youtubeDs: com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource,
    ) {
        companion object {
            private const val TAG = "YtResearchProbe"
        }

        private fun captureFile(): File {
            val dir = File(context.filesDir, "research")
            if (!dir.exists()) dir.mkdirs()
            return File(dir, "captures.log")
        }

        /** Append one JSON line. Failures are swallowed — research must never break playback. */
        fun persist(c: ResearchCapture) {
            try {
                captureFile().appendText(c.toJsonLine() + "\n")
                Log.i(TAG, "capture persisted: ${c.endpoint} summary=${c.resultSummary}")
            } catch (e: Exception) {
                Log.w(TAG, "capture persist failed: ${e.message}")
            }
        }

        /**
         * The account's live YouTube Music home shelves ("Listen again", "Quick picks", …).
         * Returns one capture per non-empty shelf.
         */
        suspend fun probeMusicHome(): List<ResearchCapture> =
            withContext(Dispatchers.IO) {
                val captures = mutableListOf<ResearchCapture>()
                val shelves =
                    try {
                        authClient.getMusicHomeShelves()
                    } catch (e: Exception) {
                        Log.w(TAG, "music home probe failed: ${e.message}")
                        emptyList()
                    }
                shelves.forEach { shelf ->
                    val c =
                        ResearchCapture(
                            endpoint = "music_home_shelf:${shelf.title}",
                            videoId = "",
                            ts = System.currentTimeMillis(),
                            resultSummary =
                                mapOf(
                                    "shelfItems" to shelf.items.size.toString(),
                                    "firstIds" to shelf.items.take(3).joinToString(",") { it.id },
                                ),
                            items = shelf.items.map { it.id },
                        )
                    captures.add(c)
                    persist(c)
                }
                if (captures.isEmpty()) {
                    val empty =
                        ResearchCapture(
                            endpoint = "music_home",
                            videoId = "",
                            ts = System.currentTimeMillis(),
                            resultSummary = mapOf("shelves" to "0"),
                            items = emptyList(),
                        )
                    persist(empty)
                    captures.add(empty)
                }
                captures
            }

        /**
         * The real "Up Next" algorithm for [videoId] from BOTH providers
         * (SmartTube TVHTML5 + InnerTube ANDROID_MUSIC). This is the ground
         * truth for what native YouTube autoplays next for this account.
         */
        suspend fun probeAutoplayGraph(videoId: String): List<ResearchCapture> =
            withContext(Dispatchers.IO) {
                val captures = mutableListOf<ResearchCapture>()

                val upNext =
                    try {
                        smartTubeEngine.fetchUpNext(videoId)
                    } catch (e: Exception) {
                        Log.w(TAG, "up-next probe failed: ${e.message}")
                        null
                    }
                val musicNext =
                    try {
                        innerTubeClient.fetchNextAutoplay(videoId)
                    } catch (e: Exception) {
                        Log.w(TAG, "music /next probe failed: ${e.message}")
                        null
                    }

                captures.add(
                    ResearchCapture(
                        endpoint = "up_next_smarttube",
                        videoId = videoId,
                        ts = System.currentTimeMillis(),
                        resultSummary =
                            mapOf(
                                "nextVideoId" to (upNext?.id ?: "none"),
                                "nextDurationMs" to (upNext?.duration?.toString() ?: "0"),
                            ),
                        items = upNext?.let { listOf(it.id) } ?: emptyList(),
                    )
                )
                captures.add(
                    ResearchCapture(
                        endpoint = "up_next_innertube_music",
                        videoId = videoId,
                        ts = System.currentTimeMillis(),
                        resultSummary =
                            mapOf(
                                "nextVideoId" to (musicNext?.videoId ?: "none"),
                                "nextDurationSec" to (musicNext?.durationSeconds?.toString() ?: "0"),
                            ),
                        items = musicNext?.let { listOf(it.videoId) } ?: emptyList(),
                    )
                )
                captures.forEach { persist(it) }
                captures
            }

        /**
         * Related-videos rail for [videoId] (NewPipe-primary extractor now).
         */
        suspend fun probeRelated(videoId: String): ResearchCapture =
            withContext(Dispatchers.IO) {
                val related =
                    try {
                        youtubeDs.getRelatedVideos(videoId).take(10)
                    } catch (e: Exception) {
                        Log.w(TAG, "related probe failed: ${e.message}")
                        emptyList()
                    }
                val c =
                    ResearchCapture(
                        endpoint = "related_videos",
                        videoId = videoId,
                        ts = System.currentTimeMillis(),
                        resultSummary = mapOf("count" to related.size.toString()),
                        items = related.map { it.id },
                    )
                persist(c)
                c
            }

        /** Convenience method: capture related videos for [videoId] and return a short status
         * string. The JSON payload is appended to captures.log.
         */
        suspend fun captureRelatedVideos(videoId: String): String {
            val capture = probeRelated(videoId)
            return when {
                capture.items.isEmpty() -> "NO_RELATED: ${capture.endpoint}"
                capture.items.size == 1 -> "1_RELATED: ${capture.items[0]}"
                else -> "${capture.items.size}_RELATED: ${capture.items[0]},${capture.items[1]}"
            }
        }
    }
