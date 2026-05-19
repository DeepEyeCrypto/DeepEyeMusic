package com.deepeye.musicpro

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
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
import com.deepeye.musicpro.ui.DeepEyeMusicApp
import com.deepeye.musicpro.ui.theme.DeepEyeTheme
import com.deepeye.musicpro.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
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
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var playerController: com.deepeye.musicpro.player.controller.PlayerController

    private val themeViewModel: ThemeViewModel by viewModels()

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
        enableEdgeToEdge()

        checkAndRequestPermissions()

        // Observe player state to dynamically toggle Android 12+ auto-PiP
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerController.playerState.collect { state ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val shouldAutoEnter = state.isVideo && state.isPlaying
                        setPictureInPictureParams(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .setAutoEnterEnabled(shouldAutoEnter)
                                .setSeamlessResizeEnabled(true)
                                .build()
                        )
                    }
                }
            }
        }

        setContent {
            val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()

            DeepEyeTheme(overrideColors = dynamicColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DeepEyeMusicApp(isInPipMode = isInPipMode)
                }
            }
        }
    }

    // ── Picture-in-Picture ──

    /**
     * Pre-Android 12 fallback: auto-enter PiP on Home button press
     * ONLY when video is actively playing.
     * On Android 12+, setAutoEnterEnabled handles this automatically.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Only pre-12 needs this; Android 12+ uses setAutoEnterEnabled
            if (::playerController.isInitialized) {
                val state = playerController.playerState.value
                if (state.isVideo && state.isPlaying) {
                    enterPipMode()
                }
            }
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

        if (!isInPictureInPictureMode) {
            // Returned from PiP — restore foreground state
            if (::playerController.isInitialized) {
                playerController.setAppInForeground(true)
            }
        }
    }

    /**
     * Public API for Compose: manually enter PiP mode.
     * Guarded: will no-op if not playing video.
     */
    fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Guard: only enter PiP if video is actually playing
        if (::playerController.isInitialized) {
            val state = playerController.playerState.value
            if (!state.isVideo) return
        }

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
        enterPictureInPictureMode(params)
    }

    /**
     * Update PiP params (e.g., source rect hint for smooth animation).
     */
    fun updatePipParams(sourceRect: Rect? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        if (sourceRect != null) {
            builder.setSourceRectHint(sourceRect)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val state = if (::playerController.isInitialized) playerController.playerState.value else null
            val shouldAutoEnter = state?.isVideo == true && state.isPlaying
            builder.setAutoEnterEnabled(shouldAutoEnter)
            builder.setSeamlessResizeEnabled(true)
        }

        setPictureInPictureParams(builder.build())
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
}
