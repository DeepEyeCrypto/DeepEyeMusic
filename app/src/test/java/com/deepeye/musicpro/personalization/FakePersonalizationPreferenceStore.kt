// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.personalization

import com.deepeye.musicpro.data.prefs.PersonalizationPreferenceStore
import com.deepeye.musicpro.data.prefs.PersonalizationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [PersonalizationPreferenceStore] for plain-JVM repository tests. */
class FakePersonalizationPreferenceStore(
    initial: PersonalizationPreferences = PersonalizationPreferences(),
) : PersonalizationPreferenceStore {

    private val _state = MutableStateFlow(initial)
    val state = _state.asStateFlow()

    override fun observe(): Flow<PersonalizationPreferences> = _state

    override suspend fun current(): PersonalizationPreferences = _state.value

    override suspend fun update(transform: (PersonalizationPreferences) -> PersonalizationPreferences) {
        _state.value = transform(_state.value)
    }
}
