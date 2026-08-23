package com.deepeye.musicpro.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
    var showTorDialog by remember { mutableStateOf(false) }
    var showMeshDialog by remember { mutableStateOf(false) }
    val myChatId by viewModel.myChatId.collectAsState()
    val isTorEnabled by viewModel.isTorEnabled.collectAsState()
    val isMeshEnabled by viewModel.isMeshEnabled.collectAsState()
    val connectedPeersCount by viewModel.connectedPeersCount.collectAsState()
    val activeMeshNodes by viewModel.activeMeshNodes.collectAsState()
    val torStatus by viewModel.torStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Encrypted Vault",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isMeshEnabled) Color(0xFF00E5FF) else if (isTorEnabled) Color(0xFFFFAB00) else Color(0xFF00E676),
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isMeshEnabled) "Off-Grid P2P Mesh ($connectedPeersCount Peers)" else if (isTorEnabled) "Tor Onion Anonymous Routing" else "RSA-2048 E2E Protocol",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMeshEnabled) Color(0xFF00E5FF) else if (isTorEnabled) Color(0xFFFFAB00) else Color(0xFF00E676)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMeshDialog = true },
                        modifier = Modifier
                            .background(
                                if (isMeshEnabled) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0x15FFFFFF),
                                CircleShape
                            )
                    ) {
                        Text("📡", fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { showTorDialog = true },
                        modifier = Modifier
                            .background(
                                if (isTorEnabled) Color(0xFFFFAB00).copy(alpha = 0.25f) else Color(0x15FFFFFF),
                                CircleShape
                            )
                    ) {
                        Text("🧅", fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { showNewChatDialog = true },
                        modifier = Modifier
                            .background(Color(0xFF7B3FE4).copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, "New Chat", tint = Color(0xFF00E5FF))
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0x337B3FE4), Color(0x1100E5FF))
                            )
                        )
                        .border(1.dp, Color(0x337B3FE4), RoundedCornerShape(20.dp))
                        .clickable { showSetIdDialog = true }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF7B3FE4).copy(alpha = 0.25f), CircleShape)
                                .border(1.dp, Color(0xFF7B3FE4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("My Secure Identity", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            Text(myChatId ?: "Tap to set handle (@username)", color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Icon(Icons.Default.Settings, contentDescription = "Edit ID", tint = Color(0xFF00E5FF))
                    }
                }
            }

            // Empty state illustration for real production use
            item {
                Spacer(Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF7B3FE4).copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color(0xFF7B3FE4).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "No Chats",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No Active Encrypted Chats",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap the + button to start a private, end-to-end encrypted conversation with a friend's handle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
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

        if (showTorDialog) {
            TorSettingsDialog(
                isTorEnabled = isTorEnabled,
                torStatus = torStatus,
                onToggleTor = { enabled -> viewModel.toggleTor(enabled) },
                onVerifyTor = { viewModel.verifyTor() },
                onDismiss = { showTorDialog = false }
            )
        }

        if (showMeshDialog) {
            MeshSettingsDialog(
                isMeshEnabled = isMeshEnabled,
                connectedPeersCount = connectedPeersCount,
                activeMeshNodes = activeMeshNodes,
                onToggleMesh = { enabled -> viewModel.toggleMesh(enabled) },
                onDismiss = { showMeshDialog = false }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7B3FE4), Color(0xFF00E5FF))
                        ),
                        CircleShape
                    )
                    .padding(2.dp)
                    .background(Color(0xFF121212), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("E2E", color = Color(0xFF00E676), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    lastMessage,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
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

@Composable
fun TorSettingsDialog(
    isTorEnabled: Boolean,
    torStatus: com.deepeye.musicpro.domain.network.TorVerificationResult?,
    onToggleTor: (Boolean) -> Unit,
    onVerifyTor: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
            modifier = Modifier.border(1.dp, Color(0x44FFAB00), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧅", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Tor Onion Routing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "SOCKS5 Proxy (127.0.0.1:9050)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFAB00)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anonymize All Traffic", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Route chat through Tor network", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isTorEnabled,
                        onCheckedChange = onToggleTor,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFAB00)
                        )
                    )
                }

                if (isTorEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1A000000), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Engine Mode: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    text = "Embedded Native Tor Proxy",
                                    color = Color(0xFF00E5FF),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tor Status: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    text = if (torStatus == null) "Active (Internal SOCKS5)" else if (torStatus.isTor) "Connected (Onion Exit)" else "Routed via Standalone Proxy",
                                    color = Color(0xFF00E676),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (torStatus != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "IP: ${torStatus.ip}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onVerifyTor,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFAB00))
                    ) {
                        Text("Verify Tor Exit Node IP", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B3FE4))
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun MeshSettingsDialog(
    isMeshEnabled: Boolean,
    connectedPeersCount: Int,
    activeMeshNodes: List<com.deepeye.musicpro.domain.network.MeshNode>,
    onToggleMesh: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.border(1.dp, Color(0x4400E5FF), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📡", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Off-Grid P2P Mesh Network",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "Wi-Fi Direct / Local P2P Port 8888",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Off-Grid P2P Mode", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Send E2E chats without Internet / SIM", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isMeshEnabled,
                        onCheckedChange = onToggleMesh,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF00E5FF)
                        )
                    )
                }

                if (isMeshEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Nearby Mesh Peers ($connectedPeersCount Connected):",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeMeshNodes.isEmpty()) {
                        Text("Searching for nearby mesh nodes...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeMeshNodes.forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x1A000000), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(node.deviceName, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${node.address} • Hop Distance: ${node.hopDistance}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text("P2P Mesh", color = Color(0xFF00E676), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B3FE4))
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
