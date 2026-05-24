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
    autoPlay: Boolean = true
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
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
                webChromeClient = WebChromeClient()
                setBackgroundColor(android.graphics.Color.BLACK)

                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    buildInlineEmbedHtml(videoId, autoPlay),
                    "text/html", "UTF-8", null
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
                        "text/html", "UTF-8", null
                    )
                }
            } ?: run {
                webView.tag = videoId
            }
        }
    )
}

private fun buildInlineEmbedHtml(
    videoId: String,
    autoPlay: Boolean
): String = """
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
  src="https://www.youtube.com/embed/$videoId?autoplay=${if (autoPlay) 1 else 0}&mute=0&controls=0&modestbranding=1&playsinline=1&rel=0&showinfo=0&iv_load_policy=3"
  allow="autoplay; encrypted-media"
  allowfullscreen>
</iframe>
</body>
</html>
""".trimIndent()
