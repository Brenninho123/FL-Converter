package com.flconverter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFCA311),
    onPrimary = Color(0xFF0E1730),
    background = Color(0xFF0E1730),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF16233F),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF1E2F57),
    onSurfaceVariant = Color(0xFFA9B4CC),
    outline = Color(0xFF3A4C78),
    tertiary = Color(0xFF5CD68D),
    onTertiary = Color(0xFF0E1730),
    tertiaryContainer = Color(0xFF12392A),
    onTertiaryContainer = Color(0xFFB6F2CE),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A07),
    errorContainer = Color(0xFF4A1D24),
    onErrorContainer = Color(0xFFFFD9D6)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFFCA311),
    onPrimary = Color(0xFF0E1730),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0E1730),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1730),
    surfaceVariant = Color(0xFFE3E9F5),
    onSurfaceVariant = Color(0xFF55627F),
    outline = Color(0xFFB7C2DA),
    tertiary = Color(0xFF1F8A4C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDF5E7),
    onTertiaryContainer = Color(0xFF0B3A21),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE3E1),
    onErrorContainer = Color(0xFF5C1410)
)

@Composable
fun FlConverterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
