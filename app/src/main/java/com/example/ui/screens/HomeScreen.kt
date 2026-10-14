package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.BannerCreationViewModel
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.state.AuthViewModel
import com.example.ui.theme.neumorphicInset

@Composable
fun HomeScreen(
    bannerViewModel: BannerCreationViewModel,
    onStartNewBanner: () -> Unit,
    onNavigateToPromptChat: () -> Unit,
    onNavigateToImageStudio: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    val authState by authViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Neumorphic Bottom Navigation Bar with system navigation bar insets
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .neumorphicConvex(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), elevation = 8.dp)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(icon = "🪄", label = "Banner", isSelected = true) {
                        bannerViewModel.resetToDefaults()
                        onStartNewBanner()
                    }
                    NavItem(icon = "💬", label = "AI Chat", isSelected = false) {
                        onNavigateToPromptChat()
                    }
                    NavItem(icon = "🎨", label = "Studio", isSelected = false) {
                        onNavigateToImageStudio()
                    }
                    NavItem(icon = "📂", label = "History", isSelected = false) {
                        onNavigateToHistory()
                    }
                    NavItem(icon = "⚙️", label = "Settings", isSelected = false) {
                        onNavigateToSettings()
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            ScreenHeader(
                title = "BannerCraft",
                onSettingsClick = onNavigateToSettings,
                isDark = isDark,
                onToggleDark = onToggleDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Greeting Section with Login / Account Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (authState.isLoggedIn) "Hey ${authState.displayName}! 👋" else "Hey Creator! 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Account / Login Pill Button
                Box(
                    modifier = Modifier
                        .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                        .clickable {
                            if (authState.isLoggedIn) onNavigateToSettings()
                            else onNavigateToLogin()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (authState.isLoggedIn) "👤 ${authState.displayName.take(8)}" else "🔐 Sign In",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (authState.isLoggedIn) SafeMint else CoralRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Ready to give your YouTube channel a fresh, viral look?",
                fontSize = 13.sp,
                color = SlateNeutral,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Start Banner Hero Card
            NeumorphicCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            bannerViewModel.resetToDefaults()
                            onStartNewBanner()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🪄", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Start Banner",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "1546 × 423 Safe",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeMint,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Design YouTube safe-zone banners with AI",
                                fontSize = 12.sp,
                                color = SlateNeutral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Create ›",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryButton(
                    text = "Start New Banner",
                    onClick = {
                        bannerViewModel.resetToDefaults()
                        onStartNewBanner()
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowForward
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Prompt Chat Assistant Card
            NeumorphicCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPromptChat() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💬", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI Prompt Chat Assistant",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(CoralRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "LIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = CoralRed)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Chat with Gemini to brainstorm banner ideas & prompts",
                                fontSize = 12.sp,
                                color = SlateNeutral
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Chat",
                        tint = CoralRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Image Studio Card (Create & Edit Images)
            NeumorphicCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToImageStudio() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Image Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Generate & edit visuals",
                                fontSize = 12.sp,
                                color = SlateNeutral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Open ›",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Room History Quick Button Card
            NeumorphicCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHistory() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recent Banners",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Saved in local database",
                                fontSize = 12.sp,
                                color = SlateNeutral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "History ›",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Safe-Zone Tip Card
            NeumorphicCard {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "The Safe-Zone Secret 🎯",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "YouTube crops your banner dramatically on phones! Keep all your main logos, social handles, and big text strictly in the center 1546 × 423 px area so nothing gets chopped off.",
                            fontSize = 12.sp,
                            color = SlateNeutral,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✔ Auto-guaranteed on all BannerCraft templates",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeMint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.NavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (isSelected) Modifier.background(CoralRed.copy(alpha = 0.15f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 17.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) CoralRed else SlateNeutral,
            maxLines = 1,
            softWrap = false
        )
    }
}
