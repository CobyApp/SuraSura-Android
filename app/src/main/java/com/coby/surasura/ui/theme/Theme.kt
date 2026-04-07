package com.coby.surasura.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = White,
    primaryContainer = AccentBlue,
    onPrimaryContainer = White,
    background = LightBackground,
    onBackground = Black,
    surface = LightSurface,
    onSurface = Black,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF636366),
    secondary = Color(0xFF636366),
    onSecondary = White,
    error = Color(0xFFFF3B30),
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = White,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = White,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFFAEAEB2),
    secondary = Color(0xFFAEAEB2),
    onSecondary = Black,
    error = Color(0xFFFF453A),
    onError = White
)

@Composable
fun SuraSuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
