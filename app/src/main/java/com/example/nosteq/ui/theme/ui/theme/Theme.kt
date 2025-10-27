package com.example.nosteq.ui.theme.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.nosteq.ui.theme.Typography
import kotlin.Result.Companion.success


//theme in use
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1E3A8A), // Dark blue
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF3B82F6), // Lighter blue accent
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFE5E7EB),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A), // Dark blue
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF3B82F6), // Lighter blue accent
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF), // White background
    onBackground = Color(0xFF1E3A8A), // Dark blue text
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E3A8A),
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF)
)
@Composable
fun NosteqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
