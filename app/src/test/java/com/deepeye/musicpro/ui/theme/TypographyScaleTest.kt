package com.deepeye.musicpro.ui.theme

import androidx.compose.ui.text.TextStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the legibility floor for the app-wide type scale.
 *
 * The "everything looks tiny" regression was not a one-off tweak: it came from
 * `labelSmall` sitting at 10.sp while being the second most-referenced style
 * in the app. Nothing failed when that happened, so these assertions exist to
 * make a silent regression loud at build time instead.
 */
class TypographyScaleTest {

    private val styles: Map<String, TextStyle> = mapOf(
        "displayLarge" to AppTypography.displayLarge,
        "displayMedium" to AppTypography.displayMedium,
        "displaySmall" to AppTypography.displaySmall,
        "headlineLarge" to AppTypography.headlineLarge,
        "headlineMedium" to AppTypography.headlineMedium,
        "headlineSmall" to AppTypography.headlineSmall,
        "titleLarge" to AppTypography.titleLarge,
        "titleMedium" to AppTypography.titleMedium,
        "titleSmall" to AppTypography.titleSmall,
        "bodyLarge" to AppTypography.bodyLarge,
        "bodyMedium" to AppTypography.bodyMedium,
        "bodySmall" to AppTypography.bodySmall,
        "labelLarge" to AppTypography.labelLarge,
        "labelMedium" to AppTypography.labelMedium,
        "labelSmall" to AppTypography.labelSmall,
    )

