package com.filmtracker.app.ui.screens.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.vector.ImageVector

enum class PrimaryTool(val label: String, val icon: ImageVector) {
    AI("AI协助", Icons.Default.AutoAwesome),
    FILTER("创意滤镜", Icons.Default.FilterVintage),
    CROP("裁剪", Icons.Default.Crop),
    COLOR("调色", Icons.Default.Palette),
    DEPTH("景深", Icons.Default.BlurOn),
    CUTOUT("抠图", Icons.Default.ContentCut)
}
