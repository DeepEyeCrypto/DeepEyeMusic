package com.deepeye.musicpro.domain.util

object LanguageUtils {
    val allKnownLanguages = listOf(
        "hindi", "english", "punjabi", "bhojpuri", 
        "tamil", "telugu", "haryanvi", "bengali", 
        "malayalam", "kannada", "marathi", "gujarati"
    )

    /**
     * Generates negative query constraints (e.g. "-bhojpuri -english") to pass to YouTube 
     * search API, preventing unselected regional languages from bleeding into personalized feeds.
     */
    fun buildNegativeLanguageConstraints(preferredLangs: Set<String>): String {
        if (preferredLangs.isEmpty()) return ""
        
        val nonPreferred = allKnownLanguages.filter { lang -> 
            preferredLangs.none { it.equals(lang, ignoreCase = true) } 
        }
        return nonPreferred.joinToString(" ") { "-$it" }
    }

    /**
     * Checks if a detected language string (e.g. "Bhojpuri") is explicitly NOT in the user's preferred languages.
     */
    fun isLanguageBlocked(language: String?, preferredLangs: Set<String>): Boolean {
        if (language == null || preferredLangs.isEmpty()) return false
        
        // If it's one of the known languages but NOT in preferred, it's blocked.
        val lowerLang = language.lowercase()
        val isKnown = allKnownLanguages.any { lowerLang.contains(it) }
        if (isKnown) {
            val isPreferred = preferredLangs.any { lowerLang.contains(it.lowercase()) }
            return !isPreferred
        }
        return false
    }
}
