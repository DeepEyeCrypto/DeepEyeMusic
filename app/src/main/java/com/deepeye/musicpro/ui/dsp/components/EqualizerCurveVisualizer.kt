// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.dsp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.deepeye.musicpro.ui.modifiers.rememberParentScrollLockConnection
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

val EQ_FREQUENCIES = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

data class EqPreset(val name: String, val gains: FloatArray)

val STUDIO_EQ_PRESETS = listOf(
    EqPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqPreset("Sub-Bass Boost", floatArrayOf(8f, 6f, 4f, 2f, 0f, 0f, 1f, 2f, 3f, 4f)),
    EqPreset("Punchy 808", floatArrayOf(3f, 6f, 4f, 0f, -1f, 0f, 1f, 2f, 2.5f, 2f)),
    EqPreset("Vocal Clarity", floatArrayOf(-1f, -0.5f, 0f, 1.5f, 3.5f, 4f, 3.5f, 4.5f, 6f, 7f)),
    EqPreset("Club / EDM", floatArrayOf(7f, 5f, 3f, 1f, 0f, 0f, 0.5f, 1.5f, 2.5f, 3f)),
    EqPreset("Rock & Metal", floatArrayOf(6f, 4f, 2f, 0f, -1f, 1f, 4f, 5f, 6f, 6f)),
    EqPreset("Acoustic Warmth", floatArrayOf(3f, 3f, 2f, 1f, 2f, 3f, 4f, 3f, 2f, 1f)),
    EqPreset("Treble Air", floatArrayOf(-2f, -1f, 0f, 0f, 1f, 3f, 5f, 7f, 8.5f, 9f)),
)

private val neonCyan = Color(0xFF00E5FF)
private val neonPurple = Color(0xFF7B1FA2)
private val darkSurface = Color(0xFF131722).copy(alpha = 0.85f)
private val glassBorder = Color(0x22FFFFFF)

// ── Fader bank geometry / touch engineering constants ──
/** Hard cap on rendered bands; the touch surface is always divided into this many slots. */
internal const val EQ_MAX_BANDS = 10
internal const val EQ_MAX_DB = 12f
internal const val EQ_MIN_DB = -12f

/**
 * Half-width of the "magnetic" flat spot around unity gain, in dB.
 *
 * Any raw gain within `[-EQ_MAGNETIC_DETENT_DB, +EQ_MAGNETIC_DETENT_DB]` collapses to
 * exactly `0f`, so a fader released near unity parks dead-centre instead of resting at a
 * fractional value that renders as a stray `-1` / `+1` and cannot be dialled back out.
 */
internal const val EQ_MAGNETIC_DETENT_DB = 1.0f

/**
 * Pure pointer → value mapping for the 10-band fader bank.
 *
 * Extracted from the Composable so the gesture arithmetic is unit-testable without
 * a running Compose hierarchy. All coordinates are in the touch surface's local
 * pixel space; the surface is a contiguous, gap-free strip, so every X maps to
 * exactly one band.
 */
internal object EqTouchMath {

    /**
     * Resolves the band index for a horizontal touch position.
     *
     * @param touchX X offset inside the fader surface, in pixels.
     * @param surfaceWidth Usable width of the fader surface, in pixels.
     * @param bandCount Number of bands rendered.
     * @return The locked band index, or `null` when the geometry is degenerate
     *   (zero width, zero bands, or NaN input) and the gesture must be ignored.
     */
    fun bandIndexForTouchX(touchX: Float, surfaceWidth: Float, bandCount: Int): Int? {
        if (bandCount <= 0 || surfaceWidth <= 0f) return null
        if (touchX.isNaN() || surfaceWidth.isNaN()) return null
        val slotWidth = surfaceWidth / bandCount
        if (slotWidth <= 0f) return null
        return (touchX / slotWidth).toInt().coerceIn(0, bandCount - 1)
    }

    /**
     * Resolves the band index under a touch while the strip is panned.
     *
     * When [contentWidth] exceeds the viewport the strip is scrolled, so a raw
     * viewport-relative X would resolve the wrong band. Re-basing the touch by
     * [scrollOffsetPx] puts it back into content space first, which makes this
     * the single hit-test entry point for both the panned and unpanned cases.
     *
     * @param touchX X offset inside the visible viewport, in pixels.
     * @param scrollOffsetPx Horizontal pan of the strip, in pixels (`0` when unpanned).
     * @param contentWidth Width of the full gap-free strip, in pixels.
     * @param bandCount Number of bands rendered.
     */
    fun bandIndexForTouchX(
        touchX: Float,
        scrollOffsetPx: Float,
        contentWidth: Float,
        bandCount: Int,
    ): Int? = bandIndexForTouchX(touchX + scrollOffsetPx, contentWidth, bandCount)

