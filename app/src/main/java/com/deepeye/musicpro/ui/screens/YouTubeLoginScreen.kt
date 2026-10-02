package com.deepeye.musicpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.domain.auth.YouTubeDeviceAuthManager
import com.deepeye.musicpro.ui.components.QrCodeView
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLoginScreen(
    onLoginSuccess: (accessToken: String, refreshToken: String?) -> Unit,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: com.deepeye.musicpro.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val authManager = remember { YouTubeDeviceAuthManager(OkHttpClient()) }
    
    var userCode by remember { mutableStateOf("") }
    var verificationUrl by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Initializing login...") }
    var isLoading by remember { mutableStateOf(true) }

    val fullActivationUrl = remember(verificationUrl, userCode) {
        if (verificationUrl.isNotBlank() && userCode.isNotBlank()) {
            if (verificationUrl.contains("?")) "$verificationUrl&user_code=$userCode" else "$verificationUrl?user_code=$userCode"
        } else verificationUrl.ifBlank { "https://www.youtube.com/activate" }
    }

    LaunchedEffect(Unit) {
        statusMessage = "Connecting with Google OAuth Server..."
        isLoading = true
        val response = authManager.requestDeviceCode()
        
        if (response != null) {
            userCode = response.userCode
            verificationUrl = response.verificationUrl
            statusMessage = "Scan QR code or enter code on your phone/browser"
            isLoading = false
            
            // Start polling for user approval
            authManager.pollForToken(response.deviceCode, response.interval) { token ->
                coroutineScope.launch {
                    statusMessage = "Authorization Successful!"
                    settingsViewModel.saveYouTubeTokens(token.accessToken, token.refreshToken)
                    android.widget.Toast.makeText(context, "YouTube TV Connected! \uD83C\uDF89", android.widget.Toast.LENGTH_LONG).show()
                    kotlinx.coroutines.delay(1200)
                    onLoginSuccess(token.accessToken, token.refreshToken)
                }
            }
        } else {
            statusMessage = "Failed to get device code. Check internet connection."
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Connect YouTube TV Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0E14))
            )
        },
        containerColor = Color(0xFF07090E)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (isLoading) {
                    Spacer(Modifier.height(80.dp))
                    CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = statusMessage,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (userCode.isNotEmpty()) {
                    // QR Code & Direct Link Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121622)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Scan with your phone camera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            // 1. Live Interactive QR Code
                            QrCodeView(
                                content = fullActivationUrl,
                                size = 200.dp,
                                modifier = Modifier
                                    .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            )

                            Text(
                                text = "OR visit youtube.com/activate manually",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            // 2. User Activation Code Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF1F2839), Color(0xFF141926))
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ACTIVATION CODE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00E5FF),
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = userCode,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 4.sp
                                    )
                                }
                            }

                            // 3. Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(userCode))
                                        android.widget.Toast.makeText(context, "Code copied: $userCode", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy Code", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { uriHandler.openUri(fullActivationUrl) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Live Polling Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusMessage,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Spacer(Modifier.height(80.dp))
                    Text(
                        text = statusMessage,
                        color = Color(0xFFFF5252),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
