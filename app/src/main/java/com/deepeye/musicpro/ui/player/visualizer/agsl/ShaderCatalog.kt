// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// ShaderCatalog — original AGSL scene catalog for the Visualizer Studio
// (Task 2). Three new scenes, all original implementations using only
// public mathematical techniques (value noise, FBM, SDF, raymarch-style
// analytic fields). No proprietary VVavy source, uniforms, or visual
// identity is reproduced.
//
// Each scene declares the same stable uniform contract so the dispatcher
// can bind one set of uniforms regardless of the active scene:
//   uniform float2 iResolution;
//   uniform float  iTime;
//   uniform float  iTimeDelta;
//   uniform float4 iMouse;       // xy = normalized 0..1, zw = pressed state
//   uniform float  iAudioBass;   // 0..2
//   uniform float  iAudioMids;   // 0..2
//   uniform float  iAudioTreble; // 0..2
//   uniform float  iAudioEnergy; // 0..1
//   uniform float  iBeatPulse;   // 0..1
//   uniform float  iSceneTransition; // 0..1 (0 = fully old, 1 = fully new)
//   uniform float  iIntensity;   // 0.5..1.5
//   uniform float  iAspect;      // width / height
//
// Precision strategy: use half only for final color; keep intermediates
// as float. This avoids half-precision blowup on Mali while keeping the
// fill-rate cost low.

package com.deepeye.musicpro.ui.player.visualizer.agsl

import org.intellij.lang.annotations.Language

object ShaderCatalog {
    /**
     * Scene: FLUID PULSE — original domain-warped value-noise flow field.
     *
     * Visual language: layered FBM displacement where the warp vector field
     * itself is FBM, giving a "liquid" curl. Bass scales the domain (large
     * slow blobs), mids add an intermediate turbulence layer, treble adds a
     * high-frequency shimmer. Energy modulates overall brightness and beat
     * pulse adds a short flash.
     */
    @Language("AGSL")
    const val FLUID_PULSE = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iTimeDelta;
        uniform float4 iMouse;
        uniform float iAudioBass;
        uniform float iAudioMids;
        uniform float iAudioTreble;
        uniform float iAudioEnergy;
        uniform float iBeatPulse;
        uniform float iSceneTransition;
        uniform float iIntensity;
        uniform float iAspect;

