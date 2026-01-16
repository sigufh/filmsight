package com.filmtracker.app.ui.screens.models

import androidx.compose.ui.graphics.Color

enum class CurveChannel(val label: String, val color: Color) {
    RGB("RGB", Color.White),
    RED("红", Color.Red),
    GREEN("绿", Color.Green),
    BLUE("蓝", Color.Blue)
}
