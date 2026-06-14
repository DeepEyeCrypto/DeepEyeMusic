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
        vocalRemoverProcessor: com.deepeye.musicpro.dsp.processor.VocalRemoverProcessor,
        crossfeedProcessor: com.deepeye.musicpro.dsp.processor.CrossfeedProcessor
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
                    .setAudioProcessors(arrayOf(vocalRemoverProcessor, crossfeedProcessor))
                    .build()
            }
        }
        renderersFactory.setExtensionRendererMode(
            androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        ).setEnableDecoderFallback(true)

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setPreferredVideoMimeTypes("video/av01", "video/vp9") // Prefer AV1 via dav1d
                .setTunnelingEnabled(true)
        )

        // Custom load control for music streaming (low latency startup & low sync lag)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000, // minBufferMs
                100000, // maxBufferMs
                1500,  // bufferForPlaybackMs
                2500   // bufferForPlaybackAfterRebufferMs
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val stableSessionId = audioManager.generateAudioSessionId()

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://m.youtube.com/",
                "Origin" to "https://m.youtube.com",
                "Accept" to "*/*",
                "sec-ch-ua" to "\"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"114\", \"Android WebView\";v=\"114\"",
                "sec-ch-ua-mobile" to "?1",
                "sec-ch-ua-platform" to "\"Android\""
            ))
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
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
        var cookies = webviewCookieManager.getCookie(url)

        // Map .youtube.com cookies dynamically to .googlevideo.com requests
        if (uri?.host?.contains("googlevideo.com") == true) {
            val ytCookies = webviewCookieManager.getCookie("https://m.youtube.com")
            if (!ytCookies.isNullOrEmpty()) {
                cookies = if (cookies.isNullOrEmpty()) ytCookies else "$cookies; $ytCookies"
                android.util.Log.d("WebViewCookieHandler", "Mapped .youtube.com cookies to googlevideo.com request")
            }
        }

        android.util.Log.d("WebViewCookieHandler", "Request URL: $url, Found cookies: $cookies")
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
