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
    playbackSpeed: Float = 1.0f,
    isMuted: Boolean = false,
    seekTrigger: Int = 0,
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
                            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=0&controls=0&playsinline=1&enablejsapi=1&origin=https://www.youtube-nocookie.com" 
                            referrerpolicy="strict-origin-when-cross-origin"
                            allow="autoplay; encrypted-media" 
                            allowfullscreen>
                        </iframe>
                        <script>
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
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": "loadVideoById",
                                        "args": [newVideoId, 0]
                                    }), "*");
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
                                var iframe = document.getElementById('player');
                                if (iframe && iframe.contentWindow) {
                                    var funcName = isMuted ? "mute" : "unMute";
                                    iframe.contentWindow.postMessage(JSON.stringify({
                                        "event": "command",
                                        "func": funcName,
                                        "args": ""
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
            
            // Sync speed state
            webView.evaluateJavascript("if (typeof setSpeed === 'function') setSpeed($playbackSpeed);", null)
            
            // Sync mute state
            webView.evaluateJavascript("if (typeof setMute === 'function') setMute($isMuted);", null)
            
            // Sync seek triggers
            val prevSeekTrigger = webView.getTag(1001) as? Int ?: 0
            if (seekTrigger != 0 && seekTrigger != prevSeekTrigger) {
                webView.setTag(1001, seekTrigger)
                if (seekTrigger > prevSeekTrigger) {
                    webView.evaluateJavascript("if (typeof seekForward === 'function') seekForward();", null)
                } else if (seekTrigger < prevSeekTrigger) {
                    webView.evaluateJavascript("if (typeof seekBackward === 'function') seekBackward();", null)
                }
            }
        },
        modifier = modifier
    )
}
