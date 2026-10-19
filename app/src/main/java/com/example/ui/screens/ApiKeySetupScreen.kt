package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ApiProviderConfig
import com.example.data.ApiProviderPresets
import com.example.data.ConnectionTestResult
import com.example.data.ModelOption
import com.example.data.ProviderPreset
import com.example.state.ApiKeyViewModel
import com.example.state.ConfigTab
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

@Composable
fun ApiKeySetupScreen(
    apiKeyViewModel: ApiKeyViewModel,
    onContinueToHome: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val state by apiKeyViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentConfig = state.currentEditingConfig
    val currentProfileId = currentConfig.id

    // Form inputs dynamically re-bound to current editing config and active tab
    var selectedPresetId by remember(currentProfileId, currentConfig.providerId, state.activeTab) {
        mutableStateOf(currentConfig.providerId)
    }
    var providerName by remember(currentProfileId, currentConfig.providerName, state.activeTab) {
        mutableStateOf(currentConfig.providerName)
    }
    fun extractCleanAccountId(url: String): String {
        val trimmed = url.trim().removeSurrounding("\"").removeSurrounding("'")
        val stripped = trimmed.trimStart('{', '<', '[', '(').trimEnd('}', '>', ']', ')')
        if (stripped.isNotBlank() && !stripped.contains("/") && !stripped.contains(".")) {
            return stripped.replace(Regex("(?i)^account[-_]?id[:= ]*"), "").trim()
        }
        val match = Regex("""accounts/([^/]+)""").find(trimmed)
        val extracted = match?.groupValues?.get(1)?.trim()
            ?.trimStart('{', '<', '[', '(')?.trimEnd('}', '>', ']', ')')?.trim()
        return if (extracted != null && !extracted.equals("ACCOUNT_ID", ignoreCase = true)) extracted else ""
    }

    var baseUrl by remember(currentProfileId, currentConfig.baseUrl, state.activeTab) {
        mutableStateOf(currentConfig.baseUrl)
    }
    var cloudflareAccountId by remember(currentProfileId, currentConfig.baseUrl, state.activeTab) {
        mutableStateOf(extractCleanAccountId(currentConfig.baseUrl))
    }
    var inputKey by remember(currentProfileId, currentConfig.apiKey, state.activeTab) {
        mutableStateOf(currentConfig.apiKey)
    }
    var chatModel by remember(currentProfileId, currentConfig.chatModel, state.activeTab) {
        mutableStateOf(currentConfig.chatModel)
    }
    var imageModel by remember(currentProfileId, currentConfig.imageModel, state.activeTab) {
        mutableStateOf(currentConfig.imageModel)
    }

    var isKeyVisible by remember { mutableStateOf(false) }
    var inlineErrorText by remember { mutableStateOf<String?>(null) }
    var showGeminiChatModelDialog by remember { mutableStateOf(false) }
    var showGeminiImageModelDialog by remember { mutableStateOf(false) }

    val activePreset = remember(selectedPresetId) {
        ApiProviderPresets.getPresetById(selectedPresetId)
    }

    // Automatically synchronize form inputs whenever the active editing profile switches
    LaunchedEffect(currentConfig) {
        selectedPresetId = currentConfig.providerId
        providerName = currentConfig.providerName
        baseUrl = currentConfig.baseUrl
        cloudflareAccountId = extractCleanAccountId(currentConfig.baseUrl)
        inputKey = currentConfig.apiKey
        chatModel = currentConfig.chatModel
        imageModel = currentConfig.imageModel
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SlateNeutral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Box(
                        modifier = Modifier
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "API Provider Setup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                        .clickable { onContinueToHome() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = SlateNeutral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Title
            Text(
                text = "OpenAI Compatible Providers",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect multiple API keys, providers & models (like Roo Code)",
                fontSize = 12.sp,
                color = SlateNeutral,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Neumorphic Configuration Tab Selector (AI Chat vs Image Generator)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tab 1: AI Chat Provider Setup
                val isChatTab = state.activeTab == ConfigTab.CHAT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .neumorphicConvex(shape = RoundedCornerShape(14.dp), elevation = if (isChatTab) 1.dp else 3.dp)
                        .then(
                            if (isChatTab) Modifier.border(1.5.dp, CoralRed, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable { apiKeyViewModel.setActiveTab(ConfigTab.CHAT) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💬 AI Chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isChatTab) CoralRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Brainstorm & Ideas",
                            fontSize = 10.sp,
                            color = SlateNeutral
                        )
                    }
                }

                // Tab 2: Image Generation Setup
                val isImageTab = state.activeTab == ConfigTab.IMAGE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .neumorphicConvex(shape = RoundedCornerShape(14.dp), elevation = if (isImageTab) 1.dp else 3.dp)
                        .then(
                            if (isImageTab) Modifier.border(1.5.dp, CoralRed, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable { apiKeyViewModel.setActiveTab(ConfigTab.IMAGE) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎨 Image / Banner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isImageTab) CoralRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Imagen 3 / Flux / SD",
                            fontSize = 10.sp,
                            color = SlateNeutral
                        )
                    }
                }
            }

            // Saved Profiles & Keys Carousel
            if (state.savedProfiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Profiles & Keys (${state.savedProfiles.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .clickable {
                                apiKeyViewModel.startNewProfile(ApiProviderPresets.PRESET_OPENAI_COMPATIBLE)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New Key",
                                tint = CoralRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Add Key",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.savedProfiles.forEach { profile ->
                        val isCurrentActive = profile.id == state.providerConfig.id
                        Box(
                            modifier = Modifier
                                .width(210.dp)
                                .neumorphicConvex(
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = if (isCurrentActive) 1.dp else 3.dp
                                )
                                .then(
                                    if (isCurrentActive) Modifier.border(1.5.dp, SafeMint, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable {
                                    apiKeyViewModel.switchActiveProfile(profile.id)
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = profile.providerName.ifBlank { "Custom API" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (isCurrentActive) {
                                        Box(
                                            modifier = Modifier
                                                .background(SafeMint.copy(alpha = 0.15f), CircleShape)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Active ●",
                                                color = SafeMint,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (state.savedProfiles.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = SlateNeutral,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    apiKeyViewModel.deleteProfile(profile.id)
                                                }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (profile.apiKey.isNotBlank()) "Key: ••••••••${profile.apiKey.takeLast(4)}" else "No Key Set",
                                    fontSize = 10.sp,
                                    color = SlateNeutral,
                                    maxLines = 1
                                )

                                Text(
                                    text = if (state.activeTab == ConfigTab.CHAT) {
                                        "Chat: ${profile.chatModel.ifBlank { "Default" }}"
                                    } else {
                                        "Image: ${profile.imageModel.ifBlank { "Default" }}"
                                    },
                                    fontSize = 10.sp,
                                    color = SlateNeutral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Provider Preset Selector (Horizontal scrollable chips)
            Text(
                text = "Configure Selected Profile Preset",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayedPresets = if (state.activeTab == ConfigTab.IMAGE) {
                    listOf(
                        ApiProviderPresets.PRESET_CLOUDFLARE,
                        ApiProviderPresets.PRESET_OPENAI_COMPATIBLE,
                        ApiProviderPresets.PRESET_GEMINI,
                        ApiProviderPresets.PRESET_HUGGINGFACE
                    ) + ApiProviderPresets.ALL_PRESETS.filter {
                        it.id != "cloudflare" && it.id != "openai_compatible" && it.id != "gemini" && it.id != "huggingface"
                    }
                } else {
                    ApiProviderPresets.ALL_PRESETS
                }
                displayedPresets.forEach { preset ->
                    val isSelected = preset.id.equals(selectedPresetId, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .neumorphicConvex(
                                shape = RoundedCornerShape(12.dp),
                                elevation = if (isSelected) 1.dp else 3.dp
                            )
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, CoralRed, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable {
                                apiKeyViewModel.selectPreset(preset)
                                inlineErrorText = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Provider Configuration Card
            NeumorphicCard(padding = 16.dp) {
                // Preset Description Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activePreset.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field 1: Provider Name
                Text(
                    text = "Provider Name",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                InsetTextField(
                    value = providerName,
                    onValueChange = { providerName = it },
                    placeholder = "e.g. OpenRouter, Groq, DeepSeek",
                    leadingIcon = Icons.Default.Psychology
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2: Base URL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "API Base URL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedPresetId != "gemini") {
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                .clickable {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            baseUrl = clip.trim()
                                            val extracted = extractCleanAccountId(clip)
                                            if (extracted.isNotBlank()) {
                                                cloudflareAccountId = extracted
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CoralRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                InsetTextField(
                    value = baseUrl,
                    onValueChange = { input ->
                        baseUrl = input
                        val extracted = extractCleanAccountId(input)
                        if (extracted.isNotBlank()) {
                            cloudflareAccountId = extracted
                        }
                    },
                    placeholder = if (selectedPresetId == "gemini") {
                        "https://generativelanguage.googleapis.com"
                    } else if (selectedPresetId == "cloudflare") {
                        "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/"
                    } else {
                        "Enter or paste Base URL (e.g. https://api.openai.com/v1)"
                    },
                    leadingIcon = Icons.Default.Link
                )

                // Dedicated Section: Cloudflare Account ID (Right below Base URL)
                if (selectedPresetId == "cloudflare" || baseUrl.contains("cloudflare", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(shape = RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🆔",
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cloudflare Account ID",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralRed
                                    )
                                }

                                // Quick Paste Button for Account ID
                                Box(
                                    modifier = Modifier
                                        .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                        .clickable {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    val cleanClip = extractCleanAccountId(clip)
                                                    if (cleanClip.isNotBlank()) {
                                                        cloudflareAccountId = cleanClip
                                                        baseUrl = "https://api.cloudflare.com/client/v4/accounts/$cleanClip/ai/run/"
                                                        inlineErrorText = null
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = CoralRed,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Paste ID",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            InsetTextField(
                                value = cloudflareAccountId,
                                onValueChange = { input ->
                                    val cleanInput = input.trimStart('{', '<', '[', '(')
                                        .trimEnd('}', '>', ']', ')')
                                        .trim()
                                    cloudflareAccountId = cleanInput
                                    if (cleanInput.isNotBlank()) {
                                        baseUrl = "https://api.cloudflare.com/client/v4/accounts/$cleanInput/ai/run/"
                                    }
                                    inlineErrorText = null
                                },
                                placeholder = "32-hex Account ID (e.g. 5b8f32c964ddbb7630b784f8dff71277)",
                                leadingIcon = Icons.Default.Key
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Status badge
                            if (cloudflareAccountId.length == 32 && !cloudflareAccountId.contains(" ")) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SafeMint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .border(1.dp, SafeMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "✓ Valid 32-character Account ID (Synced with Base URL)",
                                        color = SafeMint,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (cloudflareAccountId.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CoralRed.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                        .border(1.dp, CoralRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Account ID is ${cloudflareAccountId.length}/32 characters. Do not include curly braces { }.",
                                        color = CoralRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "💡 Find your Account ID in Cloudflare Dashboard: Overview (right sidebar) → Account ID.",
                                    fontSize = 10.sp,
                                    color = SlateNeutral,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field 3: API Key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.activeTab == ConfigTab.CHAT) "AI Chat API Key" else "Image Generator API Key",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Quick Helper: Copy from Chat Provider key if on Image tab
                        if (state.activeTab == ConfigTab.IMAGE && state.chatConfig.apiKey.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                    .clickable {
                                        inputKey = state.chatConfig.apiKey
                                        inlineErrorText = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📋 Copy Chat Key",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeMint
                                )
                            }
                        }

                        // Quick Paste from clipboard
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                .clickable {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            inputKey = clip.trim()
                                            inlineErrorText = null
                                        }
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CoralRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                InsetTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        inlineErrorText = null
                    },
                    placeholder = activePreset.keyHint,
                    leadingIcon = Icons.Default.Key,
                    visualTransformation = if (isKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { isKeyVisible = !isKeyVisible },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = SlateNeutral,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                // Real-time Key Validation Feedback Badge
                val isCloudflare = selectedPresetId == "cloudflare" || baseUrl.contains("cloudflare", ignoreCase = true)
                val isNvidia = selectedPresetId == "openai_compatible" || baseUrl.contains("nvidia", ignoreCase = true)
                val isGemini = selectedPresetId == "gemini"
                val isHuggingFace = selectedPresetId == "huggingface" || baseUrl.contains("huggingface", ignoreCase = true)
                if (inputKey.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isCloudflare) {
                        if (inputKey.length >= 25 && !inputKey.contains(" ")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeMint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SafeMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✓ Valid Cloudflare API Token format (Workers AI permissions required)",
                                    color = SafeMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚠️ Cloudflare tokens are 40-character tokens created in your dashboard. Ensure it has Workers AI Read/Edit permission.",
                                    color = CoralRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isHuggingFace) {
                        if (inputKey.startsWith("hf_")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeMint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SafeMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✓ Valid Hugging Face Access Token format (hf_...)",
                                    color = SafeMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚠️ Hugging Face tokens typically start with 'hf_'. Make sure your token has Inference Providers permission.",
                                    color = CoralRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isGemini) {
                        if (inputKey.startsWith("AIzaSy")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeMint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SafeMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✓ Valid Google AI Studio API Key format (AIzaSy...)",
                                    color = SafeMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (inputKey.startsWith("nvapi-")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚠️ This is an NVIDIA key (nvapi-...). Google Gemini requires a Google key (AIzaSy...). Tap the 'OpenAI Compatible' preset above to configure NVIDIA NIM!",
                                    color = CoralRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isNvidia) {
                        if (inputKey.startsWith("nvapi-")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeMint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SafeMint.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "✓ Valid NVIDIA NIM API Key format (nvapi-...)",
                                    color = SafeMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (inputKey.startsWith("AIzaSy")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CoralRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⚠️ This is a Google Gemini key (AIzaSy...). For NVIDIA NIM, use an nvapi-... key, or tap the 'Google Gemini' preset above to configure Gemini!",
                                    color = CoralRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Field 4: Chat / Prompt Model ID (Exclusively for Chat Setup)
                if (state.activeTab == ConfigTab.CHAT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat / Prompt Model ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedPresetId == "gemini") {
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                .clickable { showGeminiChatModelDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Select Model ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                .clickable {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            chatModel = clip.trim()
                                        }
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CoralRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                InsetTextField(
                    value = chatModel,
                    onValueChange = { chatModel = it },
                    placeholder = if (selectedPresetId == "gemini") "e.g. gemini-2.0-flash" else "Enter or paste Chat Model ID (e.g. gpt-4o, llama-3.3)",
                    leadingIcon = Icons.Default.Bolt
                )

                // Selectable Chat Model chips
                if (selectedPresetId == "gemini") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ApiProviderPresets.GEMINI_CHAT_MODELS.forEach { modelOpt ->
                            val isSelected = chatModel.equals(modelOpt.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = CircleShape, elevation = if (isSelected) 1.dp else 2.dp)
                                    .clickable { chatModel = modelOpt.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = modelOpt.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else if (activePreset.popularChatModels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activePreset.popularChatModels.forEach { popModel ->
                            val isSelected = chatModel.equals(popModel, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = CircleShape, elevation = if (isSelected) 1.dp else 2.dp)
                                    .clickable { chatModel = popModel }
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
                }
                }

                // Field 5: Image Generation Model ID (Exclusively for Image Setup)
                if (state.activeTab == ConfigTab.IMAGE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Image Generation Model ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedPresetId == "gemini") {
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                .clickable { showGeminiImageModelDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Select Model ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(8.dp), elevation = 2.dp)
                                .clickable {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            imageModel = clip.trim()
                                        }
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CoralRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                InsetTextField(
                    value = imageModel,
                    onValueChange = { imageModel = it },
                    placeholder = if (selectedPresetId == "gemini") "e.g. imagen-3.0-generate-002" else "Enter or paste Image Model ID (e.g. dall-e-3, flux-1-schnell)",
                    leadingIcon = Icons.Default.Bolt
                )

                // Selectable Image Model chips
                if (selectedPresetId == "gemini") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ApiProviderPresets.GEMINI_IMAGE_MODELS.forEach { imgOpt ->
                            val isSelected = imageModel.equals(imgOpt.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = CircleShape, elevation = if (isSelected) 1.dp else 2.dp)
                                    .clickable { imageModel = imgOpt.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = imgOpt.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else if (selectedPresetId == "cloudflare") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ApiProviderPresets.CLOUDFLARE_IMAGE_MODELS.forEach { imgOpt ->
                            val isSelected = imageModel.equals(imgOpt.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = CircleShape, elevation = if (isSelected) 1.dp else 2.dp)
                                    .clickable { imageModel = imgOpt.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = imgOpt.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else if (activePreset.popularImageModels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activePreset.popularImageModels.forEach { popImg ->
                            val isSelected = imageModel.equals(popImg, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .neumorphicConvex(shape = CircleShape, elevation = if (isSelected) 1.dp else 2.dp)
                                    .clickable { imageModel = popImg }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = popImg,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Connection Button & Result Box
                SecondaryButton(
                    text = if (state.isTestingConnection) "Testing Connection..." else if (state.activeTab == ConfigTab.CHAT) "⚡ Test Chat Connection" else "⚡ Test Image Connection",
                    enabled = !state.isTestingConnection && (inputKey.isNotBlank() || selectedPresetId == "ollama"),
                    onClick = {
                        apiKeyViewModel.testConnection(
                            baseUrl = baseUrl,
                            apiKey = inputKey,
                            model = if (state.activeTab == ConfigTab.CHAT) chatModel else imageModel
                        )
                    }
                )

                // Test Connection Result Display
                state.testConnectionResult?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    when (res) {
                        is ConnectionTestResult.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeMint.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, SafeMint.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafeMint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeMint
                                    )
                                }
                            }
                        }
                        is ConnectionTestResult.Failure -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .border(1.dp, CoralRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = CoralRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.errorMessage,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CoralRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inline Error Banner
            inlineErrorText?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = err,
                    color = CoralRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save & Continue Button
            PrimaryButton(
                text = if (state.isLoading) "Saving..." else if (state.activeTab == ConfigTab.CHAT) "Save Chat Provider & Continue" else "Save Image Provider & Continue",
                enabled = !state.isLoading,
                onClick = {
                    val configToSave = ApiProviderConfig(
                        id = currentProfileId,
                        providerId = selectedPresetId,
                        providerName = providerName.trim(),
                        baseUrl = baseUrl.trim(),
                        apiKey = inputKey.trim(),
                        chatModel = chatModel.trim(),
                        imageModel = imageModel.trim(),
                        isOpenAiCompatible = activePreset.isOpenAiCompatible
                    )

                    apiKeyViewModel.saveProviderConfig(
                        config = configToSave,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (state.activeTab == ConfigTab.CHAT) "AI Chat Provider saved successfully!"
                                    else "Image Generator Provider saved successfully!"
                                )
                            }
                            onContinueToHome()
                        },
                        onError = { err ->
                            inlineErrorText = err
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Security & Privacy Shield Badge
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
                        text = "Keys and URLs are stored encrypted on-device with Android Keystore. Direct requests only.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Gemini Chat Model Picker Dialog
        if (showGeminiChatModelDialog) {
            GeminiModelPickerDialog(
                title = "💬 Gemini Chat Models",
                subtitle = "Select a model for AI prompt creation & brainstorming",
                options = ApiProviderPresets.GEMINI_CHAT_MODELS,
                selectedId = chatModel,
                onSelect = { chatModel = it },
                onDismiss = { showGeminiChatModelDialog = false }
            )
        }

        // Gemini Image Model Picker Dialog
        if (showGeminiImageModelDialog) {
            GeminiModelPickerDialog(
                title = "🎨 Google Image Models",
                subtitle = "Select a model for YouTube banner image rendering",
                options = ApiProviderPresets.GEMINI_IMAGE_MODELS,
                selectedId = imageModel,
                onSelect = { imageModel = it },
                onDismiss = { showGeminiImageModelDialog = false }
            )
        }
    }
}

@Composable
private fun GeminiModelPickerDialog(
    title: String,
    subtitle: String,
    options: List<ModelOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f)
                .neumorphicConvex(shape = RoundedCornerShape(24.dp), elevation = 10.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
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
                        Text(text = "✕", fontSize = 14.sp, color = SlateNeutral, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { item ->
                        val isSelected = item.id.equals(selectedId, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicConvex(shape = RoundedCornerShape(14.dp), elevation = if (isSelected) 1.dp else 2.dp)
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, CoralRed, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable {
                                    onSelect(item.id)
                                    onDismiss()
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = SlateNeutral
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CoralRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
