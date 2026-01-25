package com.filmtracker.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// M3 Shape tokens
val FilmTrackerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// 原有深色主题（专业修图模式）- Complete M3 color roles
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    surfaceTint = DarkSurfaceTint
)

// 胶卷仿拍 Ins 风格主题 - Complete M3 color roles
private val FilmVintageColorScheme = lightColorScheme(
    primary = FilmCaramelOrange,
    onPrimary = FilmWhite,
    primaryContainer = VintagePrimaryContainer,
    onPrimaryContainer = VintageOnPrimaryContainer,
    secondary = FilmMintGreen,
    onSecondary = FilmWhite,
    secondaryContainer = VintageSecondaryContainer,
    onSecondaryContainer = VintageOnSecondaryContainer,
    tertiary = FilmMilkyBlue,
    onTertiary = FilmInkBlack,
    tertiaryContainer = VintageTertiaryContainer,
    onTertiaryContainer = VintageOnTertiaryContainer,
    error = VintageError,
    onError = VintageOnError,
    errorContainer = VintageErrorContainer,
    onErrorContainer = VintageOnErrorContainer,
    background = FilmWarmBeige,
    onBackground = FilmInkBlack,
    surface = FilmWhite,
    onSurface = FilmInkBlack,
    surfaceVariant = FilmMilkyBlue,
    onSurfaceVariant = FilmDarkGray,
    outline = FilmLightGray,
    outlineVariant = FilmSprocketGray,
    scrim = VintageScrim,
    inverseSurface = VintageInverseSurface,
    inverseOnSurface = VintageInverseOnSurface,
    inversePrimary = VintageInversePrimary,
    surfaceTint = VintageSurfaceTint
)

@Composable
fun FilmTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useVintageTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        // Dynamic color for Android 12+ (API 31+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useVintageTheme) {
                dynamicLightColorScheme(context)
            } else {
                dynamicDarkColorScheme(context)
            }
        }
        // Vintage theme (light)
        useVintageTheme -> FilmVintageColorScheme
        // Professional dark theme (default)
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useVintageTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FilmTrackerShapes,
        content = content
    )
}
