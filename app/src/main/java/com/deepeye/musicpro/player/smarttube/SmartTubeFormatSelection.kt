// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.player.smarttube

import androidx.compose.runtime.Immutable

/**
 * Categorization of media stream type in the SmartTube pipeline.
 */
enum class DeepEyeStreamType {
    VIDEO_PROGRESSIVE,
    VIDEO_ONLY,
    AUDIO_ONLY,
    UNKNOWN
}

/**
 * Normalized video codecs supported by SmartTube stream candidates.
 */
enum class DeepEyeVideoCodec(val displayName: String) {
    AVC("H.264 (AVC)"),
    VP9("VP9"),
    AV1("AV1"),
    HEVC("H.265 (HEVC)"),
    UNKNOWN("Unknown Codec")
}

/**
 * Normalized audio codecs supported by SmartTube stream candidates.
 */
enum class DeepEyeAudioCodec(val displayName: String) {
    OPUS("Opus"),
    AAC("AAC"),
    AC3("AC-3"),
    EAC3("E-AC-3 (Dolby Digital Plus)"),
    UNKNOWN("Unknown Codec")
}

/**
 * High Dynamic Range / Standard Dynamic Range metadata.
 */
enum class DeepEyeDynamicRange(val displayName: String) {
    SDR("SDR"),
    HDR10("HDR10"),
    HLG("HLG"),
    DOLBY_VISION("Dolby Vision"),
    UNKNOWN("SDR")
}

/**
 * High-level quality presets matching SmartTube playback preset behavior.
 */
enum class VideoQualityPreset(val displayName: String, val description: String) {
    AUTO("Automatic", "Dynamically chooses best compatible stream for current network & device"),
    DATA_SAVER("Data Saver", "Limits video to 360p / 480p and reduces cellular bandwidth consumption"),
    BALANCED("Balanced", "Smooth 720p / 1080p standard HD streaming with optimal battery efficiency"),
    HIGH_QUALITY("High Quality", "Prioritizes 1080p60 / 1440p high-bitrate stream"),
    BEST_COMPATIBLE("Best Compatible", "Highest resolution & frame rate verified supported by hardware decoders"),
    CUSTOM("Custom", "Manual stream format override")
}

/**
 * SmartTube video codec preference settings.
 */
enum class VideoCodecPreference(val displayName: String) {
    AUTO("Auto (SmartTube Selection)"),
    PREFER_AV1("Prefer AV1 (Next-Gen Efficiency)"),
    PREFER_VP9("Prefer VP9 (Broad HD Compatibility)"),
    PREFER_AVC("Prefer AVC / H.264 (Universal Hardware)"),
    PREFER_HEVC("Prefer HEVC / H.265")
}

/**
 * SmartTube audio language selection preferences.
 */
enum class AudioLanguagePreference(val displayName: String) {
    ORIGINAL("Original Language"),
    SYSTEM("Match Device Language"),
    HINDI("Hindi (हिन्दी)"),
    ENGLISH("English"),
    NO_PREFERENCE("Default Stream Track")
}

/**
 * SmartTube audio quality preferences.
 */
enum class AudioQualityPreference(val displayName: String) {
    AUTO("Automatic (Highest Quality)"),
    DATA_SAVER("Data Saver (Low Bitrate)"),
    BEST_COMPATIBLE("Best Compatible (Audiophile)")
}

/**
 * SmartTube audio codec preference settings.
 */
enum class AudioCodecPreference(val displayName: String) {
    AUTO("Auto (Best Available)"),
    PREFER_OPUS("Prefer Opus (Ultra-HQ 160kbps)"),
    PREFER_AAC("Prefer AAC (Universal Sink)"),
    PREFER_MULTICHANNEL("Prefer Multichannel 5.1+"),
    COMPATIBILITY_FIRST("Compatibility First")
}

/**
 * SmartTube channel layout preference.
 */
enum class ChannelPreference(val displayName: String) {
    AUTO("Auto (Match Stream)"),
    STEREO("Stereo (2.0)"),
    SURROUND_5_1("Surround 5.1+"),
    MATCH_DEVICE("Match Connected Audio Device")
}

/**
 * Normalized representation of a real, playable SmartTube video or audio stream candidate.
 */
@Immutable
data class DeepEyePlaybackFormat(
    val stableId: String,
    val smartTubeFormatId: String? = null,
    val streamType: DeepEyeStreamType = DeepEyeStreamType.VIDEO_PROGRESSIVE,
    val container: String? = null,
    val mimeType: String? = null,
    val videoCodec: DeepEyeVideoCodec? = null,
    val audioCodec: DeepEyeAudioCodec? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Float? = null,
    val bitrate: Long? = null,
    val dynamicRange: DeepEyeDynamicRange? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val languageTag: String? = null,
    val languageLabel: String? = null,
    val isOriginalAudio: Boolean? = null,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false,
    val isDeviceCompatible: Boolean = true,
    val incompatibilityReason: String? = null,
    val isVideoOnly: Boolean = false,
    val isAudioOnly: Boolean = false,
    val isProgressive: Boolean = false,
    val streamUrl: String? = null
) {
    val isVideo: Boolean
        get() = isVideoOnly || isProgressive || width != null || height != null

    val isAudio: Boolean
        get() = isAudioOnly || isProgressive || channelCount != null || sampleRateHz != null
}

/**
 * Maps SmartTube's rich playback format model into DeepEye's player format model.
 */
fun DeepEyePlaybackFormat.toDeepEyeFormat(groupIndex: Int = 0, trackIndex: Int = 0): com.deepeye.musicpro.player.format.DeepEyeFormat {
    val isVid = this.streamType == DeepEyeStreamType.VIDEO_PROGRESSIVE ||
            this.streamType == DeepEyeStreamType.VIDEO_ONLY ||
            (!this.isAudioOnly && (this.width != null || this.height != null))
    return com.deepeye.musicpro.player.format.DeepEyeFormat(
        id = this.stableId,
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        type = if (isVid) com.deepeye.musicpro.player.format.FormatType.VIDEO else com.deepeye.musicpro.player.format.FormatType.AUDIO,
        mimeType = this.mimeType ?: if (isVid) "video/mp4" else "audio/mp4",
        codecName = this.videoCodec?.displayName ?: this.audioCodec?.displayName ?: "Default",
        rawCodecs = this.videoCodec?.name ?: this.audioCodec?.name ?: "",
        width = this.width ?: 0,
        height = this.height ?: 0,
        frameRate = this.frameRate ?: 0f,
        bitrate = (this.bitrate ?: 0L).toInt(),
        audioChannels = this.channelCount ?: 0,
        audioSampleRate = this.sampleRateHz ?: 0,
        container = this.container ?: "",
        qualityLabel = if (this.height != null) "${this.height}p" else (if (isVid) "" else "${(this.bitrate ?: 0L) / 1000} kbps"),
        isHdr = this.dynamicRange == DeepEyeDynamicRange.HDR10 || this.dynamicRange == DeepEyeDynamicRange.HLG || this.dynamicRange == DeepEyeDynamicRange.DOLBY_VISION,
        isHardwareAccelerated = true,
        isSupported = this.isDeviceCompatible,
        isSelected = this.isSelected,
        isAuto = false
    )
}
