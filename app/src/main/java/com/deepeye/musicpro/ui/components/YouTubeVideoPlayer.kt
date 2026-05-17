package com.deepeye.musicpro.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A highly robust, compatible YouTube Video Player using the official
 * YouTube Nocookie Embed API loaded directly inside an Android WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeVideoPlayer(
    videoId: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // EXPLICIT LAYOUT PARAMS: Prevents Compose from measuring this native view as 0x0
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enable hardware acceleration for smooth video rendering
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                webChromeClient = WebChromeClient()
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false // Keep navigation inside the WebView container
                    }
                }
                
                // Allow third-party cookies so that YouTube's verification scripts can validate embedding
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }
        },
        update = { webView ->
            val prevVideoId = webView.tag as? String
            if (prevVideoId == null) {
                // First load: load the HTML containing the iframe player
                webView.tag = videoId
                
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                width: 100%;
                                height: 100%;
                                background-color: #000000;
                                overflow: hidden;
                            }
                            iframe {
                                width: 100%;
                                height: 100%;
                                border: none;
                            }
                        </style>
                    </head>
                    <body>
                        <iframe 
                            id="player"
                            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=1&controls=0&playsinline=1&enablejsapi=1&origin=https://www.youtube-nocookie.com" 
                            referrerpolicy="strict-origin-when-cross-origin"
                            allow="autoplay; encrypted-media" 
                            allowfullscreen>
                        </iframe>
                        <script>
                            function playVideo() {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    iframe.contentWindow.postMessage(JSON.stringify({"event": "command", "func": "playVideo", "args": ""}), "*");
                                }
                            }
                            function pauseVideo() {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    iframe.contentWindow.postMessage(JSON.stringify({"event": "command", "func": "pauseVideo", "args": ""}), "*");
                                }
                            }
                            function loadVideo(newVideoId) {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "loadVideoById",
                                        "args": [newVideoId, 0]
                                    }), "*");
                                }
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                
                webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
            } else if (prevVideoId != videoId) {
                // Subsequent load: transition video instantly via postMessage instead of reloading page!
                webView.tag = videoId
                webView.evaluateJavascript("if (typeof loadVideo === 'function') loadVideo('$videoId');", null)
            }
            
            // Sync play/pause state
            if (isPlaying) {
                webView.evaluateJavascript("if (typeof playVideo === 'function') playVideo();", null)
            } else {
                webView.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
            }
        },
        modifier = modifier
    )
}
