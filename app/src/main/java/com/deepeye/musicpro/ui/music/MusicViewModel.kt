// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.music

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.source.remote.youtube.InnerTubeRemoteClient
import com.deepeye.musicpro.data.source.remote.youtube.MusicFilter
import com.deepeye.musicpro.domain.model.MediaItem
import com.deepeye.musicpro.domain.model.home.HomeVideoItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedItem
import com.deepeye.musicpro.domain.model.personalization.PersonalizedFeedState
import com.deepeye.musicpro.domain.model.personalization.PersonalizedItemType
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSection
import com.deepeye.musicpro.domain.model.personalization.PersonalizedSectionType
import com.deepeye.musicpro.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicUiState(
    val personalizedFeed: PersonalizedFeedState = PersonalizedFeedState(),
    val hasAuth: Boolean = false,
)

/**
 * YT-MUSIC-OMEGA: the Music tab is 100% account-driven. This ViewModel performs a
 * single authenticated InnerTube request against `FEmusic_home` (ANDROID_MUSIC client
 * + OAuth Bearer token) and maps each returned carousel into a renderable section.
 *
 * There is intentionally NO local data source here: no MediaStore scan, no Room
 * history/queue DAOs, no `PersonalizationRepository`. When the account is missing or
 * the API yields nothing, the UI shows a loading/empty state instead of falling back
 * to on-device audio.
 */
@HiltViewModel
class MusicViewModel
@Inject
constructor(
    private val innerTubeClient: InnerTubeRemoteClient,
    private val accountSessionManager: AccountSessionManager,
    private val playerController: PlayerController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MusicViewModel"
    }

    init {
        observeAccount()
    }

    /**
     * Reacts to account connect / switch / sign-out. Shelves are fetched only for a
     * connected account, and cleared on sign-out so a previous account's shelves are
     * never shown to the next session (account isolation).
     */
    private fun observeAccount() {
        viewModelScope.launch {
            accountSessionManager.accountSession.collect { session ->
                val auth = session is AccountSession.Connected
                _uiState.update { it.copy(hasAuth = auth) }
                when {
                    auth -> loadMusicHomeShelves()
                    session is AccountSession.LoggedOut -> _uiState.update {
                        it.copy(personalizedFeed = PersonalizedFeedState(accountSession = session))
                    }
                }
            }
        }
    }

    /** Fetches `FEmusic_home` shelves and emits them as the sole feed source. */
    private fun loadMusicHomeShelves() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    personalizedFeed = current.personalizedFeed.copy(
                        isLoading = current.personalizedFeed.sections.isEmpty(),
                        isRefreshing = current.personalizedFeed.sections.isNotEmpty(),
                        globalError = null,
                    ),
                )
            }
            try {
                val shelves = innerTubeClient.fetchMusicHomeShelves()
                val sections = shelves.mapIndexedNotNull { index, shelf ->
                    val items = shelf.items
                        .filter { MusicFilter.isMusicTrack(it.title, it.channelName, it.duration) }
                        .distinctBy { it.id }
                        .take(20)
                        .map { it.toFeedItem(shelf.title) }
                    if (items.isEmpty()) {
                        null
                    } else {
                        PersonalizedSection(
                            id = "sec_yt_music_${index}_${shelf.title.hashCode()}",
                            type = PersonalizedSectionType.YOUTUBE_MUSIC_SHELF,
                            title = shelf.title,
                            subtitle = null,
                            sourceLabel = "YouTube Music",
                            explanation = "From your YouTube Music ${shelf.title} shelf.",
                            items = items,
                            isFromCache = false,
                            lastUpdatedMillis = System.currentTimeMillis(),
                        )
                    }
                }
                Log.i(TAG, "[YT-MUSIC] FEmusic_home shelves=${shelves.size} rendered=${sections.size}")
                _uiState.update {
                    it.copy(
                        personalizedFeed = PersonalizedFeedState(
                            sections = sections,
                            isLoading = false,
                            isRefreshing = false,
                            globalError = null,
                            lastUpdatedMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load FEmusic_home shelves", e)
                _uiState.update {
                    it.copy(
                        personalizedFeed = it.personalizedFeed.copy(
                            isLoading = false,
                            isRefreshing = false,
                            globalError = "Unable to load your YouTube Music home. Pull to refresh.",
                        ),
                    )
                }
            }
        }
    }

    // ── Playback Actions ──────────────────────────────────────────────────

    fun playPersonalizedItem(item: PersonalizedFeedItem, itemsInSection: List<PersonalizedFeedItem>) {
        if (item.id.isBlank()) return
        val validItems = itemsInSection.filter { it.id.isNotBlank() }
        val mediaItems = validItems.mapNotNull {
            it.mediaItem ?: MediaItem.Remote(
                id = it.id,
                title = it.title,
                artist = it.artist,
                artworkUri = it.artworkUrl?.let { url -> Uri.parse(url) },
                duration = it.durationMs,
                isVideo = (it.itemType == PersonalizedItemType.VIDEO),
            )
        }
        if (mediaItems.isEmpty()) return
        val index = validItems.indexOfFirst { it.id == item.id }
        playerController.setQueue(mediaItems, if (index >= 0) index else 0)
    }

    fun playNextPersonalizedItem(item: PersonalizedFeedItem) {
        val mediaItem = resolveMediaItem(item)
        playerController.addToQueue(mediaItem)
        // Move the just-appended item to after the currently playing item
        val q = playerController.playerState.value.queue
        val currentIdx = playerController.playerState.value.currentIndex
        if (q.isNotEmpty() && currentIdx in q.indices) {
            val fromIndex = q.size - 1
            val toIndex = currentIdx + 1
            if (fromIndex > toIndex) {
                playerController.moveQueueItem(fromIndex, toIndex)
            }
        }
        Log.d(TAG, "playNext key=${item.id}")
    }

    fun addPersonalizedItemToQueue(item: PersonalizedFeedItem) {
        playerController.addToQueue(resolveMediaItem(item))
        Log.d(TAG, "addToQueue key=${item.id}")
    }

    /** Re-fetches the cloud shelves (pull-to-refresh / retry). */
    fun refreshPersonalizedFeed() {
        loadMusicHomeShelves()
    }

    /** Single cloud source: a per-section retry simply re-fetches the shelves. */
    fun refreshPersonalizedSection(sectionType: PersonalizedSectionType) {
        loadMusicHomeShelves()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun resolveMediaItem(item: PersonalizedFeedItem): MediaItem =
        item.mediaItem ?: MediaItem.Remote(
            id = item.id,
            title = item.title,
            artist = item.artist,
            artworkUri = item.artworkUrl?.let { url -> Uri.parse(url) },
            duration = item.durationMs,
            isVideo = false,
        )

    private fun HomeVideoItem.toFeedItem(shelfTitle: String): PersonalizedFeedItem {
        return PersonalizedFeedItem(
            id = id,
            title = title,
            artist = channelName,
            channelId = channelId,
            artworkUrl = thumbnailUrl,
            durationMs = duration * 1000L,
            itemType = PersonalizedItemType.SONG,
            sourceBadge = "YOUTUBE MUSIC",
            explanation = "From your YouTube Music $shelfTitle shelf.",
            mediaItem = MediaItem.Remote(
                id = id,
                title = title,
                artist = channelName,
                artworkUri = thumbnailUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                duration = duration * 1000L,
                isVideo = false,
            ),
        )
    }
}
