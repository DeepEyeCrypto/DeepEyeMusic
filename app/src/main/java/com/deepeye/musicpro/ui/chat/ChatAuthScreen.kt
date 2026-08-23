package com.deepeye.musicpro.ui.chat

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
    val context = LocalContext.current
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    val showBiometricPrompt = {
        var activity: FragmentActivity? = context as? FragmentActivity
        if (activity == null) {
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is FragmentActivity) {
                    activity = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }
        }

        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onAuthenticated()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                android.widget.Toast.makeText(context, "Biometric error: $errString", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock DeepEye Vault")
                    .setSubtitle("Authenticate using biometrics or screen lock for E2E Chat")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                android.widget.Toast.makeText(context, "Biometrics or device PIN not configured on device", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "FragmentActivity not found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var isPasswordSet by remember { mutableStateOf(viewModel.hasChatPassword()) }

    val showBiometricResetPrompt = {
        var activity: FragmentActivity? = context as? FragmentActivity
        if (activity == null) {
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is FragmentActivity) {
                    activity = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }
        }

        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            viewModel.resetChatPassword()
                            isPasswordSet = false
                            passwordInput = ""
                            errorMessage = null
                            android.widget.Toast.makeText(context, "Identity Verified! Set a new Vault PIN.", android.widget.Toast.LENGTH_LONG).show()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                android.widget.Toast.makeText(context, "Reset cancelled: $errString", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Reset Vault Security PIN")
                    .setSubtitle("Verify biometrics or screen lock to create a new PIN")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                android.widget.Toast.makeText(context, "Biometrics or screen lock PIN required for PIN reset", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(isPasswordSet) {
        if (isPasswordSet) {
            showBiometricPrompt()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x331E1B4B),
                                Color(0x220F172A)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0x667B3FE4),
                                Color(0x3300E5FF)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .scale(scalePulse)
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x337B3FE4), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF7B3FE4).copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, Color(0xFF7B3FE4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Vault Lock",
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isPasswordSet) "DeepEye Vault" else "Set Security PIN",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isPasswordSet) "AES-256 / RSA Protected" else "Secure your chats with a PIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00E676)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { 
                        passwordInput = it
                        errorMessage = null 
                    },
                    placeholder = { Text(if (isPasswordSet) "Enter Vault PIN" else "Create New Vault PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        cursorColor = Color(0xFF00E5FF),
                        focusedContainerColor = Color(0x11FFFFFF),
                        unfocusedContainerColor = Color(0x0AFFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (passwordInput.isBlank()) {
                            errorMessage = "PIN cannot be empty"
                            return@Button
                        }
                        if (isPasswordSet) {
                            if (viewModel.verifyChatPassword(passwordInput)) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onAuthenticated()
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                errorMessage = "Incorrect PIN"
                            }
                        } else {
                            viewModel.setChatPassword(passwordInput)
                            isPasswordSet = true
                            onAuthenticated()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7B3FE4)
                    )
                ) {
                    Text(
                        text = if (isPasswordSet) "Unlock Vault" else "Set PIN & Enter",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isPasswordSet) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showBiometricPrompt() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Biometrics",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Use Biometrics",
                            color = Color(0xFF00E5FF),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Forgot PIN? Reset via Biometrics",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBiometricResetPrompt() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
