package com.filmtracker.app.ui.screens.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class SecondaryTool(val label: String, val icon: ImageVector) {
    AUTO("自动", Icons.Default.Star),
    BRIGHTNESS("亮度", Icons.Default.Star),
    COLOR_TEMP("颜色", Icons.Default.Star),
    SATURATION("饱和", Icons.Default.Star),
    EFFECTS("效果", Icons.Default.Star),
    DETAIL("细节", Icons.Default.Star),
    CURVE("曲线", Icons.Default.Star)
}
