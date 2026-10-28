package com.deepeye.musicpro.ui.auth

import com.deepeye.musicpro.ui.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 5 mandatory test: proves the app exposes ZERO Google-account UI surfaces.
 *
 * - route surface: no Google sign-in / credential-picker / Firebase login routes;
 *   the only *primary* auth entry point is YouTubeLogin. (ChatAuth is deliberately
 *   retained as a typed-UNAVAILABLE feature entry — it renders FeatureUnavailableScreen,
 *   it is not a login route, so it is intentionally NOT asserted absent.)
 * - source surface: no live Google-account UI strings ("Sign in with Google",
 *   "Continue with Google", "Add account") and no Firebase login surface in the
 *   production Compose screen source. Historical changelog text is out of scope —
 *   it is documentation of removed features, not an account entry point.
 */
class NoGoogleAccountSurfaceTest {

    private val routeNames: Set<String> =
        Routes::class.nestedClasses.mapNotNull { it.simpleName }.toSet()

    @Test
    fun `no Google sign-in route exists`() {
        assertFalse("Routes.GoogleSignIn must not exist", "GoogleSignIn" in routeNames)
        assertFalse("Routes.CredentialPicker must not exist", "CredentialPicker" in routeNames)
        assertFalse("Routes.Login must not exist", "Login" in routeNames)
        assertFalse("Routes.FirebaseAuth must not exist", "FirebaseAuth" in routeNames)
        assertFalse("Routes.FirebaseLogin must not exist", "FirebaseLogin" in routeNames)
    }

    @Test
    fun `YouTubeLogin is the only primary login route`() {
        // Primary LOGIN entry points only (auth entry screens a user signs in through).
        val loginRoutes = routeNames.filter { it.equals("Login", ignoreCase = true) || it.contains("YouTubeLogin") }
        assertTrue("YouTubeLogin must remain", "YouTubeLogin" in loginRoutes)
        assertFalse("Duplicate Google Login route must be gone", "Login" in loginRoutes)
    }

    @Test
    fun `no Google account UI strings in live Compose screens`() {
        val badStrings = listOf(
            "Sign in with Google",
            "Continue with Google",
            "Sign up with Google",
            "Add Google account",
            "Sign in to your Google account"
        )
        val offenders = badStrings.flatMap { needle ->
            liveScreenKtFiles().filter { it.readText().contains(needle) }
                .map { "${it.name}:$needle" }
        }
        assertTrue(
            "Live screens contain Google-account UI strings: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun `no Firebase login surface in live Compose screens`() {
        val offenders = liveScreenKtFiles().filter {
            val text = it.readText()
            text.contains("FirebaseAuth") ||
                text.contains("signInWithGoogle") ||
                text.contains("signUpWithEmail") ||
                text.contains("signInWithEmail")
        }.map { it.name }
        assertTrue("Live screens contain a Firebase login surface: $offenders", offenders.isEmpty())
    }

    /** Live Compose screen sources (the account/login UI lives here), not historical changelog docs. */
    private fun liveScreenKtFiles(): List<File> {
        val root = File("src/main/java/com/deepeye/musicpro/ui").takeIf { it.exists() }
            ?: File("../src/main/java/com/deepeye/musicpro/ui")
            ?: File("app/src/main/java/com/deepeye/musicpro/ui")
        return root.walkTopDown().filter { it.extension == "kt" }.toList()
    }
}
