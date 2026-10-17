package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BannerTemplate
import com.example.data.TemplateSlot
import com.example.data.TemplateSlotType
import com.example.data.YouTubeBannerTarget
import com.example.state.TemplateCreationViewModel
import com.example.ui.components.InsetTextField
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCustomizeScreen(
    viewModel: TemplateCreationViewModel,
    onBannerCreated: (finalBytes: ByteArray, masterBytes: ByteArray, templateName: String) -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val template = state.selectedTemplate
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Edge Case: Discard confirmation dialog when slots filled
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Active Slot Editing Sheet State
    var editingTextSlot by remember { mutableStateOf<TemplateSlot?>(null) }
    var activeImageSlotId by remember { mutableStateOf<String?>(null) }
    var textInputValue by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Gallery Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && activeImageSlotId != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.setSlotValue(activeImageSlotId!!, bytes)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Could not load image file.")
                }
            }
        }
    }

    // Intercept Back Press to check for unsaved changes
    BackHandler {
        if (state.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRed) },
            title = { Text(text = "Discard Customization?", fontWeight = FontWeight.Bold) },
            text = { Text("You have customized slots in this template. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.resetCustomization()
                        onBack()
                    }
                ) {
                    Text("Discard", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (template == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No template selected. Please choose a template.")
        }
        return
    }

    // Load background bitmap
    val bgBitmap = remember(template.backgroundAsset) {
        try {
            context.assets.open(template.backgroundAsset).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Sticky Bottom Action Bar with Target Device Mode & Create Banner Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .neumorphicConvex(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = 8.dp
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    val currentTarget = YouTubeBannerTarget.fromLabel(state.targetDeviceMode)

                    PrimaryButton(
                        text = if (state.isRendering) "Compositing Banner..." else "✨ Create Banner (${currentTarget.title})",
                        enabled = state.isReadyToRender && !state.isRendering,
                        onClick = {
                            scope.launch {
                                val resultBytes = viewModel.renderFinalBanner(context)
                                if (resultBytes != null && state.masterCanvasBytes != null) {
                                    onBannerCreated(
                                        resultBytes,
                                        state.masterCanvasBytes!!,
                                        template.name
                                    )
                                } else if (state.errorMessage != null) {
                                    snackbarHostState.showSnackbar(state.errorMessage ?: "Failed to render banner.")
                                }
                            }
                        }
                    )

                    if (!state.isReadyToRender) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val missingSlots = template.slots.filter { it.required && (state.slotValues[it.id] as? String).isNullOrBlank() }
                        Text(
                            text = "Fill required slot: ${missingSlots.joinToString { it.label }}",
                            fontSize = 11.sp,
                            color = CoralRed,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ScreenHeader(
                title = template.name,
                onBackClick = {
                    if (state.hasUnsavedChanges) {
                        showDiscardDialog = true
                    } else {
                        onBack()
                    }
                },
                isDark = isDark,
                onToggleDark = onToggleDark
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Tap on the highlighted slots directly or configure them below.",
                fontSize = 13.sp,
                color = SlateNeutral,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Interactive Canvas Preview Box (2560x1440 16:9 canvas scaled to screen)
            NeumorphicCard(padding = 6.dp) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                ) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight

                    // Scale factors relative to full 2560x1440 canvas
                    val scaleX = containerWidth / 2560f
                    val scaleY = containerHeight / 1440f

                    // Template Background Image
                    if (bgBitmap != null) {
                        Image(
                            bitmap = bgBitmap.asImageBitmap(),
                            contentDescription = template.name,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Tappable Slot Overlays positioned precisely at their canvas coordinates
                    template.slots.forEach { slot ->
                        val slotLeft = scaleX * slot.rect.x
                        val slotTop = scaleY * slot.rect.y
                        val slotWidth = scaleX * slot.rect.width
                        val slotHeight = scaleY * slot.rect.height

                        val rawValue = state.slotValues[slot.id]
                        val isFilled = when (slot.type) {
                            TemplateSlotType.TEXT -> (rawValue as? String)?.isNotBlank() == true
                            TemplateSlotType.IMAGE -> (rawValue as? ByteArray)?.isNotEmpty() == true
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = slotLeft, y = slotTop)
                                .size(width = slotWidth, height = slotHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .then(
                                    if (isFilled) {
                                        Modifier.background(Color.Black.copy(alpha = 0.25f))
                                    } else if (slot.required) {
                                        Modifier
                                            .border(1.5.dp, CoralRed, RoundedCornerShape(6.dp))
                                            .background(CoralRed.copy(alpha = 0.15f))
                                    } else {
                                        Modifier
                                            .border(1.dp, SafeMint, RoundedCornerShape(6.dp))
                                            .background(SafeMint.copy(alpha = 0.12f))
                                    }
                                )
                                .clickable {
                                    if (slot.type == TemplateSlotType.TEXT) {
                                        editingTextSlot = slot
                                        textInputValue = (rawValue as? String).orEmpty()
                                    } else {
                                        activeImageSlotId = slot.id
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (slot.type == TemplateSlotType.TEXT) {
                                val textContent = (rawValue as? String).orEmpty()
                                if (textContent.isNotBlank()) {
                                    val textColor = try {
                                        Color(android.graphics.Color.parseColor(slot.defaultStyle?.colorHex ?: "#FFFFFF"))
                                    } catch (e: Exception) {
                                        Color.White
                                    }
                                    Text(
                                        text = textContent,
                                        fontSize = 12.sp,
                                        fontWeight = if (slot.defaultStyle?.isBold != false) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (slot.required) "✏️ ${slot.label} *" else "✏️ ${slot.label}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (slot.required) CoralRed else SafeMint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                val imgBytes = rawValue as? ByteArray
                                if (imgBytes != null && imgBytes.isNotEmpty()) {
                                    val slotBitmap = remember(imgBytes) {
                                        try {
                                            BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (slotBitmap != null) {
                                        Image(
                                            bitmap = slotBitmap.asImageBitmap(),
                                            contentDescription = slot.label,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (slot.required) "📷 ${slot.label} *" else "📷 ${slot.label}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (slot.required) CoralRed else SafeMint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. YouTube Target Format Selector
            NeumorphicCard(padding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📐 Output Target Format",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val currentTarget = YouTubeBannerTarget.fromLabel(state.targetDeviceMode)
                    Box(
                        modifier = Modifier
                            .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${currentTarget.width} × ${currentTarget.height}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeMint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Phone Safe", "Desktop", "Full TV").forEach { mode ->
                        val isSelected = state.targetDeviceMode == mode
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
                                .clickable { viewModel.updateTargetDeviceMode(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CoralRed else SlateNeutral
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Slot Configuration Cards List
            Text(
                text = "Customize Template Slots",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            template.slots.forEach { slot ->
                val rawValue = state.slotValues[slot.id]
                val isFilled = when (slot.type) {
                    TemplateSlotType.TEXT -> (rawValue as? String)?.isNotBlank() == true
                    TemplateSlotType.IMAGE -> (rawValue as? ByteArray)?.isNotEmpty() == true
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .neumorphicConvex(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
                        .clickable {
                            if (slot.type == TemplateSlotType.TEXT) {
                                editingTextSlot = slot
                                textInputValue = (rawValue as? String).orEmpty()
                            } else {
                                activeImageSlotId = slot.id
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .neumorphicInset(shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (slot.type == TemplateSlotType.TEXT) "✏️" else "📷",
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = slot.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (slot.required) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "*", color = CoralRed, fontWeight = FontWeight.Bold)
                                    }
                                }

                                val previewDesc = when {
                                    slot.type == TemplateSlotType.TEXT && isFilled -> "\"${rawValue as String}\""
                                    slot.type == TemplateSlotType.IMAGE && isFilled -> "Custom photo attached ✓"
                                    slot.required -> "Required • Tap to fill"
                                    else -> "Optional • Tap to add"
                                }

                                Text(
                                    text = previewDesc,
                                    fontSize = 11.5.sp,
                                    color = if (isFilled) SafeMint else SlateNeutral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFilled) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(SafeMint.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SafeMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = SlateNeutral,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.clearSlotValue(slot.id) }
                                )
                            } else {
                                Text(
                                    text = "Edit ›",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Bottom Sheet for Editing Text Slot
    if (editingTextSlot != null) {
        val activeSlot = editingTextSlot!!
        ModalBottomSheet(
            onDismissRequest = { editingTextSlot = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit ${activeSlot.label}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (activeSlot.required) {
                        Text(
                            text = "Required",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InsetTextField(
                    value = textInputValue,
                    onValueChange = {
                        textInputValue = it
                        // Live update slot in state
                        viewModel.setSlotValue(activeSlot.id, it)
                    },
                    placeholder = "Enter your ${activeSlot.label.lowercase()}...",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Done",
                    onClick = {
                        viewModel.setSlotValue(activeSlot.id, textInputValue.trim())
                        editingTextSlot = null
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
