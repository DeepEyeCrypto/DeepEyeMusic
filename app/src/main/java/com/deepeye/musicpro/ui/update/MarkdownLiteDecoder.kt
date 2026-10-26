package com.deepeye.musicpro.ui.update

/**
 * No-dep Markdown-lite decoder for GitHub release-body text rendered in a plain
 * Compose Text (without a rich Markdown composition library). Handles the subset
 * that the release-notes payload actually uses:
 *   - HTML entities like &#10; &amp; &lt; &gt; &quot; &#39;
 *   - **bold** / *italic* / __bold__ / `inline code`
 *   - # heading markers
 *   - - / * / + bullet markers (normalised to "  - ")
 *   - --- / *** / ___ horizontal rules on their own line
 *   - Duplicate adjacent non-blank lines (e.g. repeated "Full Changelog" URLs)
 *   - Multiple blank lines collapsed to one
 *
 * This is intentionally minimal: it preserves readability while removing
 * formatting noise so the text fits cleanly inside a single Compose Text.
 */
object MarkdownLiteDecoder {

    fun decode(markdown: String): String {
        var s = markdown
            .replace("&#10;", "\n")
            .replace("&#xD;", "\n")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

        // Strip trailing reference links like [1]: https://example.com
        s = s.replace("(?m)^\\[\\d+\\]:\\s+.*$".toRegex(), "")

        // Formatting markers — strip the markers, keep the text content.
        s = s.replace("\\*\\*(.+?)\\*\\*".toRegex(), "$1")
        s = s.replace("\\*([^\\n]+?)\\*".toRegex(), "$1")
        s = s.replace("__(.+?)__".toRegex(), "$1")
        s = s.replace("`([^`]+?)`".toRegex(), "$1")

        // Remove heading markers (# ... ####), but keep the heading text.
        s = s.replace("(?m)^(#{1,6}\\s+)".toRegex(), "")

        // Horizontal rules on their own line become blank.
        s = s.replace("(?m)^[ \\t]*(---|\\*\\*\\*|___)[ \\t]*$".toRegex(), "")

        // Normalise bullet markers at line start to a consistent "  - ".
        s = s.replace("(?m)^\\s*[-*+]\\s+".toRegex(), "  - ")

        // Collapse three or more consecutive blank lines down to two (one blank line).
        s = s.replace("(?m)\\n{3,}".toRegex(), "\n\n")

        // Deduplicate repeated adjacent lines and repeated Full Changelog entries.
        val lines = s.lines()
        val deduped = mutableListOf<String>()
        var changelogSeen = false
        var previousNonBlank: String? = null
        for (line in lines) {
            if (line.isBlank()) {
                deduped.add(line)
                continue
            }
            if (line == previousNonBlank) {
                continue
            }
            if (line.startsWith("Full Changelog:")) {
                if (changelogSeen) continue
                changelogSeen = true
            }
            deduped.add(line)
            previousNonBlank = line
        }
        s = deduped.joinToString("\n")

        return s.trimEnd()
    }
}
