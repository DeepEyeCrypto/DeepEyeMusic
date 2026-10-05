// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.player.visualizer.agsl

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * AgslShaderEngine — Automated GLSL-to-AGSL Transpiler and RuntimeShader Compiler.
 *
 * Automatically converts WebGL/Shadertoy/VVavy GLSL source strings into valid
 * SkSL/AGSL shaders for Android 13+ (API 33+) GPU execution.
 */
object AgslShaderEngine {
    private const val TAG = "AgslShaderEngine"

    const val AGSL_STANDARD_UNIFORMS = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iTimeDelta;
        uniform float4 iMouse;
        uniform shader iChannel0;
        uniform float4 iColorPrimary;
        uniform float4 iColorSecondary;
        uniform float iBass;
        uniform float iMid;
        uniform float iTreble;
        uniform float iPeak;
    """

    /**
     * Transpiles standard GLSL ES 3.0 / WebGL Shadertoy source code to AGSL (SkSL).
     */
    fun transpileGlslToAgsl(rawGlsl: String): String {
        var source = rawGlsl

        // 1. Strip precision specifiers
        source = source.replace(Regex("""precision\s+(highp|mediump|lowp)\s+float\s*;"""), "")
        source = source.replace(Regex("""#version\s+[^\n]+"""), "")

        // 2. Transpile vector and matrix types
        source = source.replace(Regex("""\bvec2\b"""), "float2")
        source = source.replace(Regex("""\bvec3\b"""), "float3")
        source = source.replace(Regex("""\bvec4\b"""), "float4")
        source = source.replace(Regex("""\bmat2\b"""), "float2x2")
        source = source.replace(Regex("""\bmat3\b"""), "float3x3")
        source = source.replace(Regex("""\bmat4\b"""), "float4x4")
        source = source.replace(Regex("""\bivec2\b"""), "int2")
        source = source.replace(Regex("""\bivec3\b"""), "int3 intrinsics")
        source = source.replace(Regex("""\bivec4\b"""), "int4")
        source = source.replace(Regex("""\bbvec2\b"""), "bool2")
        source = source.replace(Regex("""\bbvec3\b"""), "bool3")
        source = source.replace(Regex("""\bbvec4\b"""), "bool4")

        // 3. Transpile texture lookups to shader.eval()
        // texture(iChannel0, uv) -> iChannel0.eval((uv) * float2(256.0, 1.0))
        source = source.replace(
            Regex("""texture\s*\(\s*iChannel0\s*,\s*([^)]+)\)"""),
            "iChannel0.eval(($1) * float2(256.0, 1.0))"
        )
        source = source.replace(
            Regex("""texture2D\s*\(\s*iChannel0\s*,\s*([^)]+)\)"""),
            "iChannel0.eval(($1) * float2(256.0, 1.0))"
        )

        // 4. Transpile mainImage signature: void mainImage(out vec4 fragColor, in vec2 fragCoord)
        val mainImageRegex = Regex("""void\s+mainImage\s*\(\s*out\s+float4\s+(\w+)\s*,\s*in\s+float2\s+(\w+)\s*\)\s*\{""")
        val match = mainImageRegex.find(source)
        if (match != null) {
            val fragColorVar = match.groupValues[1]
            val fragCoordVar = match.groupValues[2]

            source = source.replace(match.value, "half4 main(float2 $fragCoordVar) {\n        float4 $fragColorVar = float4(0.0);")
            
            // Replace trailing closing brace with return of fragColor if not already returning
            val lastBrace = source.lastIndexOf('}')
            if (lastBrace != -1) {
                val prefix = source.substring(0, lastBrace)
                source = "$prefix\n        return half4($fragColorVar);\n}"
            }
        }

        // 5. Prepend standard uniforms if not already present
        if (!source.contains("uniform float2 iResolution")) {
            source = "$AGSL_STANDARD_UNIFORMS\n$source"
        }

        return source.trimIndent()
    }

    /**
     * Compiles a GLSL/AGSL source string into an Android [RuntimeShader] on API 33+.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun compileShader(rawGlsl: String): Result<RuntimeShader> {
        return runCatching {
            val agslSource = if (rawGlsl.contains("half4 main(float2")) {
                if (!rawGlsl.contains("uniform float2 iResolution")) {
                    "$AGSL_STANDARD_UNIFORMS\n$rawGlsl"
                } else {
                    rawGlsl
                }
            } else {
                transpileGlslToAgsl(rawGlsl)
            }
            RuntimeShader(agslSource)
        }.onFailure { err ->
            Log.e(TAG, "[AgslShaderEngine] Shader compilation failed: ${err.message}", err)
        }
    }
}
