package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.AuthViewModel
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CoralRedLight
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN, SIGN_UP
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by authViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // React to messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            authViewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            authViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Header with back & theme toggle
            ScreenHeader(
                title = if (authMode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                onBackClick = onBack,
                isDark = isDark,
                onToggleDark = onToggleDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Branding Icon Hero
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .neumorphicConvex(shape = CircleShape, elevation = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (authMode == AuthMode.SIGN_IN) "🔐" else "🚀",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (authMode == AuthMode.SIGN_IN) "Welcome Back, Creator!" else "Join BannerCraft",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (authMode == AuthMode.SIGN_IN)
                    "Access your saved banners, presets and cloud sync."
                else
                    "Create your account to start generating pro YouTube banners.",
                fontSize = 12.sp,
                color = SlateNeutral,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Neumorphic Mode Switcher Tab (Sign In vs Register)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(shape = RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 1: Sign In
                val isSignIn = authMode == AuthMode.SIGN_IN
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSignIn) Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                            else Modifier
                        )
                        .clickable { authMode = AuthMode.SIGN_IN }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = "Sign In",
                            tint = if (isSignIn) CoralRed else SlateNeutral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sign In",
                            fontSize = 13.sp,
                            fontWeight = if (isSignIn) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSignIn) CoralRed else SlateNeutral
                        )
                    }
                }

                // Tab 2: Register
                val isSignUp = authMode == AuthMode.SIGN_UP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSignUp) Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                            else Modifier
                        )
                        .clickable { authMode = AuthMode.SIGN_UP }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Register",
                            tint = if (isSignUp) CoralRed else SlateNeutral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create Account",
                            fontSize = 13.sp,
                            fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSignUp) CoralRed else SlateNeutral
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Input Container Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Name Field (Only on Sign Up)
                    AnimatedVisibility(visible = authMode == AuthMode.SIGN_UP) {
                        Column {
                            AuthInputField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = "YOUR NAME / CHANNEL NAME",
                                placeholder = "e.g. Alex Tech, GameCraft",
                                leadingIcon = Icons.Default.Person,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Email Field
                    AuthInputField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = "EMAIL ADDRESS",
                        placeholder = "creator@example.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    AuthInputField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = "PASSWORD",
                        placeholder = if (authMode == AuthMode.SIGN_UP) "Min. 6 characters" else "••••••••",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (authMode == AuthMode.SIGN_IN) {
                                authViewModel.signInWithEmail(emailInput, passwordInput) {
                                    onLoginSuccess()
                                }
                            } else {
                                authViewModel.signUpWithEmail(nameInput, emailInput, passwordInput) {
                                    onLoginSuccess()
                                }
                            }
                        })
                    )

                    // Forgot Password Link (Only on Sign In)
                    if (authMode == AuthMode.SIGN_IN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoralRed,
                                modifier = Modifier.clickable {
                                    if (emailInput.isNotBlank()) {
                                        authViewModel.sendPasswordReset(emailInput)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Enter your email address above first.")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Submit Action Button
                    PrimaryButton(
                        text = if (state.isLoading) "Please wait..."
                        else if (authMode == AuthMode.SIGN_IN) "Sign In"
                        else "Create Account",
                        icon = if (authMode == AuthMode.SIGN_IN) Icons.AutoMirrored.Filled.Login else Icons.Default.PersonAdd,
                        enabled = !state.isLoading && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        onClick = {
                            focusManager.clearFocus()
                            if (authMode == AuthMode.SIGN_IN) {
                                authViewModel.signInWithEmail(emailInput, passwordInput) {
                                    onLoginSuccess()
                                }
                            } else {
                                authViewModel.signUpWithEmail(nameInput, emailInput, passwordInput) {
                                    onLoginSuccess()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider Or
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(SlateNeutral.copy(alpha = 0.2f))
                )
                Text(
                    text = "  OR  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNeutral
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(SlateNeutral.copy(alpha = 0.2f))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Guest Mode Option ("Continue as Guest")
            SecondaryButton(
                text = "Continue as Guest",
                onClick = {
                    authViewModel.signInAsGuest {
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Guest mode allows you to create banners without signing in. You can link your account later in Settings.",
                fontSize = 11.sp,
                color = SlateNeutral,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNeutral,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = SlateNeutral,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = SlateNeutral.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        cursorBrush = SolidColor(CoralRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (isPassword) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = SlateNeutral,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onTogglePasswordVisibility() }
                    )
                }
            }
        }
    }
}
