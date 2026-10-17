package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.BannerTemplate
import com.example.data.TemplateSlotType
import com.example.state.TemplateCreationViewModel
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset

@Composable
fun TemplatePickerScreen(
    viewModel: TemplateCreationViewModel,
    onNavigateToCustomize: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val categories = remember(state.templates) {
        listOf("All") + state.templates.map { it.category }.distinct()
    }

    val filteredTemplates = remember(state.templates, state.selectedCategory) {
        if (state.selectedCategory == "All") {
            state.templates
        } else {
            state.templates.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ScreenHeader(
                title = "Choose a Template",
                onBackClick = onBack,
                isDark = isDark,
                onToggleDark = onToggleDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pre-designed, YouTube safe-zone calibrated banners. Pick one and personalize in seconds.",
                fontSize = 13.sp,
                color = SlateNeutral,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chip Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = state.selectedCategory == cat
                    val icon = when (cat) {
                        "Gaming" -> "🎮"
                        "Minimal" -> "📐"
                        "Vlog" -> "🌴"
                        "Tech" -> "⚡"
                        "Education" -> "🎓"
                        else -> "✨"
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.neumorphicInset(shape = CircleShape)
                                } else {
                                    Modifier.neumorphicConvex(shape = CircleShape, elevation = 2.dp)
                                }
                            )
                            .clickable { viewModel.selectCategory(cat) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CoralRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of Template Cards (2 columns)
            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No templates found in this category.",
                        color = SlateNeutral,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onClick = {
                                viewModel.selectTemplate(template)
                                onNavigateToCustomize()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: BannerTemplate,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbBitmap = remember(template.thumbnailAsset) {
        try {
            context.assets.open(template.thumbnailAsset).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    val textSlotCount = template.slots.count { it.type == TemplateSlotType.TEXT }
    val imageSlotCount = template.slots.count { it.type == TemplateSlotType.IMAGE }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicConvex(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = template.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SafeMint,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Category pill on top of thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = template.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = template.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val slotsSummary = buildString {
                    if (textSlotCount > 0) append("$textSlotCount Text")
                    if (imageSlotCount > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("$imageSlotCount Logo")
                    }
                }
                Text(
                    text = slotsSummary,
                    fontSize = 11.sp,
                    color = SlateNeutral
                )
            }
        }
    }
}
