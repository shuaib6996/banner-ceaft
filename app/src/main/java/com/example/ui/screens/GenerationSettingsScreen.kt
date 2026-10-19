package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.navigationBarsPadding
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ApiProviderConfig
import com.example.data.ApiProviderPresets
import com.example.state.ApiKeyViewModel
import com.example.state.BannerCreationViewModel
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PositionGrid3x3
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset

@Composable
fun GenerationSettingsScreen(
    bannerViewModel: BannerCreationViewModel,
    apiKeyViewModel: ApiKeyViewModel,
    onGenerateBanner: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by bannerViewModel.state.collectAsState()
    val apiKeyState by apiKeyViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var photoPickerError by remember { mutableStateOf<String?>(null) }
    var showImageModelSwitcherDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    if (bytes.size > 10 * 1024 * 1024) { // 10MB limit
                        photoPickerError = "That image is too large, please choose a smaller one"
                    } else {
                        photoPickerError = null
                        val fileName = uri.lastPathSegment ?: "Custom_Avatar.png"
                        bannerViewModel.updateLogoBytes(bytes, fileName)
                    }
                }
            } catch (e: Exception) {
                photoPickerError = "Could not load image file."
            }
        }
    }

    val styleOptions = listOf(
        StyleOption("3D Render", "Tactile & Bold", "🎨"),
        StyleOption("Studio Real", "Crisp & Clean", "📷"),
        StyleOption("Anime Draft", "Vibrant & Expressive", "✨"),
        StyleOption("Cyberpunk", "Neon Glow & Tech", "⚡"),
        StyleOption("Minimal Flat", "Clean & Elegant", "📐")
    )

    // Validation rules
    val isAspectRatioValid = state.selectedAspectRatio.isNotBlank()
    val isStyleValid = state.selectedStyle.isNotBlank()

    val isTextValid = if (state.wantsText) {
        val posValid = !state.textPosition.isNullOrBlank()
        val contentValid = if (state.useAiTextInImage) !state.textContent.isNullOrBlank() else true
        posValid && contentValid
    } else true

    val isLogoValid = if (state.wantsLogo) {
        !state.logoPosition.isNullOrBlank() && !state.logoFile.isNullOrBlank()
    } else true

    val isGenerateEnabled = isAspectRatioValid && isStyleValid && isTextValid && isLogoValid

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Fixed / Sticky Bottom Footer with Generate Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .neumorphicConvex(shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), elevation = 8.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PrimaryButton(
                    text = "✨ Generate My Banner Now",
                    enabled = true,
                    onClick = {
                        if (!isGenerateEnabled) {
                            val missing = buildList {
                                if (!isAspectRatioValid) add("Aspect Ratio")
                                if (!isStyleValid) add("Banner Style")
                                if (state.wantsText && !isTextValid) {
                                    if (state.textPosition.isNullOrBlank()) add("Text Position")
                                    if (state.useAiTextInImage && state.textContent.isNullOrBlank()) add("Channel Text Content")
                                }
                                if (state.wantsLogo && !isLogoValid) {
                                    if (state.logoFile.isNullOrBlank()) add("Logo / Avatar Image")
                                    if (state.logoPosition.isNullOrBlank()) add("Logo Position")
                                }
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "⚠️ Please complete required: ${missing.joinToString(", ")}"
                                )
                            }
                        } else {
                            bannerViewModel.buildFinalPrompt()
                            onGenerateBanner()
                        }
                    },
                    subtext = if (!isGenerateEnabled) "⚠️ Complete required selections above to generate" else "⚡ Free generation • Ready in ~15 seconds"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ScreenHeader(
            title = "Generation Settings",
            stepLabel = "Step 2 of 2",
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
            Text(text = "Style & Composition", fontSize = 12.sp, color = SlateNeutral, fontWeight = FontWeight.Bold)
            Text(text = "100% READY", fontSize = 12.sp, color = SafeMint, fontWeight = FontWeight.Bold)
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
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(SafeMint, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Banner Look & Layout",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Aspect Ratio & YouTube Target Device Selector
        NeumorphicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📐 YouTube Banner Size Target",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val currentTarget = com.example.data.YouTubeBannerTarget.fromLabel(state.targetDeviceMode)
                    Text(
                        text = "● ${currentTarget.title} (${currentTarget.width}×${currentTarget.height})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeMint,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device Format Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Phone Safe", "Desktop", "Full TV").forEach { mode ->
                    val isSelected = state.targetDeviceMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.neumorphicInset(shape = CircleShape)
                                } else {
                                    Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                }
                            )
                            .clickable { bannerViewModel.updateTargetDeviceMode(mode) },
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

            Spacer(modifier = Modifier.height(12.dp))

            val currentTarget = com.example.data.YouTubeBannerTarget.fromLabel(state.targetDeviceMode)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentTarget.title} Target • ${currentTarget.width} × ${currentTarget.height} px",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (currentTarget) {
                            com.example.data.YouTubeBannerTarget.MOBILE -> "Strict Mobile Boundary: AI concentrates all core artwork & text inside the 1546×423 center zone. Output is 2560×1440 YouTube Ready with sharp safe zone."
                            com.example.data.YouTubeBannerTarget.DESKTOP -> "Desktop Boundary: Fills full desktop/laptop banner (2560×423 px). AI keeps primary action inside the horizontal band."
                            com.example.data.YouTubeBannerTarget.FULL_TV -> "Universal Canvas: Complete 2560×1440 16:9 canvas visible on Smart TVs, perfectly safe-zone centered for smaller screens."
                        },
                        fontSize = 11.sp,
                        color = SlateNeutral,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(20.dp))

        // Visual Style Selector
        Text(
            text = "🎨 Pick a Visual Style *",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(styleOptions) { opt ->
                val isSelected = state.selectedStyle.equals(opt.title, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .border(2.dp, CoralRed, RoundedCornerShape(18.dp))
                                    .neumorphicConvex(shape = RoundedCornerShape(18.dp), elevation = 6.dp)
                            } else {
                                Modifier.neumorphicConvex(shape = RoundedCornerShape(18.dp), elevation = 3.dp)
                            }
                        )
                        .clickable { bannerViewModel.updateSelectedStyle(opt.title) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = opt.icon, fontSize = 22.sp)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(CoralRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = opt.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = opt.subtitle,
                            fontSize = 11.sp,
                            color = SlateNeutral
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Text Section Card
        NeumorphicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "T. Add text to banner?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Include titles & upload schedule",
                        fontSize = 12.sp,
                        color = SlateNeutral
                    )
                }

                Switch(
                    checked = state.wantsText,
                    onCheckedChange = { bannerViewModel.updateWantsText(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CoralRed
                    )
                )
            }

            AnimatedVisibility(visible = state.wantsText) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Text Position in Safe Area *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PositionGrid3x3(
                        selectedPosition = state.textPosition ?: "Center",
                        onPositionSelected = { bannerViewModel.updateTextPosition(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    InsetTextField(
                        value = state.textContent ?: "",
                        onValueChange = { bannerViewModel.updateTextContent(it, state.subtextContent) },
                        label = "Channel Title" + if (state.useAiTextInImage) " *" else " (for preview overlay)",
                        placeholder = "Alex Gaming HD"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InsetTextField(
                        value = state.subtextContent ?: "",
                        onValueChange = { bannerViewModel.updateTextContent(state.textContent, it) },
                        label = "Subtext / Tagline",
                        placeholder = "New videos every Tuesday!"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Render text directly inside AI image",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Experimental Path A (text generated in image)",
                                fontSize = 11.sp,
                                color = SlateNeutral
                            )
                        }
                        Switch(
                            checked = state.useAiTextInImage,
                            onCheckedChange = { bannerViewModel.updateUseAiTextInImage(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CoralRed
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logo / Avatar Section Card
        NeumorphicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👤 Add logo or avatar?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Smart cutout into the safe zone",
                        fontSize = 12.sp,
                        color = SlateNeutral
                    )
                }

                Switch(
                    checked = state.wantsLogo,
                    onCheckedChange = { bannerViewModel.updateWantsLogo(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CoralRed
                    )
                )
            }

            AnimatedVisibility(visible = state.wantsLogo) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    NeumorphicCard(padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .neumorphicInset(shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = state.logoFile ?: "Alex_Avatar_Cutout.png",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafeMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (state.logoImageBytes != null) "Custom uploaded photo active" else "Background removed automatically",
                                    fontSize = 11.sp,
                                    color = SlateNeutral
                                )
                                Text(
                                    text = "Replace image",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed,
                                    modifier = Modifier.clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (photoPickerError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = photoPickerError ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Avatar Alignment *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val alignOptions = listOf("Left of Title", "Right of Title")
                        alignOptions.forEach { align ->
                            val isSelected = state.logoPosition.equals(align, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(CoralRed, RoundedCornerShape(12.dp))
                                        } else {
                                            Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                                        }
                                    )
                                    .clickable { bannerViewModel.updateLogoPosition(align) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = align,
                                    color = if (isSelected) Color.White else SlateNeutral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Image Generation Model Selector Card
        NeumorphicCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎨 Image Model",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SafeMint.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = SafeMint,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${apiKeyState.imageConfig.providerName}: ${apiKeyState.imageConfig.imageModel}",
                        fontSize = 11.sp,
                        color = SlateNeutral,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                        .clickable { showImageModelSwitcherDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Switch ▾",
                        color = CoralRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showImageModelSwitcherDialog) {
        ImageModelSwitcherDialog(
            currentConfig = apiKeyState.imageConfig,
            savedProfiles = apiKeyState.savedImageProfiles,
            onSelectProfile = { profileId ->
                apiKeyViewModel.switchActiveImageProfile(profileId)
                showImageModelSwitcherDialog = false
            },
            onSelectModel = { modelId ->
                apiKeyViewModel.switchImageModel(modelId)
                showImageModelSwitcherDialog = false
            },
            onDismiss = { showImageModelSwitcherDialog = false }
        )
    }
}

@Composable
fun ImageModelSwitcherDialog(
    currentConfig: ApiProviderConfig,
    savedProfiles: List<ApiProviderConfig>,
    onSelectProfile: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customImageModelInput by remember(currentConfig.imageModel) {
        mutableStateOf(currentConfig.imageModel)
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
                                text = "🎨 Switch Image Model",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select rendering engine for your YouTube banner",
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
                    val effectiveImageProfiles = if (savedProfiles.isEmpty()) {
                        listOf(
                            ApiProviderConfig(
                                id = "default_image_nvidia",
                                providerId = "openai_compatible",
                                providerName = "NVIDIA NIM (Images)",
                                baseUrl = "https://integrate.api.nvidia.com/v1",
                                apiKey = "",
                                imageModel = "black-forest-labs/flux.1-dev",
                                isOpenAiCompatible = true
                            ),
                            ApiProviderConfig(
                                id = "default_image_gemini",
                                providerId = "gemini",
                                providerName = "Google Gemini (Direct)",
                                baseUrl = "https://generativelanguage.googleapis.com",
                                apiKey = "",
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
                        effectiveImageProfiles.forEach { profile ->
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
                        text = "Available Image Models",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentConfig.providerId == "gemini") {
                        // Gemini Image Models List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ApiProviderPresets.GEMINI_IMAGE_MODELS) { modelOpt ->
                                val isSelected = currentConfig.imageModel.equals(modelOpt.id, ignoreCase = true)
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
                    } else if (currentConfig.providerId == "cloudflare") {
                        // Cloudflare Workers AI Image Models List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ApiProviderPresets.CLOUDFLARE_IMAGE_MODELS) { modelOpt ->
                                val isSelected = currentConfig.imageModel.equals(modelOpt.id, ignoreCase = true)
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
                    } else if (currentConfig.providerId == "huggingface") {
                        // Hugging Face Image Models List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ApiProviderPresets.HUGGINGFACE_IMAGE_MODELS) { modelOpt ->
                                val isSelected = currentConfig.imageModel.equals(modelOpt.id, ignoreCase = true)
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
                    } else {
                        // OpenAI-compatible / Custom Image Model Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Quick Select Models (NVIDIA / Open)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateNeutral
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val popularList = listOf(
                                "black-forest-labs/flux.1-dev",
                                "black-forest-labs/flux.1-schnell",
                                "qwen-image",
                                "qwen-image-edit",
                                "flux.2-klein-4b",
                                "stable-diffusion-3.5-large",
                                "FLUX.1-Kontext-dev"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                popularList.forEach { modelName ->
                                    val isSelected = customImageModelInput.equals(modelName, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .neumorphicConvex(
                                                shape = CircleShape,
                                                elevation = if (isSelected) 1.dp else 2.dp
                                            )
                                            .clickable { customImageModelInput = modelName }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = modelName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            InsetTextField(
                                value = customImageModelInput,
                                onValueChange = { customImageModelInput = it },
                                label = "Image Model ID",
                                placeholder = "e.g. qwen-image, flux.2-klein-4b"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            PrimaryButton(
                                text = "Apply Image Model",
                                enabled = customImageModelInput.isNotBlank(),
                                onClick = {
                                    onSelectModel(customImageModelInput.trim())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StyleOption(val title: String, val subtitle: String, val icon: String)
