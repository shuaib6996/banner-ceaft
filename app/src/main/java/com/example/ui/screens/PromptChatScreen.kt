package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.window.Dialog
import com.example.data.ApiProviderConfig
import com.example.data.ApiProviderPresets
import com.example.data.ModelOption
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.state.ApiKeyViewModel
import com.example.state.BannerCreationViewModel
import com.example.state.PromptChatViewModel
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

@Composable
fun PromptChatScreen(
    promptChatViewModel: PromptChatViewModel,
    apiKeyViewModel: ApiKeyViewModel,
    bannerViewModel: BannerCreationViewModel,
    onUsePromptInBanner: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val chatState by promptChatViewModel.state.collectAsState()
    val apiKeyState by apiKeyViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputMessage by remember { mutableStateOf("") }
    var showModelSwitcherDialog by remember { mutableStateOf(false) }

    val quickSuggestions = listOf(
        "🎮 Neon Cyberpunk Gaming banner idea",
        "💻 Minimalist Coding & Tech Channel",
        "✈️ Aesthetic Travel Vlog banner concept",
        "📈 High-end Finance & Business branding",
        "🍳 Cozy Aesthetic Cooking & Food channel"
    )

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size, chatState.isTyping) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    if (showModelSwitcherDialog) {
        ChatModelSwitcherDialog(
            currentConfig = apiKeyState.chatConfig,
            savedProfiles = apiKeyState.savedChatProfiles,
            onSelectProfile = { profileId ->
                apiKeyViewModel.switchActiveChatProfile(profileId)
                showModelSwitcherDialog = false
            },
            onSelectModel = { modelId ->
                apiKeyViewModel.switchChatModel(modelId)
                showModelSwitcherDialog = false
            },
            onDismiss = { showModelSwitcherDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Quick Suggestion Chips (Shown on empty/initial chat, smoothly hides when conversation grows)
                AnimatedVisibility(visible = chatState.messages.size <= 2 && !chatState.isTyping) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSuggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                    .clickable(enabled = !chatState.isTyping) {
                                        promptChatViewModel.sendMessage(
                                            userText = suggestion,
                                            providerConfig = apiKeyState.chatConfig
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }

                // Neumorphic Input Bar pinned cleanly above the keyboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(shape = RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = {
                                Text(
                                    text = "Ask for banner ideas, themes, styles...",
                                    fontSize = 13.sp,
                                    color = SlateNeutral
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = false,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = if (inputMessage.isNotBlank() && !chatState.isTyping) 3.dp else 1.dp)
                                .clickable(enabled = inputMessage.isNotBlank() && !chatState.isTyping) {
                                    val toSend = inputMessage
                                    inputMessage = ""
                                    promptChatViewModel.sendMessage(
                                        userText = toSend,
                                        providerConfig = apiKeyState.chatConfig
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputMessage.isNotBlank() && !chatState.isTyping) CoralRed else SlateNeutral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                ScreenHeader(
                    title = "AI Prompt Chat",
                    onBackClick = onBack,
                    isDark = isDark,
                    onToggleDark = onToggleDark
                )

                // Active Model Info Pill & Clear Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                            .clickable { showModelSwitcherDialog = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SafeMint, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val modelSuffix = if (apiKeyState.chatConfig.chatModel.isNotBlank()) ": ${apiKeyState.chatConfig.chatModel}" else ""
                            Text(
                                text = "${apiKeyState.chatConfig.providerName}$modelSuffix",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "▼",
                                fontSize = 9.sp,
                                color = CoralRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .clickable { promptChatViewModel.clearChat() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear",
                                tint = SlateNeutral,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNeutral
                            )
                        }
                    }
                }

                // Error banner if any
                if (chatState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    NeumorphicCard(padding = 10.dp) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chatState.errorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = CoralRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { promptChatViewModel.clearError() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = SlateNeutral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // 1-Tap Quick Switchers to alternate provider
                            val isGemini = apiKeyState.chatConfig.providerId == "gemini"
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isGemini) {
                                    Box(
                                        modifier = Modifier
                                            .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 3.dp)
                                            .clickable {
                                                apiKeyViewModel.switchToNvidiaChat()
                                                promptChatViewModel.clearError()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "⚡ Switch to NVIDIA NIM",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SafeMint
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 3.dp)
                                            .clickable {
                                                apiKeyViewModel.switchToGoogleChat()
                                                promptChatViewModel.clearError()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "✨ Switch to Google Gemini",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Messages List — Scrolls edge-to-edge across the full page!
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatState.messages, key = { it.id }) { message ->
                    ChatBubbleItem(
                        message = message,
                        onCopyText = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Prompt", text)
                            clipboard.setPrimaryClip(clip)
                            scope.launch { snackbarHostState.showSnackbar("Copied to clipboard!") }
                        },
                        onApplyPrompt = { text ->
                            val cleanTopic = text.lines().firstOrNull { it.isNotBlank() }
                                ?.removePrefix("#")?.removePrefix("*")?.trim()
                                ?: text.take(50)
                            bannerViewModel.updateChannelTopic(cleanTopic.take(60))
                            bannerViewModel.updateCustomPromptDetails(text.take(150))
                            onUsePromptInBanner()
                        }
                    )
                }

                if (chatState.isTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CoralRed,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AI is crafting creative ideas...",
                                fontSize = 12.sp,
                                color = SlateNeutral,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onCopyText: (String) -> Unit,
    onApplyPrompt: (String) -> Unit
) {
    if (message.isUser) {
        // User Message Bubble (Right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .neumorphicInset(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        // AI Assistant Message Bubble (Left aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .neumorphicConvex(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        elevation = 3.dp
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gemini Creative Director",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }

                        // Copy button
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = CircleShape, elevation = 1.dp)
                                .clickable { onCopyText(message.text) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = SlateNeutral,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "Copy", fontSize = 10.sp, color = SlateNeutral, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Use as Banner Prompt" Action button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                            .clickable { onApplyPrompt(message.text) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = SafeMint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Use This Idea for My Banner 🚀",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeMint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatModelSwitcherDialog(
    currentConfig: ApiProviderConfig,
    savedProfiles: List<ApiProviderConfig>,
    onSelectProfile: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customModelInput by remember(currentConfig.chatModel) {
        mutableStateOf(currentConfig.chatModel)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Switch Chat Model",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Change AI model or provider instantly",
                                fontSize = 11.sp,
                                color = SlateNeutral
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SlateNeutral,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Provider Profiles Row (Always shown for instant switching)
                    val effectiveProfiles = if (savedProfiles.isEmpty()) {
                        listOf(
                            ApiProviderConfig(
                                id = "default_chat_nvidia",
                                providerId = "openai_compatible",
                                providerName = "NVIDIA NIM",
                                baseUrl = "https://integrate.api.nvidia.com/v1",
                                apiKey = "",
                                chatModel = "meta/llama-3.2-11b-vision-instruct",
                                isOpenAiCompatible = true
                            ),
                            ApiProviderConfig(
                                id = "default_chat_gemini",
                                providerId = "gemini",
                                providerName = "Google Gemini (Direct)",
                                baseUrl = "https://generativelanguage.googleapis.com",
                                apiKey = "",
                                chatModel = "gemini-3.8-flash",
                                isOpenAiCompatible = false
                            )
                        )
                    } else savedProfiles

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Provider Profile",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        effectiveProfiles.forEach { profile ->
                            val isSelected = profile.id == currentConfig.id ||
                                (profile.providerId == currentConfig.providerId && profile.isOpenAiCompatible == currentConfig.isOpenAiCompatible)
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = if (isSelected) 1.dp else 2.dp)
                                    .clickable { onSelectProfile(profile.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(SafeMint, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = profile.providerName.ifBlank { "Custom" },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Available Chat Models",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentConfig.providerId == "gemini") {
                        // Gemini Models List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ApiProviderPresets.GEMINI_CHAT_MODELS) { modelOpt ->
                                val isSelected = currentConfig.chatModel.equals(modelOpt.id, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = if (isSelected) 1.dp else 2.dp)
                                        .clickable { onSelectModel(modelOpt.id) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = modelOpt.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = modelOpt.description,
                                                fontSize = 10.sp,
                                                color = SlateNeutral,
                                                lineHeight = 14.sp
                                            )
                                        }

                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = SafeMint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (currentConfig.baseUrl.contains("nvidia", ignoreCase = true) || currentConfig.providerId == "openai_compatible") {
                        // NVIDIA Tested Models List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ApiProviderPresets.NVIDIA_CHAT_MODELS) { modelOpt ->
                                val isSelected = currentConfig.chatModel.equals(modelOpt.id, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = if (isSelected) 1.dp else 2.dp)
                                        .clickable {
                                            customModelInput = modelOpt.id
                                            onSelectModel(modelOpt.id)
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = modelOpt.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = modelOpt.description,
                                                fontSize = 10.sp,
                                                color = SlateNeutral,
                                                lineHeight = 14.sp
                                            )
                                        }

                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = SafeMint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {

                            InsetTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                label = "Model ID",
                                placeholder = "e.g. meta/llama-3.2-11b-vision-instruct, gpt-4o"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            PrimaryButton(
                                text = "Apply Model",
                                enabled = customModelInput.isNotBlank(),
                                onClick = {
                                    onSelectModel(customModelInput.trim())
                                }
                            )
                        }
                    } else {
                        // General Custom OpenAI-compatible Provider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val preset = ApiProviderPresets.getPresetById(currentConfig.providerId)
                            if (preset.popularChatModels.isNotEmpty()) {
                                Text(
                                    text = "Quick Select Models",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateNeutral
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    preset.popularChatModels.forEach { popModel ->
                                        val isSelected = customModelInput.equals(popModel, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .neumorphicConvex(shape = CircleShape, elevation = if (isSelected) 1.dp else 2.dp)
                                                .clickable {
                                                    customModelInput = popModel
                                                    onSelectModel(popModel)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = popModel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            InsetTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                label = "Model ID",
                                placeholder = "e.g. gpt-4o, claude-3-5-sonnet"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            PrimaryButton(
                                text = "Apply Model",
                                enabled = customModelInput.isNotBlank(),
                                onClick = {
                                    onSelectModel(customModelInput.trim())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
