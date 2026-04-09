package com.ubimatic.payunpaids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = AmberDark,
    primaryContainer = DarkSurface,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = Color.White,
    tertiary = Green,
    onTertiary = Color.White,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkBg,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorderLight,
    outlineVariant = DarkBorder,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = WarningBg,
    onErrorContainer = WarningText,
)

@Composable
fun PayUnpaidsTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
