// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later
//
// VvavyTopology — Procedural Multi-Topology Generative Geometry Engine
// Zero-allocation pre-allocated geometry pool for 60/120 FPS audio-reactive visualizers.
//
package com.deepeye.musicpro.ui.player.visualizer.vvavy

import kotlin.math.*
import kotlin.random.Random

enum class VvavyTopologyMode {
    SIERPINSKI_FRACTAL,   // Classic recursive 4-way fractal quadtree
    SACRED_MERKABA,       // Interlocking dual-triangle star with radial expansion
    POLYHEDRON_3D,        // 3D wireframe octahedron with depth projection
    ORBITAL_VORTEX,       // Concentric spiraling logarithmic triangles
    DECONSTRUCTED_LATTICE // Displaced floating vertices reforming on transients
}

data class VvavyVertex(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var targetX: Float = 0f,
    var targetY: Float = 0f,
    var targetZ: Float = 0f
) {
    fun set(nx: Float, ny: Float, nz: Float = 0f) {
        x = nx
        y = ny
        z = nz
        targetX = nx
        targetY = ny
        targetZ = nz
    }

    fun setTarget(tx: Float, ty: Float, tz: Float = 0f) {
        targetX = tx
        targetY = ty
        targetZ = tz
    }

    fun morph(speed: Float = 0.15f) {
        x += (targetX - x) * speed
        y += (targetY - y) * speed
        z += (targetZ - z) * speed
    }
}

data class VvavyTriangle(
    val a: VvavyVertex = VvavyVertex(),
    val b: VvavyVertex = VvavyVertex(),
    val c: VvavyVertex = VvavyVertex(),
    var depth: Int = 0,
    var energyBin: Int = 0,
    var alpha: Float = 1f,
    var kickPhase: Float = 0f,
    var kickVx: Float = 0f,
    var kickVy: Float = 0f,
    var driftAngle: Float = 0f,
    var driftSpeed: Float = 0f
)

/**
 * High-performance, zero-garbage generative geometry engine.
 * Pre-allocates up to [MAX_TRIS] triangles and projects them directly to 2D screen coordinates.
 */
