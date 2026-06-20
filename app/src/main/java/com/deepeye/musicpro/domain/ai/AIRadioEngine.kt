// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.ai

import android.net.Uri
import com.deepeye.musicpro.data.source.remote.youtube.YoutubeRemoteDataSource
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.MoodMix
import com.deepeye.musicpro.player.controller.PlayerController
import com.deepeye.musicpro.player.queue.QueueManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.deepeye.musicpro.domain.repository.TasteProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRadioEngine @Inject constructor(
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val playerController: PlayerController,
    private val queueManager: QueueManager,
    private val tasteProfileRepo: TasteProfileRepository
) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /**
     * Parses a natural language prompt, fetches relevant songs, and starts playback.
     */
    suspend fun generateRadioFromPrompt(prompt: String) {
        if (prompt.isBlank()) return
        
        _isGenerating.value = true
        
        try {
            withContext(ioDispatcher) {
                val tasteProfile = try { tasteProfileRepo.getTasteProfile().first() } catch (e: Exception) { null }
                val langs = tasteProfile?.preferredLanguages?.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: "hindi punjabi english"
                val artists = tasteProfile?.favoriteArtists?.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: ""
                val personalSuffix = "$langs $artists".trim()
                
                // Mock NLP parser: We extract core keywords to feed to YouTube Music
                val query = parseIntentToSearchQuery(prompt, personalSuffix)
                
                // Fetch tracks from YouTube Music
                val tracks = youtubeRemoteDataSource.searchMusic(query).take(15)
                
                if (tracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        // Replace the queue and start playing
                        val mediaItems = tracks.map { track ->
                            MediaItem.Remote(
                                id = track.id,
                                title = track.title,
                                artist = track.artist,
                                artworkUri = Uri.parse(track.thumbnailUrl),
                                duration = track.duration * 1000L,
                                isVideo = false
                            )
                        }
                        
                        queueManager.setQueue(mediaItems, 0)
                        playerController.playMedia(mediaItems.first())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isGenerating.value = false
        }
    }

    /**
     * Starts playback based on a Mood Chip click.
     */
    suspend fun playMoodMix(moodMix: MoodMix) {
        _isGenerating.value = true
        try {
            withContext(ioDispatcher) {
                val query = "${moodMix.label} ${moodMix.query}"
                val tracks = youtubeRemoteDataSource.searchMusic(query).take(15)
                
                if (tracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val mediaItems = tracks.map { track ->
                            MediaItem.Remote(
                                id = track.id,
                                title = track.title,
                                artist = track.artist,
                                artworkUri = Uri.parse(track.thumbnailUrl),
                                duration = track.duration * 1000L,
                                isVideo = false
                            )
                        }
                        queueManager.setQueue(mediaItems, 0)
                        playerController.playMedia(mediaItems.first())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isGenerating.value = false
        }
    }

    private fun parseIntentToSearchQuery(prompt: String, personalSuffix: String): String {
        val lowerPrompt = prompt.lowercase().trim()
        
        // Remove conversational filler words
        val fillerWords = listOf("play", "some", "music", "songs", "i want to listen to", "show me", "give me")
        var coreQuery = lowerPrompt
        for (filler in fillerWords) {
            coreQuery = coreQuery.replace(filler, "")
        }
        
        // Add specific genre boosts if detected
        if (coreQuery.contains("romantic") || coreQuery.contains("love")) {
            coreQuery += " romantic"
        }
        if (coreQuery.contains("party") || coreQuery.contains("dance")) {
            coreQuery += " party anthems"
        }
        if (coreQuery.contains("sad") || coreQuery.contains("cry")) {
            coreQuery += " sad lofi emotional"
        }
        
        val finalQuery = "${coreQuery.trim()} $personalSuffix".trim()
        return finalQuery.ifBlank { "top hits $personalSuffix".trim() }
    }
}
