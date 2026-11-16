package com.tumba.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tumba.ai.domain.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = TumbaAccent,
    onPrimary = TumbaBlack,
    primaryContainer = TumbaSurfaceRaised,
    onPrimaryContainer = TumbaText,
    secondary = TumbaTextSecondary,
    onSecondary = TumbaBlack,
    secondaryContainer = TumbaSurfaceRaised,
    onSecondaryContainer = TumbaText,
    tertiary = TumbaAccent,
    onTertiary = TumbaBlack,
    background = TumbaBlack,
    onBackground = TumbaText,
    surface = TumbaSurface,
    onSurface = TumbaText,
    surfaceVariant = TumbaSurfaceRaised,
    onSurfaceVariant = TumbaTextSecondary,
    surfaceContainerLowest = TumbaBlack,
    surfaceContainerLow = TumbaSurfaceLow,
    surfaceContainer = TumbaSurface,
    surfaceContainerHigh = TumbaSurfaceRaised,
    surfaceContainerHighest = TumbaSurfaceRaised,
    outline = TumbaOutline,
    outlineVariant = TumbaOutlineSoft,
    error = TumbaError,
    onError = TumbaBlack,
    errorContainer = TumbaSurfaceRaised,
    onErrorContainer = TumbaError,
    inverseSurface = TumbaText,
    inverseOnSurface = TumbaBlack,
    scrim = TumbaBlack,
)

private val LightColors = lightColorScheme(
    primary = PaperAccent,
    onPrimary = PaperBackground,
    primaryContainer = PaperSurfaceRaised,
    onPrimaryContainer = PaperText,
    secondary = PaperTextSecondary,
    onSecondary = PaperBackground,
    secondaryContainer = PaperSurfaceRaised,
    onSecondaryContainer = PaperText,
    tertiary = PaperAccent,
    onTertiary = PaperBackground,
    background = PaperBackground,
    onBackground = PaperText,
    surface = PaperSurface,
    onSurface = PaperText,
    surfaceVariant = PaperSurfaceRaised,
    onSurfaceVariant = PaperTextSecondary,
    surfaceContainerLowest = PaperBackground,
    surfaceContainerLow = PaperSurfaceLow,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceRaised,
    surfaceContainerHighest = PaperSurfaceRaised,
    outline = PaperOutline,
    outlineVariant = PaperOutlineSoft,
    error = PaperError,
    onError = PaperBackground,
    errorContainer = PaperSurfaceRaised,
    onErrorContainer = PaperError,
    inverseSurface = PaperText,
    inverseOnSurface = PaperBackground,
    scrim = PaperText,
)

@Composable
fun TumbaTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TumbaTypography,
        shapes = TumbaShapes,
        content = content,
    )
}
