package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.state.BannerCreationViewModel
import com.example.state.HistoryViewModel
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistoryTab { IMAGES, PROMPTS }

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    bannerViewModel: BannerCreationViewModel,
    onSelectBanner: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val historyList by historyViewModel.recentBannersState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf(HistoryTab.IMAGES) }
    var selectedImageIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedPromptIds by remember { mutableStateOf(setOf<Long>()) }

    // Filter non-empty items
    val currentSelectedIds = if (activeTab == HistoryTab.IMAGES) selectedImageIds else selectedPromptIds
    val allCurrentIds = historyList.map { it.id }.toSet()
    val isAllSelected = allCurrentIds.isNotEmpty() && currentSelectedIds.containsAll(allCurrentIds)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            ScreenHeader(
                title = "Creation History",
                stepLabel = "${historyList.size} Saved Items",
                onBackClick = onBack,
                isDark = isDark,
                onToggleDark = onToggleDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Dual Tab Switcher: [ 🎨 Images ] and [ 💬 Prompts ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Images Tab
                val isImages = activeTab == HistoryTab.IMAGES
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .then(
                            if (isImages) Modifier.neumorphicInset(shape = RoundedCornerShape(12.dp))
                            else Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 3.dp)
                        )
                        .clickable { activeTab = HistoryTab.IMAGES },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎨 Images (${historyList.size})",
                        fontSize = 13.sp,
                        fontWeight = if (isImages) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isImages) CoralRed else SlateNeutral
                    )
                }

                // Prompts Tab
                val isPrompts = activeTab == HistoryTab.PROMPTS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .then(
                            if (isPrompts) Modifier.neumorphicInset(shape = RoundedCornerShape(12.dp))
                            else Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 3.dp)
                        )
                        .clickable { activeTab = HistoryTab.PROMPTS },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💬 Prompts (${historyList.size})",
                        fontSize = 13.sp,
                        fontWeight = if (isPrompts) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isPrompts) CoralRed else SlateNeutral
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NeumorphicCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .neumorphicInset(shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = SlateNeutral,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (activeTab == HistoryTab.IMAGES) "No Saved Images Yet" else "No Saved Prompts Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Banners and prompts you generate will be automatically cached here for easy access, copying, and reuse.",
                                fontSize = 13.sp,
                                color = SlateNeutral,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 2. Selection & Batch Deletion Control Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicConvex(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Select All" Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (activeTab == HistoryTab.IMAGES) {
                                    selectedImageIds = if (isAllSelected) emptySet() else allCurrentIds
                                } else {
                                    selectedPromptIds = if (isAllSelected) emptySet() else allCurrentIds
                                }
                            }
                        ) {
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = { checked ->
                                    if (activeTab == HistoryTab.IMAGES) {
                                        selectedImageIds = if (checked) allCurrentIds else emptySet()
                                    } else {
                                        selectedPromptIds = if (checked) allCurrentIds else emptySet()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CoralRed,
                                    uncheckedColor = SlateNeutral
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAllSelected) "Deselect All" else "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentSelectedIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${currentSelectedIds.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CoralRed
                                )
                            }
                        }

                        // Delete Actions: Delete Selection OR Clear All
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentSelectedIds.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 3.dp)
                                        .clickable {
                                            val count = currentSelectedIds.size
                                            historyViewModel.deleteItems(currentSelectedIds)
                                            if (activeTab == HistoryTab.IMAGES) {
                                                selectedImageIds = emptySet()
                                            } else {
                                                selectedPromptIds = emptySet()
                                            }
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Deleted $count selected items.")
                                            }
                                        }
                                        .background(CoralRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Selection",
                                            tint = CoralRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Delete Selected (${currentSelectedIds.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralRed
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Clear All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateNeutral,
                                    modifier = Modifier
                                        .clickable {
                                            historyViewModel.clearAllHistory()
                                            selectedImageIds = emptySet()
                                            selectedPromptIds = emptySet()
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Cleared history.")
                                            }
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Tab Content Display
                if (activeTab == HistoryTab.IMAGES) {
                    // ===== IMAGES TAB =====
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(historyList, key = { it.id }) { item ->
                            val isSelected = selectedImageIds.contains(item.id)
                            val bitmap = remember(item.id) {
                                try {
                                    BitmapFactory.decodeByteArray(item.imageBytes, 0, item.imageBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            val dateString = remember(item.timestamp) {
                                SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(item.timestamp))
                            }

                            NeumorphicCard {
                                Column {
                                    // Image Banner with Selection Checkbox Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black)
                                            .clickable {
                                                selectedImageIds = if (isSelected) {
                                                    selectedImageIds - item.id
                                                } else {
                                                    selectedImageIds + item.id
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = item.channelTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Checkbox in top-right corner
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(
                                                    if (isSelected) CoralRed else Color.Black.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                                .size(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedImageIds = if (checked) {
                                                        selectedImageIds + item.id
                                                    } else {
                                                        selectedImageIds - item.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = CoralRed,
                                                    uncheckedColor = Color.White,
                                                    checkmarkColor = Color.White
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.channelTitle,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = dateString,
                                                fontSize = 11.sp,
                                                color = SlateNeutral
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Single Delete Button
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                                    .clickable {
                                                        historyViewModel.deleteItem(item.id)
                                                        selectedImageIds = selectedImageIds - item.id
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = CoralRed,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            // Open / Restore Button
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                                    .clickable {
                                                        bannerViewModel.restoreBannerFromHistory(item.imageBytes, item.channelTitle)
                                                        onSelectBanner()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Open",
                                                    tint = SafeMint,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ===== PROMPTS TAB =====
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(historyList, key = { it.id }) { item ->
                            val isSelected = selectedPromptIds.contains(item.id)
                            val dateString = remember(item.timestamp) {
                                SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(item.timestamp))
                            }

                            NeumorphicCard {
                                Column {
                                    // Top Row: Title, Date, Checkbox
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedPromptIds = if (checked) {
                                                        selectedPromptIds + item.id
                                                    } else {
                                                        selectedPromptIds - item.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = CoralRed,
                                                    uncheckedColor = SlateNeutral
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = item.channelTitle.ifBlank { "Prompt Idea" },
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = dateString,
                                                    fontSize = 10.sp,
                                                    color = SlateNeutral
                                                )
                                            }
                                        }

                                        // Delete Single Prompt Button
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                                .clickable {
                                                    historyViewModel.deleteItem(item.id)
                                                    selectedPromptIds = selectedPromptIds - item.id
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = CoralRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Full Prompt Text Container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .neumorphicInset(shape = RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = item.prompt.ifBlank { "No prompt text recorded." },
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons: Copy Prompt & Use in Banner
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Copy Button
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Banner Prompt", item.prompt)
                                                    clipboard.setPrimaryClip(clip)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Copied prompt to clipboard!")
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = SlateNeutral,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Copy Prompt",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SlateNeutral
                                                )
                                            }
                                        }

                                        // Use in Banner Button
                                        Box(
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(38.dp)
                                                .neumorphicConvex(shape = RoundedCornerShape(10.dp), elevation = 2.dp)
                                                .clickable {
                                                    bannerViewModel.updateChannelTopic(item.channelTitle.take(50))
                                                    bannerViewModel.updateCustomPromptDetails(item.prompt)
                                                    bannerViewModel.restoreBannerFromHistory(item.imageBytes, item.channelTitle)
                                                    onSelectBanner()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.RocketLaunch,
                                                    contentDescription = "Use",
                                                    tint = SafeMint,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Use for Banner 🚀",
                                                    fontSize = 11.sp,
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
                }
            }
        }
    }
}
