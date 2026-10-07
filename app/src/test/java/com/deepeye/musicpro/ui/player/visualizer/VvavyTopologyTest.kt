package com.deepeye.musicpro.ui.player.visualizer

import com.deepeye.musicpro.ui.player.visualizer.vvavy.VvavyGeometryEngine
import com.deepeye.musicpro.ui.player.visualizer.vvavy.VvavyTopologyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VvavyTopologyTest {

    private lateinit var engine: VvavyGeometryEngine
    private val dummySpectrum = FloatArray(256) { 0.5f }

    @Before
    fun setUp() {
        engine = VvavyGeometryEngine(512)
    }

    @Test
    fun testAllTopologiesGenerateValidGeometry() {
        val modes = VvavyTopologyMode.entries

        for (mode in modes) {
            engine.mutateTopology(mode)
            assertEquals(mode, engine.currentMode)

            engine.build(
                cx = 500f,
                cy = 500f,
                radius = 300f,
                bass = 1.2f,
                mids = 0.8f,
                treble = 0.5f,
                spectrum = dummySpectrum,
                time = 1.5f,
                dropActive = false,
                dt = 0.016f
            )

            assertTrue("Mode $mode must generate at least 1 triangle", engine.count > 0)
            assertTrue("Mode $mode must not exceed MAX_TRIS", engine.count <= VvavyGeometryEngine.MAX_TRIS)

            // Test 3D to 2D projection
            engine.project(500f, 500f, 400f, 1f)

            for (i in 0 until engine.count) {
                val ax = engine.projX[i * 3 + 0]
                val ay = engine.projY[i * 3 + 0]
                val bx = engine.projX[i * 3 + 1]
                val by = engine.projY[i * 3 + 1]
                val cx = engine.projX[i * 3 + 2]
                val cy = engine.projY[i * 3 + 2]

                assertFalse("Projected ax must not be NaN in $mode", ax.isNaN())
                assertFalse("Projected ay must not be NaN in $mode", ay.isNaN())
                assertFalse("Projected bx must not be NaN in $mode", bx.isNaN())
                assertFalse("Projected by must not be NaN in $mode", by.isNaN())
                assertFalse("Projected cx must not be NaN in $mode", cx.isNaN())
                assertFalse("Projected cy must not be NaN in $mode", cy.isNaN())
            }
        }
    }

    @Test
    fun testDropImpulseTriggersKickPhase() {
        engine.mutateTopology(VvavyTopologyMode.SIERPINSKI_FRACTAL)
        engine.build(
            cx = 500f,
            cy = 500f,
            radius = 300f,
            bass = 2.0f,
            mids = 1.0f,
            treble = 1.0f,
            spectrum = dummySpectrum,
            time = 2.0f,
            dropActive = true,
            dt = 0.016f
        )

        val activeKicks = (0 until engine.count).count { engine.pool[it].kickPhase > 0f }
        assertTrue("Bass drop must trigger outward kick phase on triangles", activeKicks > 0)
    }
}
