package com.deepeye.musicpro.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AccountSessionViewModel @Inject constructor(
    manager: AccountSessionManager,
) : ViewModel() {
    val session: StateFlow<AccountSession> = manager.accountSession
}
