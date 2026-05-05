package com.autodoc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.autodoc.ui.AppColors

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Gold,
    onPrimary = AppColors.Navy,

    secondary = AppColors.SoftText,
    onSecondary = Color.White,

    tertiary = AppColors.Warning,
    onTertiary = AppColors.Navy,

    background = AppColors.DeepBg,
    onBackground = Color.White,

    surface = AppColors.Navy,
    onSurface = Color.White,

    surfaceVariant = AppColors.CardBg,
    onSurfaceVariant = AppColors.SoftText,

    error = AppColors.Danger,
    onError = Color.White,

    outline = AppColors.Border
)

@Composable
fun AutoDocTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}