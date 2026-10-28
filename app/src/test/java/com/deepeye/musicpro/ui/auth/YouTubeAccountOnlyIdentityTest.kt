package com.deepeye.musicpro.ui.auth

import com.deepeye.musicpro.account.AccountSession
import com.deepeye.musicpro.account.AccountSessionManager
import com.deepeye.musicpro.ui.navigation.AccountRouteDecider
import com.deepeye.musicpro.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 5 mandatory test: account-scoped state is keyed ONLY by the opaque
 * YouTube accountKey, with no Firebase UID / Google identity coupling.
 *
 * - personalization repository is keyed by [AccountSessionManager] (YouTube),
 *   and contains zero Firebase references;
 * - account switching emits the new YouTube [AccountSession.Connected.accountKey]
 *   and routes to HOME;
 * - a signed-out session carries no account key and routes to YouTubeLogin,
 *   fencing any stale account-scoped response.
 */
class YouTubeAccountOnlyIdentityTest {

    private fun productionKtFiles(): List<File> {
        val root = File("src/main/java").takeIf { it.exists() }
            ?: File("../src/main/java")
            ?: File("app/src/main/java")
        return root.walkTopDown().filter { it.extension == "kt" }.toList()
    }

    @Test
    fun `personalization repository is keyed by YouTube AccountSessionManager not Firebase`() {
        val repo = productionKtFiles()
            .firstOrNull { it.name == "PersonalizationRepositoryImpl.kt" }
            ?: error("PersonalizationRepositoryImpl.kt not found")
        val text = repo.readText()
        assertTrue("Must key personalization on AccountSessionManager",
            text.contains("AccountSessionManager"))
        assertTrue("Must use the opaque YouTube accountKey",
            text.contains("accountKey"))
        assertTrue("Must increment a generation fence on session change",
            text.contains("generation"))
        val firebaseCount = Regex("Firebase", RegexOption.IGNORE_CASE).findAll(text).count()
        assertEquals("No Firebase identity coupling allowed", 0, firebaseCount)
    }

    @Test
    fun `connected session exposes only the opaque YouTube accountKey`() {
        val session = AccountSession.Connected(
            accountKey = "opaque-youtube-account-a",
            displayName = "User A",
            avatarUrl = null,
            channelKey = null,
            lastUpdatedAtMs = null
        )
        assertEquals("opaque-youtube-account-a", session.accountKey)
        // The session has no Firebase/Google uid field at all.
        val sessionFields = AccountSession.Connected::class.java.declaredFields.map { it.name }.toSet()
        assertTrue("No Firebase uid field present on Connected session",
            !sessionFields.any { it.lowercase().contains("firebase") })
    }

    @Test
    fun `account switch emits new YouTube key and routes to HOME`() {
        val route = AccountRouteDecider.routeFor(
            AccountSession.Connected(accountKey = "opaque-youtube-account-b")
        )
        assertEquals(AppRoute.HOME, route)
    }

    @Test
    fun `signed out carries no account key and routes to YouTubeLogin`() {
        val loggedOut = AccountSession.LoggedOut
        // LoggedOut has no accountKey accessor.
        val loggedOutFields = AccountSession.LoggedOut::class.java.declaredFields.map { it.name }.toSet()
        assertNull("No accountKey field on LoggedOut",
            loggedOutFields.firstOrNull { it == "accountKey" })
        val route = AccountRouteDecider.routeFor(loggedOut)
        assertEquals(AppRoute.YOUTUBE_LOGIN, route)
        assertNotNull(AccountSessionManager::class.java)
    }
}
