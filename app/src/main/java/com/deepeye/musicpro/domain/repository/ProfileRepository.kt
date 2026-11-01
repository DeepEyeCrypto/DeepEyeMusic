// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.domain.repository

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.data.db.ProfileCacheDao
import com.deepeye.musicpro.data.db.ProfileCacheEntity
import com.deepeye.musicpro.data.prefs.SettingsDataStore
import com.deepeye.musicpro.domain.model.profile.ProfilePhase
import com.deepeye.musicpro.domain.model.profile.ProfileState
import com.deepeye.musicpro.domain.repository.library.LibraryRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only profile dashboard source for P1.
 *
 * Combines the three already-wired data sources — [AccountSessionManager],
 * [LibraryRepository] and [SettingsDataStore] — into a single
 * [ProfileState]. No network call is made here: the avatar URL and display
 * name come from the OAuth profile snapshot stored in [SettingsDataStore], and
 * the counters come from the live library flow.
 *
 * The [ProfileCacheDao] row is written only when the live flow has produced a
 * real snapshot; it is read back only when the live flow is unavailable
 * (offline first launch). Account isolation is preserved because the cache
 * is keyed by [AccountSession.Connected.accountKey].
 */
@Singleton
class ProfileRepository
    @Inject
    constructor(
        private val accountSessionManager: AccountSessionManager,
        private val libraryRepository: LibraryRepository,
        private val settingsDataStore: SettingsDataStore,
        private val profileCacheDao: ProfileCacheDao,
    ) {
    /**
     * Live profile state. Combines the account session, the library home
     * counters and the stored OAuth profile fields.
     */
    fun observeProfile(): kotlinx.coroutines.flow.Flow<ProfileState> =
        combine(
            accountSessionManager.accountSession,
            libraryRepository.observeLibraryHome(),
            settingsDataStore.settings,
        ) { session, libraryHome, settings ->
            val connected = session as? AccountSession.Connected
            val avatarUrl = settings.youtubeUserAvatar ?: connected?.avatarUrl
            val displayName = settings.youtubeUserName ?: connected?.displayName
            val phase =
                when {
                    session is AccountSession.Error -> ProfilePhase.ERROR
                    connected == null -> ProfilePhase.SIGNED_OUT
                    else -> ProfilePhase.READY
                }
            ProfileState(
                phase = phase,
                isConnected = connected != null,
                accountKey = connected?.accountKey,
                avatarUrl = avatarUrl,
                displayName = displayName,
                email = settings.youtubeUserEmail,
                subscribedCount = libraryHome.subscribedCount,
                likedCount = libraryHome.likedCount,
                playlistCount = libraryHome.playlistCount,
                downloadCount = libraryHome.downloadCount,
                errorMessage = (session as? AccountSession.Error)?.userMessage,
                isFromCache = false,
                lastSyncedAt = System.currentTimeMillis(),
            )
        }

    /**
     * Persists the current counters to the per-account cache so the profile
     * can show them on the next offline first launch.
     */
    suspend fun persistCache(accountKey: String, state: ProfileState) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            profileCacheDao.upsert(
                ProfileCacheEntity(
                    accountKey = accountKey,
                    subscribedCount = state.subscribedCount,
                    likedCount = state.likedCount,
                    playlistCount = state.playlistCount,
                    downloadCount = state.downloadCount,
                    lastSyncedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * Reads the cached counters for the given account. Null when no row exists.
     */
    suspend fun getCachedProfile(accountKey: String): ProfileState? =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            profileCacheDao.get(accountKey)?.let {
                ProfileState(
                    accountKey = accountKey,
                    subscribedCount = it.subscribedCount,
                    likedCount = it.likedCount,
                    playlistCount = it.playlistCount,
                    downloadCount = it.downloadCount,
                    isFromCache = true,
                    lastSyncedAt = it.lastSyncedAt,
                )
            }
        }
}