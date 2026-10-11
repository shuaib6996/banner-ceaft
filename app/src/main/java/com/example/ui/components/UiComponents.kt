package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AiModelConfig
import com.example.data.AiModelItem
import com.example.ui.theme.SafeMint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CoralRedLight
import com.example.ui.theme.SlateNeutral
import com.example.ui.theme.isDarkTheme
import com.example.ui.theme.neumorphicConvex
import com.example.ui.theme.neumorphicInset

/**
 * Primary Hero Tactile Pill Button with Coral Red Gradient & Glow effect.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    subtext: String? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "primaryBtnScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (isPressed) 2.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = CoralRed,
                    spotColor = CoralRed
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (enabled) listOf(CoralRedLight, CoralRed)
                        else listOf(Color.Gray, Color.DarkGray)
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.2).sp
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (subtext != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtext,
                fontSize = 12.sp,
                color = SlateNeutral,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Secondary Tactile Button matching Neumorphic substrate.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDanger: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shape = CircleShape
    val isDark = isDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (isPressed) {
                    Modifier.neumorphicInset(shape = shape)
                } else {
                    Modifier.neumorphicConvex(shape = shape, elevation = 4.dp)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!enabled) Color.Gray else if (isDanger) CoralRed else if (isDark) Color.White else SlateNeutral,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (!enabled) Color.Gray else if (isDanger) CoralRed else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Neumorphic Card Container
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .neumorphicConvex(shape = shape, elevation = 6.dp)
            .padding(padding)
    ) {
        Column {
            content()
        }
    }
}

/**
 * Debossed Recessed Input Field
 */
@Composable
fun InsetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    required: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }
    val isDark = isDarkTheme()

    Column(modifier = modifier) {
        if (label != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (required) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "*", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = if (isDark) Color(0xFF151821) else Color(0xFFE4EAFA)
                )
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 2.dp,
                            color = CoralRed.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = SlateNeutral.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        keyboardOptions = keyboardOptions,
                        visualTransformation = visualTransformation,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * Directional 3x3 Position Grid Selector
 */
@Composable
fun PositionGrid3x3(
    selectedPosition: String,
    onPositionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val positions = listOf(
        listOf("Top-Left", "Top", "Top-Right"),
        listOf("Left", "Center", "Right"),
        listOf("Bottom-Left", "Bottom", "Bottom-Right")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .neumorphicInset(shape = RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        positions.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { pos ->
                    val isSelected = selectedPosition.equals(pos, ignoreCase = true)
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CoralRed)
                                } else {
                                    Modifier.neumorphicConvex(shape = RoundedCornerShape(12.dp), elevation = 2.dp)
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onPositionSelected(pos) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (pos) {
                                "Top-Left" -> "↖"
                                "Top" -> "↑"
                                "Top-Right" -> "↗"
                                "Left" -> "←"
                                "Center" -> "⁞"
                                "Right" -> "→"
                                "Bottom-Left" -> "↙"
                                "Bottom" -> "↓"
                                "Bottom-Right" -> "↘"
                                else -> pos
                            },
                            color = if (isSelected) Color.White else SlateNeutral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Bar Header
 */
@Composable
fun ScreenHeader(
    title: String,
    stepLabel: String? = null,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    isDark: Boolean = isDarkTheme(),
    onToggleDark: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .neumorphicConvex(shape = CircleShape, elevation = 4.dp)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f, fill = false)) {
                if (stepLabel != null) {
                    Text(
                        text = stepLabel.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onToggleDark != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neumorphicConvex(shape = CircleShape, elevation = 3.dp)
                        .clickable { onToggleDark() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDark) "☀️" else "🌙",
                        fontSize = 15.sp
                    )
                }
            }
            if (onSettingsClick != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neumorphicConvex(shape = CircleShape, elevation = 3.dp)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Settings",
                        tint = CoralRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Model Selection Modal Dialog for choosing Gemini / Imagen / Embedding models.
 */
@Composable
fun ModelSelectionDialog(
    selectedModel: String,
    onModelSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val groupedModels = remember { AiModelConfig.ALL_MODELS.groupBy { it.category } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
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
                            text = "🤖 Select AI Model",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose model for your API key",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedModels.forEach { (category, models) ->
                        item(key = category) {
                            Text(
                                text = category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(models, key = { it.id }) { model ->
                            val isSelected = selectedModel.equals(model.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(2.dp, CoralRed, RoundedCornerShape(16.dp))
                                                .neumorphicInset(shape = RoundedCornerShape(16.dp))
                                        } else {
                                            Modifier.neumorphicConvex(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
                                        }
                                    )
                                    .clickable {
                                        onModelSelect(model.id)
                                        onDismiss()
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = model.displayName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (model.isRecommended) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(SafeMint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Recommended",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SafeMint
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = model.description,
                                            fontSize = 11.sp,
                                            color = SlateNeutral,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .then(
                                                if (isSelected) Modifier.background(CoralRed, CircleShape)
                                                else Modifier.border(1.5.dp, SlateNeutral.copy(alpha = 0.5f), CircleShape)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryButton(
                    text = "Close",
                    onClick = onDismiss
                )
            }
        }
    }
}

