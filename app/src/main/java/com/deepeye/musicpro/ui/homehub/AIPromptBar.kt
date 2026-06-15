// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.homehub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIPromptBar(
    isGenerating: Boolean,
    onSubmitPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var promptText by remember { mutableStateOf("") }
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00D2FF), Color(0xFF7C4DFF))
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(gradientBrush, shape = RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            TextField(
                value = promptText,
                onValueChange = { promptText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (isGenerating) "AI is curating..." else "Play chill romantic songs...",
                        color = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF00D2FF)
                ),
                singleLine = true,
                enabled = !isGenerating,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (promptText.isNotBlank() && !isGenerating) {
                            onSubmitPrompt(promptText)
                            promptText = ""
                        }
                    }
                )
            )

            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    color = Color(0xFF00D2FF),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onSubmitPrompt(promptText)
                            promptText = ""
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (promptText.isNotBlank()) Color(0xFF00D2FF) else Color.Gray
                    )
                }
            }
        }
    }
}
