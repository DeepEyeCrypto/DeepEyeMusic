package com.deepeye.musicpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.deepeye.musicpro.ui.DeepEyeMusicApp
import com.deepeye.musicpro.ui.theme.DeepEyeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity host for the Compose-based UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeepEyeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DeepEyeMusicApp()
                }
            }
        }
    }
}
