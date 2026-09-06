package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = CleanGreenPrimaryLight,
    onPrimary = CleanGreenPrimaryDark,
    primaryContainer = CleanGreenPrimary,
    onPrimaryContainer = CleanGreenPrimaryLight,
    secondary = CleanGreenAccent,
    onSecondary = CleanLightSurface,
    tertiary = CleanOrangeAccent,
    onTertiary = CleanLightSurface,
    tertiaryContainer = CleanWarningBg,
    onTertiaryContainer = CleanWarningText,
    background = CleanDarkBackground,
    onBackground = CleanDarkOnSurface,
    surface = CleanDarkSurface,
    onSurface = CleanDarkOnSurface,
    surfaceVariant = CleanDarkSurfaceVariant,
    onSurfaceVariant = CleanDarkOnSurface,
    outline = CleanDarkOutline,
    outlineVariant = CleanDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = CleanGreenPrimary,
    onPrimary = CleanBlueOnPrimary,
    primaryContainer = CleanGreenPrimaryLight,
    onPrimaryContainer = CleanGreenPrimaryDark,
    secondary = CleanGreenAccent,
    onSecondary = CleanLightSurface,
    tertiary = CleanOrangeAccent,
    onTertiary = CleanLightSurface,
    tertiaryContainer = CleanWarningBg,
    onTertiaryContainer = CleanWarningText,
    background = CleanLightBackground,
    onBackground = CleanLightOnSurface,
    surface = CleanLightSurface,
    onSurface = CleanLightOnSurface,
    surfaceVariant = CleanLightSurfaceVariant,
    onSurfaceVariant = CleanLightOnSurfaceMuted,
    outline = CleanLightOutline,
    outlineVariant = CleanLightOutlineVariant
)

@Composable
fun ZomorrodDriverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val context = LocalContext.current

    SideEffect {
        if (context is Activity) {
            context.window.statusBarColor = CleanGreenPrimary.toArgb()
        }
    }

    // Force RTL direction for Persian Driver App
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
