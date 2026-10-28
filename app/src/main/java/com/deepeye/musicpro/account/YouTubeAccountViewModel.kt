package com.deepeye.musicpro.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.deepeye.musicpro.workers.BackgroundWorkNames
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class YouTubeAccountViewModel @Inject constructor(
    private val accountSessionManager: AccountSessionManager,
    private val settingsDataStore: com.deepeye.musicpro.data.prefs.SettingsDataStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val session: StateFlow<AccountSession> = accountSessionManager.accountSession

    fun signOut() {
        viewModelScope.launch {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(BackgroundWorkNames.REC_REFRESH)
                cancelUniqueWork(BackgroundWorkNames.QUEUE_PREFETCH)
                cancelUniqueWork(BackgroundWorkNames.CHANNEL_SYNC)
            }
            accountSessionManager.clearAccountScopedStateOnSignOut()
            settingsDataStore.setYouTubeTokens("", null)
        }
    }
}
