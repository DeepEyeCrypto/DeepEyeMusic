// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.hometheater.model

import androidx.compose.runtime.Immutable

/**
 * Core domain models for the Kodi-style Home Theater Media Center.
 */

enum class MediaType {
    MOVIE,
    TV_SHOW,
    EPISODE,
    MUSIC_TRACK,
    MUSIC_ALBUM,
    MUSIC_ARTIST,
    PHOTO,
    NETWORK_STREAM,
    IPTV_CHANNEL
}

enum class NetworkSourceType {
    LOCAL_STORAGE,
    SMB_SHARE,
    WEBDAV,
    DLNA_UPNP,
    NFS,
    HTTP_DIRECT,
    IPTV_M3U
}

@Immutable
data class NetworkShareSource(
    val id: String,
    val name: String,
    val type: NetworkSourceType,
    val serverAddress: String,
    val sharePath: String,
    val username: String? = null,
    val isProtected: Boolean = false,
    val isOnline: Boolean = true
)

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val mediaType: MediaType,
    val uri: String,
    val year: Int? = null,
    val releaseDate: String? = null,
    val durationMs: Long = 0L,
    val rating: Float = 0f, // 0.0 to 10.0
    val genres: List<String> = emptyList(),
    val director: String? = null,
    val cast: List<String> = emptyList(),
    val overview: String? = null,
    val tagline: String? = null,
    val posterUrl: String? = null,
    val fanartUrl: String? = null, // Backdrop 1080p/4K
    val logoUrl: String? = null,
    val resolution: String = "1080p", // 4K HDR, 1080p, 720p
    val videoCodec: String = "HEVC",
    val audioCodec: String = "DTS-HD / Dolby Atmos",
    val audioChannels: String = "7.1",
    val watchProgressFraction: Float = 0f, // 0.0 to 1.0
    val isWatched: Boolean = false,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

@Immutable
data class TvShowSeries(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val fanartUrl: String?,
    val overview: String?,
    val seasonCount: Int,
    val episodeCount: Int,
    val unwatchedCount: Int,
    val rating: Float,
    val genres: List<String>
)

@Immutable
data class PhotoItem(
    val id: String,
    val title: String,
    val uri: String,
    val dateTaken: Long,
    val cameraModel: String? = null,
    val resolution: String? = null,
    val orientationDegrees: Int = 0
)

@Immutable
data class WeatherInfo(
    val city: String,
    val currentTempC: Int,
    val condition: WeatherCondition,
    val highTempC: Int,
    val lowTempC: Int,
    val humidityPercent: Int,
    val windSpeedKmh: Int,
    val forecastDays: List<ForecastDay>
)

enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    THUNDERSTORM,
    SNOWY,
    FOGGY
}

@Immutable
data class ForecastDay(
    val dayName: String,
    val highTemp: Int,
    val lowTemp: Int,
    val condition: WeatherCondition
)
