package com.deepeye.musicpro.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateToChat: (chatId: String, receiverId: String) -> Unit,
    onBack: () -> Unit
) {
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showSetIdDialog by remember { mutableStateOf(false) }
    val myChatId by viewModel.myChatId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encrypted Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(Icons.Default.Add, "New Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("All chats are end-to-end encrypted", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth().clickable { showSetIdDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your Chat ID", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            Text(myChatId ?: "Not set (Tap to create)", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.Default.Settings, contentDescription = "Edit ID", tint = Color.Gray)
                    }
                }
            }

            // Mock Data for MVP demo
            item {
                ChatListItem(
                    name = "Alice",
                    lastMessage = "Sent a secure message",
                    onClick = { onNavigateToChat("demo_chat_alice", "alice_uid") }
                )
            }
            item {
                ChatListItem(
                    name = "Bob",
                    lastMessage = "Hey, did you listen to the new album?",
                    onClick = { onNavigateToChat("demo_chat_bob", "bob_uid") }
                )
            }
        }

        if (showNewChatDialog) {
            NewChatDialog(
                onDismiss = { showNewChatDialog = false },
                onStartChat = { receiverId ->
                    showNewChatDialog = false
                    onNavigateToChat("chat_${System.currentTimeMillis()}", receiverId)
                }
            )
        }

        if (showSetIdDialog) {
            SetChatIdDialog(
                currentId = myChatId ?: "",
                onDismiss = { showSetIdDialog = false },
                onSave = { newId ->
                    viewModel.updateCustomChatId(newId)
                    showSetIdDialog = false
                }
            )
        }
    }
}

@Composable
fun ChatListItem(
    name: String,
    lastMessage: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF7B3FE4).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.first().uppercase(), color = Color(0xFF7B3FE4), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(lastMessage, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onStartChat: (String) -> Unit
) {
    var userId by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("New Encrypted Chat", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it.replace(" ", "") },
                    label = { Text("Friend's Chat ID or UID") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (userId.isNotBlank()) onStartChat(userId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B3FE4))
                    ) {
                        Text("Start Chat")
                    }
                }
            }
        }
    }
}

@Composable
fun SetChatIdDialog(
    currentId: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newId by remember { mutableStateOf(currentId.removePrefix("@")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Set Custom Chat ID", style = MaterialTheme.typography.titleLarge)
                Text("Create a unique username so friends can easily find you.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newId,
                    onValueChange = { newId = it.replace(" ", "") },
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (newId.isNotBlank()) onSave(newId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B3FE4))
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
