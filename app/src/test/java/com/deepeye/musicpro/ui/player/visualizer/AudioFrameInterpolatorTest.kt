package com.deepeye.musicpro.ui.player.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFrameInterpolatorTest {

    /** Quiet-but-nonzero floor, so "no change" and "changed" are distinguishable. */
    private companion object {
        const val IDLE = 0.1f
    }

    private val bands = FloatArray(6)
    private val spectrum = FloatArray(32)

    private fun update(
        interp: AudioFrameInterpolator,
        dtNanos: Long,
        bandValue: Float,
        intensity: Float = 1f,
        reducedMotion: Boolean = false
    ): Long {
        bands.fill(bandValue)
        spectrum.fill(0f)
        interp.update(bands, spectrum, intensity, reducedMotion, dtNanos)
        return dtNanos
    }

    @Test
    fun `first frame snaps to target instead of fading from zero`() {
        val interp = AudioFrameInterpolator()
        update(interp, 1_000_000_000L, bandValue = 0.8f)
        assertEquals(0.8f, interp.bands[0], 0.0001f)
    }

    @Test
    fun `output is continuous across frames with a stepped input`() {
        // The production bug: FFT arrives at ~19Hz while the draw loop runs at
        // 48-60fps, so raw values step every ~3rd frame and the render stutters.
        // Interpolating must produce a monotonic ramp instead of a staircase.
        val interp = AudioFrameInterpolator()
        val start = 1_000_000_000L
        val FRAME_NANOS = 16_666_667L

        // Idle: the source settles at a quiet but non-zero level.
        update(interp, start, bandValue = IDLE)

        // One transient: the source jumps up once, then the NEXT sample is already
        // back to idle -- exactly the ~19Hz-stepping / 60fps-drawing mismatch.
        val transient = update(interp, start + FRAME_NANOS, bandValue = 1f)
        val samples = mutableListOf<Float>()

        for (i in 1..10) {
            update(interp, transient + FRAME_NANOS * i, bandValue = IDLE)
            samples.add(interp.bands[0])
        }

        // Attack must rise above idle immediately (transient is not swallowed)...
        assertTrue(
            "Transient did not raise the band above idle ($IDLE): ${samples.first()}",
            samples.first() > IDLE
        )
        // ...then decay smoothly rather than snapping straight back down.
        samples.zipWithNext().forEach { (a, b) ->
            assertTrue("Expected monotonic decay, got $a then $b", b <= a)
        }
    }

    @Test
    fun `values converge to target and stay bounded`() {
        val interp = AudioFrameInterpolator()
        var now = 0L
        val start = 1_000_000_000L
        update(interp, start, bandValue = 0.5f)
        for (i in 1..200) {
            now += 16_666_667L
            update(interp, start + now, bandValue = 0.5f)
        }
        assertEquals(0.5f, interp.bands[0], 0.01f)
        interp.bands.forEach { assertTrue("Band $it escaped [0,1]", it in 0f..1f) }
    }

    @Test
    fun `intensity scales the smoothed output`() {
        val interp = AudioFrameInterpolator()
        update(interp, 1_000_000_000L, bandValue = 0.4f, intensity = 1.5f)
        assertEquals(0.6f, interp.bands[0], 0.0001f)
    }

    @Test
    fun `NaN intensity does not poison the smoothing`() {
        val interp = AudioFrameInterpolator()
        update(interp, 1_000_000_000L, bandValue = 0.5f, intensity = Float.NaN)
        assertTrue("NaN leaked into band output", !interp.bands[0].isNaN())
        assertEquals(0.5f, interp.bands[0], 0.0001f)
    }

    @Test
    fun `negative intensity is handled without producing NaN`() {
        val interp = AudioFrameInterpolator()
        update(interp, 1_000_000_000L, bandValue = 0.5f, intensity = -1f)
        assertTrue("Negative intensity produced NaN", !interp.bands[0].isNaN())
    }

    @Test
    fun `short input arrays are tolerated`() {
        // Defensive: the engine could hand back a differently sized array after a
        // capture-size change. That must not throw inside the draw loop.
        val interp = AudioFrameInterpolator()
        interp.update(
            FloatArray(2), FloatArray(4), 1f, false, 1_000_000_000L
        )
        assertTrue(!interp.bands[0].isNaN())
        assertTrue(!interp.spectrum[0].isNaN())
    }

    @Test
    fun `backwards clock does not produce NaN`() {
        val interp = AudioFrameInterpolator()
        update(interp, 2_000_000_000L, bandValue = 0.5f)
        update(interp, 1_000_000_000L, bandValue = 0.5f)
        assertTrue("Backwards clock produced NaN", !interp.bands[0].isNaN())
    }

    @Test
    fun `reset clears state so the next start does not lerp from stale values`() {
        val interp = AudioFrameInterpolator()
        update(interp, 1_000_000_000L, bandValue = 0.9f)
        interp.reset()
        assertEquals(0f, interp.bands[0], 0.0001f)

        // After reset the first update must snap, not ramp from zero.
        update(interp, 2_000_000_000L, bandValue = 0.3f)
        assertEquals(0.3f, interp.bands[0], 0.0001f)
    }

    @Test
    fun `large frame gap is clamped rather than causing a jump`() {
        val interp = AudioFrameInterpolator()
        val start = 1_000_000_000L
        update(interp, start, bandValue = 1f)
        // Simulate a 5-second stall (GC / app backgrounded).
        update(interp, start + 5_000_000_000L, bandValue = 0f)
        // Clamping to MAX_DT means we move, but stay finite and sane.
        assertTrue(!interp.bands[0].isNaN())
        assertTrue(interp.bands[0] >= 0f)
    }

    @Test
    fun `reduced motion damps the response`() {
        val normal = AudioFrameInterpolator()
        val reduced = AudioFrameInterpolator()
        val start = 1_000_000_000L

        update(normal, start, bandValue = 0f)
        update(reduced, start, bandValue = 0f)
        update(normal, start + 16_666_667L, bandValue = 1f)
        update(reduced, start + 16_666_667L, bandValue = 1f, reducedMotion = true)

        // Attack uses the same fast constant, so compare the release direction,
        // which is where reduced motion is meant to visibly calm the output.
        update(normal, start + 33_333_334L, bandValue = 0f)
        update(reduced, start + 33_333_334L, bandValue = 0f, reducedMotion = true)

        assertTrue(
            "Reduced motion should decay more slowly than normal",
            reduced.bands[0] > normal.bands[0]
        )
    }
}