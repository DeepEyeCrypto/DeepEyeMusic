// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.visualizer

import com.deepeye.musicpro.ui.player.visualizer.agsl.AgslShaderEngine
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

        assertTrue(agsl.contains("half4 main(float2 fragCoord)"))
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
}
