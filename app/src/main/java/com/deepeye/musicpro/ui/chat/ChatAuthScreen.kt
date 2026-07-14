package com.deepeye.musicpro.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAuthScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit
) {
    val hasPassword = remember { viewModel.hasChatPassword() }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF7B3FE4)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (hasPassword) "Enter Chat Password" else "Set a Chat Password",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasPassword) "Unlock encrypted chats." else "Protect your secret chats with a password.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { 
                    passwordInput = it
                    errorMessage = null 
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7B3FE4),
                    cursorColor = Color(0xFF7B3FE4)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (passwordInput.isBlank()) {
                        errorMessage = "Password cannot be empty"
                        return@Button
                    }
                    if (hasPassword) {
                        if (viewModel.verifyChatPassword(passwordInput)) {
                            onAuthenticated()
                        } else {
                            errorMessage = "Incorrect password"
                        }
                    } else {
                        viewModel.setChatPassword(passwordInput)
                        onAuthenticated()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B3FE4))
            ) {
                Text(if (hasPassword) "Unlock" else "Set Password & Continue")
            }
        }
    }
}
