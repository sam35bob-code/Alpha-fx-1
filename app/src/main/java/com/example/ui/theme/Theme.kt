package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AlphaFxColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00242B),
    primaryContainer = CyanAccentMuted,
    onPrimaryContainer = Color(0xFFD6F8FF),
    secondary = GoldAccent,
    onSecondary = Color(0xFF281C00),
    secondaryContainer = Color(0xFF4A3400),
    onSecondaryContainer = Color(0xFFFFDEA3),
    tertiary = PurpleAccent,
    onTertiary = Color(0xFF1E004B),
    background = TerminalBackground,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TerminalBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always preserve crisp trading terminal styling
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AlphaFxColorScheme,
        typography = Typography,
        content = content
    )
}