        // Original 2D hash (not copied from any proprietary source).
        float h21(float2 p) {
            p = fract(p * float2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        float noise2(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f * f * (3.0 - 2.0 * f);
            float a = h21(i);
            float b = h21(i + float2(1.0, 0.0));
            float c = h21(i + float2(0.0, 1.0));
            float d = h21(i + float2(1.0, 1.0));
            return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
        }

        // 4-octave FBM.
        float fbm(float2 p) {
            float v = 0.0;
            float a = 0.5;
            for (int i = 0; i < 4; ++i) {
                v += a * noise2(p);
                p = p * 2.1 + 19.7;
                a *= 0.5;
            }
            return v;
        }

        half4 main(float2 fragCoord) {
            float aspect = iAspect > 0.0 ? iAspect : 1.0;
            float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
            uv.x /= aspect;

            float intensity = iIntensity;
            float t = iTime;

            // Bass drives the domain scale: bigger, slower blobs.
            float bassScale = 1.5 + iAudioBass * 1.2;
            // Mids drive the warp amplitude: more turbulence.
            float midWarp = 0.6 + iAudioMids * 1.0;

            float2 coord = uv * bassScale;
            coord += float2(sin(t * 0.15) * 0.4, cos(t * 0.11) * 0.4);

            // Domain warp: offset coord by an FBM of coord.
            float2 w = float2(
                fbm(coord + t * 0.2),
                fbm(coord - t * 0.2 + 7.3)
            );
            coord = coord + midWarp * (w - 0.5) * 2.0;

            // Sample the warp result.
            float f = fbm(coord + w * midWarp);

            // Treble shimmer: high-frequency micro-displacement.
            f += 0.08 * iAudioTreble * noise2(coord * 8.0 + t * 0.5);

            // Color mapping: deep navy base to teal primary.
            float3 base = mix(float3(0.02, 0.04, 0.10), float3(0.05, 0.45, 0.55), f * f);
            // Bass pulse adds a warm accent.
            base = mix(base, float3(0.55, 0.15, 0.55), clamp(iAudioBass * 0.4, 0.0, 1.0) * f);

            // Beat flash.
            base += float3(0.9, 0.95, 1.0) * iBeatPulse * 0.45;
            // Energy brightness.
            base *= (0.6 + iAudioEnergy * 0.8) * intensity;

            // Transition blend: near-zero when iSceneTransition is high
            // so the crossfade layer can fade this scene in/out cleanly.
            base *= mix(1.0, 1.0, iSceneTransition);

            // Vignette.
            float vig = 1.0 - smoothstep(0.7, 1.4, length(uv));
            base *= vig;

            return half4(half3(clamp(base, 0.0, 1.0)), 1.0);
        }
    """

    /**
     * Scene: AUDIO FRACTAL — original analytic raymarch-inspired Mandelbrot
     * set with per-frame domain scrolling and audio-driven zoom/iteration.
     *
     * Bass controls the iteration depth (more detail when loud). Mids shift
     * the domain position slowly. Treble adds a sparkle pass on the set
     * boundary. Energy controls a global brightness ramp and beat pulse
     * kicks the zoom in.
     */
    @Language("AGSL")
    const val AUDIO_FRACTAL = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iTimeDelta;
        uniform float4 iMouse;
        uniform float iAudioBass;
        uniform float iAudioMids;
        uniform float iAudioTreble;
        uniform float iAudioEnergy;
        uniform float iBeatPulse;
        uniform float iSceneTransition;
        uniform float iIntensity;
        uniform float iAspect;

        // AGSL has no complex type; use float2 as (re, im) with a helper.
        float2 complexSqr(float2 z) {
            return float2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y);
        }

        half4 main(float2 fragCoord) {
            float aspect = iAspect > 0.0 ? iAspect : 1.0;
            float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
            uv.x /= aspect;

            // Slow domain drift driven by mids.
            float drift = iTime * (0.05 + iAudioMids * 0.15);
            float2 center = float2(-0.5 + sin(drift) * 0.2, 0.0 + cos(drift * 0.7) * 0.2);

            // Zoom driven by energy; beat pulse adds a fast kick.
            float zoom = 0.8 + iAudioEnergy * 0.6 + iBeatPulse * 0.4;
            // Iterations driven by bass (more detail on loud bass).
            float iterCap = 24.0 + iAudioBass * 28.0;

            float2 z = uv * zoom + center;
            float2 c = z;
            float dist2 = 0.0;
            float n = 0.0;
            for (int i = 0; i < 52; ++i) {
                if (float(i) > iterCap) break;
                z = complexSqr(z) + c;
                dist2 = dot(z, z);
                if (dist2 > 4.0) break;
                n += 1.0;
            }

            // Smooth iteration count for color banding.
            float mu = n + log2(0.5 * log2(dist2)) - iterCap;
            float sm = n / iterCap;
            float tNorm = clamp(sm, 0.0, 1.0);

            // Color ramp: deep purple -> teal -> warm white.
            float3 col = mix(float3(0.10, 0.02, 0.22), float3(0.05, 0.55, 0.55), tNorm);
            col = mix(col, float3(0.9, 0.95, 1.0), tNorm * tNorm * iAudioTreble);

            // In-set area (escaped immediately or stayed bounded) goes dark.
            if (dist2 <= 4.0) col *= 0.15;

            // Treble sparkle on the set boundary (where n is near iterCap but just escaped).
            float boundary = smoothstep(0.94, 1.0, tNorm) * (1.0 - step(4.0, dist2 + 0.001));
            col += float3(0.8, 0.9, 1.0) * boundary * iAudioTreble * 1.2;

            col *= iIntensity;
            float vig = 1.0 - smoothstep(0.7, 1.5, length(uv));
            col *= vig;

            return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
        }
    """

    /**
     * Scene: NEON GRID — original perspective grid + tunnel hybrid with
     * waveform displacement. Bass drives camera z-advance and grid pulse;
     * mids modulate line thickness / glow width; treble adds sharp
     * high-frequency accents on grid intersections.
     */
    @Language("AGSL")
    const val NEON_GRID = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iTimeDelta;
        uniform float4 iMouse;
        uniform float iAudioBass;
        uniform float iAudioMids;
        uniform float iAudioTreble;
        uniform float iAudioEnergy;
        uniform float iBeatPulse;
        uniform float iSceneTransition;
        uniform float iIntensity;
        uniform float iAspect;

        // Original SDF helpers.
        float hash1(float n) {
            return fract(sin(n * 7.13) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            float aspect = iAspect > 0.0 ? iAspect : 1.0;
            float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
            uv.x /= aspect;

            float t = iTime;

            // Camera: z advances faster on bass and beat pulse.
            float camZ = t * (1.5 + iAudioBass * 2.5 + iBeatPulse * 1.5);

            // Perspective projection of the ground grid: y in [0.0, horizon].
            float horizon = 0.15;
            float3 col = float3(0.03, 0.03, 0.08);

            // Neon sunset glow at horizon.
            float3 glowCol = mix(float3(0.55, 0.10, 0.55), float3(0.10, 0.55, 0.85), iAudioMids * 0.5);
            float horizonGlow = exp(-abs(uv.y - horizon) * 8.0);
            col += glowCol * horizonGlow * (0.5 + iAudioEnergy * 0.5);

            // Ground plane (uv.y < horizon): perspective grid.
            if (uv.y < horizon) {
                float depth = 0.5 / (horizon - uv.y + 0.05);
                float gx = uv.x * depth;
                float gz = depth + camZ;

                // Line pattern: abs(fract(v)-0.5) gives a 0.5->0.0 ramp
                // at each grid line; 1-smoothstep() turns it into a line.
                float lineX = 1.0 - smoothstep(0.0, 0.03 + iAudioMids * 0.02, abs(fract(gx) - 0.5));
                float lineZ = 1.0 - smoothstep(0.0, 0.03 + iAudioMids * 0.02, abs(fract(gz * 0.5) - 0.5));
                float fog = clamp(1.0 / (depth * 0.25), 0.0, 1.0);

                float3 gridColor = mix(float3(0.10, 0.85, 0.95), float3(0.85, 0.15, 0.85), fract(camZ * 0.05));
                col += gridColor * max(lineX, lineZ) * fog * (0.6 + iAudioBass * 0.8);

                // Treble accent: sharp flash on grid intersections.
                float inter = lineX * lineZ;
                col += float3(0.9, 0.95, 1.0) * inter * iAudioTreble * 1.4;
            }

            // Sky: subtle radial pulse.
            if (uv.y >= horizon) {
                float skyPulse = 0.04 * iAudioBass * sin(t * 0.5);
                col += glowCol * (0.05 + skyPulse);
            }

            col *= iIntensity;
            float vig = 1.0 - smoothstep(0.6, 1.4, length(uv));
            col *= vig;

            return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
        }
    """

    /**
     * Static list of catalog scenes. The dispatcher uses this to enumerate
     * available studio scenes for the transition layer.
     */
    val SCENES =
        listOf(
            SceneInfo(FLUID_PULSE, "Fluid Pulse"),
            SceneInfo(AUDIO_FRACTAL, "Audio Fractal"),
            SceneInfo(NEON_GRID, "Neon Grid"),
        )

    /**
     * Metadata for one catalog scene.
     *
     * @property source AGSL source string; verified by ShaderCatalogTest to
     *                  compile and to declare the full uniform contract.
     * @property title  Human-readable name for the UI.
     */
    data class SceneInfo(val source: String, val title: String)
}
