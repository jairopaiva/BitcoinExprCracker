package com.jairopaiva.exprlab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ExprColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF06102B),
    secondary = Color(0xFF81C995),
    onSecondary = Color(0xFF06210F),
    tertiary = Color(0xFFF8BBA0),
    onTertiary = Color(0xFF3B1F00),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF121A2E),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1B2540),
    onSurfaceVariant = Color(0xFFC4CBE0),
    outline = Color(0xFF3A4668),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun ExprLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ExprColors,
        content = content
    )
}
