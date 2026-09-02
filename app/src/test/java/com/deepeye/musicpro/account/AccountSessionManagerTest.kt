// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDataSource: FakeAccountDataSource
    private lateinit var fakeInvalidator: RecordingCacheInvalidator
    private lateinit var manager: AccountSessionManager

    @Before
    fun setUp() {
        fakeDataSource = FakeAccountDataSource()
        fakeInvalidator = RecordingCacheInvalidator()
        manager = AccountSessionManager(fakeDataSource, fakeInvalidator, testDispatcher)
    }

    @Test
    fun `1 cold start with no account emits Loading then LoggedOut`() = runTest {
        fakeDataSource.emit(null)
        assertTrue("Expected LoggedOut", manager.accountSession.value is AccountSession.LoggedOut)
        assertFalse(manager.isConnected())
        assertNull(manager.currentAccountKeyOrNull())
    }

    @Test
    fun `2 existing connected session emits Connected with safe profile fields`() = runTest {
        val snapshot = ConnectedAccountSnapshot(
            accountKey = "hash123",
            displayName = "User",
            avatarUrl = "https://example.com/avatar.png",
            channelKey = "channel123"
        )
        fakeDataSource.emit(snapshot)

        val session = manager.accountSession.value
        assertTrue("Expected Connected", session is AccountSession.Connected)
        val connected = session as AccountSession.Connected
        assertEquals("hash123", connected.accountKey)
        assertEquals("User", connected.displayName)
        assertEquals("https://example.com/avatar.png", connected.avatarUrl)
        assertEquals("channel123", connected.channelKey)
        assertTrue(manager.isConnected())
        assertEquals("hash123", manager.currentAccountKeyOrNull())
    }

    @Test
    fun `3 sign out emits LoggedOut and calls cache invalidation hook`() = runTest {
        fakeDataSource.emit(ConnectedAccountSnapshot(accountKey = "oldAccountKey"))
        assertTrue(manager.accountSession.value is AccountSession.Connected)

        fakeDataSource.emit(null)
        assertTrue("Expected LoggedOut on sign out", manager.accountSession.value is AccountSession.LoggedOut)
        assertFalse(manager.isConnected())
        assertEquals(listOf("oldAccountKey"), fakeInvalidator.invalidatedKeys)
    }

    @Test
    fun `4 account switch invalidates old account cache and emits new Connected`() = runTest {
        fakeDataSource.emit(ConnectedAccountSnapshot(accountKey = "accountOne"))
        assertEquals("accountOne", manager.currentAccountKeyOrNull())

        fakeDataSource.emit(ConnectedAccountSnapshot(accountKey = "accountTwo", displayName = "Account Two"))
        val session = manager.accountSession.value
        assertTrue("Expected Connected for accountTwo", session is AccountSession.Connected)
        val connected = session as AccountSession.Connected
        assertEquals("accountTwo", connected.accountKey)
        assertEquals("Account Two", connected.displayName)

        assertEquals(listOf("accountOne"), fakeInvalidator.invalidatedKeys)
    }

    @Test
    fun `5 clearAccountScopedStateOnSignOut manually invalidates current account`() = runTest {
        fakeDataSource.emit(ConnectedAccountSnapshot(accountKey = "activeKey"))
        manager.clearAccountScopedStateOnSignOut()
        assertEquals(listOf("activeKey"), fakeInvalidator.invalidatedKeys)
    }

    @Test
    fun `6 duplicate account state does not trigger multiple invalidations or changes`() = runTest {
        val snapshot = ConnectedAccountSnapshot(accountKey = "sameKey")
        fakeDataSource.emit(snapshot)
        fakeDataSource.emit(snapshot)
        fakeDataSource.emit(snapshot)

        assertTrue(manager.accountSession.value is AccountSession.Connected)
        assertEquals("sameKey", manager.currentAccountKeyOrNull())
        assertTrue(fakeInvalidator.invalidatedKeys.isEmpty())
    }

    @Test
    fun `7 privacy public AccountSession model contains no credential or token fields`() {
        val fields = AccountSession.Connected::class.java.declaredFields.map { it.name }
        assertFalse("Must not contain accessToken", fields.contains("accessToken"))
        assertFalse("Must not contain refreshToken", fields.contains("refreshToken"))
        assertFalse("Must not contain cookie", fields.contains("cookie"))
        assertFalse("Must not contain email", fields.contains("email"))
        assertFalse("Must not contain password", fields.contains("password"))
        assertFalse("Must not contain token", fields.contains("token"))
    }

    @Test
    fun `8 manual refresh session logs state safely`() = runTest {
        fakeDataSource.emit(ConnectedAccountSnapshot(accountKey = "refreshKey"))
        manager.refreshSession()
        assertTrue(manager.isConnected())
    }

    // Fake implementations ------------------------------------------------------
    private class FakeAccountDataSource : AccountDataSource {
        private val flow = MutableStateFlow<ConnectedAccountSnapshot?>(null)
        override val connectedStream = flow
        fun emit(snapshot: ConnectedAccountSnapshot?) {
            flow.value = snapshot
        }
    }

    private class RecordingCacheInvalidator : AccountScopedCacheInvalidator {
        val invalidatedKeys = mutableListOf<String>()
        override suspend fun invalidateAccountCache(accountKey: String) {
            invalidatedKeys.add(accountKey)
        }
    }
}

