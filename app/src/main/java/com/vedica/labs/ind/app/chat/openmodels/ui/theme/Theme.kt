package com.vedica.labs.ind.app.chat.openmodels.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = VibrantIndigo,
    tertiary = SuccessGreen,
    background = DarkObsidian,
    surface = DarkCard,
    surfaceVariant = DarkCard,
    onPrimary = DarkObsidian,
    onSecondary = DarkObsidian,
    onTertiary = DarkObsidian,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ErrorRed,
    onError = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightNavy,
    secondary = VibrantIndigo,
    tertiary = SuccessGreen,
    background = LightPorcelain,
    surface = LightCard,
    surfaceVariant = LightCard,
    onPrimary = LightCard,
    onSecondary = LightCard,
    onTertiary = LightCard,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = ErrorRed,
    onError = LightCard
)

@Composable
fun OpenModelsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpenModelsTypography,
        content = content
    )
}