    /**
     * Maps an absolute vertical touch position to a band gain in dB.
     *
     * Uses the absolute Y (not a per-frame delta) so the fader tracks the finger
     * 1:1 and cannot accumulate rounding error or drift from persisted state.
     *
     * @param touchY Y offset inside the fader surface, in pixels.
     * @param trackTop Top edge of the visible track, in surface-local pixels.
     * @param trackHeight Height of the visible track, in pixels.
     * @return Gain in the range [EQ_MIN_DB, EQ_MAX_DB]; `null` when the track has
     *   not been laid out yet (zero height) and the gesture must be ignored.
     */
    fun gainForTouchY(touchY: Float, trackTop: Float, trackHeight: Float): Float? {
        if (trackHeight <= 0f) return null
        if (touchY.isNaN() || trackTop.isNaN() || trackHeight.isNaN()) return null
        val fraction = ((touchY - trackTop) / trackHeight).coerceIn(0f, 1f)
        return (EQ_MAX_DB - fraction * (EQ_MAX_DB - EQ_MIN_DB))
            .coerceIn(EQ_MIN_DB, EQ_MAX_DB)
    }

    /**
     * True when a touch starting at [touchY] should be owned by the fader bank
     * rather than passed through to the parent scroll container.
     */
    fun isWithinTouchZone(
        touchY: Float,
        trackTop: Float,
        trackHeight: Float,
        overshootPx: Float,
    ): Boolean {
        if (trackHeight <= 0f) return false
        return touchY >= trackTop - overshootPx &&
            touchY <= trackTop + trackHeight + overshootPx
    }

    /**
     * Horizontal centre of the slot owned by [index] within a gap-free strip.
     *
     * Single source of truth for band geometry: the fader bank draws its tracks, the
     * curve visualiser draws its frequency guides, and the curve draws its spline
     * nodes all through this one function, so a grid line cannot drift off the centre
     * of the band it is labelling.
     *
     * @return The centre X in pixels, or `null` when the geometry is degenerate.
     */
    fun slotCenterX(index: Int, surfaceWidth: Float, bandCount: Int): Float? {
        if (bandCount <= 0 || surfaceWidth <= 0f) return null
        if (surfaceWidth.isNaN()) return null
        val slotWidth = surfaceWidth / bandCount
        if (slotWidth <= 0f) return null
        return (slotWidth * index) + (slotWidth / 2f)
    }

    /**
     * Pannable slot-centre helper: same gap-free math as [slotCenterX], re-based
     * by the strip's pan so the returned X lands inside the visible viewport.
     *
     * Curve guides, spline nodes, fader tracks, knob caps, the dB readout and the
     * frequency labels all route through this one function, so a panned strip
     * cannot render a guide on one band and a knob on another.
     *
     * @return The on-screen centre X in pixels, or `null` when the geometry is
     *   degenerate (zero width, zero bands, or NaN input).
     */
    fun slotCenterX(
        index: Int,
        contentWidth: Float,
        bandCount: Int,
        scrollOffsetPx: Float,
    ): Float? {
        if (scrollOffsetPx.isNaN()) return null
        val center = slotCenterX(index, contentWidth, bandCount) ?: return null
        return center - scrollOffsetPx
    }

    /**
     * Content width required to give every band at least [minSlotWidthPx].
     *
     * This is the floor that keeps the 48dp touch-target guarantee alive inside
     * a narrow landscape grid cell: the strip is laid out this wide and panned
     * within the viewport, rather than being squeezed below the target size.
     *
     * @return `bandCount * minSlotWidthPx`, or `0f` for a degenerate band count
     *   or a non-positive / NaN [minSlotWidthPx].
     */
    fun requiredContentWidth(bandCount: Int, minSlotWidthPx: Float): Float {
        if (bandCount <= 0) return 0f
        if (minSlotWidthPx <= 0f || minSlotWidthPx.isNaN()) return 0f
        return minSlotWidthPx * bandCount
    }

    /**
     * Maximum legal horizontal pan for a strip of [contentWidth] inside a
     * [viewportWidth] viewport. Zero when the strip fits, so the panning
     * machinery is inert rather than conditional in the wide layout.
     */
    fun maxScrollOffset(contentWidth: Float, viewportWidth: Float): Float {
        if (contentWidth.isNaN() || viewportWidth.isNaN()) return 0f
        return (contentWidth - viewportWidth).coerceAtLeast(0f)
    }

    /**
     * Clamps a horizontal pan offset to the strip's legal travel.
     *
     * The strip may only be dragged left far enough to reveal its right edge and
     * right back to zero; anything beyond that is clamped back to the bound so a
     * fling cannot park a band permanently off-screen or leave a dead gap.
     */
    fun clampScrollOffset(offsetPx: Float, contentWidth: Float, viewportWidth: Float): Float {
        if (offsetPx.isNaN()) return 0f
        return offsetPx.coerceIn(0f, maxScrollOffset(contentWidth, viewportWidth))
    }

