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
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        okHttpClient: okhttp3.OkHttpClient,
        vocalRemoverProcessor: com.deepeye.musicpro.dsp.processor.VocalRemoverProcessor,
        crossfeedProcessor: com.deepeye.musicpro.dsp.processor.CrossfeedProcessor,
        tubeSimulatorProcessor: com.deepeye.musicpro.dsp.processor.TubeSimulatorProcessor,
        lufsAnalyzerProcessor: com.deepeye.musicpro.dsp.processor.LufsAnalyzerProcessor
    ): ExoPlayer {
        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(vocalRemoverProcessor, crossfeedProcessor, tubeSimulatorProcessor, lufsAnalyzerProcessor))
                    .build()
            }
        }
        renderersFactory.setExtensionRendererMode(
            androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        ).setEnableDecoderFallback(true)

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setPreferredVideoMimeTypes("video/av01", "video/vp9") // Prefer AV1/VP9 for highest quality
                .setTunnelingEnabled(false) // Tunneling breaks AudioProcessors and causes video failure on many devices
                .setForceHighestSupportedBitrate(false) // Don't force highest, let ExoPlayer adapt to network and device decoding limits
                .setAllowVideoNonSeamlessAdaptiveness(true)
        )

        // Custom load control for music streaming (ultra-low latency startup & responsive playback)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs (15s buffer for smooth background playback)
                50000, // maxBufferMs (50s buffer cap)
                250,   // bufferForPlaybackMs (Only 250ms buffer needed to start playback instantly!)
                1000   // bufferForPlaybackAfterRebufferMs (1s buffer after rebuffering)
            )
            .setBackBuffer(
                10000, // backBufferDurationMs (10s back buffer for instant scrubbing/seeking)
                /* retainBackBufferFromKeyframe = */ true
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val stableSessionId = audioManager.generateAudioSessionId()

        val playerOkHttpClient = okHttpClient.newBuilder()
            .cache(null) // Disable HTTP disk cache for streaming to avoid 206 caching conflicts and stale conditional headers
            .addInterceptor { chain ->
                var request = chain.request()
                val urlStr = request.url.toString()
                if (urlStr.contains("googlevideo.com")) {
                    val isIos = urlStr.contains("c=IOS") || urlStr.contains("c=IPHONE")
                    val isWeb = urlStr.contains("c=WEB")
                    val reqBuilder = request.newBuilder()
                    if (isIos) {
                        reqBuilder
                            .header("User-Agent", "com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)")
                            .removeHeader("Origin")
                            .removeHeader("Referer")
                    } else if (isWeb) {
                        reqBuilder
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                            .header("Origin", "https://www.youtube.com")
                            .header("Referer", "https://www.youtube.com/")
                    } else {
                        reqBuilder
                            .header("User-Agent", "com.google.android.youtube/20.10.33 (Linux; U; Android 12) gzip")
                            .removeHeader("Origin")
                            .removeHeader("Referer")
                    }
                    request = reqBuilder.build()
                }
                val response = chain.proceed(request)
                if (!response.isSuccessful) {
                    val errorBody = try { response.peekBody(2048).string() } catch (e: Exception) { "unavailable: ${e.message}" }
                    android.util.Log.e("PlayerOkHttp", "Media stream request failed: HTTP ${response.code} ${response.message}\nReq headers: ${request.headers}\nResp headers: ${response.headers}\nBody: $errorBody\n[URL: ${request.url}]")
                }
                response
            }
            .build()

        val httpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(playerOkHttpClient)
            .setUserAgent("com.google.android.youtube/20.10.33 (Linux; U; Android 12) gzip")
            .setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive",
            ))
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Enable DRM (including L1 Widevine) support
        val drmSessionManagerProvider = androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(httpDataSourceFactory)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
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
