package com.deepeye.musicpro.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.view.WindowManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    receiverId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId, receiverId) {
        viewModel.initChat(chatId, receiverId)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val selectedTtl by viewModel.selectedTtl.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var showTtlMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current

    // Security: Block screenshots only inside this chat room
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF7B3FE4), Color(0xFF00E5FF))
                                    ),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                                .padding(1.5.dp)
                                .background(Color(0xFF121212), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = receiverId.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                receiverId,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2E Encrypted",
                                    modifier = Modifier.size(10.dp),
                                    tint = Color(0xFF00E676)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "RSA-2048 / AES-GCM Encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showTtlMenu = true },
                            modifier = Modifier.background(
                                if (selectedTtl != null) Color(0xFF7B3FE4).copy(alpha = 0.3f) else Color.Transparent,
                                androidx.compose.foundation.shape.CircleShape
                            )
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Self Destruct Timer",
                                tint = if (selectedTtl != null) Color(0xFF00E5FF) else Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showTtlMenu,
                            onDismissRequest = { showTtlMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Off") }, onClick = { viewModel.setTtl(null); showTtlMenu = false })
                            DropdownMenuItem(text = { Text("5 seconds") }, onClick = { viewModel.setTtl(5); showTtlMenu = false })
                            DropdownMenuItem(text = { Text("10 seconds") }, onClick = { viewModel.setTtl(10); showTtlMenu = false })
                            DropdownMenuItem(text = { Text("30 seconds") }, onClick = { viewModel.setTtl(30); showTtlMenu = false })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Encrypted message...", color = Color.Gray) },
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        cursorColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0x18FFFFFF),
                        unfocusedContainerColor = Color(0x0EFFFFFF)
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        val txt = inputText
                        if (txt.isBlank()) return@IconButton
                        inputText = ""
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.sendMessage(txt) { success ->
                            if (!success) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                android.widget.Toast.makeText(
                                    context, 
                                    "Cannot send! User not found or hasn't setup Secure Chat.", 
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                inputText = txt
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF7B3FE4), Color(0xFF00E5FF))
                            ),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    text = msg.plaintext,
                    isMine = msg.isMine,
                    expiresAt = msg.expiresAt
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    text: String,
    isMine: Boolean,
    expiresAt: Long? = null
) {
    var timeLeft by remember { mutableStateOf<Long?>(null) }
    var isDecrypting by remember { mutableStateOf(!isMine) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        if (isDecrypting) {
            kotlinx.coroutines.delay(400)
            isDecrypting = false
        }
    }

    LaunchedEffect(expiresAt) {
        if (expiresAt != null) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val diff = expiresAt - now
                if (diff > 0) {
                    timeLeft = diff / 1000
                } else {
                    timeLeft = 0
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { 40 }, animationSpec = tween(300)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .run {
                        if (isDecrypting) this.shimmer() else this
                    }
                    .background(
                        brush = if (isMine) {
                            Brush.linearGradient(listOf(Color(0xFF7B3FE4), Color(0xFF9E75FF)))
                        } else {
                            Brush.linearGradient(listOf(Color(0x2AFFFFFF), Color(0x1AFFFFFF)))
                        },
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isMine) 20.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 20.dp
                        )
                    )
                    .run {
                        if (!isMine) {
                            this.border(
                                1.dp,
                                Color(0x22FFFFFF),
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 20.dp
                                )
                            )
                        } else this
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDecrypting) "🔒 Decrypting payload..." else text,
                            color = if (isDecrypting) Color(0xFF00E5FF) else Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isMine && !isDecrypting) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    if (timeLeft != null && !isDecrypting) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "🔥 ${timeLeft}s",
                            color = Color(0xFFFF5252),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}
