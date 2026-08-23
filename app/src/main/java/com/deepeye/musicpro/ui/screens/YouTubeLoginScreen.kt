package com.deepeye.musicpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.auth.YouTubeDeviceAuthManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
fun YouTubeLoginScreen(
    onLoginSuccess: (accessToken: String, refreshToken: String?) -> Unit,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: com.deepeye.musicpro.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    // In production, inject OkHttpClient via Hilt/Dagger
    val authManager = remember { YouTubeDeviceAuthManager(OkHttpClient()) }
    
    var userCode by remember { mutableStateOf("") }
    var verificationUrl by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Initializing login...") }

    LaunchedEffect(Unit) {
        statusMessage = "Getting activation code from Google..."
        val response = authManager.requestDeviceCode()
        
        if (response != null) {
            userCode = response.userCode
            verificationUrl = response.verificationUrl
            statusMessage = "Waiting for you to authorize..."
            
            // Start polling
            authManager.pollForToken(response.deviceCode, response.interval) { token ->
                coroutineScope.launch {
                    statusMessage = "Login Successful!"
                    settingsViewModel.saveYouTubeTokens(token.accessToken, token.refreshToken)
                    android.widget.Toast.makeText(context, "YouTube Connected! \uD83C\uDF89", android.widget.Toast.LENGTH_LONG).show()
                    kotlinx.coroutines.delay(1500)
                    onLoginSuccess(token.accessToken, token.refreshToken)
                }
            }
        } else {
            statusMessage = "Failed to get activation code. Check network."
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Sign in to YouTube",
                style = MaterialTheme.typography.headlineMedium
            )
            
            if (userCode.isNotEmpty()) {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                val context = androidx.compose.ui.platform.LocalContext.current

                Card(
                    modifier = Modifier.padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "1. On your phone or computer, go to:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = verificationUrl,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(onClick = { uriHandler.openUri(verificationUrl) }) {
                            Text("Open Link")
                        }
                        
                        Text(
                            text = "2. Enter this code:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = userCode,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                        }
                        Button(onClick = { 
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(userCode))
                            android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy Code")
                        }
                    }
                }
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = onCancel,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
