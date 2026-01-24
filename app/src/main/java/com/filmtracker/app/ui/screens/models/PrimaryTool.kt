package com.filmtracker.app.ui.screens.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class PrimaryTool(val label: String, val icon: ImageVector) {
    AI("AI协助", Icons.Default.Star),
    FILTER("创意滤镜", Icons.Default.Face),
    CROP("裁剪", Icons.Default.Create),
    COLOR("调色", Icons.Default.Settings),
    DEPTH("景深", Icons.Default.Edit),
    CUTOUT("抠图", Icons.Default.Build)
}
