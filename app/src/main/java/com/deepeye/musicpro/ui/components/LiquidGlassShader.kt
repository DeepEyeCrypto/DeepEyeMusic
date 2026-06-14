// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.components

import org.intellij.lang.annotations.Language

object LiquidGlassShader {
    @Language("AGSL")
    const val ShaderCode = """
        uniform shader content;
        uniform float2 size;
        uniform float2 tilt;
        uniform float2 touch;
        uniform float refractionHeight;
        uniform float chromaticAberration;
        uniform float specularAlpha;
        uniform float noiseFactor;

        float random(float2 co) {
            return frac(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            // 1. Normalized coordinates
            float2 uv = fragCoord / size;
            
            // 2. Refraction & Chromatic Aberration Displacement
            float2 center = float2(0.5, 0.5);
            float2 toCenter = uv - center;
            float dist = length(toCenter);
            
            // Lens deformation vector (simulates lens shape)
            float2 refractOffset = toCenter * dist * refractionHeight * 0.05;
            
            // Chromatic Aberration: different offsets for Red, Green, Blue
            float2 offsetR = refractOffset * (1.0 + chromaticAberration);
            float2 offsetG = refractOffset;
            float2 offsetB = refractOffset * (1.0 - chromaticAberration);
            
            // Sample channels separately
            half4 colorR = content.eval(fragCoord + offsetR * size);
            half4 colorG = content.eval(fragCoord + offsetG * size);
            half4 colorB = content.eval(fragCoord + offsetB * size);
            
            half4 color = half4(colorR.r, colorG.g, colorB.b, (colorR.a + colorG.a + colorB.a) / 3.0);
            
            // 3. Specular Highlight (Gyroscope Driven)
            float2 highlightPos = (center + tilt * 0.3) * size;
            float highlightDist = length(fragCoord - highlightPos);
            float specularGlow = smoothstep(size.x * 0.4, 0.0, highlightDist);
            half4 specular = half4(1.0, 1.0, 1.0, 1.0) * specularGlow * specularAlpha;
            
            color += specular;
            
            // 4. Noise Grain
            float noise = random(fragCoord + tilt);
            color.rgb += (noise - 0.5) * noiseFactor;
            
            return color;
        }
    """
}
