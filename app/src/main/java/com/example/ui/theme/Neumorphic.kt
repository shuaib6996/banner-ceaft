package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Neumorphic Soft Design Colors
val CoralRed = Color(0xFFFF4B4B)
val CoralRedLight = Color(0xFFFF6B6B)
val CoralGlow = Color(0x66FF4B4B)
val SlateNeutral = Color(0xFF505F76)
val SafeMint = Color(0xFF00855B)
val WarningAmber = Color(0xFFF59E0B)

val NeumorphicCanvasLight = Color(0xFFF0F4F9)
val NeumorphicCardLight = Color(0xFFF0F4F9)
val NeumorphicShadowDarkLight = Color(0xFFC2CBD6)
val NeumorphicShadowLightLight = Color(0xFFFFFFFF)

val NeumorphicCanvasDark = Color(0xFF181C24)
val NeumorphicCardDark = Color(0xFF202530)
val NeumorphicShadowDarkDark = Color(0xFF101319)
val NeumorphicShadowLightDark = Color(0xFF2C3342)

@Composable
fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.red < 0.3f
}

/**
 * Convex Soft Shadow modifier simulating lighting from top-left.
 */
@Composable
fun Modifier.neumorphicConvex(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    cornerRadius: Dp = 20.dp
): Modifier {
    val isDark = isDarkTheme()
    val shadowDark = if (isDark) NeumorphicShadowDarkDark else NeumorphicShadowDarkLight
    val shadowLight = if (isDark) NeumorphicShadowLightDark else NeumorphicShadowLightLight
    val cardBg = if (isDark) NeumorphicCardDark else NeumorphicCardLight

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowDark,
            spotColor = shadowDark
        )
        .background(cardBg, shape = shape)
        .clip(shape)
}

/**
 * Debossed Inset Shadow simulation for input fields and selected sunken cavities.
 */
@Composable
fun Modifier.neumorphicInset(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null
): Modifier {
    val isDark = isDarkTheme()
    val shadowDark = if (isDark) Color(0x33000000) else Color(0x33000000)
    val shadowLight = if (isDark) Color(0x1AFFFFFF) else Color(0xB3FFFFFF)
    val bg = backgroundColor ?: if (isDark) Color(0xFF151821) else Color(0xFFE4EAFA)

    return this
        .clip(shape)
        .background(bg, shape = shape)
        .drawWithContent {
            drawContent()
            clipRect {
                // Top-Left inner shadow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(shadowDark, Color.Transparent),
                        startY = 0f,
                        endY = 12.dp.toPx()
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(shadowDark, Color.Transparent),
                        startX = 0f,
                        endX = 12.dp.toPx()
                    )
                )
                // Bottom-Right inner highlight
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, shadowLight),
                        startY = size.height - 12.dp.toPx(),
                        endY = size.height
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, shadowLight),
                        startX = size.width - 12.dp.toPx(),
                        endX = size.width
                    )
                )
            }
        }
}

/**
 * Tactile Pressable Modifier with spring scale shift and tactile haptic response.
 */
@Composable
fun Modifier.tactilePressable(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "pressScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