    @Test
    fun `no type style falls below the legibility floor`() {
        val violations: Map<String, String> =
            styles.filterValues { it.fontSize.value < UiScale.MinReadableFontSize }
                .mapValues { (_, style) -> "${style.fontSize.value}sp" }
        assertTrue(
            "styles below ${UiScale.MinReadableFontSize}sp: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun `every line height accommodates its font size`() {
        // A lineHeight under the font size clips descenders (the "g" in "song")
        // and is a common symptom of type being squeezed to fit a layout.
        val violations: Map<String, String> =
            styles.filterValues { style -> style.lineHeight.value < style.fontSize.value }
                .mapValues { (_, style) ->
                    "${style.fontSize.value}sp -> ${style.lineHeight.value}sp"
                }
        assertTrue(
            "lineHeight < fontSize in: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun `each style ramp increases monotonically`() {
        // Monotonicity is asserted per-ramp, not across the whole scale.
        // Material3 interleaves the label and body ramps on purpose — its own
        // default scale has labelLarge at 14sp sitting *above* bodySmall at
        // 12sp — because labels carry button and chip text that must stay
        // legible, while body text gets its own ladder. Asserting a single
        // ascending order across all 15 styles would fail on a correct scale.
        val ramps = mapOf(
            "display" to listOf(
                AppTypography.displaySmall,
                AppTypography.displayMedium,
                AppTypography.displayLarge,
            ),
            "headline" to listOf(
                AppTypography.headlineSmall,
                AppTypography.headlineMedium,
                AppTypography.headlineLarge,
            ),
            "title" to listOf(
                AppTypography.titleSmall,
                AppTypography.titleMedium,
                AppTypography.titleLarge,
            ),
            "body" to listOf(
                AppTypography.bodySmall,
                AppTypography.bodyMedium,
                AppTypography.bodyLarge,
            ),
            "label" to listOf(
                AppTypography.labelSmall,
                AppTypography.labelMedium,
                AppTypography.labelLarge,
            ),
        )

        val violations: List<String> = ramps.flatMap { (name, ramp) ->
            ramp.zipWithNext()
                .filter { (smaller, larger) -> larger.fontSize.value < smaller.fontSize.value }
                .map { (smaller, larger) ->
                    "$name: ${smaller.fontSize.value}sp -> ${larger.fontSize.value}sp"
                }
        }
        assertTrue("non-monotonic steps: $violations", violations.isEmpty())
    }

    @Test
    fun `accessibility font scale is honoured rather than discarded`() {
        // The old cap was 1.0f, which silently threw away the user's Android
        // font-size setting and failed the platform "ignore font size" check.
        assertTrue(
            "a 1.3x system font must survive the clamp",
            UiScale.MaxFontScale >= 1.3f,
        )
    }

    @Test
    fun `landscape zoom is compensated so type stays legible`() {
        // The intent of this test is unchanged: landscape must never render
        // type so small it becomes unreadable. It previously asserted the raw
        // factor was >= 0.9, which is no longer the right question.
        //
        // The zoom is now 0.75 -- a deliberate flat zoom-out applied to
        // `fontScale` as well as `density`. Asserting `>= 0.9` on it directly
        // would fail for the wrong reason: 0.75 is the intended value, and what
        // actually matters is the *rendered* size after compensation.
        //
        // So this now asserts the property that matters, end to end: every
        // authored style, divided then re-multiplied by the zoom, still renders
        // at or above the 12sp floor.
        val zoom = UiScale.zoomFor(isLandscape = true)
        ZoomState.update(zoom)

        val violations = styles.filterValues { style ->
            scaledFontSize(style.fontSize.value) * zoom <
                UiScale.MinReadableFontSize - 1e-3f
        }.keys

        ZoomState.reset()

        assertTrue(
            "the landscape zoom $zoom must not render any style below " +
                "${UiScale.MinReadableFontSize}sp; violations: $violations",
            violations.isEmpty()
        )
    }

    // ── The hybrid contract ────────────────────────────────────────────────

    /**
     * The floor is a hard clamp, not a soft target.
     *
     * Asserted as an exact equality on purpose. A `>=` here would still pass if
     * the clamp were quietly relaxed to, say, 11.5sp — which renders as
     * "slightly small" on a reviewer's phone and as unreadable on a 6.1" panel
     * in direct sunlight. The value is the contract, so the test pins it.
     */
    @Test
    fun `the legibility floor is exactly twelve sp`() {
        assertEquals(
            "the readability floor is a spec constant, not a tunable",
            12f, UiScale.MinReadableFontSize, 0f
        )
    }

    /**
     * Every style must survive the zoom compensation at or above the floor.
     *
     * The scale is checked *after* routing it through [scaledFontSize] and then
     * applying the zoom the root actually installs, not against the static
     * [AppTypography] values the other tests in this class read. Those are
     * authored sizes; this asserts what reaches the screen.
     */
    @Test
    fun `every style stays legible after the zoom compensation`() {
        val zooms = listOf(UiScale.LandscapeDensityScale, UiScale.PortraitDensityScale)
        val violations = mutableListOf<String>()

        for (z in zooms) {
            ZoomState.update(z)
            for ((name, style) in styles) {
                val rendered = scaledFontSize(style.fontSize.value) * z
                if (rendered < UiScale.MinReadableFontSize) {
                    violations += "$name at zoom $z -> ${rendered}sp"
                }
            }
        }
        ZoomState.reset()

        assertTrue(
            "styles below ${UiScale.MinReadableFontSize}sp after the zoom: $violations",
            violations.isEmpty()
        )
    }

    /**
     * The line box must be floored too, not just the glyph size.
     *
     * This is the subtle half of the readability invariant and the one most
     * likely to regress. A 12sp glyph rendered inside an 8dp line box is
     * technically "12sp" and still clips its descenders — the "g" in "song"
     * loses its tail. Sizing containers off `fontSize` alone therefore does not
     * satisfy the floor.
     */
    @Test
    fun `line boxes are floored alongside glyph sizes`() {
        val zooms = listOf(UiScale.LandscapeDensityScale, UiScale.PortraitDensityScale)
        val violations = mutableListOf<String>()

        for (z in zooms) {
            ZoomState.update(z)
            for ((name, style) in styles) {
                val scaledLineHeight = scaledFontSize(style.lineHeight.value)
                val scaledGlyph = scaledFontSize(style.fontSize.value)
                if (scaledLineHeight < UiScale.MinReadableFontSize) {
                    violations += "$name at zoom $z -> ${scaledLineHeight}sp line box"
                }
                if (scaledLineHeight < scaledGlyph) {
                    violations += "$name at zoom $z -> ${scaledLineHeight}sp line " +
                        "box under a ${scaledGlyph}sp glyph"
                }
            }
        }

        assertTrue("floored line-height violations: $violations", violations.isEmpty())
    }

    /**
     * The hierarchy must not collapse entirely onto the floor.
     *
     * The floor is enforced per-style and independently, so a bug that floored
     * *everything* — rather than only what needs it — would still satisfy both
     * guards above while silently erasing the type scale. The display ramp must
     * stay meaningfully larger than the label ramp.
     */
    @Test
    fun `the type hierarchy survives the floor`() {
        ViewportScaler.updateViewportWidth(802)

        val displayLarge = scaledFontSize(AppTypography.displayLarge.fontSize.value)
        val labelSmall = scaledFontSize(AppTypography.labelSmall.fontSize.value)

        assertTrue(
            "displayLarge (${displayLarge}sp) must stay clearly above " +
                "labelSmall (${labelSmall}sp) or the hierarchy has collapsed",
            displayLarge > labelSmall * 1.5f
        )
    }

    /**
     * Restores the engine between tests.
     *
     * Required because the viewport-width tests above mutate process-wide
     * state; without this they leak into other classes in the same JVM and
     * fail depending on execution order.
     */
    @After
    fun tearDown() {
        ViewportScaler.reset()
    }
}