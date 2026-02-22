package com.example.andrioddock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary, onPrimary = OnPrimaryDark, primaryContainer = PrimaryDark, onPrimaryContainer = PrimaryLight,
    secondary = Secondary, onSecondary = OnSecondaryDark, secondaryContainer = SecondaryDark, onSecondaryContainer = SecondaryLight,
    background = BackgroundDark, onBackground = OnBackgroundDark, surface = SurfaceDark, onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark, error = Error, errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer, outline = OutlineDark, outlineVariant = SurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary, onPrimary = OnPrimaryLight, primaryContainer = PrimaryLight, onPrimaryContainer = PrimaryDark,
    secondary = Secondary, onSecondary = OnSecondaryLight, secondaryContainer = SecondaryLight, onSecondaryContainer = SecondaryDark,
    background = BackgroundLight, onBackground = OnBackgroundLight, surface = SurfaceLight, onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight, onSurfaceVariant = OnSurfaceVariantLight, error = Error, errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer, outline = OutlineLight, outlineVariant = SurfaceVariantLight
)

@Composable
fun AndrioddDockTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { val context = LocalContext.current; if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect { val window = (view.context as Activity).window; window.statusBarColor = colorScheme.background.toArgb(); WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
