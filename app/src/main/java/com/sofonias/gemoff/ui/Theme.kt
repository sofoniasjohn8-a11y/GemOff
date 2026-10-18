package com.sofonias.gemoff.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GemPurple = Color(0xFF8F6FFF)
private val GemPurpleLight = Color(0xFFB69CFF)
private val DeepBackground = Color(0xFF12121E)
private val GemContainer = Color(0xFF2A2440)

private val DarkColors = darkColorScheme(
    primary = GemPurpleLight,
    onPrimary = Color(0xFF1B1B2F),
    background = DeepBackground,
    onBackground = Color(0xFFEAE7F5),
    surface = Color(0xFF1B1B2F),
    onSurface = Color(0xFFEAE7F5),
    surfaceVariant = GemContainer,
    onSurfaceVariant = Color(0xFFD6CFFF),
    secondaryContainer = GemContainer,
    onSecondaryContainer = Color(0xFFEAE7F5)
)

private val LightColors = lightColorScheme(
    primary = GemPurple,
    onPrimary = Color.White,
    background = Color(0xFFF7F5FF),
    onBackground = Color(0xFF1B1B2F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B2F),
    surfaceVariant = Color(0xFFEAE4FF),
    onSurfaceVariant = Color(0xFF3B3557)
)

@Composable
fun GemOffTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
