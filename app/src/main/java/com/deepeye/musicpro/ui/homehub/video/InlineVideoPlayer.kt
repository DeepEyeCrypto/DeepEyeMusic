// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub.video

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InlineVideoPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = !autoPlay
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                        val url = uri.toString()
                        val host = uri.host ?: ""
                        val path = uri.path ?: ""

                        if (url.contains("ad_status.js")) {
                            return super.shouldInterceptRequest(view, request)
                        }

                        if (host.contains("doubleclick.net") ||
                            host.contains("googleads") ||
                            host.contains("pagead") ||
                            host.contains("googlesyndication.com") ||
                            host.contains("adservice.google") ||
                            host.contains("google-analytics.com") ||
                            host.contains("analytics") ||
                            host.contains("telemetry") ||
                            path.contains("/log") ||
                            path.contains("log_event") ||
                            path.contains("generate_204") ||
                            path.contains("error_204") ||
                            path.contains("ptracking") ||
                            url.contains("s.youtube.com") ||
                            url.contains("googlevideo.com/api/stats")
                        ) {
                            android.util.Log.d("InlineVideoPlayer", "🚫 Blocked Ad/Telemetry Request: $url")
                            return android.webkit.WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                webChromeClient = WebChromeClient()
                setBackgroundColor(android.graphics.Color.BLACK)

                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    buildInlineEmbedHtml(videoId, autoPlay),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        update = { webView ->
            // Only reload if videoId changes
            webView.tag?.let { tag ->
                if (tag as? String != videoId) {
                    webView.tag = videoId
                    webView.loadDataWithBaseURL(
                        "https://www.youtube.com",
                        buildInlineEmbedHtml(videoId, autoPlay),
                        "text/html", "UTF-8", null,
                    )
                }
            } ?: run {
                webView.tag = videoId
            }
        },
    )
}

private fun buildInlineEmbedHtml(
    videoId: String,
    autoPlay: Boolean,
): String =
    """
    <!DOCTYPE html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
    <style>
      * { margin:0; padding:0; box-sizing:border-box; }
      html, body { width:100%; height:100%; background:#000; overflow:hidden; }
      iframe {
        width:100%; height:100%;
        border:none; display:block;
      }
    </style>
    </head>
    <body>
    <iframe
      src="https://www.youtube.com/embed/$videoId?autoplay=${if (autoPlay) 1 else 0}&mute=1&controls=0&modestbranding=1&playsinline=1&rel=0&showinfo=0&iv_load_policy=3"
      allow="autoplay; encrypted-media"
      allowfullscreen>
    </iframe>
    </body>
    </html>
    """.trimIndent()
