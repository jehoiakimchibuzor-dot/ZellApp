package com.example.zell.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Global state for theme management
enum class AppTheme { LIGHT, DARK, SYSTEM }
val LocalAppTheme = compositionLocalOf { mutableStateOf(AppTheme.SYSTEM) }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = LightBackground,
    secondary = ZellTeal,
    tertiary = ZellPink,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightBackground,
    secondary = ZellTeal,
    tertiary = ZellPink,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground
)

@Composable
fun ZellTheme(
    themeState: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeState) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
