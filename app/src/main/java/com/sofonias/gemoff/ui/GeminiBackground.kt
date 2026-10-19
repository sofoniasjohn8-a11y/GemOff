package com.sofonias.gemoff.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun GeminiBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F0F1A) else Color(0xFFF8FAFC))
    ) {
        // Gemini Signature Aurora Glowing Orbs / Ambient Nebulae
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDark) {
                // Top-left Gemini purple/indigo aurora
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.32f), // Indigo
                            Color(0xFF8B5CF6).copy(alpha = 0.20f), // Purple
                            Color.Transparent
                        ),
                        center = Offset(width * 0.2f, height * 0.15f),
                        radius = width * 0.95f
                    )
                )

                // Center-right Gemini cyan/electric blue aurora
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0284C7).copy(alpha = 0.28f), // Cyan Blue
                            Color(0xFF38BDF8).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.88f, height * 0.48f),
                        radius = width * 0.9f
                    )
                )

                // Bottom-left subtle deep violet flare
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4C1D95).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.85f),
                        radius = width * 0.85f
                    )
                )
            } else {
                // Light mode Gemini aurora flares
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC4B5FD).copy(alpha = 0.45f), // Soft Violet
                            Color(0xFFEDE9FE).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.25f, height * 0.12f),
                        radius = width * 0.85f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBAE6FD).copy(alpha = 0.45f), // Soft Sky Blue
                            Color(0xFFE0F2FE).copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.5f),
                        radius = width * 0.9f
                    )
                )
            }
        }

        content()
    }
}
