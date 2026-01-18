package com.filmtracker.app.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 原有深色主题（专业修图模式）
private val DarkColorScheme = darkColorScheme(
    primary = FilmTrackerPrimary,
    secondary = FilmTrackerSecondary,
    surface = FilmTrackerSurface,
    background = FilmTrackerDark,
    onPrimary = FilmTrackerDark,
    onSecondary = FilmTrackerOnDark,
    onSurface = FilmTrackerOnSurface,
    onBackground = FilmTrackerOnDark
)

// 胶卷仿拍 Ins 风格主题（新增）
private val FilmVintageColorScheme = lightColorScheme(
    primary = FilmCaramelOrange,           // 焦糖橘（主按钮、选中状态）
    secondary = FilmMintGreen,             // 薄荷绿（辅助按钮、强调）
    tertiary = FilmMilkyBlue,              // 奶灰蓝（功能区背景）
    background = FilmWarmBeige,            // 暖调米白（主背景）
    surface = FilmWhite,                   // 纯白（卡片、面板）
    surfaceVariant = FilmMilkyBlue,        // 奶灰蓝（次要面板）
    onPrimary = FilmWhite,                 // 主色上的文字（白色）
    onSecondary = FilmWhite,               // 辅助色上的文字（白色）
    onTertiary = FilmInkBlack,             // 第三色上的文字（墨黑）
    onBackground = FilmInkBlack,           // 背景上的文字（墨黑）
    onSurface = FilmInkBlack,              // 表面上的文字（墨黑）
    onSurfaceVariant = FilmDarkGray,       // 次要表面上的文字（深灰）
    outline = FilmLightGray,               // 边框、分隔线（浅灰）
    outlineVariant = FilmSprocketGray      // 次要边框（胶片齿孔色）
)

@Composable
fun FilmTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useVintageTheme: Boolean = false,  // 新增：是否使用胶卷仿拍主题
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useVintageTheme -> FilmVintageColorScheme  // 胶卷仿拍 Ins 风格
        else -> DarkColorScheme                     // 专业修图深色主题
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
        content = content
    )
}
