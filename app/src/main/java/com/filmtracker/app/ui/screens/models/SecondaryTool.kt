package com.filmtracker.app.ui.screens.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

enum class SecondaryTool(val label: String, val icon: ImageVector) {
    AUTO("自动", Icons.Default.AutoFixHigh),
    BRIGHTNESS("亮度", Icons.Default.WbSunny),
    COLOR_TEMP("颜色", Icons.Default.Thermostat),
    SATURATION("饱和", Icons.Default.Palette),
    EFFECTS("效果", Icons.Default.AutoAwesome),
    DETAIL("细节", Icons.Default.Tune),
    CURVE("曲线", Icons.Default.ShowChart)
}
