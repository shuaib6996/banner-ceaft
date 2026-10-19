package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.ApiKeyViewModel
import com.example.state.HistoryViewModel
import com.example.state.ImageEditorMode
import com.example.state.ImageEditorViewModel
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StudioSideDrawer
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

@Composable
fun ImageEditorScreen(
    imageEditorViewModel: ImageEditorViewModel,
    apiKeyViewModel: ApiKeyViewModel,
    historyViewModel: HistoryViewModel? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by imageEditorViewModel.state.collectAsState()
    val apiKeyState by apiKeyViewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val historyItems = historyViewModel?.recentBannersState?.collectAsState()?.value ?: emptyList()

    // Android Photo Picker launcher for zero-permission image upload
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    imageEditorViewModel.setSourceImage(bytes)
                }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Failed to load selected photo.") }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            StudioSideDrawer(
                currentConfig = apiKeyState.imageConfig,
                savedProfiles = apiKeyState.savedImageProfiles,
                onSelectProfile = { profileId ->
                    apiKeyViewModel.switchActiveImageProfile(profileId)
                },
                onSelectModel = { modelId ->
                    apiKeyViewModel.switchImageModel(modelId)
                },
                onSwitchToGoogle = {
                    apiKeyViewModel.switchToGoogleImage()
                },
                onSwitchToNvidia = {
                    apiKeyViewModel.switchToNvidiaImage()
                },
                onSwitchToCloudflare = {
                    apiKeyViewModel.switchToCloudflareImage()
                },
                recentHistory = historyItems,
                onSelectHistoryItem = { item ->
                    imageEditorViewModel.updatePrompt(item.prompt)
                    imageEditorViewModel.setSourceImage(item.imageBytes)
                    scope.launch {
                        drawerState.close()
                        snackbarHostState.showSnackbar("Loaded banner from history into Studio!")
                    }
                },
                onClose = {
                    scope.launch { drawerState.close() }
                },
                onManageApiKeys = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings?.invoke()
                },
                isDark = isDark
            )
        }
    ) {
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
                    title = "AI Image Studio",
                    stepLabel = "${apiKeyState.imageConfig.providerName} Engine",
                    onBackClick = onBack,
                    isDark = isDark,
                    onToggleDark = onToggleDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Engine Switcher Pill & Side Panel Drawer Trigger Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasKey = apiKeyState.imageConfig.apiKey.isNotBlank() || apiKeyState.imageConfig.providerId == "ollama"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .clickable { scope.launch { drawerState.open() } }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (hasKey) SafeMint else CoralRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${apiKeyState.imageConfig.providerName}: ${apiKeyState.imageConfig.imageModel.substringAfterLast('/')}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "▾ Switch",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .neumorphicConvex(shape = CircleShape, elevation = 3.dp)
                            .clickable { scope.launch { drawerState.open() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Engines & History",
                            tint = CoralRed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

            // Mobile-Friendly Mode Selector Tabs (Create vs Edit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isCreate = state.mode == ImageEditorMode.CREATE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp) // Touch target >= 48dp
                        .then(
                            if (isCreate) Modifier.neumorphicInset(shape = CircleShape)
                            else Modifier.neumorphicConvex(shape = CircleShape, elevation = 3.dp)
                        )
                        .clickable { imageEditorViewModel.updateMode(ImageEditorMode.CREATE) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isCreate) CoralRed else SlateNeutral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create Image",
                            fontSize = 13.sp,
                            fontWeight = if (isCreate) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isCreate) CoralRed else SlateNeutral
                        )
                    }
                }

                val isEdit = state.mode == ImageEditorMode.EDIT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp) // Touch target >= 48dp
                        .then(
                            if (isEdit) Modifier.neumorphicInset(shape = CircleShape)
                            else Modifier.neumorphicConvex(shape = CircleShape, elevation = 3.dp)
                        )
                        .clickable { imageEditorViewModel.updateMode(ImageEditorMode.EDIT) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isEdit) CoralRed else SlateNeutral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Image",
                            fontSize = 13.sp,
                            fontWeight = if (isEdit) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isEdit) CoralRed else SlateNeutral
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mode 1: Create Image Card
            if (state.mode == ImageEditorMode.CREATE) {
                NeumorphicCard {
                    Text(
                        text = "Create New Image",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Describe what you want Gemini AI to create from scratch.",
                        fontSize = 12.sp,
                        color = SlateNeutral
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    InsetTextField(
                        value = state.prompt,
                        onValueChange = { imageEditorViewModel.updatePrompt(it) },
                        label = "Text Prompt",
                        placeholder = "e.g., Cyberpunk neon samurai standing in a rainy Tokyo street at night...",
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // YouTube Banner Target Device Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📐 YouTube Banner Target",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val badgeText = when {
                            state.selectedAspectRatio.contains("Desktop", ignoreCase = true) -> "● 2560×423"
                            state.selectedAspectRatio.contains("Full", ignoreCase = true) || state.selectedAspectRatio == "16:9" -> "● 2560×1440"
                            state.selectedAspectRatio == "1:1" || state.selectedAspectRatio == "4:3" || state.selectedAspectRatio == "9:16" -> "● ${state.selectedAspectRatio}"
                            else -> "● 1546×423"
                        }
                        Box(
                            modifier = Modifier
                                .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeMint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choose your target display. Mobile Safe restricts all essential visuals to the 1546×423 phone safe area.",
                        fontSize = 11.sp,
                        color = SlateNeutral,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data class DeviceOption(val mode: String, val icon: String, val size: String)
                        val deviceOptions = listOf(
                            DeviceOption("Phone Safe", "📱", "1546×423"),
                            DeviceOption("Desktop", "🖥️", "2560×423"),
                            DeviceOption("Full TV", "📺", "2560×1440")
                        )
                        deviceOptions.forEach { opt ->
                            val isSelected = when (opt.mode) {
                                "Phone Safe" -> state.selectedAspectRatio == "Phone Safe" ||
                                    state.selectedAspectRatio.contains("1546") ||
                                    state.selectedAspectRatio.contains("Mobile", ignoreCase = true)
                                "Desktop" -> state.selectedAspectRatio == "Desktop" ||
                                    state.selectedAspectRatio.contains("2560×423") ||
                                    state.selectedAspectRatio.contains("Desktop", ignoreCase = true)
                                else -> state.selectedAspectRatio == "Full TV" ||
                                    state.selectedAspectRatio.contains("Full") ||
                                    state.selectedAspectRatio == "16:9"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(1.5.dp, CoralRed, RoundedCornerShape(14.dp))
                                                .neumorphicInset(shape = RoundedCornerShape(14.dp))
                                        } else {
                                            Modifier.neumorphicConvex(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
                                        }
                                    )
                                    .clickable { imageEditorViewModel.updateAspectRatio(opt.mode) }
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = opt.icon,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = opt.mode,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = opt.size,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) CoralRed.copy(alpha = 0.7f) else SlateNeutral,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Informational tips for active device mode
                    val isMobileSelected = state.selectedAspectRatio == "Phone Safe" ||
                        state.selectedAspectRatio.contains("1546") ||
                        state.selectedAspectRatio.contains("Mobile", ignoreCase = true)
                    val isDesktopSelected = state.selectedAspectRatio == "Desktop" ||
                        state.selectedAspectRatio.contains("2560×423") ||
                        state.selectedAspectRatio.contains("Desktop", ignoreCase = true)
                    val isFullTvSelected = state.selectedAspectRatio == "Full TV" ||
                        state.selectedAspectRatio.contains("Full") ||
                        state.selectedAspectRatio == "16:9"

                    AnimatedVisibility(visible = isMobileSelected) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeMint.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📱 Mobile Safe: AI creates a full 2560×1440 YouTube-ready banner with your artwork focused inside the 1546×423 phone safe zone.",
                                    fontSize = 11.sp,
                                    color = SafeMint,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isDesktopSelected) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CoralRed.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "🖥️ Desktop: AI creates a full 2560×1440 YouTube-ready banner with artwork focused inside the 2560×423 desktop strip.",
                                    fontSize = 11.sp,
                                    color = CoralRed,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isFullTvSelected) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateNeutral.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📺 Full Banner: Full 2560×1440 canvas for large displays and Smart TVs.",
                                    fontSize = 11.sp,
                                    color = SlateNeutral,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Standard Aspect Ratios (for non-banner image creation)
                    Text(
                        text = "Other Aspect Ratios (Non-Banner)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateNeutral
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1:1", "4:3", "9:16").forEach { ratio ->
                            val selected = state.selectedAspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .then(
                                        if (selected) Modifier.neumorphicInset(shape = CircleShape)
                                        else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                    )
                                    .clickable { imageEditorViewModel.updateAspectRatio(ratio) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) CoralRed else SlateNeutral
                                )
                            }
                        }
                    }
                }
            } else {

                // Mode 2: Edit Existing Image Card
                NeumorphicCard {
                    Text(
                        text = "Edit Existing Image",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload a photo and tell Gemini AI what to add, remove, or change.",
                        fontSize = 12.sp,
                        color = SlateNeutral
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Source Image Upload / Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .neumorphicInset(shape = RoundedCornerShape(16.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val sourceBytes = state.sourceImageBytes ?: state.resultImageBytes
                        if (sourceBytes != null && sourceBytes.isNotEmpty()) {
                            val bitmap = remember(sourceBytes) {
                                try {
                                    BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Source Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Upload",
                                    tint = CoralRed,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap to pick photo from device",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Zero storage permissions needed",
                                    fontSize = 11.sp,
                                    color = SafeMint
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    InsetTextField(
                        value = state.editInstruction,
                        onValueChange = { imageEditorViewModel.updateEditInstruction(it) },
                        label = "Edit Instructions",
                        placeholder = "e.g., Change the background to a sunset beach, add glowing neon eyes...",
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button
            PrimaryButton(
                text = if (state.isProcessing) "Processing Image..." else if (state.mode == ImageEditorMode.CREATE) "Generate Image 🚀" else "Apply Image Edits 🪄",
                enabled = !state.isProcessing,
                onClick = {
                    imageEditorViewModel.processImage(
                        apiKey = apiKeyState.imageConfig.apiKey,
                        model = apiKeyState.imageConfig.imageModel,
                        providerConfig = apiKeyState.imageConfig,
                        onSaveToHistory = { title, prompt, bytes ->
                            historyViewModel?.saveToHistory(title, prompt, bytes)
                        }
                    )
                },
                icon = Icons.Default.AutoAwesome
            )

            // Display Processing Overlay or Error / Output
            if (state.isProcessing) {
                Spacer(modifier = Modifier.height(18.dp))
                NeumorphicCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator(color = CoralRed, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.stageMessage ?: "Gemini 3.1 Flash AI is processing...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(18.dp))
                NeumorphicCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.errorMessage ?: "An error occurred.",
                            fontSize = 13.sp,
                            color = CoralRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else if (state.resultImageBytes != null && state.resultImageBytes!!.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                // Determine dynamic dimensions from image bytes
                val (imgWidth, imgHeight) = remember(state.resultImageBytes) {
                    try {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(state.resultImageBytes, 0, state.resultImageBytes!!.size, options)
                        Pair(options.outWidth, options.outHeight)
                    } catch (e: Exception) {
                        Pair(2560, 1440)
                    }
                }

                val isYouTubeFormat = imgWidth == 1546 || imgWidth == 2560 || state.masterCanvasBytes != null

                // Result Output Display Card
                NeumorphicCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SafeMint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    imgWidth == 1546 && imgHeight == 423 -> "Mobile Safe Ready"
                                    imgWidth == 2560 && imgHeight == 423 -> "Desktop Banner Ready"
                                    imgWidth == 2560 && imgHeight == 1440 -> "Full Banner Ready"
                                    else -> "Result Image Ready"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "$imgWidth × $imgHeight PNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val resultBitmap = remember(state.resultImageBytes) {
                        try {
                            BitmapFactory.decodeByteArray(state.resultImageBytes, 0, state.resultImageBytes!!.size)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val previewHeight = when {
                        imgWidth == 2560 && imgHeight == 423 -> 90.dp
                        imgWidth == 1546 && imgHeight == 423 -> 125.dp
                        else -> 200.dp
                    }

                    if (resultBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(previewHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = resultBitmap.asImageBitmap(),
                                contentDescription = "Result Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    if (isYouTubeFormat && state.masterCanvasBytes != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // 1-Tap Target Device Switcher in Studio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Phone Safe" to "1546×423",
                                "Desktop" to "2560×423",
                                "Full TV" to "2560×1440"
                            ).forEach { (label, _) ->
                                val isSelected = when (label) {
                                    "Phone Safe" -> imgWidth == 1546 && imgHeight == 423
                                    "Desktop" -> imgWidth == 2560 && imgHeight == 423
                                    else -> imgWidth == 2560 && imgHeight == 1440
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .then(
                                            if (isSelected) Modifier.neumorphicInset(shape = CircleShape)
                                            else Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                        )
                                        .clickable { imageEditorViewModel.switchDeviceTarget(label) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) CoralRed else SlateNeutral
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val downloadLabel = when {
                        imgWidth == 1546 && imgHeight == 423 -> "Save Mobile Banner (1546 × 423) 📥"
                        imgWidth == 2560 && imgHeight == 423 -> "Save Desktop Banner (2560 × 423) 📥"
                        imgWidth == 2560 && imgHeight == 1440 -> "Save Full Banner (2560 × 1440) 📥"
                        else -> "Save Image to Gallery ($imgWidth × $imgHeight) 📥"
                    }

                    PrimaryButton(
                        text = if (state.isSaving) "Saving to Photos..." else downloadLabel,
                        enabled = !state.isSaving,
                        onClick = {
                            imageEditorViewModel.saveToGallery(context) { success, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        icon = Icons.Default.Download
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}


