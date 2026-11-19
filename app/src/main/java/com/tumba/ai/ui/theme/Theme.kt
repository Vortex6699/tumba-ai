package com.tumba.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tumba.ai.domain.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = TumbaAccent,
    onPrimary = Color(0xFF140F24),
    primaryContainer = TumbaSurfaceRaised,
    onPrimaryContainer = TumbaText,
    secondary = TumbaAccentMint,
    onSecondary = Color(0xFF06211C),
    secondaryContainer = TumbaSurfaceRaised,
    onSecondaryContainer = TumbaText,
    tertiary = TumbaAccentDeep,
    onTertiary = TumbaText,
    background = TumbaBlack,
    onBackground = TumbaText,
    surface = TumbaSurface,
    onSurface = TumbaText,
    surfaceVariant = TumbaSurfaceRaised,
    onSurfaceVariant = TumbaTextSecondary,
    surfaceTint = TumbaAccent,
    surfaceDim = TumbaBlack,
    surfaceBright = TumbaSurfaceElevated,
    surfaceContainerLowest = TumbaBlack,
    surfaceContainerLow = TumbaSurfaceLow,
    surfaceContainer = TumbaSurface,
    surfaceContainerHigh = TumbaSurfaceRaised,
    surfaceContainerHighest = TumbaSurfaceElevated,
    outline = TumbaOutline,
    outlineVariant = TumbaOutlineSoft,
    error = TumbaError,
    onError = Color(0xFF2A0F10),
    errorContainer = Color(0xFF2E1A1D),
    onErrorContainer = TumbaError,
    inverseSurface = TumbaText,
    inverseOnSurface = TumbaBlack,
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = PaperAccent,
    onPrimary = Color.White,
    primaryContainer = PaperSurfaceRaised,
    onPrimaryContainer = PaperText,
    secondary = PaperAccentMint,
    onSecondary = Color.White,
    secondaryContainer = PaperSurfaceRaised,
    onSecondaryContainer = PaperText,
    tertiary = PaperAccentDeep,
    onTertiary = Color.White,
    background = PaperBackground,
    onBackground = PaperText,
    surface = PaperSurface,
    onSurface = PaperText,
    surfaceVariant = PaperSurfaceRaised,
    onSurfaceVariant = PaperTextSecondary,
    surfaceTint = PaperAccent,
    surfaceDim = PaperSurfaceRaised,
    surfaceBright = PaperSurface,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurfaceLow,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceRaised,
    surfaceContainerHighest = PaperSurfaceElevated,
    outline = PaperOutline,
    outlineVariant = PaperOutlineSoft,
    error = PaperError,
    onError = Color.White,
    errorContainer = Color(0xFFF7E4E4),
    onErrorContainer = PaperError,
    inverseSurface = PaperText,
    inverseOnSurface = PaperBackground,
    scrim = PaperText,
)

/** Theme-aware brand gradient: brighter iris on dark, deeper violet on light paper. */
@Composable
fun tumbaBrandGradient(): Brush {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (light) {
        Brush.linearGradient(listOf(PaperAccentDeep, PaperAccent, PaperAccentMint))
    } else {
        TumbaBrandGradient
    }
}

/** Content colour that stays readable on top of [tumbaBrandGradient]. */
@Composable
fun onBrandGradient(): Color {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (light) Color.White else Color(0xFF100E18)
}

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
