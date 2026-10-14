package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.BannerCreationViewModel
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset

data class MoodItem(val name: String, val desc: String, val icon: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptBuilderScreen(
    bannerViewModel: BannerCreationViewModel,
    onNavigateNext: () -> Unit,
    onNavigateToPromptChat: (() -> Unit)? = null,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by bannerViewModel.state.collectAsState()

    val moodOptions = listOf(
        MoodItem("Energetic", "High-tempo, electric pulse", "⚡"),
        MoodItem("Calm & Cozy", "Soft pastel warmth", "🌱"),
        MoodItem("Bold & Intense", "Striking, punchy cuts", "🔥"),
        MoodItem("Minimal", "Sleek geometry", "✨"),
        MoodItem("Playful", "Dynamic & charming", "🎉"),
        MoodItem("Professional", "Authoritative & crisp", "💼")
    )

    val quickStarters = listOf("⚡ Gaming", "🍳 Cooking & Food", "✈️ Travel & Vlog", "💻 Tech & Coding")

    // Validation rules: User can provide either channel topic OR a direct custom prompt (or both!)
    val isTopicValid = state.channelTopic.trim().length >= 3
    val isPromptValid = (state.customPromptDetails ?: "").trim().length >= 3
    val isMoodValid = state.selectedMood.trim().isNotBlank()
    val isNextEnabled = (isTopicValid || isPromptValid) && isMoodValid

    val customDetailsLength = (state.customPromptDetails ?: "").length

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader(
            title = "Prompt Builder",
            stepLabel = "Step 1 of 2",
            onBackClick = onBack,
            isDark = isDark,
            onToggleDark = onToggleDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Channel Details", fontSize = 12.sp, color = SlateNeutral, fontWeight = FontWeight.Bold)
            Text(text = "50% COMPLETED", fontSize = 12.sp, color = CoralRed, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .neumorphicInset(shape = CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(6.dp)
                    .background(CoralRed, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Your Channel Vibe 🪄",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Set the tone. BannerCraft translates this into pixel-perfect safe zones.",
            fontSize = 12.sp,
            color = SlateNeutral,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Channel Topic Input Card
        NeumorphicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. What's your channel topic? (or Niche)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isTopicValid) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✔ Valid",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeMint,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            InsetTextField(
                value = state.channelTopic,
                onValueChange = { bannerViewModel.updateChannelTopic(it) },
                placeholder = "e.g., Cozy Minecraft gaming, healthy 15-min meal prep...",
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "TAP QUICK STARTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateNeutral)

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickStarters.forEach { starter ->
                    Box(
                        modifier = Modifier
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .clickable {
                                val current = state.channelTopic
                                val clean = starter.substringAfter(" ")
                                bannerViewModel.updateChannelTopic(if (current.isBlank()) clean else "$current, $clean")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = starter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateNeutral)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Custom AI Banner Prompt Card (DIRECT PROMPT OPTION)
        NeumorphicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. Custom AI Banner Prompt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPromptValid) "✔ Active" else "Direct Prompt",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeMint
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Type or paste your complete prompt idea, specific characters, neon lighting, background scene, or aesthetic details:",
                fontSize = 11.sp,
                color = SlateNeutral,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            InsetTextField(
                value = state.customPromptDetails ?: "",
                onValueChange = {
                    bannerViewModel.updateCustomPromptDetails(it)
                    // If channel topic is empty, automatically suggest first few words as topic
                    if (state.channelTopic.isBlank() && it.isNotBlank()) {
                        val autoTopic = it.take(30).substringBefore(",").substringBefore(".").trim()
                        if (autoTopic.length >= 3) {
                            bannerViewModel.updateChannelTopic(autoTopic)
                        }
                    }
                },
                placeholder = "e.g., Cyberpunk neon samurai standing in a rainy Tokyo street at night, glowing purple & cyan holographic reflections, high detail 8k cinematic...",
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateToPromptChat != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToPromptChat() }
                    ) {
                        Text(text = "💬", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Brainstorm with AI Chat ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = "$customDetailsLength/500",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (customDetailsLength >= 500) CoralRed else SlateNeutral
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Pick a Mood / Vibe
        Text(
            text = "3. Pick a mood / vibe *",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val chunked = moodOptions.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { item ->
                        val isSelected = state.selectedMood.equals(item.name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .border(2.dp, CoralRed, RoundedCornerShape(20.dp))
                                            .neumorphicConvex(shape = RoundedCornerShape(20.dp), elevation = 6.dp)
                                    } else {
                                        Modifier.neumorphicConvex(shape = RoundedCornerShape(20.dp), elevation = 3.dp)
                                    }
                                )
                                .clickable { bannerViewModel.updateSelectedMood(item.name) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.icon, fontSize = 20.sp)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(CoralRed, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.desc,
                                    fontSize = 11.sp,
                                    color = SlateNeutral,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Zero Prompt Engineering Card
        NeumorphicCard(padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neumorphicConvex(shape = CircleShape, elevation = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SafeMint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Zero prompt engineering required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "We translate your choices into tailored YouTube safe-box canvases (2560 × 1440) automatically.",
                        fontSize = 12.sp,
                        color = SlateNeutral,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(
            text = "Next: Choose Banner Style ➔",
            enabled = isNextEnabled,
            onClick = {
                bannerViewModel.buildBasePrompt()
                onNavigateNext()
            },
            subtext = if (!isNextEnabled) "Please enter channel topic (at least 3 characters)" else null
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
