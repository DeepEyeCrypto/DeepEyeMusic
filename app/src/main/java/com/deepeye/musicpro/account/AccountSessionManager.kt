// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.account

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSessionManager @Inject constructor(
    private val dataSource: AccountDataSource,
    private val cacheInvalidator: AccountScopedCacheInvalidator? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // Start with Loading before truth arrives
    private val _accountSession = MutableStateFlow<AccountSession>(AccountSession.Loading)
    val accountSession: StateFlow<AccountSession> = _accountSession.asStateFlow()

    init {
        observeState()
    }

    private fun observeState() {
        scope.launch {
            dataSource.connectedStream
                .distinctUntilChanged()
                .collectLatest { snapshot ->
                    val currentKey = snapshot?.accountKey
                    val prevState = _accountSession.value
                    val prevKey = (prevState as? AccountSession.Connected)?.accountKey
                    
                    // Account switch detection
                    if (currentKey != null && prevKey != null && currentKey != prevKey) {
                        emitSafeLog("account_session_changed", "loading", currentKey)
                        _accountSession.value = AccountSession.Loading
                        
                        // Invalidate old account cache before emitting the new one
                        try {
                            cacheInvalidator?.invalidateAccountCache(prevKey)
                        } catch (e: Exception) {
                            Log.e(TAG, "Cache invalidation failed on account switch", e)
                        }
                    }

                    // Sign-out detection
                    if (currentKey == null && prevKey != null) {
                        try {
                            // Clean up previous account cache
                            cacheInvalidator?.invalidateAccountCache(prevKey)
                        } catch (e: Exception) {
                            Log.e(TAG, "Cache invalidation failed on sign out", e)
                        }
                    }

                    if (snapshot == null) {
                        emitSafeLog("account_session_changed", "logged_out", null)
                        _accountSession.value = AccountSession.LoggedOut
                    } else {
                        emitSafeLog(
                            event = "account_session_changed",
                            state = "connected",
                            accountKey = snapshot.accountKey,
                            hasDisplayName = snapshot.displayName != null,
                            hasAvatar = snapshot.avatarUrl != null
                        )
                        _accountSession.value = AccountSession.Connected(
                            accountKey = snapshot.accountKey,
                            displayName = snapshot.displayName,
                            avatarUrl = snapshot.avatarUrl,
                            channelKey = snapshot.channelKey,
                            lastUpdatedAtMs = System.currentTimeMillis()
                        )
                    }
                }
        }
    }

    suspend fun clearAccountScopedStateOnSignOut() {
        val currentKey = currentAccountKeyOrNull()
        if (currentKey != null) {
            emitSafeLog("account_session_refresh", "logged_out", null)
            cacheInvalidator?.invalidateAccountCache(currentKey)
        }
    }

    fun isConnected(): Boolean {
        return _accountSession.value is AccountSession.Connected
    }

    fun currentAccountKeyOrNull(): String? {
        return (_accountSession.value as? AccountSession.Connected)?.accountKey
    }

    /**
     * Refreshes the session status.
     */
    suspend fun refreshSession() {
        val connected = isConnected()
        emitSafeLog(
            event = "account_session_refresh",
            state = if (connected) "success" else "logged_out",
            accountKey = currentAccountKeyOrNull(),
            reason = "manual_refresh"
        )
    }

    private fun emitSafeLog(
        event: String, 
        state: String, 
        accountKey: String?,
        hasDisplayName: Boolean? = null,
        hasAvatar: Boolean? = null,
        reason: String? = null
    ) {
        val hash = accountKey?.take(6) ?: "null"
        val displayStr = hasDisplayName?.let { " hasDisplayName=$it" } ?: ""
        val avatarStr = hasAvatar?.let { " hasAvatar=$it" } ?: ""
        val reasonStr = reason?.let { " reason=$it" } ?: ""
        
        try {
            Log.d(TAG, "DeepEyePersonalization: event=$event state=$state accountKeyHash=$hash$displayStr$avatarStr$reasonStr")
        } catch (_: Exception) {
            // In pure JVM unit test environment, android.util.Log might not be mocked
        }
    }

    companion object {
        private const val TAG = "AccountSessionManager"
    }
}
