// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.deepeye.musicpro.data.prefs.ThemeMode
import com.deepeye.musicpro.ui.DeepEyeMusicApp
import com.deepeye.musicpro.ui.FullscreenMode
import com.deepeye.musicpro.ui.theme.DeepEyeMusicTheme
import com.deepeye.musicpro.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Single Activity host for the Compose-based UI.
 * Supports Picture-in-Picture (PiP) mode for video playback.
 *
 * PiP best practices (developer.android.com):
 * - Only enter PiP when video is actively playing AND user is on video screen
 * - Use setAutoEnterEnabled(true) on Android 12+ for seamless Home-button PiP
 * - Disable auto-enter when leaving video screen or pausing
 * - Hide all overlays/controls in PiP mode for a clean video-only view
 */
@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var playerController: com.deepeye.musicpro.player.controller.PlayerController

    @javax.inject.Inject
    lateinit var contentFetcher: com.deepeye.musicpro.domain.recommendation.ContentFetcher

    private val themeViewModel: ThemeViewModel by viewModels()

    /** Fullscreen mode controller for auto-rotate to landscape on fullscreen */
    val fullscreenMode = FullscreenMode().apply {
        onEnterFullscreen = { forceLandscape ->
            // Lock to sensor landscape for fullscreen video only if requested (e.g. via button)
            if (forceLandscape) {
                this@MainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Fix for SurfaceView edge-to-edge black bar bug on Android 14+
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            }
        }
        onExitFullscreen = {
            // Restore to unspecified so the user can freely rotate again
            this@MainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            enableEdgeToEdge(
                statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
        }
    }

    private lateinit var pipEngine: com.deepeye.musicpro.player.controller.PipEngine

    private var orientationEventListener: android.view.OrientationEventListener? = null
    private var hasRotatedToLandscape = false

    /** Observable PiP state for Compose UI */
    var isInPipMode by mutableStateOf(false)
        private set

    override fun onStart() {
        super.onStart()
        if (::playerController.isInitialized) {
            playerController.setAppInForeground(true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::playerController.isInitialized && !isInPipMode) {
            playerController.setAppInForeground(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener?.disable()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // Permissions granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        pipEngine = com.deepeye.musicpro.player.controller.PipEngine(this, playerController)

        checkAndRequestPermissions()

        orientationEventListener = object : android.view.OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                // Allow a generous range for landscape and portrait
                val isPortrait = (orientation in 0..30) || (orientation in 330..359) || (orientation in 150..210)
                val isLandscape = (orientation in 60..120) || (orientation in 240..300)
                
                if (fullscreenMode.isFullscreen) {
                    if (isLandscape) {
                        hasRotatedToLandscape = true
                    } else if (isPortrait && hasRotatedToLandscape) {
                        // User physically rotated back to portrait after being in landscape!
                        fullscreenMode.exit()
                    }
                } else {
                    hasRotatedToLandscape = false
                }
            }
        }
        if (orientationEventListener?.canDetectOrientation() == true) {
            orientationEventListener?.enable()
        }

        // Initialize Brave Shields Engine for 0-latency tracking protection and background auto-sync
        com.deepeye.musicpro.engine.BraveShieldsEngine.initialize(applicationContext)

        // Observe player state to dynamically toggle Android 12+ auto-PiP.
        // Only react to fields that affect PiP params (video mode + play/pause),
        // NOT the 250ms position ticks which would otherwise rebuild PiP params
        // and remote actions four times per second.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerController.playerState
                    .map { it.isVideo to it.isPlaying }
                    .distinctUntilChanged()
                    .collect {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            pipEngine.updatePipParams()
                        }
                    }
            }
        }

        setContent {
            val appSettings by themeViewModel.settings.collectAsStateWithLifecycle()
            val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()
            val windowSizeClass = androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(this)

            val isDarkTheme = when (appSettings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val useDynamicColor = appSettings.dynamicColor

            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            val clampedDensity = androidx.compose.ui.unit.Density(
                density = currentDensity.density.coerceAtMost(3.0f), // Don't let layout scale too extremely either
                fontScale = 1.0f // STRICTLY enforce 1.0x font scaling for editor-grade UI stability
            )

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides clampedDensity
            ) {
                DeepEyeMusicTheme(
                    darkTheme = isDarkTheme,
                    useDynamicColor = useDynamicColor,
                    amoledMode = appSettings.amoledMode,
                    overrideColors = dynamicColors
                ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        // Premium Fog / Mesh Background
                        if (!appSettings.amoledMode) {
                            val color1 = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            val color2 = androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            val color3 = androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(color1, androidx.compose.ui.graphics.Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        radius = size.width * 1.2f
                                    ),
                                    radius = size.width * 1.2f,
                                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                                )
                                drawCircle(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(color2, androidx.compose.ui.graphics.Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.4f),
                                        radius = size.width
                                    ),
                                    radius = size.width,
                                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.4f)
                                )
                                drawCircle(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(color3, androidx.compose.ui.graphics.Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height),
                                        radius = size.width * 1.1f
                                    ),
                                    radius = size.width * 1.1f,
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height)
                                )
                            }
                        }

                        DeepEyeMusicApp(
                            isInPipMode = isInPipMode,
                            fullscreenMode = fullscreenMode,
                            playerController = playerController,
                            windowSizeClass = windowSizeClass
                        )
                    }
                }
            }
            }
        }
        
        handleIntent(intent)
    }

    // ── Picture-in-Picture ──

    /**
     * Pre-Android 12 fallback: auto-enter PiP on Home button press
     * ONLY when video is actively playing.
     * On Android 12+, setAutoEnterEnabled handles this automatically.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::pipEngine.isInitialized) {
            pipEngine.onUserLeaveHint()
        }
    }

    /**
     * System callback when PiP mode changes.
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode

        if (::pipEngine.isInitialized) {
            pipEngine.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
    }

    /**
     * Public API for Compose: manually enter PiP mode.
     * Guarded: will no-op if not playing video.
     */
    fun enterPipMode() {
        if (::pipEngine.isInitialized) {
            pipEngine.enterPipMode()
        }
    }

    /**
     * Update PiP params (e.g., source rect hint for smooth animation).
     */
    fun updatePipParams(sourceRect: Rect? = null) {
        if (::pipEngine.isInitialized) {
            pipEngine.updatePipParams(sourceRect)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Required for visualizer
        permissions.add(Manifest.permission.RECORD_AUDIO)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val action = intent?.action
        
        // Handle YouTube links (either via Share Sheet text OR clicked URL)
        var youtubeUrl: String? = null
        if (action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            youtubeUrl = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
        } else if (action == android.content.Intent.ACTION_VIEW && intent.data != null) {
            val uriStr = intent.data.toString()
            if (uriStr.contains("youtube.com") || uriStr.contains("youtu.be")) {
                youtubeUrl = uriStr
            }
        }

        if (youtubeUrl != null) {
            val youtubeUrlRegex = Regex("(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/|youtube\\.com/shorts/)([^\"&?/\\s]{11})")
            val match = youtubeUrlRegex.find(youtubeUrl)
            
            if (match != null) {
                val videoId = match.groups[1]?.value ?: return
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val results = contentFetcher.searchByQuery(videoId, 1)
                        val videoItem = results.firstOrNull { it.videoId == videoId } ?: results.firstOrNull()
                        
                        val title = videoItem?.title ?: "Shared Audio"
                        val artist = videoItem?.artist ?: "YouTube"
                        
                        val remoteItem = com.deepeye.musicpro.domain.model.MediaItem.Remote(
                            id = videoId,
                            title = title,
                            artist = artist,
                            artworkUri = android.net.Uri.parse("https://img.youtube.com/vi/$videoId/hqdefault.jpg"),
                            duration = 0L,
                            isVideo = true
                        )
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            playerController.playMedia(remoteItem)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error handling shared YouTube link", e)
                    }
                }
                return
            }
        }

        if (intent?.action == android.content.Intent.ACTION_VIEW || intent?.action == android.content.Intent.ACTION_SEND) {
            val uri = intent.data ?: (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri
            }) ?: return
            
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                var title = "Unknown Title"
                var artist = "Unknown Artist"
                var duration = 0L
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(this@MainActivity, uri)
                    title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
                    artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                    val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    duration = durationStr?.toLongOrNull() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error extracting metadata", e)
                }

                if (title == "Unknown Title" && uri.scheme == "content") {
                    try {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    val displayName = cursor.getString(nameIndex)
                                    if (displayName != null) {
                                        title = displayName.substringBeforeLast('.')
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {}
                } else if (title == "Unknown Title" && uri.scheme == "file") {
                    title = uri.lastPathSegment?.substringBeforeLast('.') ?: "Unknown Title"
                }

                val song = com.deepeye.musicpro.domain.model.Song(
                    id = uri.hashCode().toLong(),
                    title = title,
                    artist = artist,
                    album = "",
                    albumId = 0L,
                    artistId = 0L,
                    uri = uri,
                    duration = duration,
                    size = 0L,
                    path = uri.toString(),
                    artUri = null
                )
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    playerController.playMedia(com.deepeye.musicpro.domain.model.MediaItem.Local(song))
                }
            }
        }
    }
}
