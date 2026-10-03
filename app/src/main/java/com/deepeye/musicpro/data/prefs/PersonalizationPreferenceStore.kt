// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.data.prefs

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over persisted personalization preferences so repository logic is
 * testable without an Android Context, while production uses a DataStore-backed impl.
 */
interface PersonalizationPreferenceStore {
    fun observe(): Flow<PersonalizationPreferences>
    suspend fun current(): PersonalizationPreferences
    suspend fun update(transform: (PersonalizationPreferences) -> PersonalizationPreferences)
}
