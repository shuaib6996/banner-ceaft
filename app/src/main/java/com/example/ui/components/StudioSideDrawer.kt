package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudioSideDrawer(
    currentConfig: ApiProviderConfig,
    savedProfiles: List<ApiProviderConfig>,
    onSelectProfile: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onSwitchToGoogle: () -> Unit,
    onSwitchToNvidia: () -> Unit,
    onSwitchToCloudflare: () -> Unit = {},
    recentHistory: List<BannerHistoryItem>,
    onSelectHistoryItem: (BannerHistoryItem) -> Unit,
    onClose: () -> Unit,
    onManageApiKeys: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val isCloudflare = currentConfig.providerId == "cloudflare" ||
        currentConfig.baseUrl.contains("cloudflare", ignoreCase = true) ||
        currentConfig.imageModel.startsWith("@cf/", ignoreCase = true)
    val isGoogle = currentConfig.providerId == "gemini"
    val isHuggingFace = currentConfig.providerId == "huggingface" || currentConfig.apiKey.startsWith("hf_", ignoreCase = true)
    val isNvidia = currentConfig.providerId == "openai_compatible" || currentConfig.baseUrl.contains("nvidia", ignoreCase = true)
    val hasKey = currentConfig.apiKey.isNotBlank() || currentConfig.providerId == "ollama"

    // Only Image Models in Studio
    val relevantImageModels = if (isCloudflare) {
        ApiProviderPresets.CLOUDFLARE_IMAGE_MODELS
    } else if (isHuggingFace) {
        ApiProviderPresets.HUGGINGFACE_IMAGE_MODELS
    } else if (isGoogle) {
        ApiProviderPresets.GEMINI_IMAGE_MODELS
    } else {
        ApiProviderPresets.NVIDIA_IMAGE_MODELS
    }

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
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Image Studio",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Engines & Banner History",
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
                        text = "ACTIVE IMAGE ENGINE",
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
                    text = "Model: ${currentConfig.imageModel}",
                    fontSize = 11.sp,
                    color = SlateNeutral,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Quick 1-Tap Switch Pills (NVIDIA FLUX vs Google Imagen vs Cloudflare)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(
                        if (isNvidia) Modifier.neumorphicInset(shape = CircleShape)
                        else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                    )
                    .clickable { onSwitchToNvidia() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡ FLUX",
                    fontSize = 11.sp,
                    fontWeight = if (isNvidia) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isNvidia) CoralRed else SlateNeutral
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(
                        if (isGoogle) Modifier.neumorphicInset(shape = CircleShape)
                        else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                    )
                    .clickable { onSwitchToGoogle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨ Imagen",
                    fontSize = 11.sp,
                    fontWeight = if (isGoogle) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isGoogle) CoralRed else SlateNeutral
                )
            }

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .height(36.dp)
                    .then(
                        if (isCloudflare) Modifier.neumorphicInset(shape = CircleShape)
                        else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                    )
                    .clickable { onSwitchToCloudflare() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "☁️ Cloudflare",
                    fontSize = 11.sp,
                    fontWeight = if (isCloudflare) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isCloudflare) CoralRed else SlateNeutral
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Saved Profiles Row (Horizontal Scroll)
        val effectiveImageProfiles = if (savedProfiles.isEmpty()) {
            listOf(
                ApiProviderConfig(
                    id = "default_image_nvidia",
                    providerId = "openai_compatible",
                    providerName = "NVIDIA NIM",
                    imageModel = "black-forest-labs/flux.1-dev",
                    isOpenAiCompatible = true
                ),
                ApiProviderConfig(
                    id = "default_image_gemini",
                    providerId = "gemini",
                    providerName = "Google Gemini (Direct)",
                    imageModel = "imagen-3.0-generate-002",
                    isOpenAiCompatible = false
                ),
                ApiProviderConfig(
                    id = "default_image_cloudflare",
                    providerId = "cloudflare",
                    providerName = "Cloudflare Workers AI",
                    baseUrl = "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/",
                    apiKey = "",
                    imageModel = "@cf/black-forest-labs/flux-1-schnell",
                    isOpenAiCompatible = false
                )
            )
        } else savedProfiles

        Text(
            text = "SAVED IMAGE PROFILES",
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
            effectiveImageProfiles.forEach { profile ->
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

        // Scrollable content area: Image Models + Recent History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section A: Image Models (Only Image Models!)
            item {
                Text(
                    text = "SELECT IMAGE MODEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNeutral,
                    letterSpacing = 0.5.sp
                )
            }

            items(relevantImageModels) { model ->
                val isSelected = currentConfig.imageModel.equals(model.id, ignoreCase = true)
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

            // Section B: Divider & History Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STUDIO & BANNER HISTORY (${recentHistory.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (recentHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(shape = RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved images yet. Generate an image to see it here!",
                            fontSize = 11.sp,
                            color = SlateNeutral
                        )
                    }
                }
            } else {
                items(recentHistory) { item ->
                    val bitmap = remember(item.id) {
                        try {
                            BitmapFactory.decodeByteArray(item.imageBytes, 0, item.imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val dateFormatted = remember(item.timestamp) {
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                            .clickable { onSelectHistoryItem(item) }
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Thumbnail Preview
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "History preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(SlateNeutral.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = SlateNeutral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.channelTitle.ifBlank { "Generated Image" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.prompt,
                                    fontSize = 10.sp,
                                    color = SlateNeutral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormatted,
                                    fontSize = 9.sp,
                                    color = CoralRed.copy(alpha = 0.8f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .neumorphicInset(shape = CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Load",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Footer: Settings / API Key Setup button
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
                    text = "Manage Image API Keys",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
