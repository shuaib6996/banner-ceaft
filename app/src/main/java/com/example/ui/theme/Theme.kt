package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CoralRedPrimary,
    onPrimary = OnCoralRed,
    primaryContainer = Color(0xFF8B1212),
    secondary = SlateNeutralSecondary,
    onSecondary = Color.White,
    tertiary = SafeMintTertiary,
    background = NeumorphicBgDark,
    onBackground = OnSurfaceDark,
    surface = NeumorphicSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF282E3D),
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CoralRedPrimary,
    onPrimary = OnCoralRed,
    primaryContainer = CoralRedContainer,
    secondary = SlateNeutralSecondary,
    onSecondary = Color.White,
    tertiary = SafeMintTertiary,
    background = NeumorphicBgLight,
    onBackground = OnSurfaceLight,
    surface = NeumorphicSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE4EAFA),
    onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun BannerCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
