package com.deepeye.musicpro.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteDecoderTest {

    @Test
    fun `html entity 10 becomes newline`() {
        val raw = "line1&#10;line2"
        val out = MarkdownLiteDecoder.decode(raw)
        assertEquals("line1\nline2", out)
    }

    @Test
    fun `html entity amp becomes ampersand`() {
        val out = MarkdownLiteDecoder.decode("a &amp; b")
        assertEquals("a & b", out)
    }

    @Test
    fun `html entity gt lt quot`() {
        val out = MarkdownLiteDecoder.decode("&lt;tag&gt; &quot;x&quot; &#39;y&#39;")
        assertEquals("<tag> \"x\" 'y'", out)
    }

    @Test
    fun `bold markers stripped`() {
        val out = MarkdownLiteDecoder.decode("**bold** and **more**")
        assertEquals("bold and more", out)
    }

    @Test
    fun `italic markers stripped`() {
        val out = MarkdownLiteDecoder.decode("*italic*")
        assertEquals("italic", out)
    }

    @Test
    fun `underscore bold stripped`() {
        val out = MarkdownLiteDecoder.decode("__bold__")
        assertEquals("bold", out)
    }

    @Test
    fun `inline code backticks stripped`() {
        val out = MarkdownLiteDecoder.decode("some `code` here")
        assertEquals("some code here", out)
    }

    @Test
    fun `heading hashes stripped`() {
        val out = MarkdownLiteDecoder.decode("## Header\n# Title")
        assertEquals("Header\nTitle", out)
    }

    @Test
    fun `bullet markers normalised to two-space dash`() {
        val out = MarkdownLiteDecoder.decode("- item\n* other\n+ third")
        assertEquals("  - item\n  - other\n  - third", out)
    }

    @Test
    fun `consecutive blank lines collapsed to one`() {
        val out = MarkdownLiteDecoder.decode("a\n\n\n\nb")
        assertEquals("a\n\nb", out)
    }

    @Test
    fun `duplicate Full Changelog lines are deduped`() {
        val raw = "stuff\n\n**Full Changelog**: x\n**Full Changelog**: x\n**Full Changelog**: x"
        val out = MarkdownLiteDecoder.decode(raw)
        val occurrences = out.lines().count { it.startsWith("Full Changelog: x") }
        assertEquals(1, occurrences)
    }

    @Test
    fun `duplicate adjacent non-blank lines deduped`() {
        val out = MarkdownLiteDecoder.decode("one\ntwo\ntwo\nthree")
        val occurrences = out.lines().count { it == "two" }
        assertEquals(1, occurrences)
    }

    @Test
    fun `blank lines are not collapsed away entirely`() {
        val out = MarkdownLiteDecoder.decode("a\n\n\n\nb")
        assertTrue(out.contains("a\n\nb"))
    }

    @Test
    fun `end-to-end GitHub-style release body decodes cleanly`() {
        val raw = "**What's new**&#10;&#10;- **Item A**&#10;  - sub&#10;&#10;**Full Changelog**: https://x/v1...v2&#10;&#10;**Full Changelog**: https://x/v0...v2&#10;&#10;**Full Changelog**: https://x/v1...v3"
        val out = MarkdownLiteDecoder.decode(raw)
        assertFalse(out.contains("**"))
        assertFalse(out.contains("&#10;"))
        val occurrences = out.lines().count { it.startsWith("Full Changelog: ") }
        assertEquals(1, occurrences)
    }

    @Test
    fun `horizontal rule lines stripped`() {
        val out = MarkdownLiteDecoder.decode("a\n---\nb")
        assertFalse(out.lines().any { it.trim() == "---" })
        assertTrue(out.contains("a"))
        assertTrue(out.contains("b"))
    }

    @Test
    fun `blank input returns blank`() {
        assertEquals("", MarkdownLiteDecoder.decode(""))
        assertEquals("", MarkdownLiteDecoder.decode("   "))
    }
}
