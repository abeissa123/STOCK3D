package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = SlateDark,
    secondary = EmeraldGlow,
    onSecondary = SlateDark,
    tertiary = AmberGlow,
    background = SlateDark,
    surface = SlateMedium,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = SlateMedium,
    onSurfaceVariant = Color.LightGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force gorgeous dark mode system-wide
    dynamicColor: Boolean = false, // Disable to keep our custom neon palettes branded nicely
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
