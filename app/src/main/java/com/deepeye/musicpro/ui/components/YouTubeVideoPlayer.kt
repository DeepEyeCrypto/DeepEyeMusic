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
                
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    
                    // Set a modern mobile Chrome User-Agent matching the Motorola device
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; motorola edge 30 pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
            }
        },
        update = { webView ->
            // Check if we need to load a new video
            if (webView.tag != videoId) {
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
                            #player {
                                width: 100%;
                                height: 100%;
                            }
                        </style>
                    </head>
                    <body>
                        <div id="player"></div>
                        <script>
                            var tag = document.createElement('script');
                            tag.src = "https://www.youtube.com/iframe_api";
                            var firstScriptTag = document.getElementsByTagName('script')[0];
                            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                            var player;
                            function onYouTubeIframeAPIReady() {
                                player = new YT.Player('player', {
                                    videoId: '$videoId',
                                    playerVars: {
                                        'autoplay': 1,
                                        'mute': 1,
                                        'controls': 0,
                                        'playsinline': 1,
                                        'rel': 0,
                                        'showinfo': 0,
                                        'modestbranding': 1,
                                        'origin': 'https://www.youtube.com'
                                    },
                                    events: {
                                        'onReady': function(event) {
                                            event.target.playVideo();
                                            // Sync initial play state
                                            if (${isPlaying}) {
                                                event.target.playVideo();
                                            } else {
                                                event.target.pauseVideo();
                                            }
                                        }
                                    }
                                });
                            }

                            function playVideo() {
                                if (player && typeof player.playVideo === 'function') {
                                    player.playVideo();
                                }
                            }

                            function pauseVideo() {
                                if (player && typeof player.pauseVideo === 'function') {
                                    player.pauseVideo();
                                }
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                
                webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            } else {
                // Sync play/pause state
                if (isPlaying) {
                    webView.evaluateJavascript("if (typeof playVideo === 'function') playVideo();", null)
                } else {
                    webView.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
                }
            }
        },
        modifier = modifier
    )
}
