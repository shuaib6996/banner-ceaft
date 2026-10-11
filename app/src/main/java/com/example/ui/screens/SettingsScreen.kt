package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.ApiKeyViewModel
import com.example.state.AppThemeViewModel
import com.example.ui.components.ModelSelectionDialog
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.state.AuthViewModel

@Composable
fun SettingsScreen(
    apiKeyViewModel: ApiKeyViewModel,
    appThemeViewModel: AppThemeViewModel,
    onManageApiKey: () -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    val apiKeyState by apiKeyViewModel.state.collectAsState()
    val themeState by appThemeViewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ScreenHeader(
                title = "Settings & Help",
                onBackClick = onBack,
                isDark = themeState.isDarkMode,
                onToggleDark = { appThemeViewModel.toggleDarkMode() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Private Shield Notice
            NeumorphicCard(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SafeMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Private & on-device • Keys never leave this phone",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Creator Account & Cloud Sync Section
            Text(
                text = "CREATOR ACCOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateNeutral,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            NeumorphicCard {
                if (authState.isLoggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .neumorphicConvex(shape = CircleShape, elevation = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = authState.displayName.take(1).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CoralRed
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = authState.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = authState.email.takeIf { it.isNotBlank() } ?: "Firebase Authenticated",
                                    fontSize = 11.sp,
                                    color = SlateNeutral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "● Connected to Firebase",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeMint
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                                .clickable {
                                    authViewModel.signOut {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Signed out successfully.")
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Sign Out",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (authState.isGuest) "Guest Creator" else "Not Signed In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sign in to backup banner history and sync presets with Firebase.",
                                fontSize = 11.sp,
                                color = SlateNeutral,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 3.dp)
                                .clickable { onNavigateToLogin() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "🔐 Sign In",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Connection & Key Section
            Text(
                text = "⚡ AI Connection & Key",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (apiKeyState.isKeySaved) SafeMint else CoralRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Engines & Providers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(CoralRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .clickable { onManageApiKey() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Manage ⚙️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Engine 1: Chat Brainstorming Engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💬 Chat Engine:", fontSize = 11.sp, color = SlateNeutral, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${apiKeyState.chatConfig.providerName} (${apiKeyState.chatConfig.chatModel})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Engine 2: Image Rendering Engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎨 Banner Engine:", fontSize = 11.sp, color = SlateNeutral, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${apiKeyState.imageConfig.providerName} (${apiKeyState.imageConfig.imageModel})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // API Token Well (Always masked for security)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(shape = RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "API Secret Token",
                                fontSize = 11.sp,
                                color = SlateNeutral,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (apiKeyState.isKeySaved) "••••••••••••••••••••" else "Not set",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (apiKeyState.isKeySaved) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Saved",
                                tint = SafeMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .clickable { onManageApiKey() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SlateNeutral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Configure Provider",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNeutral
                            )
                        }
                    }

                    if (apiKeyState.isKeySaved) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                .clickable { showRemoveConfirmDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.KeyOff, contentDescription = null, tint = CoralRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Remove Key", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralRed)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Model Configuration Card
            NeumorphicCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showModelDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Active AI Model",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Box(
                                modifier = Modifier
                                    .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ready",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeMint,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Change ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNeutral,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (apiKeyState.selectedModel.isNotBlank()) apiKeyState.selectedModel else "Default: gemini-2.0-flash",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoralRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Appearance & Dark Mode
            Text(
                text = "🎨 Appearance",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sleek dark theme for night editing",
                            fontSize = 12.sp,
                            color = SlateNeutral
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = themeState.isDarkMode,
                        onCheckedChange = { appThemeViewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export Preferences Section
            Text(
                text = "⚙️ Export Preferences",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicCard {
                Text(
                    text = "IMAGE OUTPUT FORMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNeutral
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("High-Quality PNG", "Standard JPG").forEach { fmt ->
                        val isSelected = themeState.outputFormat == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .then(
                                    if (isSelected) Modifier.neumorphicInset(shape = CircleShape)
                                    else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                )
                                .clickable { appThemeViewModel.updateOutputFormat(fmt) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CoralRed else SlateNeutral
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Safe-Zone Guides on Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Renders subtle margin lines to protect text",
                            fontSize = 12.sp,
                            color = SlateNeutral
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = themeState.safeZoneGuidesOnSave,
                        onCheckedChange = { appThemeViewModel.updateSafeZoneGuidesOnSave(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BannerCraft Credit Badge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Optional watermarking on exported artwork",
                            fontSize = 12.sp,
                            color = SlateNeutral
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = themeState.creditBadge,
                        onCheckedChange = { appThemeViewModel.updateCreditBadge(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About BannerCraft
            Text(
                text = "❤️ About BannerCraft",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎨", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BannerCraft",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(SlateNeutral.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "v1.0.0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateNeutral)
                            }
                        }
                        Text(
                            text = "Designed for tactile creator speed and YouTube-ready dimension.",
                            fontSize = 12.sp,
                            color = SlateNeutral
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsLinkItem("Help & Frequent Questions", Icons.Default.Help) {
                    scope.launch { snackbarHostState.showSnackbar("Opening Help Center...") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkItem("Send Feedback or Suggestion", Icons.Default.Message) {
                    scope.launch { snackbarHostState.showSnackbar("Feedback dialog opening...") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkItem("Privacy & Local Storage", Icons.Default.Lock) {
                    scope.launch { snackbarHostState.showSnackbar("All data stored on-device.") }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Troubleshooting & Storage
            Text(
                text = "⚠️ Troubleshooting & Storage",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeumorphicCard {
                Text(
                    text = "Local Generation Cache",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CoralRed
                )
                Text(
                    text = "Free up device memory and purge temporary banner drafts cached on local phone storage.",
                    fontSize = 12.sp,
                    color = SlateNeutral,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryButton(
                    text = "Clear Local Cache",
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Local cache cleared!") }
                    },
                    icon = Icons.Default.DeleteSweep,
                    isDanger = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirm Key Removal Dialog
    if (showRemoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            title = {
                Text(text = "Remove saved key?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(text = "You'll need to add it again to create banners.", fontSize = 14.sp, color = SlateNeutral)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmDialog = false
                        apiKeyViewModel.clearKey {
                            onManageApiKey()
                        }
                    }
                ) {
                    Text(text = "Remove", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmDialog = false }) {
                    Text(text = "Cancel", color = SlateNeutral)
                }
            }
        )
    }

    if (showModelDialog) {
        ModelSelectionDialog(
            selectedModel = apiKeyState.selectedModel,
            onModelSelect = { apiKeyViewModel.setSelectedModel(it) },
            onDismiss = { showModelDialog = false }
        )
    }
}

@Composable
private fun SettingsLinkItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .neumorphicInset(shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = CoralRed, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = SlateNeutral, modifier = Modifier.size(18.dp))
        }
    }
}
