package com.sofonias.gemoff.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sofonias.gemoff.AppTheme

private val GeminiIndigoDark = Color(0xFF818CF8)
private val GeminiIndigoLight = Color(0xFF4F46E5)
private val DeepSpaceBackground = Color(0xFF0F0F1A)
private val DarkBubbleSurface = Color(0xFF222236)

private val DarkColors = darkColorScheme(
    primary = GeminiIndigoDark,
    onPrimary = Color(0xFF0F172A),
    background = DeepSpaceBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF181826),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkBubbleSurface,
    onSurfaceVariant = Color(0xFFE2E8F0),
    secondaryContainer = DarkBubbleSurface,
    onSecondaryContainer = Color(0xFFF1F5F9)
)

private val LightColors = lightColorScheme(
    primary = GeminiIndigoLight,
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2FF),
    onSurfaceVariant = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF1E1B4B)
)

@Composable
fun GemOffTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
