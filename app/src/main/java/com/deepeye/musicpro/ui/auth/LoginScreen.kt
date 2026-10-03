// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.musicpro.R
import com.deepeye.musicpro.ui.theme.TouchTargets
import com.deepeye.musicpro.ui.theme.sdp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onYouTubeLoginClick: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var isSignUpMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Handle Auth States
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A)),
    ) {
        // Ambient Premium Background.
        //
        // These are radial gradients rather than blurred Boxes. A 120dp blur on a
        // 300dp Box is clipped to that Box's own bounds, so it renders as a
        // visibly hard-edged rectangle instead of a halo — the seams were
        // measurable in the a11y tree as a [342,0][894,360] node. A radial
        // gradient reaches zero alpha at its own edge, so it fades out with no
        // seam and needs no offscreen render pass.
        Box(
            modifier = Modifier
                .offset(x = (-150).dp, y = (-120).dp)
                .size(520.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF).copy(alpha = 0.22f),
                            Color(0xFF7C4DFF).copy(alpha = 0.0f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .offset(x = 150.dp, y = 170.dp)
                .size(520.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00D2FF).copy(alpha = 0.22f),
                            Color(0xFF00D2FF).copy(alpha = 0.0f),
                        ),
                    ),
                ),
        )

        // The form is taller than a landscape phone viewport (the stack of
        // fields, CTA, divider and two sign-in buttons needs ~600dp), so it is
        // scrollable and vertically centred. Without this the trailing "Sign Up"
        // link and the guest button were laid out past the bottom edge and
        // could not be reached at all — verified via the a11y tree, where the
        // last node sat at [616,683][989,720], flush against the 720px floor.
        LoginFormBody(
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            isSignUpMode = isSignUpMode,
            onToggleMode = { isSignUpMode = !isSignUpMode },
            primaryAction = if (isSignUpMode) "Create Account" else "Sign In",
            onPrimaryAction = {
                if (isSignUpMode) viewModel.signUpWithEmail(email, password)
                else viewModel.signInWithEmail(email, password)
            },
            onYouTubeLoginClick = onYouTubeLoginClick,
            onGoogleLogin = { viewModel.signInWithGoogle(context) },
            onSkipAsGuest = onLoginSuccess,
            isLoading = authState is AuthState.Loading,
        )
    }
}

/**
 * The sign-in form's contents, independent of auth state and view model.
 *
 * Split out so the layout contract — that the form scrolls and that no control
 * is laid out past the bottom edge on a short viewport — can be verified
 * directly by [LoginScreenLayoutTest] without standing up Hilt.
 */
@Composable
fun LoginFormBody(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isSignUpMode: Boolean,
    onToggleMode: () -> Unit,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    onYouTubeLoginClick: () -> Unit,
    onGoogleLogin: () -> Unit,
    onSkipAsGuest: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    // `email`/`password` are hoisted: the fields are driven straight from the
    // caller's state so that `onPrimaryAction` always submits what the user
    // typed. Local mirrors used to be kept here instead, which meant typing
    // updated only this composable and the parent still submitted its initial
    // empty values.
    BoxWithConstraints(modifier = modifier) {
        val compact = maxHeight < 480.dp
        val gutter = (if (compact) 24 else 32).sdp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gutter, vertical = (if (compact) 16 else 24).sdp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App Logo / Title
            Text(
                text = "DEEPEYE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = Color.White
            )
            Text(
                text = "MUSIC PRO",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 12.sp,
                color = Color(0xFF00D2FF)
            )

            Spacer(modifier = Modifier.height((if (compact) 20 else 64).sdp))

            // Email & Password Fields
            PremiumTextField(
                value = email,
                onValueChange = onEmailChange,
                hint = "Email Address",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height((if (compact) 12 else 16).sdp))

            PremiumTextField(
                value = password,
                onValueChange = onPasswordChange,
                hint = "Password",
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true,
                onImeAction = onPrimaryAction
            )

            Spacer(modifier = Modifier.height((if (compact) 16 else 32).sdp))

            // Main Action Button
            PremiumActionButton(
                text = primaryAction,
                isLoading = isLoading,
                onClick = onPrimaryAction
            )

            // Vertical rhythm scales with the viewport, and the `compact` branch
            // halves it further. The two levers multiply rather than either/or:
            // `compact` already exists because landscape is the short axis, while
            // `.sdp` handles the width. Keeping them independent means a narrow
            // but not compact viewport still gets proportional spacing.
            Spacer(modifier = Modifier.height((if (compact) 12 else 24).sdp))

            // Toggle Mode Text
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleMode() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                AnimatedContent(
                    targetState = isSignUpMode,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    }, label = ""
                ) { signUp ->
                    Text(
                        text = if (signUp) "Sign In" else "Sign Up",
                        color = Color(0xFF00D2FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height((if (compact) 16 else 32).sdp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                Text(
                    text = "OR",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.sdp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
            }

            Spacer(modifier = Modifier.height((if (compact) 16 else 32).sdp))

            // Google Sign In Button
            Spacer(modifier = Modifier.height((if (compact) 12 else 16).sdp))

            Button(
                onClick = onYouTubeLoginClick,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.sdp.coerceAtLeast(TouchTargets.Min)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Login with YouTube (TV)", color = Color.White, fontWeight = FontWeight.Bold)
            }

            GoogleSignInButton(
                isLoading = isLoading,
                onClick = onGoogleLogin
            )

            Spacer(modifier = Modifier.height((if (compact) 8 else 12).sdp))

            TextButton(
                onClick = onSkipAsGuest,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Skip & Continue as Guest →",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            }
        }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    isPassword: Boolean = false,
    onImeAction: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isFocused) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.1f),
        animationSpec = tween(300), label = ""
    )

    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.02f),
        animationSpec = tween(300), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.sdp.coerceAtLeast(TouchTargets.Min))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.sdp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        focusManager.clearFocus()
                        onImeAction()
                    }
                ),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(Color(0xFF00D2FF)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumActionButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.sdp.coerceAtLeast(TouchTargets.Min))
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00D2FF), Color(0xFF7C4DFF))
                )
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}


@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = ""
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.sdp.coerceAtLeast(TouchTargets.Min))
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            // Usually we'd load an actual Google icon here. Since we don't have the drawable yet, we use a colored text stand-in
            Text(
                text = "G",
                color = Color(0xFFEA4335), // Google Red
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "Continue with Google",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
