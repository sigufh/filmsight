package com.filmtracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.components.*
import com.filmtracker.app.ui.components.SuggestionItem
import com.filmtracker.app.ui.components.CurveEditor
import com.filmtracker.app.ui.components.CurveType
import com.filmtracker.app.ui.components.MultiCurveEditor
import com.filmtracker.app.ui.components.HSLAdjuster
import com.filmtracker.app.ui.components.HSLHueSegment

/**
 * 基础色调面板
 */
@Composable
fun BasicTonePanel(
    filmParams: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "基础调整",
        onDismiss = onDismiss
    ) {
        ParameterSlider(
            label = "曝光",
            value = filmParams.globalExposure,
            onValueChange = { onParamsChange(filmParams.copy(globalExposure = it)) },
            valueRange = -3f..3f,
            steps = 60
        )
        
        ParameterSlider(
            label = "对比度",
            value = filmParams.contrast,
            onValueChange = { onParamsChange(filmParams.copy(contrast = it)) },
            valueRange = 0.5f..2.0f
        )
        
        ParameterSlider(
            label = "饱和度",
            value = filmParams.saturation,
            onValueChange = { onParamsChange(filmParams.copy(saturation = it)) },
            valueRange = 0.0f..2.0f
        )

        ParameterSlider(
            label = "高光",
            value = filmParams.highlights,
            onValueChange = { onParamsChange(filmParams.copy(highlights = it)) },
            valueRange = -1.0f..1.0f
        )

        ParameterSlider(
            label = "阴影",
            value = filmParams.shadows,
            onValueChange = { onParamsChange(filmParams.copy(shadows = it)) },
            valueRange = -1.0f..1.0f
        )

        ParameterSlider(
            label = "白场",
            value = filmParams.whites,
            onValueChange = { onParamsChange(filmParams.copy(whites = it)) },
            valueRange = -1.0f..1.0f
        )

        ParameterSlider(
            label = "黑场",
            value = filmParams.blacks,
            onValueChange = { onParamsChange(filmParams.copy(blacks = it)) },
            valueRange = -1.0f..1.0f
        )

        ParameterSlider(
            label = "清晰度",
            value = filmParams.clarity,
            onValueChange = { onParamsChange(filmParams.copy(clarity = it)) },
            valueRange = -1.0f..1.0f
        )

        ParameterSlider(
            label = "自然饱和度",
            value = filmParams.vibrance,
            onValueChange = { onParamsChange(filmParams.copy(vibrance = it)) },
            valueRange = -1.0f..1.0f
        )
    }
}

/**
 * 曲线面板（重新设计：一张图显示所有曲线，按钮切换编辑通道）
 */
@Composable
fun CurvePanel(
    filmParams: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCurveType by remember { mutableStateOf(CurveType.RGB) }
    
    EditPanelContent(
        title = "色调曲线",
        onDismiss = onDismiss
    ) {
        // 曲线类型选择器（按钮切换）
        CurveSelector(
            selectedCurve = selectedCurveType,
            onCurveSelected = { selectedCurveType = it },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 多曲线编辑器：在一张图上显示所有曲线，通过按钮选择编辑
        MultiCurveEditor(
            rgbCurve = filmParams.rgbCurve,
            redCurve = filmParams.redCurve,
            greenCurve = filmParams.greenCurve,
            blueCurve = filmParams.blueCurve,
            selectedCurve = selectedCurveType,
            onRgbCurveChange = { onParamsChange(filmParams.copy(rgbCurve = it)) },
            onRedCurveChange = { onParamsChange(filmParams.copy(redCurve = it)) },
            onGreenCurveChange = { onParamsChange(filmParams.copy(greenCurve = it)) },
            onBlueCurveChange = { onParamsChange(filmParams.copy(blueCurve = it)) },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * HSL 面板
 */
@Composable
fun HSLPanel(
    filmParams: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "HSL 调整",
        onDismiss = onDismiss
    ) {
        // 默认开启，直接显示调整器
        HSLAdjuster(
            hueShift = filmParams.hslHueShift,
            saturation = filmParams.hslSaturation,
            luminance = filmParams.hslLuminance,
            onHueShiftChange = { index, value ->
                val newHueShift = filmParams.hslHueShift.copyOf()
                newHueShift[index] = value
                onParamsChange(filmParams.copy(hslHueShift = newHueShift))
            },
            onSaturationChange = { index, value ->
                val newSaturation = filmParams.hslSaturation.copyOf()
                newSaturation[index] = value
                onParamsChange(filmParams.copy(hslSaturation = newSaturation))
            },
            onLuminanceChange = { index, value ->
                val newLuminance = filmParams.hslLuminance.copyOf()
                newLuminance[index] = value
                onParamsChange(filmParams.copy(hslLuminance = newLuminance))
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * 颗粒面板（已移除，保留空实现以兼容）
 */
@Composable
fun GrainPanel(
    filmParams: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "颗粒",
        onDismiss = onDismiss
    ) {
        Text(
            "颗粒功能已移除",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * AI 调色面板（已移除，保留空实现以兼容）
 */
@Composable
fun AITonePanel(
    imageUri: String?,
    onApplySuggestion: (BasicAdjustmentParams) -> Unit,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "AI 调色",
        onDismiss = onDismiss
    ) {
        Text(
            "AI 调色功能已移除",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * AI 美颜面板（已移除，保留空实现以兼容）
 */
@Composable
fun AIBeautyPanel(
    imageUri: String?,
    currentParams: BasicAdjustmentParams,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "AI 美颜",
        onDismiss = onDismiss
    ) {
        Text(
            "AI 美颜功能已移除",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * 编辑面板内容容器
 */
@Composable
fun EditPanelContent(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "关闭")
            }
        }
        
        Divider()
        
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}
