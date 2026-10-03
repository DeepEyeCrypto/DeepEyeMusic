package com.deepeye.musicpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepeye.musicpro.domain.auth.YouTubeDeviceAuthManager
import com.deepeye.musicpro.ui.player.PlayerViewModel
import com.deepeye.musicpro.ui.components.QrCodeView
import com.deepeye.musicpro.ui.player.components.AmbilightBackground
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLoginScreen(
    onLoginSuccess: (accessToken: String, refreshToken: String?) -> Unit,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: com.deepeye.musicpro.ui.settings.SettingsViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val authManager = remember { YouTubeDeviceAuthManager(OkHttpClient()) }
    
    var userCode by remember { mutableStateOf("") }
    var verificationUrl by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Connecting to Google OAuth...") }
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
            statusMessage = "Waiting for you to authorize on your device..."
            isLoading = false
            
            // Start polling for user approval
            authManager.pollForToken(response.deviceCode, response.interval) { token ->
                coroutineScope.launch {
                    statusMessage = "Authorization Successful!"
                    settingsViewModel.saveYouTubeTokens(token.accessToken, token.refreshToken)
                    val profile = authManager.fetchUserProfile(token.accessToken)
                    if (profile != null) {
                        settingsViewModel.saveYouTubeProfile(profile.name, profile.pictureUrl, profile.email)
                    }
                    android.widget.Toast.makeText(context, "YouTube Connected! \uD83C\uDF89", android.widget.Toast.LENGTH_LONG).show()
                    kotlinx.coroutines.delay(1000)
                    onLoginSuccess(token.accessToken, token.refreshToken)
                }
            }
        } else {
            statusMessage = "Failed to connect to Google OAuth. Please check internet."
            isLoading = false
        }
    }

    // Root Container with Background Image & Ambient Blur
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        // 1. App Default Spatial Background Layer
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.deepeye.musicpro.R.drawable.spatial_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
                .alpha(0.6f),
            contentScale = ContentScale.Crop
        )

        // 2. Fallback to Music cover art if currently playing
        val currentArtworkUri = playerState.currentItem?.artworkUri
        if (currentArtworkUri != null) {
            AsyncImage(
                model = currentArtworkUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(70.dp)
                    .alpha(0.45f),
                contentScale = ContentScale.Crop
            )
        }

        // 3. Ambilight Glowing Colors
        AmbilightBackground(
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF7000FF),
            modifier = Modifier.fillMaxSize()
        ) {
            // Dark scrim to keep text and QR readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Connect YouTube Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onCancel) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    },
                    containerColor = Color.Transparent
                ) { paddingValues ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        val isLandscape = maxWidth > 640.dp

                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00E5FF),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = statusMessage,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (userCode.isNotEmpty()) {
                            if (isLandscape) {
                                // ─── 2-Column Side-by-Side Landscape Layout ───
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 32.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Column: QR Code + Scan prompt
                                    Column(
                                        modifier = Modifier.weight(0.42f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Scan with your phone",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        QrCodeView(
                                            content = fullActivationUrl,
                                            size = 180.dp,
                                            modifier = Modifier
                                                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "youtube.com/activate",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }

                                    // Right Column: Activation Code + All Actions + Polling
                                    Column(
                                        modifier = Modifier
                                            .weight(0.58f)
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Glassmorphic Activation Code Box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                                                .padding(vertical = 14.dp, horizontal = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "ACTIVATION CODE",
                                                    fontSize = 11.sp,
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

                                        // 3 Action Buttons: Copy Code, Copy Link, Open Link
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 1. Copy Code Button
                                            OutlinedButton(
                                                onClick = {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(userCode))
                                                    android.widget.Toast.makeText(context, "Code copied: $userCode", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Copy Code", fontSize = 11.sp, maxLines = 1)
                                            }

                                            // 2. Copy Link Button
                                            OutlinedButton(
                                                onClick = {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fullActivationUrl))
                                                    android.widget.Toast.makeText(context, "Activation Link copied!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                                            ) {
                                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF00E5FF))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Copy Link", fontSize = 11.sp, maxLines = 1)
                                            }

                                            // 3. Open Link Button
                                            Button(
                                                onClick = { uriHandler.openUri(fullActivationUrl) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Open Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }

                                        // Polling Status Indicator
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF00E5FF),
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = statusMessage,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            } else {
                                // ─── Portrait Fallback Layout ───
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Scan with your phone",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    QrCodeView(
                                        content = fullActivationUrl,
                                        size = 180.dp,
                                        modifier = Modifier
                                            .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(userCode))
                                                android.widget.Toast.makeText(context, "Code copied: $userCode", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy Code", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fullActivationUrl))
                                                android.widget.Toast.makeText(context, "Link copied!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy Link", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { uriHandler.openUri(fullActivationUrl) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                        ) {
                                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Open Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF00E5FF),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = statusMessage,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        }
    }
}
