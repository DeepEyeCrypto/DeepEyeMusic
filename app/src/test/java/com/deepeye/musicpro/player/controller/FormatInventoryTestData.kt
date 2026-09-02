package com.deepeye.musicpro.player.controller

import com.deepeye.musicpro.player.format.DeepEyeFormat
import com.deepeye.musicpro.player.format.FormatType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Shared deterministic test fixtures for format-inventory policies.
 * Mirrors production shapes produced by QualitySelectionEngine (isAuto prepended).
 */

fun autoVideoFormat() = DeepEyeFormat(id = "auto_v", groupIndex = 0, trackIndex = 0, type = FormatType.VIDEO,
    mimeType = "video/avc", codecName = "Auto", isAuto = true)

fun autoAudioFormat() = DeepEyeFormat(id = "auto_a", groupIndex = 0, trackIndex = 0, type = FormatType.AUDIO,
    mimeType = "audio/opus", codecName = "Auto", isAuto = true)

fun videoFormat(id: String, h: Int, c: String, isSelected: Boolean = false) = DeepEyeFormat(
    id = id, groupIndex = 0, trackIndex = 0, type = FormatType.VIDEO, mimeType = "video/$c",
    codecName = c, height = h, qualityLabel = "${h}p", isAuto = false, isSelected = isSelected)

fun audioFormat(id: String, c: String) = DeepEyeFormat(
    id = id, groupIndex = 0, trackIndex = 0, type = FormatType.AUDIO, mimeType = "audio/$c",
    codecName = c, isAuto = false)

fun richVideoList(n: Int = 20): ImmutableList<DeepEyeFormat> = (listOf(autoVideoFormat()) + (0 until n).map { i ->
    videoFormat(id = "v$i", h = 480 + (i % 5) * 360, c = if (i % 3 == 0) "avc" else if (i % 3 == 1) "vp9" else "av1")
}).toImmutableList()

fun richAudioList(n: Int = 13): ImmutableList<DeepEyeFormat> = (listOf(autoAudioFormat()) + (0 until n).map { i ->
    audioFormat(id = "a$i", c = if (i % 2 == 0) "opus" else "aac")
}).toImmutableList()

fun sparseVideoList(): ImmutableList<DeepEyeFormat> = listOf(autoVideoFormat(), videoFormat(id = "s_v", h = 720, c = "avc")).toImmutableList()
fun sparseAudioList(): ImmutableList<DeepEyeFormat> = listOf(autoAudioFormat(), audioFormat(id = "s_a", c = "aac")).toImmutableList()

fun emptyVideoList(): ImmutableList<DeepEyeFormat> = emptyList<DeepEyeFormat>().toImmutableList()
fun emptyAudioList(): ImmutableList<DeepEyeFormat> = emptyList<DeepEyeFormat>().toImmutableList()