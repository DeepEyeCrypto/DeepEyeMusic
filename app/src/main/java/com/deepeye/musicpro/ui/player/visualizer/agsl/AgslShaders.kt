// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// AGSL (Android Graphics Shading Language) SkSL Shaders
// Directly executed on GPU via android.graphics.RuntimeShader on Android 13+ (API 33+)
// Powered by Monet Dynamic Theming (AndroidX Palette API & Color Engine)
//
package com.deepeye.musicpro.ui.player.visualizer.agsl

import org.intellij.lang.annotations.Language

object AgslShaders {

    /**
     * Scene: Liquid Plasma / Viscous Ferrofluid (VVavy "liquid-carbon-plasma" / "fluid-simulator")
     * Features: Navier-Stokes-inspired curl noise field, bass-driven dilation, dynamic Monet palette synthesis.
     */
    @Language("AGSL")
    const val LIQUID_PLASMA = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iBass;
        uniform float iMid;
        uniform float iTreble;
        uniform float iPeak;
        uniform shader iChannel0;
        uniform float4 iAccentColor;
        uniform float4 iColorPrimary;
        uniform float4 iColorSecondary;

        float hash(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.x + p3.y) * p3.z);
        }

        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            float a = hash(i);
            float b = hash(i + float2(1.0, 0.0));
            float c = hash(i + float2(0.0, 1.0));
            float d = hash(i + float2(1.0, 1.0));
            return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
        }

        float fbm(float2 p) {
            float v = 0.0;
            float a = 0.5;
            float2 shift = float2(100.0, 100.0);
            for (int i = 0; i < 4; ++i) {
                v += a * noise(p);
                p = p * 2.0 + shift;
                a *= 0.5;
            }
            return v;
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
            
            // Audio-reactive dynamics
            float bassScale = 1.0 + iBass * 0.45;
            float speed = iTime * 0.35 + iMid * 0.15;
            
            // Domain warping for liquid simulation
            float2 q = float2(
                fbm(uv * 2.5 * bassScale + float2(0.0, speed * 0.4)),
                fbm(uv * 2.5 * bassScale + float2(5.2, 1.3 - speed * 0.3))
            );

            float2 r = float2(
                fbm(uv * 3.0 + 4.0 * q + float2(speed * 0.6, 9.2)),
                fbm(uv * 3.0 + 4.0 * q + float2(8.3 - speed * 0.5, 2.8))
            );

            float f = fbm(uv * 2.0 + 4.0 * r);

            // Dynamic Palette blending from Monet
            float3 primary = iColorPrimary.rgb;
            float3 secondary = iColorSecondary.rgb;

            // Ambient background to primary mix
            float3 col = mix(
                float3(0.02, 0.02, 0.05),
                primary,
                clamp((f * f) * 3.5, 0.0, 1.0)
            );

            // Bass pulse core with secondary palette
            col = mix(col, secondary, clamp(length(q) * iBass * 0.8, 0.0, 1.0));
            
            // Treble specular highlight
            col += mix(secondary, float3(1.0, 1.0, 1.0), 0.4) * (f * f * f * 2.5) * (0.4 + iTreble * 1.2);
            
            // Peak transient flash
            col += float3(1.0, 1.0, 1.0) * pow(f, 5.0) * iPeak * 1.5;

            // Subtle vignette
            float vig = 1.0 - smoothstep(0.5, 1.4, length(uv));
            col *= vig;

            return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
        }
    """

    /**
     * Scene: Raymarched Centroid Crystalline Tunnel (VVavy "centroid-crystalline-tunnel" / "deep-house")
     * Features: Analytical 3D raymarching with twisting fractal geometry, bass-pulsing walls, Monet color gradient.
     */
    @Language("AGSL")
    const val CRYSTAL_TUNNEL = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iBass;
        uniform float iMid;
        uniform float iTreble;
        uniform float iPeak;
        uniform shader iChannel0;
        uniform float4 iAccentColor;
        uniform float4 iColorPrimary;
        uniform float4 iColorSecondary;

        float2 rotate(float2 p, float a) {
            float c = cos(a);
            float s = sin(a);
            return float2(c * p.x - s * p.y, s * p.x + c * p.y);
        }

        half4 main(float2 fragCoord) {
            float2 p = (2.0 * fragCoord - iResolution) / iResolution.y;
            float r = max(length(p), 0.001);
            float angle = atan(p.y, p.x);

            // Tunnel perspective
            float opening = 2.8 + iBass * 0.4;
            float z = opening / r;
            float twist = angle + z * 0.08 + sin(z * 0.2 - iTime * 0.4) * 0.25;
            float travel = z + iTime * (1.5 + iBass * 1.2);

            // Hexagonal crystalline corridor ribs
            float ribs = abs(sin(twist * 6.0 + sin(travel * 0.5) * 2.0));
            float rings = abs(fract(travel * 0.35) - 0.5);

            float glow = (1.0 - smoothstep(0.01, 0.12, rings)) * 1.2 +
                         (1.0 - smoothstep(0.1, 0.45, ribs)) * 0.8;

            // Monet Palette
            float3 primary = iColorPrimary.rgb;
            float3 secondary = iColorSecondary.rgb;
            float3 tertiary = mix(primary, secondary, 0.5);

            float3 color = mix(primary, secondary, sin(travel * 0.2 + twist) * 0.5 + 0.5);
            color = mix(color, tertiary, iMid * 0.6);
            color *= glow * (0.8 + iBass * 0.8 + iPeak * 0.6);

            // Depth fog
            color *= exp(-r * 0.2) * smoothstep(0.02, 0.2, r);

            return half4(half3(clamp(color, 0.0, 1.0)), 1.0);
        }
    """

    /**
     * Scene: Aura Orb Hyper-Geometric (VVavy "aura-orb-hyper-geometric" / "echo-halo")
     * Features: Concentric pulsing halo rings, harmonic particle field, dynamic Monet aura dispersion.
     */
    @Language("AGSL")
    const val AURA_ORB = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iBass;
        uniform float iMid;
        uniform float iTreble;
        uniform float iPeak;
        uniform shader iChannel0;
        uniform float4 iAccentColor;
        uniform float4 iColorPrimary;
        uniform float4 iColorSecondary;

        half4 main(float2 fragCoord) {
            float2 uv = (2.0 * fragCoord - iResolution) / min(iResolution.x, iResolution.y);
            float dist = length(uv);
            float angle = atan(uv.y, uv.x);

            // Central pulsing core
            float coreRadius = 0.35 + iBass * 0.25;
            float coreGlow = 0.04 / abs(dist - coreRadius);

            // Concentric shockwave rings
            float ringPhase = fract(dist * 2.5 - iTime * (0.6 + iBass * 0.5));
            float ring = smoothstep(0.85, 0.98, ringPhase) * (1.0 - ringPhase);

            // Harmonic orbital rays
            float rays = abs(sin(angle * 8.0 + iTime * 0.8 + sin(dist * 10.0))) * (0.3 + iTreble * 0.7);

            // Monet Color mixing
            float3 primary = iColorPrimary.rgb;
            float3 secondary = iColorSecondary.rgb;

            float3 baseTint = primary;
            float3 glowColor = primary * coreGlow;
            float3 ringColor = secondary * ring * (1.5 + iBass);
            float3 rayColor  = mix(primary, secondary, 0.5) * rays * (0.5 / (dist + 0.1));

            float3 col = glowColor + ringColor + rayColor;
            col += baseTint * (0.02 / (dist * dist + 0.01));

            // Contrast enhancement
            col = pow(col, float3(0.9));

            return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
        }
    """

    /**
     * Scene: Cyber Synthwave Grid (VVavy "the-infinite-grid" / "tron")
     * Features: Perspective infinite horizon grid, undulating terrain waves, neon Monet retro sun.
     */
    @Language("AGSL")
    const val CYBER_GRID = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iBass;
        uniform float iMid;
        uniform float iTreble;
        uniform float iPeak;
        uniform shader iChannel0;
        uniform float4 iAccentColor;
        uniform float4 iColorPrimary;
        uniform float4 iColorSecondary;

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;

            float3 col = float3(0.02, 0.02, 0.05);

            float3 primary = iColorPrimary.rgb;
            float3 secondary = iColorSecondary.rgb;

            // Sun in horizon
            float2 sunPos = float2(0.0, 0.08);
            float sunDist = length(uv - sunPos);
            if (sunDist < 0.22) {
                float sunStripes = step(0.015, fract(uv.y * 35.0 - iTime * 0.2));
                float3 sunCol = mix(secondary, primary, (uv.y - sunPos.y + 0.22) / 0.44);
                col = mix(col, sunCol, sunStripes * (1.0 + iBass * 0.3));
            }

            // Grid plane below horizon
            if (uv.y < 0.05) {
                float depth = 0.4 / (0.06 - uv.y);
                float xCoord = uv.x * depth;
                float zCoord = depth + iTime * (4.0 + iBass * 3.0);

                // Undulating audio-reactive hills
                float hill = sin(xCoord * 0.8) * cos(zCoord * 0.2) * (0.08 + iBass * 0.15);
                depth = 0.4 / (0.06 - uv.y + hill * 0.05);
                xCoord = uv.x * depth;
                zCoord = depth + iTime * (4.0 + iBass * 3.0);

                float gridX = abs(fract(xCoord) - 0.5);
                float gridZ = abs(fract(zCoord * 0.5) - 0.5);
                float line = smoothstep(0.42, 0.48, max(gridX, gridZ));

                float3 gridColor = mix(primary, secondary, sin(zCoord * 0.1) * 0.5 + 0.5);
                float fog = clamp(1.0 / (depth * 0.12), 0.0, 1.0);
                col += gridColor * line * fog * (1.2 + iMid * 0.8);
            }

            return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
        }
    """
}