class VvavyGeometryEngine(
    private val maxTriangles: Int = MAX_TRIS
) {
    companion object {
        const val MAX_TRIS = 512
        const val MAX_DEPTH = 5
        private const val KICK_DECAY = 0.88f
    }

    val pool = Array(maxTriangles) { VvavyTriangle() }
    var count = 0
        private set

    val projX = FloatArray(maxTriangles * 3)
    val projY = FloatArray(maxTriangles * 3)
    val projZ = FloatArray(maxTriangles * 3)

    var currentMode = VvavyTopologyMode.SIERPINSKI_FRACTAL
    private var randomSeed = 1337L
    private val rng = Random(randomSeed)

    /**
     * Mutates the random seed and selects the next topology mode dynamically.
     */
    fun mutateTopology(newMode: VvavyTopologyMode? = null) {
        randomSeed = rng.nextLong()
        currentMode = newMode ?: when (currentMode) {
            VvavyTopologyMode.SIERPINSKI_FRACTAL   -> VvavyTopologyMode.SACRED_MERKABA
            VvavyTopologyMode.SACRED_MERKABA       -> VvavyTopologyMode.POLYHEDRON_3D
            VvavyTopologyMode.POLYHEDRON_3D        -> VvavyTopologyMode.ORBITAL_VORTEX
            VvavyTopologyMode.ORBITAL_VORTEX       -> VvavyTopologyMode.DECONSTRUCTED_LATTICE
            VvavyTopologyMode.DECONSTRUCTED_LATTICE -> VvavyTopologyMode.SIERPINSKI_FRACTAL
        }
    }

    /**
     * Rebuilds the geometry for the current frame based on active topology mode and audio metrics.
     */
    fun build(
        cx: Float,
        cy: Float,
        radius: Float,
        bass: Float,
        mids: Float,
        treble: Float,
        spectrum: FloatArray,
        time: Float,
        dropActive: Boolean,
        dt: Float
    ) {
        count = 0
        val effectiveR = radius * (1f + bass * 0.45f)
        val subdivisionDepth = (1 + (mids * 3.5f).toInt()).coerceIn(1, MAX_DEPTH)

        when (currentMode) {
            VvavyTopologyMode.SIERPINSKI_FRACTAL -> {
                buildSierpinski(cx, cy, effectiveR, subdivisionDepth, time, spectrum, bass)
            }
            VvavyTopologyMode.SACRED_MERKABA -> {
                buildMerkaba(cx, cy, effectiveR, subdivisionDepth, time, spectrum, bass, mids)
            }
            VvavyTopologyMode.POLYHEDRON_3D -> {
                buildPolyhedron3D(cx, cy, effectiveR, time, spectrum, bass, mids, treble)
            }
            VvavyTopologyMode.ORBITAL_VORTEX -> {
                buildOrbitalVortex(cx, cy, effectiveR, time, spectrum, bass, mids)
            }
            VvavyTopologyMode.DECONSTRUCTED_LATTICE -> {
                buildDeconstructedLattice(cx, cy, effectiveR, time, spectrum, bass, mids, treble)
            }
        }

        // Apply dynamic kinetics (drift, beat kick, and audio vertex displacement)
        for (i in 0 until count) {
            val t = pool[i]

            // Drift displacement
            val driftDx = cos(t.driftAngle) * t.driftSpeed * bass * dt
            val driftDy = sin(t.driftAngle) * t.driftSpeed * bass * dt
            t.a.x += driftDx; t.a.y += driftDy
            t.b.x += driftDx; t.b.y += driftDy
            t.c.x += driftDx; t.c.y += driftDy

            // Beat kick impulse decay
            if (t.kickPhase > 0.01f) {
                val kx = t.kickVx * t.kickPhase
                val ky = t.kickVy * t.kickPhase
                t.a.x += kx; t.a.y += ky
                t.b.x += kx; t.b.y += ky
                t.c.x += kx; t.c.y += ky
                t.kickPhase *= KICK_DECAY
            }

            // On a detected bass drop — explosive outward impulse
            if (dropActive && t.kickPhase < 0.1f) {
                val triCx = (t.a.x + t.b.x + t.c.x) / 3f
                val triCy = (t.a.y + t.b.y + t.c.y) / 3f
                val dir = atan2(triCy - cy, triCx - cx)
                val strength = 1.2f + bass * 1.5f
                t.kickPhase = strength
                t.kickVx = cos(dir) * 0.8f
                t.kickVy = sin(dir) * 0.8f
            }
        }
    }

    // ── Topology 1: Sierpinski Fractal ──────────────────────────────────────
    private fun buildSierpinski(
        cx: Float, cy: Float, radius: Float,
        maxDepth: Int, time: Float, spectrum: FloatArray, bass: Float
    ) {
        for (seed in 0 until 3) {
            val globalAngle = (seed * 2f * PI.toFloat() / 3f) + time * 0.12f
            val tipAngle = globalAngle - (PI.toFloat() / 2f)
            val angles = floatArrayOf(tipAngle, tipAngle + 2.094f, tipAngle + 4.188f)
            val ax = cx + cos(angles[0]) * radius
            val ay = cy + sin(angles[0]) * radius
            val bx = cx + cos(angles[1]) * radius
            val by = cy + sin(angles[1]) * radius
            val ccx = cx + cos(angles[2]) * radius
            val ccy = cy + sin(angles[2]) * radius

            subdivideSierpinski(ax, ay, 0f, bx, by, 0f, ccx, ccy, 0f, 0, maxDepth, seed, spectrum, bass)
        }
    }

    private fun subdivideSierpinski(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        depth: Int, maxDepth: Int, seed: Int,
        spectrum: FloatArray, bass: Float
    ) {
        if (count >= maxTriangles) return
        val t = pool[count++]
        t.a.set(ax, ay, az)
        t.b.set(bx, by, bz)
        t.c.set(cx, cy, cz)
        t.depth = depth
        t.driftAngle = ((seed * 1.618f + depth * 0.9f) % (2f * PI.toFloat()))
        t.driftSpeed = (80f + seed * 30f + depth * 15f)
        t.energyBin = ((seed * 11 + depth * 7) % spectrum.size.coerceAtLeast(1))
        if (t.kickPhase == 0f) { t.kickVx = 0f; t.kickVy = 0f }

        if (depth >= maxDepth) return

        val mabx = (ax + bx) * 0.5f; val maby = (ay + by) * 0.5f; val mabz = (az + bz) * 0.5f
        val mbcx = (bx + cx) * 0.5f; val mbcy = (by + cy) * 0.5f; val mbcz = (bz + cz) * 0.5f
        val mcax = (cx + ax) * 0.5f; val mcay = (cy + ay) * 0.5f; val mcaz = (cz + az) * 0.5f

        subdivideSierpinski(ax, ay, az, mabx, maby, mabz, mcax, mcay, mcaz, depth + 1, maxDepth, seed, spectrum, bass)
        subdivideSierpinski(mabx, maby, mabz, bx, by, bz, mbcx, mbcy, mbcz, depth + 1, maxDepth, seed, spectrum, bass)
        subdivideSierpinski(mbcx, mbcy, mbcz, cx, cy, cz, mcax, mcay, mcaz, depth + 1, maxDepth, seed, spectrum, bass)
        subdivideSierpinski(mabx, maby, mabz, mbcx, mbcy, mbcz, mcax, mcay, mcaz, depth + 1, maxDepth, seed, spectrum, bass)
    }

    // ── Topology 2: Sacred Merkaba (Dual Interlocking Star) ────────────────
    private fun buildMerkaba(
        cx: Float, cy: Float, radius: Float,
        maxDepth: Int, time: Float, spectrum: FloatArray, bass: Float, mids: Float
    ) {
        val countStars = 4
        for (s in 0 until countStars) {
            val r = radius * ((s + 1f) / countStars) * (1f + mids * 0.3f)
            val rot = (if (s % 2 == 0) 1f else -1f) * time * (0.2f + s * 0.05f)

            // Upward triangle
            addSingleTri(
                cx + cos(rot) * r, cy + sin(rot) * r, 0f,
                cx + cos(rot + 2.094f) * r, cy + sin(rot + 2.094f) * r, 0f,
                cx + cos(rot + 4.188f) * r, cy + sin(rot + 4.188f) * r, 0f,
                depth = s, energyBin = (s * 4) % spectrum.size.coerceAtLeast(1)
            )

            // Inverted triangle
            val invRot = rot + PI.toFloat()
            addSingleTri(
                cx + cos(invRot) * r, cy + sin(invRot) * r, 0f,
                cx + cos(invRot + 2.094f) * r, cy + sin(invRot + 2.094f) * r, 0f,
                cx + cos(invRot + 4.188f) * r, cy + sin(invRot + 4.188f) * r, 0f,
                depth = s, energyBin = (s * 4 + 2) % spectrum.size.coerceAtLeast(1)
            )
        }
    }

    // ── Topology 3: 3D Wireframe Polyhedron ────────────────────────────────
    private fun buildPolyhedron3D(
        cx: Float, cy: Float, radius: Float,
        time: Float, spectrum: FloatArray, bass: Float, mids: Float, treble: Float
    ) {
        val rotX = time * 0.35f + bass * 0.5f
        val rotY = time * 0.45f + mids * 0.4f
        val rotZ = time * 0.25f

        // Octahedron 6 vertices
        val r = radius * 0.85f
        val rawVerts = arrayOf(
            floatArrayOf(0f, -r, 0f),  // 0 Top
            floatArrayOf(0f, r, 0f),   // 1 Bottom
            floatArrayOf(-r, 0f, 0f),  // 2 Left
            floatArrayOf(r, 0f, 0f),   // 3 Right
            floatArrayOf(0f, 0f, -r),  // 4 Front
            floatArrayOf(0f, 0f, r)    // 5 Back
        )

        // Rotate vertices in 3D
        val rotVerts = Array(6) { FloatArray(3) }
        for (i in 0 until 6) {
            val v = rawVerts[i]
            // Rot Y
            var x1 = v[0] * cos(rotY) + v[2] * sin(rotY)
            var y1 = v[1]
            var z1 = -v[0] * sin(rotY) + v[2] * cos(rotY)

            // Rot X
            val y2 = y1 * cos(rotX) - z1 * sin(rotX)
            val z2 = y1 * sin(rotX) + z1 * cos(rotX)
            val x2 = x1

            rotVerts[i][0] = cx + x2
            rotVerts[i][1] = cy + y2
            rotVerts[i][2] = z2
        }

        // 8 Faces
        val faces = arrayOf(
            intArrayOf(0, 2, 4), intArrayOf(0, 4, 3), intArrayOf(0, 3, 5), intArrayOf(0, 5, 2),
            intArrayOf(1, 4, 2), intArrayOf(1, 3, 4), intArrayOf(1, 5, 3), intArrayOf(1, 2, 5)
        )

        for (fIdx in faces.indices) {
            val f = faces[fIdx]
            val vA = rotVerts[f[0]]
            val vB = rotVerts[f[1]]
            val vC = rotVerts[f[2]]
            addSingleTri(
                vA[0], vA[1], vA[2],
                vB[0], vB[1], vB[2],
                vC[0], vC[1], vC[2],
                depth = 1, energyBin = (fIdx * 3) % spectrum.size.coerceAtLeast(1)
            )
        }
    }

    // ── Topology 4: Orbital Vortex ──────────────────────────────────────────
    private fun buildOrbitalVortex(
        cx: Float, cy: Float, radius: Float,
        time: Float, spectrum: FloatArray, bass: Float, mids: Float
    ) {
        val rings = 12
        for (ring in 0 until rings) {
            val progress = ring.toFloat() / rings
            val r = radius * (0.15f + progress * 0.85f)
            val spiralRot = time * (0.4f + (1f - progress) * 0.8f) + progress * PI.toFloat()
            val triSize = radius * (0.08f + progress * 0.12f) * (1f + bass * 0.4f)

            val countInRing = 3 + ring
            for (j in 0 until countInRing) {
                val angle = spiralRot + (j * 2f * PI.toFloat() / countInRing)
                val tcx = cx + cos(angle) * r
                val tcy = cy + sin(angle) * r

                val a1 = angle
                val a2 = angle + 2.094f
                val a3 = angle + 4.188f

                addSingleTri(
                    tcx + cos(a1) * triSize, tcy + sin(a1) * triSize, progress * 100f,
                    tcx + cos(a2) * triSize, tcy + sin(a2) * triSize, progress * 100f,
                    tcx + cos(a3) * triSize, tcy + sin(a3) * triSize, progress * 100f,
                    depth = ring % 3,
                    energyBin = (ring * 2 + j) % spectrum.size.coerceAtLeast(1)
                )
            }
        }
    }

    // ── Topology 5: Deconstructed Lattice ───────────────────────────────────
    private fun buildDeconstructedLattice(
        cx: Float, cy: Float, radius: Float,
        time: Float, spectrum: FloatArray, bass: Float, mids: Float, treble: Float
    ) {
        val gridDim = 5
        val spacing = (radius * 1.8f) / gridDim

        for (gx in 0 until gridDim) {
            for (gy in 0 until gridDim) {
                val ox = (gx - gridDim / 2f) * spacing
                val oy = (gy - gridDim / 2f) * spacing
                val dist = sqrt(ox * ox + oy * oy)
                if (dist > radius * 1.1f) continue

                val noiseVal = sin(ox * 0.05f + time) * cos(oy * 0.05f + time)
                val dynamicOx = ox + noiseVal * bass * 35f
                val dynamicOy = oy + noiseVal * mids * 35f
                val tSize = (spacing * 0.45f) * (1f + treble * 0.5f)

                val rot = time * 0.5f + (gx + gy) * 0.4f
                addSingleTri(
                    cx + dynamicOx + cos(rot) * tSize, cy + dynamicOy + sin(rot) * tSize, 0f,
                    cx + dynamicOx + cos(rot + 2.094f) * tSize, cy + dynamicOy + sin(rot + 2.094f) * tSize, 0f,
                    cx + dynamicOx + cos(rot + 4.188f) * tSize, cy + dynamicOy + sin(rot + 4.188f) * tSize, 0f,
                    depth = (gx + gy) % 3,
                    energyBin = ((gx * 5 + gy) * 3) % spectrum.size.coerceAtLeast(1)
                )
            }
        }
    }

    private fun addSingleTri(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        depth: Int, energyBin: Int
    ) {
        if (count >= maxTriangles) return
        val t = pool[count++]
        t.a.set(ax, ay, az)
        t.b.set(bx, by, bz)
        t.c.set(cx, cy, cz)
        t.depth = depth
        t.energyBin = energyBin
        t.driftAngle = rng.nextFloat() * 2f * PI.toFloat()
        t.driftSpeed = 40f + rng.nextFloat() * 80f
        t.kickPhase = 0f
        t.kickVx = 0f
        t.kickVy = 0f
    }

    /**
     * Projects 3D geometry into 2D screen coordinate buffers.
     */
    fun project(cx: Float, cy: Float, focalLength: Float, bassZ: Float) {
        for (i in 0 until count) {
            val t = pool[i]
            val zA = t.a.z * bassZ
            val zB = t.b.z * bassZ
            val zC = t.c.z * bassZ

            val scaleA = focalLength / (focalLength + zA).coerceAtLeast(1f)
            val scaleB = focalLength / (focalLength + zB).coerceAtLeast(1f)
            val scaleC = focalLength / (focalLength + zC).coerceAtLeast(1f)

            projX[i * 3 + 0] = cx + (t.a.x - cx) * scaleA
            projY[i * 3 + 0] = cy + (t.a.y - cy) * scaleA
            projZ[i * 3 + 0] = scaleA

            projX[i * 3 + 1] = cx + (t.b.x - cx) * scaleB
            projY[i * 3 + 1] = cy + (t.b.y - cy) * scaleB
            projZ[i * 3 + 1] = scaleB

            projX[i * 3 + 2] = cx + (t.c.x - cx) * scaleC
            projY[i * 3 + 2] = cy + (t.c.y - cy) * scaleC
            projZ[i * 3 + 2] = scaleC
        }
    }
}
