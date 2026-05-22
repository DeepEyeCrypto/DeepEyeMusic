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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A highly robust, compatible YouTube Video Player using the official
 * YouTube Nocookie Embed API loaded directly inside an Android WebView.
 *
 * @param muteWebViewAudio When true, forces the WebView/iframe audio to be muted.
 *        Use this when ExoPlayer is handling audio playback and the WebView
 *        should only render video output. This prevents dual-audio conflicts.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeVideoPlayer(
    webView: WebView,
    videoId: String,
    isPlaying: Boolean,
    playbackPosition: Long = 0L,
    playbackSpeed: Float = 1.0f,
    isMuted: Boolean = false,
    seekTrigger: Int = 0,
    muteWebViewAudio: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val isAppInForegroundState = remember { androidx.compose.runtime.mutableStateOf(true) }
    
    androidx.compose.runtime.LaunchedEffect(videoId) {
        android.util.Log.e("YouTubeVideoPlayer", "LaunchedEffect triggered - videoId: $videoId, isPlaying: $isPlaying, muteWebViewAudio: $muteWebViewAudio")
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

        val state = webView.tag as? YouTubePlayerState ?: YouTubePlayerState().also { webView.tag = it }
        val hasNetworkError by state.networkError.collectAsState()

        Box(modifier = modifier) {
            AndroidView(
                factory = {
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView.layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webView
                },
                update = { webView ->
                    webView.layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val updateState = webView.tag as? YouTubePlayerState ?: YouTubePlayerState().also { webView.tag = it }
                    
                    // 1. Initial html load vs subsequent loadVideo
                    if (updateState.videoId == null) {
                        updateState.videoId = videoId
                        updateState.networkError.value = false
                        val startSec = (playbackPosition / 1000).toInt()
                        val htmlContent = getYouTubeHtmlTemplate(videoId, startSec, muteWebViewAudio)
                        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
                    } else if (updateState.videoId != videoId) {
                        updateState.videoId = videoId
                        updateState.networkError.value = false
                        webView.evaluateJavascript("if (typeof loadVideo === 'function') loadVideo('$videoId');", null)
                    }
                    
                    // 2. Play/Pause state sync
                    if (isPlaying) {
                        webView.evaluateJavascript("if (typeof playVideo === 'function') playVideo();", null)
                    } else {
                        webView.evaluateJavascript("if (typeof pauseVideo === 'function') pauseVideo();", null)
                    }
                    
                    // 3. Sync speed state (local UI driven speed select)
                    webView.evaluateJavascript("if (typeof setSpeed === 'function') setSpeed($playbackSpeed);", null)
                    
                    // 4. Sync mute state (local UI driven mute toggle)
                    webView.evaluateJavascript("if (typeof setMute === 'function') setMute($isMuted);", null)
                    
                    // 5. Mute WebView audio if muteWebViewAudio is true
                    if (muteWebViewAudio) {
                        webView.evaluateJavascript("if (typeof setWebViewMuted === 'function') setWebViewMuted(true);", null)
                    }
                    
                    // 6. Sync seek triggers (local double-tap gesture seek)
                    if (seekTrigger != 0 && seekTrigger != updateState.prevSeekTrigger) {
                        val prev = updateState.prevSeekTrigger
                        updateState.prevSeekTrigger = seekTrigger
                        if (seekTrigger > prev) {
                            webView.evaluateJavascript("if (typeof seekForward === 'function') seekForward();", null)
                        } else if (seekTrigger < prev) {
                            webView.evaluateJavascript("if (typeof seekBackward === 'function') seekBackward();", null)
                        }
                    }
                },
        modifier = Modifier.fillMaxSize()
    )

    if (hasNetworkError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable {
                    // Retry logic: clear error and force reload by wiping state
                    state.networkError.value = false
                    state.videoId = null
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = "No Internet",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Network Offline",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to retry",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun createYouTubeWebView(context: android.content.Context): WebView {
    return BackgroundPlayWebView(context).apply {
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
                
                val state = view?.tag as? YouTubePlayerState
                if (error?.errorCode == android.webkit.WebViewClient.ERROR_HOST_LOOKUP || 
                    error?.errorCode == android.webkit.WebViewClient.ERROR_CONNECT || 
                    error?.errorCode == android.webkit.WebViewClient.ERROR_TIMEOUT) {
                    state?.networkError?.value = true
                }
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
                        style.textContent = `
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
            
            // Allow same-origin access in iframe
            allowFileAccess = true
            allowContentAccess = true
            setGeolocationEnabled(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // Force HTTPS
            // Appending Brave to User Agent for potential server-side ad drops
            userAgentString = userAgentString + " Brave/1.61.109"
        }
    }
}

@SuppressLint("ViewConstructor")
internal class BackgroundPlayWebView(context: android.content.Context) : android.webkit.WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        // Enforce VISIBLE visibility state to bypass Chromium background auto-pause/suspend
        super.onWindowVisibilityChanged(android.view.View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        // Enforce VISIBLE visibility state to bypass Chromium background auto-pause/suspend
        super.onVisibilityChanged(changedView, android.view.View.VISIBLE)
    }
}

class YouTubePlayerState(
    var videoId: String? = null,
    var prevSeekTrigger: Int = 0,
    var lastSentPosition: Long = -1L,
    val networkError: kotlinx.coroutines.flow.MutableStateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
)

/**
 * Reusable helper function to generate the HTML template loaded in YouTube WebView.
 */
fun getYouTubeHtmlTemplate(videoId: String, startSec: Int, muteWebViewAudio: Boolean = false): String {
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
                    position: absolute;
                    top: 0;
                    left: 0;
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
                width="100%"
                height="100%"
                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=0&playsinline=1&enablejsapi=1&start=$startSec&origin=https://www.youtube-nocookie.com" 
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

                var currentTime = $startSec;
                var skipSegments = [];
                var currentVideoId = "$videoId";
                
                var IS_WEBVIEW_MUTED = MUTED_TOKEN_PLACEHOLDER;
                
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
                var isWebViewMuted = false;

                function setWebViewMuted(muted) {
                    isWebViewMuted = muted;
                    var iframe = document.getElementById('player');
                    if (iframe && iframe.contentWindow) {
                        if (muted) {
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "mute",
                                "args": ""
                            }), "*");
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "setVolume",
                                "args": [0]
                            }), "*");
                        } else {
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "unMute",
                                "args": ""
                            }), "*");
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "setVolume",
                                "args": [100]
                            }), "*");
                        }
                    }
                }

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
                        if (isWebViewMuted) {
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

                // If this WebView should only render video (audio handled by ExoPlayer), mute immediately!
                if (IS_WEBVIEW_MUTED) {
                    setWebViewMuted(true);
                }

                // Start unmuting loop immediately on page load
                startUnmuteDaemon();

                // Zoom to fit video via object-fit cover equivalent
                function applyZoomToFit() {
                    try {
                        var iframe = document.getElementById('player');
                        if (iframe) {
                            var w = window.innerWidth;
                            var h = window.innerHeight;
                            var containerRatio = w / h;
                            var videoRatio = 16 / 9;
                            
                            if (containerRatio > videoRatio) {
                                // Container is wider than 16:9 -> fit width, overflow height (crop top/bottom)
                                var iframeWidth = w;
                                var iframeHeight = w / videoRatio;
                                iframe.style.width = iframeWidth + 'px';
                                iframe.style.height = iframeHeight + 'px';
                                iframe.style.left = '0px';
                                iframe.style.top = ((h - iframeHeight) / 2) + 'px';
                                iframe.style.transform = 'none';
                            } else {
                                // Container is taller than 16:9 -> fit height, overflow width (crop left/right)
                                var iframeHeight = h;
                                var iframeWidth = h * videoRatio;
                                iframe.style.height = iframeHeight + 'px';
                                iframe.style.width = iframeWidth + 'px';
                                iframe.style.top = '0px';
                                iframe.style.left = ((w - iframeWidth) / 2) + 'px';
                                iframe.style.transform = 'none';
                            }
                        }
                    } catch(e) {}
                }
                setInterval(applyZoomToFit, 500);

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
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "setVolume",
                                "args": [0]
                            }), "*");
                        } else {
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "unMute",
                                "args": ""
                            }), "*");
                            iframe.contentWindow.postMessage(JSON.stringify({
                                "event": "command",
                                "func": "setVolume",
                                "args": [100]
                            }), "*");
                            startUnmuteDaemon();
                        }
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    val finalHtml = if (muteWebViewAudio) {
        htmlContent.replace("MUTED_TOKEN_PLACEHOLDER", "true")
    } else {
        htmlContent.replace("MUTED_TOKEN_PLACEHOLDER", "false")
    }
    return finalHtml
}