    /**
     * Applies the magnetic detent: collapses any gain within
     * `[-EQ_MAGNETIC_DETENT_DB, +EQ_MAGNETIC_DETENT_DB]` to exactly `0f`.
     *
     * Kept separate from [gainForTouchY] on purpose — that function is the raw linear
     * Y→dB map and must stay strictly monotonic for its own tests; the detent is a
     * downstream stage that only the live gesture path composes on top of it.
     *
     * @return `0f` inside the detent, otherwise [gain] unchanged.
     */
    fun applyMagneticDetent(gain: Float): Float =
        if (abs(gain) <= EQ_MAGNETIC_DETENT_DB) 0f else gain
}

/**
 * Axis a single fader-bank gesture commits to, decided once past touch slop.
 *
 * The strip must serve two gestures on the same pixels: dragging a fader up and
 * down, and dragging left and right to pan to the bands that overflow the card.
 * Resolving that per-move instead of once per gesture would make a single finger
 * pan *and* scribble across ten bands at once, so the axis is locked on the
 * first movement that clears slop and held until the finger lifts.
 */
internal enum class EqGestureAxis { UNDECIDED, VERTICAL, HORIZONTAL }

/**
 * Diagnostics for the fader bank's gesture and pan state.
 *
 * The axis-lock and the pan offset are both invisible state that silently decide
 * whether a drag adjusts a gain or scrolls the strip. Without a trace, "my swipe
 * did nothing" is indistinguishable from "my swipe panned the wrong way", which
 * is exactly the failure this log exists to rule out.
 */
internal object EqGestureLog {
    private const val TAG = "EqGesture"

    fun axisLocked(axis: EqGestureAxis, band: Int, totalX: Float, totalY: Float, canPan: Boolean) {
        android.util.Log.d(
            TAG,
            "axisLocked axis=$axis band=$band totalX=$totalX totalY=$totalY canPan=$canPan",
        )
    }

    fun panned(offsetPx: Float, deltaX: Float, maxOffsetPx: Float) {
        android.util.Log.d(
            TAG,
            "panned offsetPx=$offsetPx deltaX=$deltaX maxOffsetPx=$maxOffsetPx",
        )
    }

    fun gestureRejected(reason: String, y: Float, trackTop: Float, trackHeight: Float) {
        android.util.Log.d(
            TAG,
            "gestureRejected reason=$reason y=$y trackTop=$trackTop trackHeight=$trackHeight",
        )
    }

    /** Unconditional per-gesture entry trace: the geometry the axis-lock will use. */
    fun gestureStart(
        viewport: Float,
        contentWidth: Float,
        canPan: Boolean,
        touchSlop: Float,
        trackTop: Float,
        trackHeight: Float,
        bandCount: Int,
    ) {
        android.util.Log.d(
            TAG,
            "gestureStart viewport=$viewport contentWidth=$contentWidth canPan=$canPan " +
                "touchSlop=$touchSlop trackTop=$trackTop trackHeight=$trackHeight " +
                "bands=$bandCount",
        )
    }

    /** Unconditional per-gesture exit trace with the axis the gesture resolved to. */
    fun gestureEnd(axis: EqGestureAxis, band: Int, offsetPx: Float) {
        android.util.Log.d(TAG, "gestureEnd axis=$axis band=$band offsetPx=$offsetPx")
    }
}

/** Visible height of a single fader track. */
private val EQ_TRACK_HEIGHT = 120.dp

/**
 * Hard ceiling on the rendered track height.
 *
 * Landscape on a short handset leaves roughly 300dp of vertical room once the
 * app bar, the curve canvas and the frequency labels are placed. The bank must
 * never claim more than this, or it clips the labels at the bottom of the card.
 */
private val EQ_TRACK_MAX_HEIGHT = 240.dp

/**
 * Minimum width of one fader's touch slot.
 *
 * The strip is laid out at `bandCount * EQ_MIN_SLOT_WIDTH` and panned horizontally
 * inside whatever viewport the host card gives it. That is what keeps the
 * fat-finger guarantee alive in the landscape DSP grid, where a cell is only
 * ~145dp wide and a naive `width / bandCount` produced ~10dp slots.
 *
 * Because the strip no longer shrinks to fit, a wide layout simply shows every
 * band with room to spare and the pan offset stays pinned at `0` — the panning
 * machinery is inert, not conditional.
 */
internal val EQ_MIN_SLOT_WIDTH = 34.dp

/** Idle width of a single fader track. */
private val EQ_TRACK_WIDTH = 22.dp

/** Hardware-console knob cap size. */
private val EQ_THUMB_SIZE = 20.dp

/**
 * Extra vertical reach of a band's touch zone beyond the visible track, on both sides.
 * Enlarges the effective target from [EQ_TRACK_HEIGHT] to
 * `EQ_TRACK_HEIGHT + 2 * EQ_TOUCH_OVERSHOOT` without changing the visual layout.
 */
private val EQ_TOUCH_OVERSHOOT = 16.dp

/**
 * Pro Hardware Studio 10-Band Equalizer with Bezier Spline Curve, Quick Studio Presets, and Tactile High-Fidelity Faders.
 */
