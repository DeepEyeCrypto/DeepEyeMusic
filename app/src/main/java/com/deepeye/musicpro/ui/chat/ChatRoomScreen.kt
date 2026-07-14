package com.deepeye.musicpro.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Secure Chat", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "E2E Encrypted",
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "End-to-End Encrypted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTtlMenu = true }) {
                            Icon(Icons.Default.Timer, contentDescription = "Self Destruct Timer", tint = if (selectedTtl != null) Color(0xFF7B3FE4) else Color.White)
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
                    placeholder = { Text("Encrypted message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7B3FE4),
                        unfocusedBorderColor = Color.White.copy(0.2f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    modifier = Modifier
                        .background(Color(0xFF7B3FE4), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isMine) Color(0xFF7B3FE4) else Color(0xFF333333),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (timeLeft != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${timeLeft}s",
                        color = Color.Red.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
