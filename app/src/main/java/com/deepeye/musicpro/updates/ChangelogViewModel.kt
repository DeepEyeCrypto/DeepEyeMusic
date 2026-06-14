package com.deepeye.musicpro.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Controls changelog popup visibility.
 * - checkForUpdate: compares current versionCode against last-shown
 * - onDismiss ("Got it"): marks version as shown, hides popup permanently
 * - snooze ("Later"): hides popup without marking, will show again next launch
 */
@HiltViewModel
class ChangelogViewModel
@Inject
constructor(
    private val prefs: UpdatePrefsManager,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state = _state.asStateFlow()

    private var hasCheckedThisSession = false

    fun checkForUpdate(currentVersionCode: Int) {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        viewModelScope.launch {
            try {
                val lastShown = prefs.getLastShownVersion()
                val shouldShow = currentVersionCode > lastShown
                android.util.Log.d(
                    "ChangelogDialogTest",
                    "currentVersionCode: $currentVersionCode, lastShown: $lastShown, shouldShow: $shouldShow",
                )

                _state.value =
                    UpdateState(
                        currentVersionCode = currentVersionCode,
                        lastShownVersionCode = lastShown,
                        shouldShowChangelog = shouldShow,
                        changelogEntries =
                        AppChangelog.entries
                            .filter { it.versionCode <= currentVersionCode }
                            .sortedByDescending { it.versionCode },
                    )
            } catch (e: Exception) {
                android.util.Log.e("ChangelogDialogTest", "Error reading prefs", e)
                // Silently skip — never block the app
            }
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            try {
                prefs.markVersionShown(_state.value.currentVersionCode)
            } catch (e: Exception) {
                android.util.Log.e("Changelog", "Error reading prefs", e)
                // Best effort
            }
            _state.update { it.copy(shouldShowChangelog = false) }
        }
    }

    fun snooze() {
        _state.update { it.copy(shouldShowChangelog = false) }
    }
}
