package com.deepeye.musicpro.data.source.remote.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class HeadlessWebViewExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "HeadlessExtractor"
    private var currentActivity: android.app.Activity? = null
    private val mutex = Mutex()

    private var activeSession: ActiveSession? = null

    private data class ActiveSession(
        val videoId: String,
        val preferVideo: Boolean,
        val onCaptured: (String) -> Unit
    )

    init {
        // Register Activity Lifecycle Callbacks to keep track of the active Activity context
        val app = context.applicationContext as? android.app.Application
        app?.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
                currentActivity = activity
            }
            override fun onActivityStarted(activity: android.app.Activity) {
                currentActivity = activity
            }
            override fun onActivityResumed(activity: android.app.Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: android.app.Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }
        })

        // Initialize global ServiceWorkerController to capture requests inside WebView Service Workers
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                android.webkit.ServiceWorkerController.getInstance().setServiceWorkerClient(object : android.webkit.ServiceWorkerClient() {
                    override fun shouldInterceptRequest(
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val uri = request?.url ?: return null
                        val url = uri.toString()
                        Log.d(tag, "🔍 SW Request: $url")
                        val session = activeSession
                        
                        if (session != null) {
                            if (url.contains(".googlevideo.com/videoplayback")) {
                                val mime = uri.getQueryParameter("mime")
                                Log.i(tag, "🎥 SW Intercepted videoplayback request: $url (mime: $mime, preferVideo: ${session.preferVideo})")
                                val isAudio = mime?.startsWith("audio") == true
                                val isVideo = mime?.startsWith("video") == true
                                if ((!session.preferVideo && isAudio) || (session.preferVideo && isVideo)) {
                                    session.onCaptured(url)
                                }
                            }
                        }

                        // Block Ads/Telemetry inside Service Workers as well
                        val host = uri.host ?: ""
                        val path = uri.path ?: ""
                        if (host.contains("doubleclick.net") ||
                            host.contains("googleads") ||
                            host.contains("pagead") ||
                            host.contains("googlesyndication.com") ||
                            host.contains("adservice.google") ||
                            path.contains("generate_204")
                        ) {
                            Log.d(tag, "🚫 SW Blocked Ad/Telemetry Request: $url")
                            val response = WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                            val headers = HashMap<String, String>()
                            headers["Access-Control-Allow-Origin"] = "https://www.youtube.com"
                            headers["Access-Control-Allow-Credentials"] = "true"
                            headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
                            headers["Access-Control-Allow-Headers"] = "*"
                            response.responseHeaders = headers
                            return response
                        }
                        return null
                    }
                })
            } catch (e: Exception) {
                Log.w(tag, "Failed to set ServiceWorkerClient: ${e.message}")
            }
        }
    }

    private var pooledWebView: WebView? = null
    private var isWebViewAttachedToRoot: Boolean = false
    private var attachedRootView: android.view.ViewGroup? = null

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractStreamUrl(videoId: String, preferVideo: Boolean = false): String? =
        mutex.withLock {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                var isResumed = false
                val activity = currentActivity
                
                if (activity == null) {
                    Log.w(tag, "⚠️ No active Activity context found! Using ApplicationContext (might be throttled by OS).")
                }
                
                // Clear all Web Storage (including registered Service Workers) via the official thread-safe API
                try {
                    android.webkit.WebStorage.getInstance().deleteAllData()
                    Log.i(tag, "🧹 Cleared WebStorage databases and storage directories via thread-safe API.")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to clear WebStorage: ${e.message}")
                }

                val webView = pooledWebView ?: WebView(context).apply {
                    clearCache(true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                }.also { pooledWebView = it }

                // Reset webview state before reuse
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()

                // Enable cookies for WebView
                try {
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        cookieManager.setAcceptThirdPartyCookies(webView, true)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed to enable cookies: ${e.message}")
                }

                // If Activity context is available, attach the WebView to layout hierarchy to bypass Chromium background throttling
                var addedToRoot = false
                var rootView: android.view.ViewGroup? = null
                if (activity != null) {
                    try {
                        rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                        if (rootView != null) {
                            val params = android.view.ViewGroup.LayoutParams(320, 180)
                            webView.visibility = android.view.View.VISIBLE
                            webView.alpha = 1.0f
                            webView.isFocusable = false
                            webView.isClickable = false
                            webView.isLongClickable = false
                            
                            // Only add if not already attached to this root
                            if (webView.parent != rootView) {
                                (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                                rootView.addView(webView, 0, params)
                            }
                            addedToRoot = true
                            isWebViewAttachedToRoot = true
                            attachedRootView = rootView
                            Log.d(tag, "Attached pooled WebView to Activity root view hierarchy at index 0.")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to attach WebView to root view: ${e.message}")
                    }
                }

                if (!addedToRoot) {
                    webView.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(320, android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(180, android.view.View.MeasureSpec.EXACTLY)
                    )
                    webView.layout(0, 0, 320, 180)
                }

                val cleanupWebView = {
                    // Do NOT destroy the pooled WebView. Just stop loading and remove from view hierarchy.
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    if (isWebViewAttachedToRoot && attachedRootView != null) {
                        try {
                            attachedRootView?.removeView(webView)
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to detach pooled WebView: ${e.message}")
                        }
                        isWebViewAttachedToRoot = false
                        attachedRootView = null
                    }
                }

                val timeoutRunnable = Runnable {
                    if (!isResumed) {
                        Log.w(tag, "Extraction timed out for $videoId")
                        isResumed = true
                        activeSession = null
                        cleanupWebView()
                        continuation.resume(null)
                    }
                }

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.postDelayed(timeoutRunnable, 10000L) // 10s timeout

                val onCapturedLocal = { url: String ->
                    if (!isResumed) {
                        isResumed = true
                        Log.i(tag, "✅ Intercepted stream URL for $videoId (URL: $url)")
                        handler.removeCallbacks(timeoutRunnable)
                        handler.post {
                            activeSession = null
                            cleanupWebView()
                            continuation.resume(url)
                        }
                    }
                }

                activeSession = ActiveSession(videoId, preferVideo, onCapturedLocal)

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                        val url = uri.toString()
                        Log.d(tag, "🔍 Request: $url")
                        val host = uri.host ?: ""
                        val path = uri.path ?: ""

                        if (url.contains("ad_status.js")) {
                            return super.shouldInterceptRequest(view, request)
                        }

                        // 0. Block Service Worker files to force direct network calls interceptable by shouldInterceptRequest
                        if (url.contains("sw.js") || url.contains("service-worker")) {
                            Log.d(tag, "🚫 Blocking Service Worker registration: $url")
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                404,
                                "Not Found",
                                null,
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }

                        // 1. Content Filtering: Ads & Telemetry Trackers
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
                            Log.d(tag, "🚫 Blocked Ad/Telemetry Request: $url")
                            // Prevent CORS errors on the page by attaching proper CORS headers targeting request origin dynamically
                            val origin = request?.requestHeaders?.get("Origin") ?: request?.requestHeaders?.get("origin") ?: "https://m.youtube.com"
                            val response = WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                            val headers = HashMap<String, String>()
                            headers["Access-Control-Allow-Origin"] = origin
                            headers["Access-Control-Allow-Credentials"] = "true"
                            headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
                            headers["Access-Control-Allow-Headers"] = "*"
                            response.responseHeaders = headers
                            return response
                        }

                        if (url.contains(".googlevideo.com/videoplayback")) {
                            val uriParam = Uri.parse(url)
                            val mime = uriParam.getQueryParameter("mime")
                            val itag = uriParam.getQueryParameter("itag")
                            Log.d(tag, "🎥 Intercepted videoplayback: itag=$itag mime=$mime preferVideo=$preferVideo")
                            
                            if (preferVideo) {
                                // For video mode: accept any video mime stream (adaptive or progressive)
                                if (mime?.startsWith("video") == true) {
                                    val reqHeaders = request?.requestHeaders ?: HashMap()
                                    Log.i(tag, "🎥 Intercepted videoplayback request headers for $videoId: $reqHeaders")
                                    onCapturedLocal(url)
                                }
                            } else {
                                // For audio mode: accept audio streams
                                if (mime?.startsWith("audio") == true) {
                                    val reqHeaders = request?.requestHeaders ?: HashMap()
                                    Log.i(tag, "🎥 Intercepted audio request headers for $videoId: $reqHeaders")
                                    onCapturedLocal(url)
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        // Force progressive stream by disabling MSE early
                        view?.evaluateJavascript("""
                            window.MediaSource = undefined;
                            window.webkitMediaSource = undefined;
                        """.trimIndent(), null)
                        Log.d(tag, "Page started: $url (MediaSource disabled, progressive mode)")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val js = """
                            (function() {
                                console.log("HeadlessExtractorJS: Embed page finished loading: " + window.location.href);
                                console.log("HeadlessExtractorJS: Page title: " + document.title);
                                if (document.body) {
                                    console.log("HeadlessExtractorJS: Body text snapshot: " + document.body.innerText.substring(0, 300).replace(/\s+/g, ' '));
                                }
                                
                                // Deregister and unregister Service Workers to bypass SW caching and force direct network requests
                                if (navigator.serviceWorker) {
                                    navigator.serviceWorker.getRegistrations().then(function(registrations) {
                                        for (var i = 0; i < registrations.length; i++) {
                                            registrations[i].unregister().then(function(success) {
                                                console.log("HeadlessExtractorJS: Service Worker unregistered: " + success);
                                            });
                                        }
                                    }).catch(function(e) {
                                        console.warn("HeadlessExtractorJS: SW lookup failed: " + e.message);
                                    });
                                }

                                function tryPlay() {
                                    var currentVideo = document.querySelector('video');
                                    var currentPlayBtn = document.querySelector('.ytp-large-play-button') || document.querySelector('.ytp-play-button');
                                    
                                    console.log("HeadlessExtractorJS: Polling check. Video: " + !!currentVideo + 
                                                ", Src: " + (currentVideo ? currentVideo.src : 'N/A') +
                                                ", Paused: " + (currentVideo ? currentVideo.paused : 'N/A') + 
                                                ", CurrentTime: " + (currentVideo ? currentVideo.currentTime : 'N/A') +
                                                ", ReadyState: " + (currentVideo ? currentVideo.readyState : 'N/A') +
                                                ", PlayBtn: " + !!currentPlayBtn);
                                                
                                    if (currentPlayBtn) {
                                        console.log("HeadlessExtractorJS: Clicking play button...");
                                        currentPlayBtn.click();
                                    }
                                    if (currentVideo) {
                                        currentVideo.play().then(function() {
                                            console.log("HeadlessExtractorJS: play() resolved successfully");
                                        }).catch(function(err) {
                                            console.warn("HeadlessExtractorJS: play() rejected: " + err.message);
                                        });
                                    }
                                }
                                
                                tryPlay();
                                var playInterval = setInterval(tryPlay, 1000);
                                
                                setTimeout(function() {
                                    console.log("HeadlessExtractorJS: Polling timeout reached.");
                                    clearInterval(playInterval);
                                }, 8000);
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(js, null)
                    }
                }

                webView.webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        Log.d("HeadlessExtractorJS", "Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                        return true
                    }
                }

                continuation.invokeOnCancellation {
                    if (!isResumed) {
                        isResumed = true
                        activeSession = null
                        handler.removeCallbacks(timeoutRunnable)
                        handler.post {
                            if (addedToRoot && rootView != null) {
                                rootView.removeView(webView)
                            }
                            webView.destroy()
                        }
                    }
                }

                Log.d(tag, "Loading YouTube mobile watch page directly for videoId: $videoId")
                val headers = HashMap<String, String>()
                headers["Referer"] = "https://m.youtube.com"
                webView.loadUrl("https://m.youtube.com/watch?v=$videoId", headers)
            }
        }
    }
}
