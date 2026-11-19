package com.example.nahwsimple

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5C6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E8EF),
    onPrimaryContainer = Color(0xFF001F26),
    secondaryContainer = Color(0xFFFFE9B8),
    onSecondaryContainer = Color(0xFF261900),
    surfaceVariant = Color(0xFFE7ECEE),
    onSurfaceVariant = Color(0xFF1D2B2F),
    background = Color(0xFFF7FAFB),
    surface = Color(0xFFF7FAFB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF86D2E3),
    onPrimary = Color(0xFF003642),
    primaryContainer = Color(0xFF0B4F5E),
    onPrimaryContainer = Color(0xFFC3E8EF),
    secondaryContainer = Color(0xFF4A3A10),
    onSecondaryContainer = Color(0xFFFFE9B8),
    surfaceVariant = Color(0xFF2A3437),
    onSurfaceVariant = Color(0xFFDDE4E6),
    background = Color(0xFF0F1415),
    surface = Color(0xFF0F1415),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
