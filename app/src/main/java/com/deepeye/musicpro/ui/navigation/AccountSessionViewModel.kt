package com.deepeye.musicpro.ui.navigation

import androidx.lifecycle.ViewModel
import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AccountSessionViewModel
    @Inject
    constructor(
        manager: AccountSessionManager,
    ) : ViewModel() {
        val session: StateFlow<AccountSession> = manager.accountSession
    }
