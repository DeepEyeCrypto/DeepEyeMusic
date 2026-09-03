// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.settings

/**
 * User actions and events supported by the Personalization Settings UI.
 */
sealed interface PersonalizationSettingsEvent {
    data class SetPersonalizationEnabled(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetUseLocalHistory(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetUseRecentSearches(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetAccountSectionsEnabled(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetSubscriptionsEnabled(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetLikedMusicEnabled(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetLocalMixEnabled(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetTrendingEnabled(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetHideNonMusic(val enabled: Boolean) : PersonalizationSettingsEvent
    data class SetCountryOverride(val region: String) : PersonalizationSettingsEvent
    data class SetPreferredMusicLanguages(val languages: Set<String>) : PersonalizationSettingsEvent
    data class ToggleLanguage(val language: String) : PersonalizationSettingsEvent
    data class SetGenreMoodPreferences(val genres: Set<String>) : PersonalizationSettingsEvent
    data class ToggleGenre(val genre: String) : PersonalizationSettingsEvent
    data class SetArtistDiversityLimit(val limit: Int) : PersonalizationSettingsEvent
    data class SetRecentlySkippedCooldown(val hours: Int) : PersonalizationSettingsEvent
    data object RefreshPersonalizedFeed : PersonalizationSettingsEvent
    data object ClearPersonalizationCacheRequested : PersonalizationSettingsEvent
    data object ClearPersonalizationCacheConfirmed : PersonalizationSettingsEvent
    data object ClearLocalHistoryRequested : PersonalizationSettingsEvent
    data object ClearLocalHistoryConfirmed : PersonalizationSettingsEvent
    data object ClearHiddenContentRequested : PersonalizationSettingsEvent
    data object ClearHiddenContentConfirmed : PersonalizationSettingsEvent
    data object DismissConfirmation : PersonalizationSettingsEvent
    data object DismissMessage : PersonalizationSettingsEvent
}
