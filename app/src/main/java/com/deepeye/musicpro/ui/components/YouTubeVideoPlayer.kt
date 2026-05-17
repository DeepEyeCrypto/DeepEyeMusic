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

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // Automated Playback Automation Injector
                        // Wait 300ms for iframe DOM creation, then trigger play programmatically
                        view?.postDelayed({
                            view.evaluateJavascript(
                                """
                                (function() {
                                    console.log("Automating Hybrid YouTube Playback...");
                                    var video = document.querySelector('video');
                                    if (video) {
                                        video.muted = true; // Hard-enforce muted autoplay bypass
                                        video.play();
                                        console.log("Muted HTML5 Video Play Called!");
                                    }
                                })();
                                """.trimIndent(),
                                null
                            )
                        }, 300)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        android.util.Log.e("YouTubeVideoPlayer", "WebView error: $errorCode - $description for $failingUrl")
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
                
                // Use Google's official privacy-friendly nocookie domain + enablejsapi to bypass consent walls
                // We load muted=1 and controls=0 for a clean, custom design!
                val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=1&controls=0&playsinline=1&enablejsapi=1"
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            // Sync play/pause state of the video with ExoPlayer reactively!
            if (isPlaying) {
                webView.evaluateJavascript(
                    """
                    (function() {
                        var video = document.querySelector('video');
                        if (video) {
                            video.play();
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            } else {
                webView.evaluateJavascript(
                    """
                    (function() {
                        var video = document.querySelector('video');
                        if (video) {
                            video.pause();
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            }
        },
        modifier = modifier
    )
}
