package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiProviderConfig
import com.example.data.ApiProviderPresets
import com.example.data.BannerHistoryItem
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset

@Composable
fun ChatSideDrawer(
    currentConfig: ApiProviderConfig,
    savedProfiles: List<ApiProviderConfig>,
    onSelectProfile: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onSwitchToGoogle: () -> Unit,
    onSwitchToNvidia: () -> Unit,
    recentHistory: List<BannerHistoryItem>,
    onSelectPromptIdea: (String) -> Unit,
    onNewChat: () -> Unit,
    onClose: () -> Unit,
    onManageApiKeys: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val isGoogle = currentConfig.providerId == "gemini"
    val isNvidia = currentConfig.providerId == "openai_compatible" || currentConfig.baseUrl.contains("nvidia", ignoreCase = true)
    val hasKey = currentConfig.apiKey.isNotBlank() || currentConfig.providerId == "ollama"

    // Only Chat Models in Chat screen
    val relevantChatModels = if (isGoogle) {
        ApiProviderPresets.GEMINI_CHAT_MODELS
    } else {
        ApiProviderPresets.NVIDIA_CHAT_MODELS
    }

    val quickCreativePrompts = listOf(
        "🎮 Neon Cyberpunk Gaming banner idea with dark aesthetic",
        "💻 Minimalist Coding & Tech Channel branding with clean typography",
        "✈️ Aesthetic Travel Vlog banner concept with warm sunset vibes",
        "📈 High-end Finance & Crypto channel branding with gold accents",
        "🍳 Cozy Aesthetic Cooking & Food channel with pastel kitchen tones",
        "🎧 Lo-Fi Chill Beats banner with purple anime sky and headphones"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(330.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(18.dp)
    ) {
        // 1. Header with Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neumorphicConvex(shape = CircleShape, elevation = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Chat Director",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Engines & Prompt History",
                        fontSize = 11.sp,
                        color = SlateNeutral
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Drawer",
                    tint = SlateNeutral,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Active Engine Status Card
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
                    Text(
                        text = "ACTIVE CHAT ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoralRed,
                        letterSpacing = 0.8.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (hasKey) SafeMint else CoralRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasKey) "Ready" else "Key Missing",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasKey) SafeMint else CoralRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${currentConfig.providerName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Model: ${currentConfig.chatModel}",
                    fontSize = 11.sp,
                    color = SlateNeutral,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Quick 1-Tap Switch Pills (Google Gemini vs NVIDIA NIM)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(
                        if (isGoogle) Modifier.neumorphicInset(shape = CircleShape)
                        else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                    )
                    .clickable { onSwitchToGoogle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨ Gemini 3.8",
                    fontSize = 11.sp,
                    fontWeight = if (isGoogle) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isGoogle) CoralRed else SlateNeutral
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(
                        if (isNvidia) Modifier.neumorphicInset(shape = CircleShape)
                        else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                    )
                    .clickable { onSwitchToNvidia() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡ NVIDIA LLaMA",
                    fontSize = 11.sp,
                    fontWeight = if (isNvidia) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isNvidia) CoralRed else SlateNeutral
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Saved Chat Profiles Row (Horizontal Scroll)
        val effectiveChatProfiles = if (savedProfiles.isEmpty()) {
            listOf(
                ApiProviderConfig(
                    id = "default_chat_gemini",
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    chatModel = "gemini-3.8-flash",
                    isOpenAiCompatible = false
                ),
                ApiProviderConfig(
                    id = "default_chat_nvidia",
                    providerId = "openai_compatible",
                    providerName = "NVIDIA NIM",
                    chatModel = "meta/llama-3.2-11b-vision-instruct",
                    isOpenAiCompatible = true
                )
            )
        } else savedProfiles

        Text(
            text = "SAVED CHAT PROFILES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNeutral,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            effectiveChatProfiles.forEach { profile ->
                val isSelected = profile.id == currentConfig.id
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .then(
                            if (isSelected) Modifier.neumorphicInset(shape = RoundedCornerShape(10.dp))
                            else Modifier.neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                        )
                        .clickable { onSelectProfile(profile.id) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (profile.apiKey.isNotBlank() || profile.providerId == "ollama") SafeMint
                                    else CoralRed,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.providerName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. "+ Start New Chat Session" Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                .clickable {
                    onNewChat()
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = null,
                    tint = CoralRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Start New Chat Session",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoralRed
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable content area: Chat Models + Prompt History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section A: Chat Models
            item {
                Text(
                    text = "SELECT CHAT MODEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNeutral,
                    letterSpacing = 0.5.sp
                )
            }

            items(relevantChatModels) { model ->
                val isSelected = currentConfig.chatModel.equals(model.id, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) Modifier.neumorphicInset(shape = RoundedCornerShape(12.dp))
                            else Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                        )
                        .clickable { onSelectModel(model.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = model.description,
                                fontSize = 10.sp,
                                color = SlateNeutral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(CoralRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section B: Prompt History & Quick Ideas
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROMPT IDEAS & HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // If user has previous banners in history, show them as inspiration
            if (recentHistory.isNotEmpty()) {
                items(recentHistory.take(4)) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                            .clickable {
                                onSelectPromptIdea(item.prompt)
                                onClose()
                            }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📌 ${item.channelTitle.ifBlank { "Banner Idea" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.prompt,
                                fontSize = 10.sp,
                                color = SlateNeutral,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Curated Quick Brainstorm Ideas
            items(quickCreativePrompts) { idea ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(shape = RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectPromptIdea(idea)
                            onClose()
                        }
                        .padding(10.dp)
                ) {
                    Text(
                        text = idea,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Footer: Settings / API Key Setup button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                .clickable { onManageApiKeys() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = CoralRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Manage Chat API Keys",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
