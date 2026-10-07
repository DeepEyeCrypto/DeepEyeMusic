// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.visualizer

import com.deepeye.musicpro.ui.player.visualizer.agsl.AgslShaderEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgslShaderEngineTest {

    @Test
    fun transpileGlslToAgsl_translatesMainImageSignature() {
        val glsl = """
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                vec2 uv = fragCoord.xy / iResolution.xy;
                fragColor = vec4(uv.x, uv.y, 0.0, 1.0);
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        // Should use fragCoordInput as parameter name to avoid shadowing
        assertTrue(agsl.contains("half4 main(float2 fragCoordInput)"))
        // Y-axis inversion should be injected
        assertTrue(agsl.contains("float2 correctCoord = float2(fragCoordInput.x, iResolution.y - fragCoordInput.y);"))
        // Original variable should still work
        assertTrue(agsl.contains("float2 fragCoord = correctCoord;"))
        assertTrue(agsl.contains("float2 uv = fragCoord.xy / iResolution.xy;"))
        assertTrue(agsl.contains("return half4(fragColor);"))
        assertTrue(agsl.contains("uniform shader iChannel0;"))
    }

    @Test
    fun transpileGlslToAgsl_translatesTextureLookupsToShaderEval() {
        val glsl = """
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                vec2 uv = fragCoord.xy / iResolution.xy;
                vec4 audio = texture(iChannel0, vec2(uv.x, 0.0));
                fragColor = audio;
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        assertTrue(agsl.contains("iChannel0.eval"))
        assertFalse(agsl.contains("texture(iChannel0"))
    }

    @Test
    fun transpileGlslToAgsl_stripsPrecisionSpecifiers() {
        val glsl = """
            precision highp float;
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                fragColor = vec4(1.0);
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        assertFalse(agsl.contains("precision highp float;"))
    }

    @Test
    fun transpileGlslToAgsl_injectsModPolyfill() {
        val glsl = """
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                fragColor = vec4(1.0);
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        assertTrue("mod() polyfill should be injected", agsl.contains("float mod(float x, float y)"))
        assertTrue("float2 mod() polyfill should be injected", agsl.contains("float2 mod(float2 x, float y)"))
    }

    @Test
    fun transpileGlslToAgsl_injectsYAxisInversion() {
        val glsl = """
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                fragColor = vec4(1.0);
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        assertTrue("Y-axis inversion should be injected", agsl.contains("correctCoord"))
        assertTrue("iResolution.y subtraction for Y inversion", agsl.contains("iResolution.y - fragCoordInput.y"))
    }

    @Test
    fun transpileGlslToAgsl_doesNotDuplicateFixes() {
        // If shader already has mod polyfill, don't duplicate
        val glsl = """
            float mod(float x, float y) { return x - y * floor(x / y); }
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                fragColor = vec4(1.0);
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        // Should not duplicate the mod polyfill - count occurrences
        val count = agsl.split("float mod(float x, float y)").size - 1
        assertEquals(1, count)
    }

    @Test
    fun transpileGlslToAgsl_preservesExistingUniforms() {
        // Shader that already has uniforms should not get duplicates
        val glsl = """
            uniform float2 iResolution;
            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                fragColor = vec4(1.0);
            }
        """.trimIndent()

        val agsl = AgslShaderEngine.transpileGlslToAgsl(glsl)

        // Should have exactly one iResolution uniform declaration
        val uniformCount = agsl.split("uniform float2 iResolution").size - 1
        assertEquals(1, uniformCount)
    }
}
