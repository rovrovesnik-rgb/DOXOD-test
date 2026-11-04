package com.doxod.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = DarkBackground,
    primaryContainer = GoldDark,
    onPrimaryContainer = TextPrimary,
    secondary = GoldLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = GoldPrimary,
    tertiary = GoldLight,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GoldBorder,
    outlineVariant = DarkSurfaceElevated,
    error = ErrorRed,
    onError = DarkBackground,
)

@Composable
fun DoXoDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
