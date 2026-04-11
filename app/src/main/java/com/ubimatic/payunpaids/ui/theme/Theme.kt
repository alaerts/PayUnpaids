package com.ubimatic.payunpaids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PayUnpaidsColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecond,
    error = Danger,
    onError = TextPrimary,
)

@Composable
fun PayUnpaidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PayUnpaidsColorScheme,
        typography = PayUnpaidsTypography,
        content = content,
    )
}
