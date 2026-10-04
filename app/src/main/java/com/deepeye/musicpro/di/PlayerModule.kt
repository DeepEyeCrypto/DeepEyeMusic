// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.net.CookieHandler
import java.net.URI
import java.util.HashMap
import android.webkit.CookieManager
import java.io.File
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.database.StandaloneDatabaseProvider

/**
 * Hilt module providing Media3/ExoPlayer dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val evictor = LeastRecentlyUsedCacheEvictor(2L * 1024 * 1024 * 1024) // 2GB storage quota
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        simpleCache: SimpleCache
    ): CacheDataSource.Factory {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("com.google.android.youtube/20.10.33 (Linux; U; Android 12)")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive"
            ))
        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        okHttpClient: okhttp3.OkHttpClient,
        cacheDataSourceFactory: CacheDataSource.Factory,
        vocalRemoverProcessor: com.deepeye.musicpro.dsp.processor.VocalRemoverProcessor,
        crossfeedProcessor: com.deepeye.musicpro.dsp.processor.CrossfeedProcessor,
        viperBassProcessor: com.deepeye.musicpro.dsp.processor.ViperBassAudioProcessor,
        viperClarityProcessor: com.deepeye.musicpro.dsp.processor.ViperClarityProcessor,
        fieldSurroundProcessor: com.deepeye.musicpro.dsp.processor.FieldSurroundProcessor,
        tubeSimulatorProcessor: com.deepeye.musicpro.dsp.processor.TubeSimulatorProcessor,
        playbackGainProcessor: com.deepeye.musicpro.dsp.processor.PlaybackGainProcessor,
        masterLimiterProcessor: com.deepeye.musicpro.dsp.processor.MasterLimiterProcessor,
        lufsAnalyzerProcessor: com.deepeye.musicpro.dsp.processor.LufsAnalyzerProcessor
    ): ExoPlayer {
        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                val bufferSizeProvider = androidx.media3.exoplayer.audio.DefaultAudioSink.AudioTrackBufferSizeProvider { minBufferSizeInBytes, _, _, _, _, _, _ ->
                    // Multiply minBufferSize by 4x to eliminate MT6835 HAL underruns and guarantee deep headroom for ViPER DSP
                    minBufferSizeInBytes * 4
                }

                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false) // Guarantee 16-bit PCM for native Visualizer capture & HAL stability
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioTrackBufferSizeProvider(bufferSizeProvider)
                    .setAudioProcessors(
                        arrayOf(
                            vocalRemoverProcessor,
                            crossfeedProcessor,
                            fieldSurroundProcessor,
                            viperBassProcessor,
                            viperClarityProcessor,
                            tubeSimulatorProcessor,
                            playbackGainProcessor,
                            masterLimiterProcessor,
                            lufsAnalyzerProcessor
                        )
                    )
                    .build()
            }
        }
        renderersFactory.setExtensionRendererMode(
            androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        ).setEnableDecoderFallback(true)

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setPreferredVideoMimeTypes("video/avc", "video/av01", "video/vp9") // Prefer AVC/AV1/VP9 for hardware acceleration and reliable video rendering
                .setTunnelingEnabled(false) // Tunneling breaks AudioProcessors and causes video failure on many devices
                .setForceHighestSupportedBitrate(false) // Adapt smoothly to network conditions and decoder limits
                .setAllowVideoNonSeamlessAdaptiveness(true)
                // Audio Offload routes the stream to the hardware DSP path where
                // audiofx effects (Visualizer FFT capture) are NOT supported.
                // Force the PCM route so the ReactiveTriangleVisualizer always
                // receives live data. NOTE: must stay LAST in the chain — this
                // method is declared on the base TrackSelectionParameters.Builder
                // and returns the base type, which would break the
                // DefaultTrackSelector-specific fluent calls above.
                .setAudioOffloadPreferences(
                    androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(
                            androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                        )
                        .build()
                )
                .build()
        )

        // Custom load control for music streaming (50s buffer & robust 2.5s playback start for heavy DSP)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000, // minBufferMs (50s buffer for smooth background playback)
                100000, // maxBufferMs (100s buffer cap)
                2500,  // bufferForPlaybackMs (2.5s buffer for rock-solid DSP convolution startup)
                5000   // bufferForPlaybackAfterRebufferMs (5s buffer after rebuffering)
            )
            .setBackBuffer(
                15000, // backBufferDurationMs (15s back buffer for instant scrubbing/seeking)
                /* retainBackBufferFromKeyframe = */ true
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val stableSessionId = audioManager.generateAudioSessionId()

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("com.google.android.youtube/20.10.33 (Linux; U; Android 12)")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive"
            ))
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Enable DRM (including L1 Widevine) support
        val drmSessionManagerProvider = androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(httpDataSourceFactory)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)

        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            
        // Enable detailed logging for debugging DRM and playback issues
        player.addAnalyticsListener(androidx.media3.exoplayer.util.EventLogger())
        // Register global WebViewCookieHandler to delegate JVM-level requests to Android WebView's CookieManager
        try {
            CookieHandler.setDefault(WebViewCookieHandler())
            android.util.Log.i("PlayerModule", "Registered global WebViewCookieHandler for ExoPlayer cookies.")
        } catch (e: Exception) {
            android.util.Log.e("PlayerModule", "Failed to set default CookieHandler: ${e.message}")
        }

        player.setAudioSessionId(stableSessionId)
        return player
    }
}

class WebViewCookieHandler : CookieHandler() {
    private val webviewCookieManager = CookieManager.getInstance()

    override fun get(
        uri: URI?,
        requestHeaders: MutableMap<String, MutableList<String>>?
    ): MutableMap<String, MutableList<String>> {
        val headers = HashMap<String, MutableList<String>>()
        val url = uri?.toString() ?: return headers
        // Do not attach YouTube web cookies to direct googlevideo.com streams as they can invalidate app client signatures
        if (uri?.host?.contains("googlevideo.com") == true) {
            return headers
        }
        val cookies = webviewCookieManager.getCookie(url)
        if (!cookies.isNullOrEmpty()) {
            headers["Cookie"] = mutableListOf(cookies)
        }
        return headers
    }

    override fun put(
        uri: URI?,
        responseHeaders: MutableMap<String, MutableList<String>>?
    ) {
        val url = uri?.toString() ?: return
        val cookiesList = responseHeaders?.get("Set-Cookie") ?: responseHeaders?.get("set-cookie")
        if (cookiesList != null) {
            for (cookie in cookiesList) {
                webviewCookieManager.setCookie(url, cookie)
            }
        }
    }
}