@Composable
fun EqualizerCurveVisualizer(
    eqBands: FloatArray,
    isEnabled: Boolean,
    onBandGainChanged: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
    onCommitBands: (FloatArray) -> Unit = { bands ->
        bands.forEachIndexed { index, gain -> onBandGainChanged(index, gain) }
    },
) {
    // ── Instant-feedback draft ───────────────────────────────────────────────
    // The engine round-trip is debounced/conflated, so `eqBands` lags the finger
    // by a frame or two during a drag. Rendering straight from the draft keeps
    // the fader, the dB readout and the curve locked to the touch point instead
    // of visibly snapping back. Seeded from the persisted curve, then resynced
    // whenever the incoming state differs and no gesture is in flight, so
    // presets and toggles still land.
    var draftBands by remember { mutableStateOf(eqBands.copyOf()) }
    var gestureInFlight by remember { mutableStateOf(false) }

    LaunchedEffect(eqBands.toList(), gestureInFlight) {
        if (!gestureInFlight) {
            draftBands = eqBands.copyOf()
        }
    }

    // Height is capped so the bank cannot outgrow a short landscape viewport;
    // the track still needs room for the 48dp touch target at both extremes.
    val cappedTrackHeight = EQ_TRACK_HEIGHT.coerceAtMost(EQ_TRACK_MAX_HEIGHT)

    val bandCount = draftBands.size.coerceAtMost(EQ_MAX_BANDS)

    // ── Pannable strip geometry ─────────────────────────────────────────────
    // The bank is ONE gap-free strip. Its slot width is fixed at 48dp rather
    // than `viewportWidth / bandCount`, so in the landscape module grid — where
    // the card is only ~145dp wide — the ten bands keep full-size touch
    // targets instead of collapsing to ~10dp apiece. The overflow is reached by
    // panning, and the pan offset is applied identically to the curve, the
    // tracks, the readouts and the hit test so they scroll as a single unit.
    val minSlotWidthPx = with(LocalDensity.current) { EQ_MIN_SLOT_WIDTH.toPx() }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }

    // Never narrower than the viewport: a wide layout shows all ten bands with
    // the offset pinned at 0 and panning never engages.
    val contentWidthPx = remember(bandCount, minSlotWidthPx, viewportWidthPx) {
        maxOf(
            EqTouchMath.requiredContentWidth(bandCount, minSlotWidthPx),
            viewportWidthPx,
        )
    }

    // A relayout (rotation, grid reflow) can shrink the legal travel while an
    // offset is live, which would leave the strip parked off its new bounds.
    LaunchedEffect(contentWidthPx, viewportWidthPx) {
        scrollOffsetPx = EqTouchMath.clampScrollOffset(
            scrollOffsetPx,
            contentWidthPx,
            viewportWidthPx,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(darkSurface)
            .border(1.dp, glassBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Curve Header & Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3300E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isEnabled) neonCyan else Color.White.copy(0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "10-Band Studio EQ Console",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color.White else Color.White.copy(0.5f)
                    )
                    Text(
                        text = "Parametric Log-Spline • 31Hz - 16kHz",
                        fontSize = 12.sp,
                        color = Color.White.copy(0.6f)
                    )
                }
            }

            // Reset to Flat button
            Surface(
                onClick = {
                    // Zero the draft too, not just the engine: a commit-only write
                    // would leave the knobs showing the old curve until the
                    // conflated round-trip lands, which reads as a dropped tap.
                    val flat = FloatArray(bandCount)
                    draftBands = flat
                    onCommitBands(flat)
                },
                enabled = isEnabled,
                shape = RoundedCornerShape(12.dp),
                color = Color(0x14FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, null, tint = neonCyan, modifier = Modifier.size(16.dp))
                    Text("Flat", color = neonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Studio Presets Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(STUDIO_EQ_PRESETS) { preset ->
                // Highlight off the DRAFT, not the engine state: during and just
                // after a drag the engine lags by a frame or two, so keying off
                // `eqBands` made the "Flat" chip lose its selection while the
                // curve visibly read 0dB.
                val isSelected = remember(draftBands.toList(), preset) {
                    val currentRounded = draftBands.take(10).map { it.roundToInt() }
                    val presetRounded = preset.gains.take(10).map { it.roundToInt() }
                    currentRounded == presetRounded
                }

                Surface(
                    onClick = {
                        if (isEnabled) {
                            // A preset is a discrete whole-curve command, so it goes
                            // through the commit path (every band must land) rather
                            // than the conflated per-band drag queue. The draft is
                            // updated first so the faders jump immediately.
                            val staged = FloatArray(bandCount)
                            preset.gains.forEachIndexed { index, gain ->
                                if (index < bandCount) staged[index] = gain
                            }
                            draftBands = staged
                            onCommitBands(staged)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x0EFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) neonCyan else glassBorder
                    ),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = if (isSelected) neonCyan else Color.White.copy(0.75f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Real-Time Studio Spline Curve Canvas
        // `clipToBounds` is what makes the pannable strip safe: the spline is
        // drawn in content space, which is WIDER than this card, so without an
        // explicit clip the curve would bleed out over the neighbouring DSP
        // modules instead of sliding under the card edge.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .clipToBounds()
                .background(Color(0xFF090B10).copy(alpha = 0.95f))
                .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val numBands = bandCount
            if (numBands < 2) return@Canvas

            // Grid Lines: 0dB, +6dB, -6dB
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.5f
            )

            val stepY = height / 4f
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, midY - stepY),
                end = Offset(width, midY - stepY),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, midY + stepY),
                end = Offset(width, midY + stepY),
                strokeWidth = 1f
            )

            // Frequency Vertical Guides
            // Slot centres come from EqTouchMath in CONTENT space (fixed 48dp
            // slots) and are then shifted by the live pan offset, so these guides
            // land exactly on the centre of the fader slot they label and travel
            // with it. Everything here is drawn in content coordinates and the
            // whole canvas is clipped to the viewport, which keeps the spline and
            // its guides in lockstep with the fader bank below.
            val slotCenters = List(numBands) { i ->
                EqTouchMath.slotCenterX(i, contentWidthPx, numBands, scrollOffsetPx)
                    ?: return@Canvas
            }

            for (i in 0 until numBands) {
                val x = slotCenters[i]
                // Skip guides scrolled out of view; drawing them is wasted work
                // and an off-screen line still costs a path segment.
                if (x < 0f || x > width) continue
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // Calculate Bezier control points
            val points = mutableListOf<Offset>()
            for (i in 0 until numBands) {
                val x = slotCenters[i]
                val gain = draftBands[i].coerceIn(EQ_MIN_DB, EQ_MAX_DB)
                val normalizedY = midY - (gain / EQ_MAX_DB) * (height * 0.42f)
                points.add(Offset(x, normalizedY))
            }

            // Smooth cubic Bezier spline
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX1 = (p0.x + p1.x) / 2f
                    val controlY1 = p0.y
                    val controlX2 = (p0.x + p1.x) / 2f
                    val controlY2 = p1.y
                    cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }
            }

            // Gradient Fill under curve
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isEnabled) neonCyan else Color.Gray).copy(alpha = 0.35f),
                        (if (isEnabled) neonPurple else Color.DarkGray).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Neon glowing stroke
            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(
                    colors = if (isEnabled) {
                        listOf(neonCyan, Color(0xFF00B0FF), neonPurple, Color(0xFFFF4081))
                    } else {
                        listOf(Color.Gray, Color.DarkGray)
                    }
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Interactive glowing band nodes
            for (pt in points) {
                drawCircle(
                    color = Color.Black,
                    radius = 6.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (isEnabled) neonCyan else Color.Gray,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }

        // ── 10-Band Tactile Studio Fader Bank ─────────────────────────────────────
        // Unified custom touch surface (fat-finger fix). Design invariants:
        //  * The ten x-slots are CONTIGUOUS in CONTENT space with a fixed 48dp
        //    width, so there are zero dead gaps and zero overlapping targets: any
        //    touch, however imprecise, deterministically resolves to exactly one
        //    band. Content is wider than the card in the landscape grid, and the
        //    overflow is reached by panning rather than by shrinking the slots.
        //  * One gesture is AXIS-LOCKED on the first movement past touch slop.
        //    Vertical locks the band and drives its gain; horizontal pans the
        //    strip. Without this a single finger trying to pan would scribble
        //    across ten bands, and a fader drag near the edge would jitter.
        //  * Once locked, the band index is held for the whole gesture, so
        //    finger drift can never hand control to a neighbouring band mid-swipe.
        //  * The pointer stream is consumed on PointerEventPass.Initial, which runs
        //    BEFORE the parent LazyVerticalGrid / verticalScroll observe the event;
        //    a nested-scroll pre-scroll lock additionally blocks parent scroll/fling.
        //  * Gain is derived from the ABSOLUTE touch Y inside the track rect, so the
        //    fader tracks the finger 1:1 and can never drift out of sync with state.
        //  * The drag renders from the local draft and only commits to the engine
        //    on gesture end, so a fast swipe cannot flood the audio thread and the
        //    final value is never lost to coalescing.
        var activeBand by remember { mutableStateOf<Int?>(null) }
        var trackTopPx by remember { mutableFloatStateOf(0f) }
        var trackHeightPx by remember { mutableFloatStateOf(0f) }
        var surfaceCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        // Half-width of the last-measured readout/label text, so centred text can be
        // shifted to a slot centre. Captured per child by onSizeChanged.
        var textHalfWidthPx by remember { mutableFloatStateOf(0f) }
        val currentOnBandGainChanged by rememberUpdatedState(onBandGainChanged)
        val currentOnCommitBands by rememberUpdatedState(onCommitBands)
        val touchOvershootPx = with(LocalDensity.current) { EQ_TOUCH_OVERSHOOT.toPx() }

        // Single funnel for every gain write: raw linear Y→dB map, then the magnetic
        // detent. Both the tap handler and every drag frame route through here,
        // so a band can never be left at a fractional gain the detent should have caught.
        fun gainFromTouchY(touchY: Float): Float =
            EqTouchMath.applyMagneticDetent(
                EqTouchMath.gainForTouchY(touchY, trackTopPx, trackHeightPx) ?: 0f
            )

        // Writes the draft (instant visual feedback) and hands the same value to
        // the ViewModel's conflated queue. Never touches the engine directly.
        fun applyGain(index: Int, gain: Float) {
            if (index !in draftBands.indices) return
            draftBands = draftBands.copyOf().also { it[index] = gain }
            currentOnBandGainChanged(index, gain)
        }

        /** Which axis this gesture committed to is tracked by [EqGestureAxis]. */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { viewportWidthPx = it.width.toFloat() }
                .nestedScroll(rememberParentScrollLockConnection(isLocked = activeBand != null))
                .onGloballyPositioned { surfaceCoords = it }
                .pointerInput(isEnabled, bandCount, contentWidthPx) {
                    if (!isEnabled || bandCount <= 0) return@pointerInput
                    // Read inside the pointer scope: the platform touch slop is only
                    // reachable from PointerInputScope, and it is the threshold that
                    // separates an intentional fader drag from finger tremor.
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        if (trackHeightPx <= 0f) {
                            EqGestureLog.gestureRejected(
                                "unlaid_out_track",
                                down.position.y,
                                trackTopPx,
                                trackHeightPx,
                            )
                            return@awaitEachGesture
                        }

                        // A touch that begins in the readout / frequency-label gutter
                        // is left unconsumed so the surrounding page still scrolls.
                        if (!EqTouchMath.isWithinTouchZone(
                                down.position.y,
                                trackTopPx,
                                trackHeightPx,
                                touchOvershootPx,
                            )
                        ) {
                            EqGestureLog.gestureRejected(
                                "outside_touch_zone",
                                down.position.y,
                                trackTopPx,
                                trackHeightPx,
                            )
                            return@awaitEachGesture
                        }

                        val pointerId = down.id
                        val viewport = size.width.toFloat()
                        // Panning is only ever a candidate when the strip actually
                        // overflows; in a wide layout every drag is a fader drag.
                        val canPan =
                            EqTouchMath.maxScrollOffset(contentWidthPx, viewport) > 0f
                        EqGestureLog.gestureStart(
                            viewport = viewport,
                            contentWidth = contentWidthPx,
                            canPan = canPan,
                            touchSlop = touchSlop,
                            trackTop = trackTopPx,
                            trackHeight = trackHeightPx,
                            bandCount = bandCount,
                        )
                        // Starve the parent scrollable from the very first event:
                        // this bank owns any touch that lands on a track.
                        down.consume()

                        var axis = EqGestureAxis.UNDECIDED
                        var lockedIndex = -1
                        gestureInFlight = true
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change =
                                    event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) break
                                change.consume()

                                if (axis == EqGestureAxis.UNDECIDED) {
                                    // Axis is decided on the CUMULATIVE displacement from
                                    // the down point, never the per-event delta. A real
                                    // finger — and an injected drag alike — arrives as a
                                    // stream of small per-frame deltas, each of which can
                                    // sit under the touch slop for the whole gesture. Testing
                                    // only the per-event delta left the axis permanently
                                    // UNDECIDED, so an ordinary slow pan silently did
                                    // nothing; testing only the total would ignore the
                                    // slop entirely and make every tremor a fader move.
                                    val total = change.position - down.position
                                    val priorAxis = axis
                                    when {
                                        canPan && abs(total.x) > touchSlop &&
                                            abs(total.x) > abs(total.y) -> {
                                            axis = EqGestureAxis.HORIZONTAL
                                        }
                                        abs(total.y) > touchSlop -> {
                                            axis = EqGestureAxis.VERTICAL
                                            lockedIndex =
                                                EqTouchMath.bandIndexForTouchX(
                                                    change.position.x,
                                                    scrollOffsetPx,
                                                    contentWidthPx,
                                                    bandCount,
                                                ) ?: -1
                                            if (lockedIndex >= 0) {
                                                activeBand = lockedIndex
                                                applyGain(
                                                    lockedIndex,
                                                    gainFromTouchY(change.position.y),
                                                )
                                            }
                                        }
                                    }
                                    if (axis != priorAxis) {
                                        EqGestureLog.axisLocked(
                                            axis,
                                            lockedIndex,
                                            total.x,
                                            total.y,
                                            canPan,
                                        )
                                    }
                                    continue
                                }

                                when (axis) {
                                    EqGestureAxis.HORIZONTAL -> {
                                        // Dragging left reveals later bands, so the
                                        // offset grows with a leftward finger delta.
                                        val deltaX = change.positionChange().x
                                        scrollOffsetPx = EqTouchMath.clampScrollOffset(
                                            scrollOffsetPx - deltaX,
                                            contentWidthPx,
                                            viewport,
                                        )
                                        EqGestureLog.panned(
                                            scrollOffsetPx,
                                            deltaX,
                                            EqTouchMath.maxScrollOffset(
                                                contentWidthPx,
                                                viewport,
                                            ),
                                        )
                                    }
                                    EqGestureAxis.VERTICAL -> if (lockedIndex >= 0) {
                                        applyGain(
                                            lockedIndex,
                                            gainFromTouchY(change.position.y),
                                        )
                                    }
                                    EqGestureAxis.UNDECIDED -> Unit
                                }
                            }

                            // A tap that never passed slop still sets its band, so a
                            // quick tap on a fader is not swallowed by the axis lock.
                            if (axis == EqGestureAxis.UNDECIDED) {
                                EqTouchMath.bandIndexForTouchX(
                                    down.position.x,
                                    scrollOffsetPx,
                                    contentWidthPx,
                                    bandCount,
                                )?.let { tapped ->
                                    activeBand = tapped
                                    applyGain(tapped, gainFromTouchY(down.position.y))
                                }
                            }
                        } finally {
                            EqGestureLog.gestureEnd(axis, lockedIndex, scrollOffsetPx)
                            if (activeBand == lockedIndex || axis == EqGestureAxis.UNDECIDED) {
                                activeBand = null
                            }
                            gestureInFlight = false
                            // Flush the final position so coalescing can never drop
                            // the last value of a fast swipe. Panning has no gain to
                            // commit, and a bare tap was already applied above.
                            if (axis == EqGestureAxis.VERTICAL) {
                                currentOnCommitBands(draftBands)
                            }
                        }
                    }
                }
        ) {
            Column(Modifier.fillMaxWidth()) {
                // dB readout row — one cell per band, positioned at the panned slot
                // centres. The Box clips so readouts for bands scrolled off the
                // edge slide out of the card instead of over the neighbouring module.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    for (i in 0 until bandCount) {
                        // Same panned slot-centre function the tracks and the curve
                        // use, so a readout can never end up labelling a neighbouring
                        // fader. Text is centred on the slot, hence the half-width
                        // shift back from the centre.
                        val centerX = EqTouchMath.slotCenterX(
                            i,
                            contentWidthPx,
                            bandCount,
                            scrollOffsetPx,
                        ) ?: continue
                        val currentGain = draftBands[i]
                        val isBoost = currentGain > 0.4f
                        val isCut = currentGain < -0.4f
                        val formattedGain = if (currentGain >= 0.4f) {
                            "+${currentGain.roundToInt()}"
                        } else if (currentGain <= -0.4f) {
                            "${currentGain.roundToInt()}"
                        } else {
                            "0"
                        }

                        Text(
                            text = formattedGain,
                            fontSize = 11.sp,
                            color = when {
                                !isEnabled -> Color.Gray
                                i == activeBand -> neonCyan
                                isBoost -> neonCyan
                                isCut -> Color(0xFFFF9100)
                                else -> Color.White.copy(0.7f)
                            },
                            fontWeight = FontWeight.Black,
                            // Positioned absolutely at the panned slot centre: the
                            // old `weight(1f)` divided the VIEWPORT into equal cells,
                            // which silently misaligns every label the moment the
                            // strip is panned. Centred text needs the half-width of
                            // its own measured box, hence the onSizeChanged capture.
                            modifier = Modifier
                                .onSizeChanged { textHalfWidthPx = it.width / 2f }
                                .offset {
                                    IntOffset(
                                        (centerX - textHalfWidthPx).roundToInt(),
                                        0,
                                    )
                                },
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Fader bank Canvas. Also reports its rect to the gesture handler
                // above so the Y → dB mapping stays exact after any relayout.
                // `clipToBounds` is required: tracks are positioned in CONTENT
                // space, which is wider than this canvas, so without it the
                // off-screen faders would paint straight over the adjacent DSP card.
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cappedTrackHeight)
                        .clipToBounds()
                        .onGloballyPositioned { canvasCoords ->
                            val root = surfaceCoords
                            if (root != null && root.isAttached && canvasCoords.isAttached) {
                                val bounds = root.localBoundingBoxOf(
                                    sourceCoordinates = canvasCoords,
                                    clipBounds = false,
                                )
                                if (abs(bounds.top - trackTopPx) > 0.5f ||
                                    abs(bounds.height - trackHeightPx) > 0.5f
                                ) {
                                    trackTopPx = bounds.top
                                    trackHeightPx = bounds.height
                                }
                            }
                        }
                ) {
                    if (bandCount <= 0) return@Canvas

                    val midY = size.height / 2f
                    val thumbRadius = EQ_THUMB_SIZE.toPx() / 2f
                    val travel = (midY - thumbRadius).coerceAtLeast(1f)
                    val baseWidth = EQ_TRACK_WIDTH.toPx()
                    val activeWidth = (EQ_TRACK_WIDTH + 6.dp).toPx()
                    val thumbCorner = minOf(8.dp.toPx(), thumbRadius)

                    // 0 dB centre guide across the whole bank
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.5f
                    )

                    for (i in 0 until bandCount) {
                        val isActive = i == activeBand
                        // Same slot-centre function as the curve visualiser's grid
                        // guides above, so track, guide and spline node cannot diverge.
                        val centerX = EqTouchMath.slotCenterX(
                            i,
                            contentWidthPx,
                            bandCount,
                            scrollOffsetPx,
                        ) ?: continue
                        val gain = draftBands[i].coerceIn(EQ_MIN_DB, EQ_MAX_DB)
                        val normGain = (gain / EQ_MAX_DB).coerceIn(-1f, 1f)
                        val thumbY = midY - normGain * travel
                        val trackWidth = if (isActive) activeWidth else baseWidth
                        val trackLeft = centerX - trackWidth / 2f
                        val trackRadius = trackWidth / 2f

                        // Track slot
                        drawRoundRect(
                            color = Color(0xFF090B10),
                            topLeft = Offset(trackLeft, 0f),
                            size = Size(trackWidth, size.height),
                            cornerRadius = CornerRadius(trackRadius),
                        )

                        // ── Locked band: the slot lights up from within ────────────
                        if (isActive) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        neonCyan.copy(alpha = 0.38f),
                                        neonCyan.copy(alpha = 0.12f),
                                        neonCyan.copy(alpha = 0.38f),
                                    )
                                ),
                                topLeft = Offset(trackLeft, 0f),
                                size = Size(trackWidth, size.height),
                                cornerRadius = CornerRadius(trackRadius),
                            )

                            // Active-gain beam from the 0 dB line out to the knob
                            val beamTop = minOf(midY, thumbY)
                            val beamHeight = abs(thumbY - midY)
                            if (beamHeight > 0.5f) {
                                drawRoundRect(
                                    color = neonCyan.copy(alpha = 0.45f),
                                    topLeft = Offset(
                                        trackLeft + trackWidth * 0.32f,
                                        beamTop,
                                    ),
                                    size = Size(trackWidth * 0.36f, beamHeight),
                                    cornerRadius = CornerRadius(trackWidth * 0.18f),
                                )
                            }
                        }

                        // Track border — brightens to signal the locked band
                        drawRoundRect(
                            color = if (isActive) {
                                neonCyan.copy(alpha = 0.95f)
                            } else {
                                glassBorder
                            },
                            topLeft = Offset(trackLeft, 0f),
                            size = Size(trackWidth, size.height),
                            cornerRadius = CornerRadius(trackRadius),
                            style = Stroke(width = if (isActive) 2.dp.toPx() else 1.dp.toPx()),
                        )

                        // Glow halo behind the knob of the locked band
                        if (isActive) {
                            drawCircle(
                                color = neonCyan.copy(alpha = 0.30f),
                                radius = thumbRadius * 1.75f,
                                center = Offset(centerX, thumbY),
                            )
                        }

                        // Hardware console fader knob cap
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                if (isEnabled) {
                                    listOf(Color(0xFF263238), Color(0xFF102027))
                                } else {
                                    listOf(Color(0xFF1E1E1E), Color(0xFF121212))
                                }
                            ),
                            topLeft = Offset(centerX - thumbRadius, thumbY - thumbRadius),
                            size = Size(thumbRadius * 2f, thumbRadius * 2f),
                            cornerRadius = CornerRadius(thumbCorner),
                        )
                        drawRoundRect(
                            color = if (isEnabled) {
                                neonCyan.copy(alpha = if (isActive) 1f else 0.8f)
                            } else {
                                Color.White.copy(0.2f)
                            },
                            topLeft = Offset(centerX - thumbRadius, thumbY - thumbRadius),
                            size = Size(thumbRadius * 2f, thumbRadius * 2f),
                            cornerRadius = CornerRadius(thumbCorner),
                            style = Stroke(
                                width = if (isActive) 1.5f.dp.toPx() else 1.dp.toPx()
                            ),
                        )

                        // Knob centre glowing indicator line
                        drawRoundRect(
                            color = if (isEnabled) neonCyan else Color.Gray,
                            topLeft = Offset(centerX - 7.dp.toPx(), thumbY - 1.25.dp.toPx()),
                            size = Size(14.dp.toPx(), 2.5.dp.toPx()),
                            cornerRadius = CornerRadius(1.25.dp.toPx()),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Frequency label row — positioned at the same panned slot centres
                // as the tracks, for the same reason as the dB readouts above.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    for (i in 0 until bandCount) {
                        val centerX = EqTouchMath.slotCenterX(
                            i,
                            contentWidthPx,
                            bandCount,
                            scrollOffsetPx,
                        ) ?: continue
                        Text(
                            text = EQ_FREQUENCIES.getOrElse(i) { "${i}k" },
                            fontSize = 11.sp,
                            color = if (i == activeBand) {
                                neonCyan
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .onSizeChanged { textHalfWidthPx = it.width / 2f }
                                .offset {
                                    IntOffset(
                                        (centerX - textHalfWidthPx).roundToInt(),
                                        0,
                                    )
                                },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
