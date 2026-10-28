package com.deepeye.musicpro.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Phase 5 mandatory test (structural): the Settings screen exposes a
 * YouTube-only account surface — YouTube status / YouTube sign-out / Ghost
 * mode — and contains NO Google account entry and NO Firebase account UI.
 *
 * Verified at the source level (no Hilt/Compose harness needed) because the
 * screen's account surface is a fixed set of Compose strings.
 */
class SettingsAccountTest {

    private fun settingsScreenSource(): String {
        val root = File("src/main/java").takeIf { it.exists() }
            ?: File("../src/main/java")
            ?: File("app/src/main/java")
        val file = root.walkTopDown().firstOrNull { it.name == "SettingsScreen.kt" }
            ?: error("SettingsScreen.kt not found")
        return file.readText()
    }

    @Test
    fun `settings shows YouTube account status and sign out`() {
        val source = settingsScreenSource()
        assertTrue("YouTube status chip must be present", source.contains("YouTube Active"))
        assertTrue("YouTube account section must be present", source.contains("YouTube Account"))
        assertTrue("YouTube disconnect action must be present", source.contains("Disconnect YouTube Account"))
    }

    @Test
    fun `settings contains no Google account entry`() {
        val source = settingsScreenSource()
        assertFalse("No 'Sign in with Google' button", source.contains("Sign in with Google"))
        assertFalse("No 'Continue with Google' button", source.contains("Continue with Google"))
        assertFalse("No 'Add account' Google entry", source.contains("Add account"))
        // The word "Google" must not appear as a user-facing account label.
        assertFalse("No 'Google account' label", source.contains("Google account"))
    }

    @Test
    fun `settings contains no Firebase account surface`() {
        val source = settingsScreenSource()
        assertFalse("No Firebase login surface", source.contains("Firebase"))
        assertFalse("No Firebase account status", source.contains("FirebaseAuth"))
    }
}
