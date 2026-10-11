package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.ApiKeyViewModel
import com.example.state.BannerCreationViewModel
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    bannerViewModel: BannerCreationViewModel,
    apiKeyViewModel: ApiKeyViewModel,
    onStartOver: () -> Unit,
    onTweakStyle: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by bannerViewModel.state.collectAsState()
    val apiKeyState by apiKeyViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedPreviewMode by remember { mutableStateOf("Phone Safe") } // "Phone Safe", "Desktop", "Full TV"
    var showEditTextSheet by remember { mutableStateOf(false) }

    var editTitleText by remember { mutableStateOf(state.textContent ?: "Alex Gaming HD") }
    var editSubtextText by remember { mutableStateOf(state.subtextContent ?: "New videos every Tuesday!") }

    var hasAttemptedGeneration by remember { mutableStateOf(false) }

    // Intercept back button during active generation or save
    BackHandler(enabled = state.isGenerating || state.isSaving) {
        // Prevent accidental back navigation during processing
    }

    // Trigger image generation on screen load if needed (guarded against unintended loops/re-triggers)
    LaunchedEffect(Unit) {
        if (!hasAttemptedGeneration && state.finalImageBytes == null && state.processedImageBytes == null && state.generatedImageBytes == null && !state.isGenerating) {
            hasAttemptedGeneration = true
            bannerViewModel.generateBanner(
                apiKey = apiKeyState.imageConfig.apiKey,
                context = context,
                model = apiKeyState.imageConfig.imageModel,
                providerConfig = apiKeyState.imageConfig
            )
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ScreenHeader(
                title = "Your Banner",
                onBackClick = onBack,
                isDark = isDark,
                onToggleDark = onToggleDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isGenerating) {
                // Real Loading Render Overlay with Live Stage Message
                NeumorphicCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CoralRed, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = state.generationStageMessage ?: "Crafting your AI Banner...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Generating 2560 × 1440 canvas using Gemini 3.1 Flash Image AI...",
                            fontSize = 13.sp,
                            color = SlateNeutral,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = CoralRed,
                            trackColor = SlateNeutral.copy(alpha = 0.2f),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "CALIBRATING SAFE ZONES • PLEASE WAIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                }
            } else if (state.generationErrorMessage != null) {
                // Error Display State
                NeumorphicCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .neumorphicConvex(shape = CircleShape, elevation = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Generation Couldn't Complete",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.generationErrorMessage ?: "Something went wrong. Please try again.",
                            fontSize = 13.sp,
                            color = SlateNeutral,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryButton(
                            text = if (state.isGenerating) "Generating..." else "Retry Generation",
                            enabled = !state.isGenerating,
                            onClick = {
                                if (!state.isGenerating) {
                                    bannerViewModel.generateBanner(
                                        apiKey = apiKeyState.imageConfig.apiKey,
                                        context = context,
                                        model = apiKeyState.imageConfig.imageModel,
                                        providerConfig = apiKeyState.imageConfig
                                    )
                                }
                            },
                            icon = Icons.Default.Refresh
                        )
                    }
                }
            } else {
                // Success Generated Output State
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎉 GEMINI AI & TEXT OVERLAY COMPLETE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeMint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your Banner is Ready!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Zero design headaches. Perfectly centered and safe-zone calibrated for every screen.",
                    fontSize = 13.sp,
                    color = SlateNeutral,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Banner Canvas Preview Card displaying final image bytes
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YouTube Safe 2025",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "2560 × 1440 PNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateNeutral
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Final Banner Image View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayBytes = state.finalImageBytes ?: state.processedImageBytes ?: state.generatedImageBytes
                        if (displayBytes != null && displayBytes.isNotEmpty()) {
                            val bitmap = remember(displayBytes) {
                                try {
                                    BitmapFactory.decodeByteArray(displayBytes, 0, displayBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Final Banner Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Final Banner Canvas Ready",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preview Modes Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Phone Safe", "Desktop", "Full TV").forEach { mode ->
                            val isSelected = selectedPreviewMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .then(
                                        if (isSelected) {
                                            Modifier.neumorphicInset(shape = CircleShape)
                                        } else {
                                            Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                        }
                                    )
                                    .clickable { selectedPreviewMode = mode },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) CoralRed else SlateNeutral
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons - Download & Save
                PrimaryButton(
                    text = if (state.isSaving) "Saving to Gallery..." else "Download & Save to Photos",
                    enabled = !state.isSaving,
                    onClick = {
                        bannerViewModel.saveBanner(context) { success, message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    },
                    icon = Icons.Default.Download
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryButton(
                    text = "Try Again (Fresh Variation)",
                    enabled = !state.isGenerating,
                    onClick = {
                        bannerViewModel.generateBanner(
                            apiKey = apiKeyState.imageConfig.apiKey,
                            context = context,
                            model = apiKeyState.imageConfig.imageModel,
                            providerConfig = apiKeyState.imageConfig
                        )
                    },
                    icon = Icons.Default.Refresh
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Tweaks Row
                Text(
                    text = "Quick Tweaks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .neumorphicConvex(shape = RoundedCornerShape(20.dp), elevation = 3.dp)
                            .clickable {
                                editTitleText = state.textContent ?: ""
                                editSubtextText = state.subtextContent ?: ""
                                showEditTextSheet = true
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .neumorphicInset(shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Edit Text",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Title & subtitle words",
                                fontSize = 11.sp,
                                color = SlateNeutral
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .neumorphicConvex(shape = RoundedCornerShape(20.dp), elevation = 3.dp)
                            .clickable { onTweakStyle() }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .neumorphicInset(shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = SafeMint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tweak Style",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Colors, mood & vibe",
                                fontSize = 11.sp,
                                color = SlateNeutral
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // How to Upload to YouTube Section
                NeumorphicCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to upload to YouTube",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Instructions", "1. Open Studio Customization (studio.youtube.com)\n2. Select Branding Tab\n3. Upload & Publish")
                                    clipboard.setPrimaryClip(clip)
                                    scope.launch { snackbarHostState.showSnackbar("Instructions copied to clipboard!") }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = SlateNeutral,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateNeutral)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    InstructionStepItem("1", "Open Studio Customization", "Go to studio.youtube.com > tap Customization on the left menu.")
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionStepItem("2", "Select Branding Tab", "Navigate to Branding and find the Banner Image section.")
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionStepItem("3", "Upload & Publish", "Hit Upload, pick your saved PNG, and click Publish at top-right!")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Start Over Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartOver() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "➕ ", fontSize = 14.sp)
                    Text(
                        text = "Start new banner from scratch",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNeutral
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet to Edit Text
    if (showEditTextSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditTextSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Edit Banner Text",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                InsetTextField(
                    value = editTitleText,
                    onValueChange = { editTitleText = it },
                    label = "Channel Title",
                    placeholder = "e.g., Alex Gaming HD"
                )

                Spacer(modifier = Modifier.height(12.dp))

                InsetTextField(
                    value = editSubtextText,
                    onValueChange = { editSubtextText = it },
                    label = "Subtext / Tagline",
                    placeholder = "e.g., New videos every Tuesday!"
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Update Text Preview",
                    onClick = {
                        bannerViewModel.updateTextAndRedraw(editTitleText, editSubtextText)
                        showEditTextSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InstructionStepItem(num: String, title: String, desc: String) {
    NeumorphicCard(padding = 10.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(CoralRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = num, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoralRed)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = SlateNeutral,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
