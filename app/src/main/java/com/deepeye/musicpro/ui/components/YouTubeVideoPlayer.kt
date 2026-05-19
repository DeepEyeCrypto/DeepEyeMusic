package com.deepeye.musicpro.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
    playbackPosition: Long = 0L,
    playbackSpeed: Float = 1.0f,
    isMuted: Boolean = false,
    seekTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val isAppInForegroundState = remember { androidx.compose.runtime.mutableStateOf(true) }
    
    androidx.compose.runtime.LaunchedEffect(videoId) {
        android.util.Log.e("YouTubeVideoPlayer", "LaunchedEffect triggered - videoId: $videoId, isPlaying: $isPlaying")
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                isAppInForegroundState.value = true
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                isAppInForegroundState.value = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val webView = remember {
        BackgroundPlayWebView(context).apply {
            // EXPLICIT LAYOUT PARAMS: Prevents Compose from measuring this native view as 0x0
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Enable hardware acceleration for smooth video rendering
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("YouTubeWebViewConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                    return true
                }
            }
            
            webViewClient = object : WebViewClient() {
                // Brave-style Aggressive Ad & Tracker Filter List
                private val braveShieldsBlocklist = listOf(
                    "doubleclick.net",
                    "googleadservices.com",
                    "google-analytics.com",
                    "/api/stats/ads",
                    "/pagead/",
                    "youtubei/v1/log_event",
                    "youtube.com/api/stats/qoe",
                    "youtube.com/ptracking",
                    "youtube.com/error_204",
                    "play.google.com/log"
                )

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    
                    // Brave Network-Level Shield: Block Ads, Telemetry, and Fingerprinting
                    if (braveShieldsBlocklist.any { url.contains(it) }) {
                        android.util.Log.d("BraveShields", "Shields UP: Blocked $url")
                        // Return empty response to kill the ad/tracker instantly
                        return android.webkit.WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    android.util.Log.e("YouTubeVideoPlayer", "WebView Error for ${request?.url}: ${error?.description} (code: ${error?.errorCode})")
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    android.util.Log.e("YouTubeVideoPlayer", "WebView HTTP Error for ${request?.url}: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}")
                }

                override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                    android.util.Log.e("YouTubeVideoPlayer", "WebView SSL Error: $error")
                    handler?.proceed() // Proceed to bypass local proxy issues if they exist
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Brave Cosmetic Filter Engine: Inject CSS/JS to vaporize remaining ad containers
                    val braveCosmeticJS = """
                        javascript:(function() {
                            const style = document.createElement('style');
                            style.innerHTML = `
                                .ytp-ad-module, .ytp-ad-overlay, .yt-ad-slot, .ytd-ad-slot-renderer,
                                .ytd-in-feed-ad-layout-renderer, .ytd-banner-promo-renderer,
                                .ad-showing, .ad-interrupting {
                                    display: none !important;
                                    opacity: 0 !important;
                                    pointer-events: none !important;
                                }
                            `;
                            document.head.appendChild(style);
                            
                            // Auto-skip logic for unkillable in-stream ads
                            setInterval(() => {
                                const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                                if (skipBtn) skipBtn.click();
                                
                                const adOverlay = document.querySelector('.ytp-ad-overlay-close-button');
                                if (adOverlay) adOverlay.click();
                            }, 500);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(braveCosmeticJS, null)
                }
            }
            
            // Allow third-party cookies for player core, but restrict privacy settings
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Brave Privacy Enhancements
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // Force HTTPS
                // Appending Brave to User Agent for potential server-side ad drops
                userAgentString = userAgentString + " Brave/1.61.109"
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            } catch (e: Exception) {
                android.util.Log.e("YouTubeVideoPlayer", "Error destroying WebView", e)
            }
        }
    }

    AndroidView(
        factory = { webView },
        update = { webView ->
            val state = webView.tag as? YouTubePlayerState ?: YouTubePlayerState().also { webView.tag = it }
            
            if (state.videoId == null) {
                // First load: load the HTML containing the iframe player
                state.videoId = videoId
                
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
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            }
                            iframe {
                                width: 100%;
                                height: 100%;
                                border: none;
                            }
                            /* Premium SmartTube HUD skip banner */
                            #skip-hud {
                                position: absolute;
                                top: 12px;
                                left: 50%;
                                transform: translateX(-50%) translateY(-60px);
                                background: rgba(0, 0, 0, 0.88);
                                border: 1.5px solid #00E676;
                                color: #FFFFFF;
                                padding: 8px 16px;
                                border-radius: 20px;
                                font-size: 11px;
                                font-weight: bold;
                                display: flex;
                                align-items: center;
                                gap: 6px;
                                transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.4s;
                                opacity: 0;
                                z-index: 99999;
                                box-shadow: 0 4px 20px rgba(0, 230, 118, 0.4);
                                white-space: nowrap;
                            }
                            #skip-hud.visible {
                                transform: translateX(-50%) translateY(0);
                                opacity: 1;
                            }
                        </style>
                    </head>
                    <body>
                        <div id="skip-hud">
                            <span style="color: #00E676; font-size: 13px;">🛡️</span> SmartTube Shield: Sponsor/Intro skipped!
                        </div>
                        <iframe 
                            id="player"
                            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=1&controls=0&playsinline=1&enablejsapi=1&origin=https://www.youtube-nocookie.com" 
                            referrerpolicy="strict-origin-when-cross-origin"
                            allow="autoplay; encrypted-media" 
                            allowfullscreen>
                        </iframe>
                        <script>
                            // Force Page Visibility to always be visible to bypass background auto-pausing!
                            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                            Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                            document.addEventListener('visibilitychange', function(e) {
                                e.stopImmediatePropagation();
                            }, true);

                            var currentTime = 0;
                            var skipSegments = [];
                            var currentVideoId = "$videoId";
                            
                            function fetchSponsorSegments(vid) {
                                var url = 'https://sponsor.ajay.app/api/skipSegments?videoID=' + vid + '&categories=["sponsor","selfpromo","interaction","intro","outro","preview"]';
                                fetch(url)
                                    .then(function(res) {
                                        if (res.status === 200) {
                                            return res.json();
                                        }
                                        return [];
                                    })
                                    .then(function(data) {
                                        skipSegments = data.map(function(item) {
                                            return {
                                                start: item.segment[0],
                                                end: item.segment[1],
                                                category: item.category
                                            };
                                        });
                                    })
                                    .catch(function(e) {
                                        skipSegments = [];
                                    });
                            }
                            
                            // Initialize segments
                            fetchSponsorSegments(currentVideoId);
                            
                            var isUserMuted = false;
                            var unmuteAttempts = 0;
                            var unmuteInterval = null;

                            function startUnmuteDaemon() {
                                if (unmuteInterval) {
                                    clearInterval(unmuteInterval);
                                }
                                unmuteAttempts = 0;
                                unmuteInterval = setInterval(function() {
                                    if (isUserMuted) {
                                        clearInterval(unmuteInterval);
                                        return;
                                    }
                                    var iframe = document.getElementById('player');
                                    if (iframe && iframe.contentWindow) {
                                        iframe.contentWindow.postMessage(JSON.stringify({
                                            "event": "command",
                                            "func": "unMute",
                                            "args": ""
                                        }), "*");
                                    }
                                    unmuteAttempts++;
                                    if (unmuteAttempts > 20) { // Stop after 10 seconds of active playback attempts to save resources
                                        clearInterval(unmuteInterval);
                                    }
                                }, 500);
                            }

                            // Start unmuting loop immediately on page load
                            startUnmuteDaemon();

                            window.addEventListener('message', function(event) {
                                try {
                                    var data = JSON.parse(event.data);
                                    if (data.event === 'infoDelivery' && data.info && data.info.currentTime !== undefined) {
                                        currentTime = data.info.currentTime;
                                        checkAndSkipSegments(currentTime);
                                    }
                                } catch(e) {}
                            });
                            
                            function checkAndSkipSegments(time) {
                                for (var i = 0; i < skipSegments.length; i++) {
                                    var seg = skipSegments[i];
                                    if (time >= seg.start && time < seg.end) {
                                        currentTime = seg.end;
                                        var iframe = document.getElementById('player');
                                        if (iframe && iframe.contentWindow) {
                                            iframe.contentWindow.postMessage(JSON.stringify({
                                                "event": "command",
                                                "func": "seekTo",
                                                "args": [seg.end, true]
                                            }), "*");
                                        }
                                        showSkipNotification();
                                        break;
                                    }
                                }
                            }
                            
                            function showSkipNotification() {
                                var hud = document.getElementById('skip-hud');
                                if (hud) {
                                    hud.classList.add('visible');
                                    setTimeout(function() {
                                        hud.classList.remove('visible');
                                    }, 3000);
                                }
                            }
                            
                            function playVideo() {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    iframe.contentWindow.postMessage(JSON.stringify({"event": "command", "func": "playVideo", "args": ""}), "*");
                                }
                            }
                            var lastPlayState = true;
                            function pauseVideo() {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    iframe.contentWindow.postMessage(JSON.stringify({"event": "command", "func": "pauseVideo", "args": ""}), "*");
                                }
                            }
                            function loadVideo(newVideoId) {
                                currentVideoId = newVideoId;
                                skipSegments = [];
                                fetchSponsorSegments(newVideoId);
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    // Load new video in muted state to bypass autoplay blocks
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "loadVideoById",
                                        "args": [newVideoId, 0]
                                    }), "*");
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "mute",
                                        "args": ""
                                    }), "*");
                                    
                                    // Re-trigger the unmuting daemon loop for the new video!
                                    startUnmuteDaemon();
                                }
                            }
                            function seekForward() {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    currentTime += 10;
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "seekTo",
                                        "args": [currentTime, true]
                                    }), "*");
                                }
                            }
                            function seekBackward() {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    currentTime = Math.max(0, currentTime - 10);
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "seekTo",
                                        "args": [currentTime, true]
                                    }), "*");
                                }
                            }
                            function setSpeed(rate) {
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "setPlaybackRate",
                                        "args": [rate]
                                    }), "*");
                                }
                            }
                            function setMute(isMuted) {
                                isUserMuted = isMuted;
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    if (isMuted) {
                                        if (unmuteInterval) {
                                            clearInterval(unmuteInterval);
                                        }
                                        iframe.contentWindow.postMessage(JSON.stringify({
                                            "event": "command",
                                            "func": "mute",
                                            "args": ""
                                        }), "*");
                                    } else {
                                        iframe.contentWindow.postMessage(JSON.stringify({
                                            "event": "command",
                                            "func": "unMute",
                                            "args": ""
                                        }), "*");
                                    }
                                }
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                
                webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
            } else if (state.videoId != videoId) {
                // Subsequent load: transition video instantly via postMessage instead of reloading page!
                state.videoId = videoId
                webView.evaluateJavascript("if (typeof loadVideo === 'function') loadVideo('$videoId');", null)
            }
            
            // Sync play/pause state
            if (isPlaying) {
                webView.evaluateJavascript("if (typeof playVideo === 'function') playVideo();", null)
            } else {
                webView.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
            }
            
            // Sync speed state
            webView.evaluateJavascript("if (typeof setSpeed === 'function') setSpeed($playbackSpeed);", null)
            
            // Sync mute state
            val finalMuteState = isMuted || !isAppInForegroundState.value
            webView.evaluateJavascript("if (typeof setMute === 'function') setMute($finalMuteState);", null)
            
            // Sync seek triggers
            if (seekTrigger != 0 && seekTrigger != state.prevSeekTrigger) {
                val prev = state.prevSeekTrigger
                state.prevSeekTrigger = seekTrigger
                if (seekTrigger > prev) {
                    webView.evaluateJavascript("if (typeof seekForward === 'function') seekForward();", null)
                } else if (seekTrigger < prev) {
                    webView.evaluateJavascript("if (typeof seekBackward === 'function') seekBackward();", null)
                }
            }

            // Sync playback position (e.g. if the user seeked manually via slider or lockscreen)
            val diff = Math.abs(playbackPosition - state.lastSentPosition)
            if (state.lastSentPosition == -1L || diff > 2500L) { // If it drifted by more than 2.5 seconds (manual seek)
                state.lastSentPosition = playbackPosition
                val seconds = playbackPosition / 1000f
                webView.evaluateJavascript("if (document.getElementById('player') && document.getElementById('player').contentWindow) { document.getElementById('player').contentWindow.postMessage(JSON.stringify({\"event\": \"command\", \"func\": \"seekTo\", \"args\": [$seconds, true]}), \"*\"); }", null)
            } else {
                state.lastSentPosition = playbackPosition
            }
        },
        modifier = modifier
    )
}

@SuppressLint("ViewConstructor")
private class BackgroundPlayWebView(context: android.content.Context) : android.webkit.WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        // Keep the WebView thinking it is always visible unless it is completely GONE (which indicates destruction)
        if (visibility != android.view.View.GONE) {
            super.onWindowVisibilityChanged(android.view.View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }
}

private class YouTubePlayerState(
    var videoId: String? = null,
    var prevSeekTrigger: Int = 0,
    var lastSentPosition: Long = -1L
)
