package com.vurnindustrys.vurn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VurnColors = darkColorScheme(
    primary = Color(0xFFC6FF3D),
    onPrimary = Color(0xFF101407),
    background = Color(0xFF090B0A),
    surface = Color(0xFF111411),
    surfaceVariant = Color(0xFF191D19),
    onBackground = Color(0xFFF2F5EE),
    onSurface = Color(0xFFF2F5EE),
    onSurfaceVariant = Color(0xFFAAB3A5),
    error = Color(0xFFFF6B6B)
)

@Composable
fun VurnTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VurnColors, content = content)
}
