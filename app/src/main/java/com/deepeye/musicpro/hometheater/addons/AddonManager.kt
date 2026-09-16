// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.hometheater.addons

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AddonCategory {
    VIDEO_SCRAPER,
    STREAM_SOURCE,
    AUDIO_VISUALIZER,
    SUBTITLES,
    SKIN_THEME,
    WEATHER_PROVIDER,
    PROGRAM_TOOL
}

@Immutable
data class AddonManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: AddonCategory,
    val iconUrl: String? = null,
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = true,
    val settingsSchema: Map<String, String> = emptyMap()
)

@Singleton
class AddonManager @Inject constructor() {

    private val _installedAddons = MutableStateFlow<List<AddonManifest>>(
        listOf(
            AddonManifest(
                id = "plugin.video.tmdb.scraper",
                name = "TheMovieDatabase Scraper V3",
                version = "3.2.0",
                author = "DeepEye Team",
                description = "Scrapes 4K fanart, cast profiles, ratings, trailers, and HDR metadata.",
                category = AddonCategory.VIDEO_SCRAPER,
                isEnabled = true,
                isInstalled = true
            ),
            AddonManifest(
                id = "plugin.source.smb.v3",
                name = "Samba / Windows Share Connector (SMB2/3)",
                version = "2.1.4",
                author = "DeepEye Network",
                description = "High-throughput SMB client with multi-threaded chunk buffering for 4K REMUX streaming.",
                category = AddonCategory.STREAM_SOURCE,
                isEnabled = true,
                isInstalled = true
            ),
            AddonManifest(
                id = "plugin.source.webdav",
                name = "WebDAV / Nextcloud Streamer",
                version = "1.8.0",
                author = "DeepEye Network",
                description = "Direct streaming from personal cloud servers and Seedboxes.",
                category = AddonCategory.STREAM_SOURCE,
                isEnabled = true,
                isInstalled = true
            ),
            AddonManifest(
                id = "plugin.audio.dsp.visualizer",
                name = "Liquid Glass Spectrum & Starfield VU",
                version = "2.0.1",
                author = "DeepEye Audio",
                description = "Hardware-accelerated AGSL canvas visualizers with 60FPS beat detection.",
                category = AddonCategory.AUDIO_VISUALIZER,
                isEnabled = true,
                isInstalled = true
            ),
            AddonManifest(
                id = "plugin.service.opensubtitles",
                name = "OpenSubtitles.com Dual Sync",
                version = "4.0.0",
                author = "Community",
                description = "Automatic hash-matched multi-language subtitle downloader with offset fine-tuning.",
                category = AddonCategory.SUBTITLES,
                isEnabled = true,
                isInstalled = true
            ),
            AddonManifest(
                id = "plugin.weather.openmeteo",
                name = "Global HyperLocal Weather Radar",
                version = "1.5.2",
                author = "DeepEye System",
                description = "High-accuracy live temperature, animated conditions, and 7-day forecast.",
                category = AddonCategory.WEATHER_PROVIDER,
                isEnabled = true,
                isInstalled = true
            )
        )
    )
    val installedAddons: StateFlow<List<AddonManifest>> = _installedAddons.asStateFlow()

    fun toggleAddon(addonId: String) {
        _installedAddons.value = _installedAddons.value.map {
            if (it.id == addonId) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun installAddon(addon: AddonManifest) {
        if (_installedAddons.value.none { it.id == addon.id }) {
            _installedAddons.value = _installedAddons.value + addon.copy(isInstalled = true, isEnabled = true)
        }
    }

    fun uninstallAddon(addonId: String) {
        _installedAddons.value = _installedAddons.value.filterNot { it.id == addonId }
    }
}
