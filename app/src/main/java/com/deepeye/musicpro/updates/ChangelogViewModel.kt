package com.deepeye.musicpro.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel
@Inject
constructor(
    private val prefs: UpdatePrefsManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
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

                if (shouldShow) {
                    val entries = AppChangelog.entries
                        .filter { it.versionCode <= currentVersionCode }
                        .sortedByDescending { it.versionCode }

                    _state.value =
                        UpdateState(
                            currentVersionCode = currentVersionCode,
                            lastShownVersionCode = lastShown,
                            shouldShowChangelog = true,
                            changelogEntries = entries,
                        )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChangelogDialogTest", "Error reading prefs", e)
            }
        }
    }

    fun showChangelogManually() {
        viewModelScope.launch {
            val currentVersionCode = com.deepeye.musicpro.BuildConfig.VERSION_CODE
            val lastShown = prefs.getLastShownVersion()
            _state.value =
                UpdateState(
                    currentVersionCode = currentVersionCode,
                    lastShownVersionCode = lastShown,
                    shouldShowChangelog = true,
                    changelogEntries = AppChangelog.entries.sortedByDescending { it.versionCode },
                )
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            try {
                prefs.markVersionShown(_state.value.currentVersionCode)
            } catch (e: Exception) {
                android.util.Log.e("Changelog", "Error reading prefs", e)
            }
            _state.update { it.copy(shouldShowChangelog = false) }
        }
    }

    fun snooze() {
        _state.update { it.copy(shouldShowChangelog = false) }
    }
}
