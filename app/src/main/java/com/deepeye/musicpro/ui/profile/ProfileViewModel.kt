// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.domain.model.profile.ProfileState
import com.deepeye.musicpro.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the P1 Profile dashboard skeleton.
 *
 * Exposes a single [StateFlow]<ProfileState> produced by
 * [ProfileRepository.observeProfile] so the screen can render the avatar,
 * name, stat chips and (non-functional) tab placeholders. When the session is
 * [AccountSession.LoggedOut] the screen shows the "Sign in" empty state; when
 * the session is [AccountSession.Loading] the screen shows a shimmer.
 *
 * No avatar write path is exposed here — P3 adds that.
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        /**
         * The single [ProfileState] the screen renders. Defaults to a
         * signed-out, zero-counted state so the UI can distinguish
         * "still loading" from "no account connected."
         */
        val state: StateFlow<ProfileState> =
            profileRepository
                .observeProfile()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ProfileState(isConnected = false),
                )

        /**
         * Manually trigger a cache persist of the current profile counters.
         * Called on screen teardown so the next cold-start can show the
         * last-known counts before the live flow arrives.
         */
        fun persistCache() {
            val s = state.value
            if (!s.isConnected) return
            viewModelScope.launch {
                profileRepository.persistCache(s.accountKey ?: "default", s)
            }
        }
    }
