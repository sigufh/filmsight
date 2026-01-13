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
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.ui.components.*
import com.filmtracker.app.ui.components.SuggestionItem
import com.filmtracker.app.ui.components.CurveEditor
import com.filmtracker.app.ui.components.CurveType
import com.filmtracker.app.ui.components.HSLAdjuster
import com.filmtracker.app.ui.components.HSLHueSegment

/**
 * 基础色调面板
 */
@Composable
fun BasicTonePanel(
    filmParams: FilmParams,
    onParamsChange: (FilmParams) -> Unit,
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
 * 曲线面板
 */
@Composable
fun CurvePanel(
    filmParams: FilmParams,
    onParamsChange: (FilmParams) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCurveType by remember { mutableStateOf(CurveType.RGB) }
    
    EditPanelContent(
        title = "色调曲线",
        onDismiss = onDismiss
    ) {
        // 曲线类型选择器
        CurveSelector(
            selectedCurve = selectedCurveType,
            onCurveSelected = { selectedCurveType = it },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 根据选择的曲线类型显示对应的曲线编辑器
        when (selectedCurveType) {
            CurveType.RGB -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RGB 总曲线")
                    Switch(
                        checked = filmParams.enableRgbCurve,
                        onCheckedChange = {
                            onParamsChange(filmParams.copy(enableRgbCurve = it))
                        }
                    )
                }
                if (filmParams.enableRgbCurve) {
                    CurveEditor(
                        curve = filmParams.rgbCurve,
                        onCurveChange = { newCurve ->
                            onParamsChange(filmParams.copy(rgbCurve = newCurve))
                        },
                        curveColor = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            CurveType.RED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("红色通道")
                    Switch(
                        checked = filmParams.enableRedCurve,
                        onCheckedChange = {
                            onParamsChange(filmParams.copy(enableRedCurve = it))
                        }
                    )
                }
                if (filmParams.enableRedCurve) {
                    CurveEditor(
                        curve = filmParams.redCurve,
                        onCurveChange = { newCurve ->
                            onParamsChange(filmParams.copy(redCurve = newCurve))
                        },
                        curveColor = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            CurveType.GREEN -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("绿色通道")
                    Switch(
                        checked = filmParams.enableGreenCurve,
                        onCheckedChange = {
                            onParamsChange(filmParams.copy(enableGreenCurve = it))
                        }
                    )
                }
                if (filmParams.enableGreenCurve) {
                    CurveEditor(
                        curve = filmParams.greenCurve,
                        onCurveChange = { newCurve ->
                            onParamsChange(filmParams.copy(greenCurve = newCurve))
                        },
                        curveColor = Color.Green,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            CurveType.BLUE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("蓝色通道")
                    Switch(
                        checked = filmParams.enableBlueCurve,
                        onCheckedChange = {
                            onParamsChange(filmParams.copy(enableBlueCurve = it))
                        }
                    )
                }
                if (filmParams.enableBlueCurve) {
                    CurveEditor(
                        curve = filmParams.blueCurve,
                        onCurveChange = { newCurve ->
                            onParamsChange(filmParams.copy(blueCurve = newCurve))
                        },
                        curveColor = Color.Blue,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * HSL 面板
 */
@Composable
fun HSLPanel(
    filmParams: FilmParams,
    onParamsChange: (FilmParams) -> Unit,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "HSL 调整",
        onDismiss = onDismiss
    ) {
        // 启用/禁用 HSL 调整
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用 HSL 调整")
            Switch(
                checked = filmParams.enableHSL,
                onCheckedChange = {
                    onParamsChange(filmParams.copy(enableHSL = it))
                }
            )
        }
        
        if (filmParams.enableHSL) {
            Spacer(modifier = Modifier.height(8.dp))
            
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
}

/**
 * 颗粒面板
 */
@Composable
fun GrainPanel(
    filmParams: FilmParams,
    onParamsChange: (FilmParams) -> Unit,
    onDismiss: () -> Unit
) {
    EditPanelContent(
        title = "颗粒",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用颗粒")
            Switch(
                checked = filmParams.grainEnabled,
                onCheckedChange = { 
                    onParamsChange(filmParams.copy(grainEnabled = it)) 
                }
            )
        }
        
        if (filmParams.grainEnabled) {
            ParameterSlider(
                label = "颗粒密度",
                value = filmParams.grainBaseDensity,
                onValueChange = { 
                    onParamsChange(filmParams.copy(grainBaseDensity = it)) 
                },
                valueRange = 0.0f..0.1f
            )
        }
    }
}

/**
 * AI 调色面板
 */
@Composable
fun AITonePanel(
    imageUri: String?,
    onApplySuggestion: (FilmParams) -> Unit,
    onDismiss: () -> Unit
) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf<FilmParams?>(null) }
    
    EditPanelContent(
        title = "AI 调色",
        onDismiss = onDismiss
    ) {
        if (imageUri == null) {
            Text(
                "请先选择图像",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else if (isAnalyzing) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text("分析中...")
            }
        } else if (suggestion != null) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "AI 建议参数：",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "曝光: ${suggestion!!.globalExposure}\n" +
                    "对比度: ${suggestion!!.contrast}\n" +
                    "饱和度: ${suggestion!!.saturation}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        onApplySuggestion(suggestion!!)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("应用建议")
                }
            }
        } else {
            Button(
                onClick = {
                    isAnalyzing = true
                    // TODO: 调用 AI 分析
                    suggestion = FilmParams.portra400()
                    isAnalyzing = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("分析图像并生成调色建议")
            }
        }
    }
}

/**
 * AI 美颜面板
 */
@Composable
fun AIBeautyPanel(
    imageUri: String?,
    currentParams: FilmParams,
    beautyAnalyzer: com.filmtracker.app.ai.BeautyAIAnalyzer,
    onApplyBeauty: (com.filmtracker.app.ai.BeautySuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var beautySuggestion by remember { mutableStateOf<com.filmtracker.app.ai.BeautySuggestion?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    EditPanelContent(
        title = "AI 美颜",
        onDismiss = onDismiss
    ) {
        if (imageUri == null) {
            Text(
                "请先选择图像",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else if (isAnalyzing) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text("分析中...")
            }
        } else if (beautySuggestion != null) {
            val suggestion = beautySuggestion!!
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "检测到 ${suggestion.faceRegions.size} 张人脸",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                SuggestionItem("皮肤平滑", "${(suggestion.params.skinSmoothing * 100).toInt()}%")
                SuggestionItem("肤色修正", if (suggestion.params.skinToneWarmth > 0) "暖色" else "冷色")
                SuggestionItem("眼部增强", "亮度 +${(suggestion.params.eyeBrightness * 100).toInt()}%")
                SuggestionItem("嘴唇增强", "饱和度 +${(suggestion.params.lipSaturation * 100).toInt()}%")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        onApplyBeauty(suggestion)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("一键应用")
                }
            }
        } else {
            Button(
                onClick = {
                    isAnalyzing = true
                    coroutineScope.launch {
                        // TODO: 加载实际图像并分析
                        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
                        beautySuggestion = beautyAnalyzer.analyzeBeauty(bitmap, 400.0f)
                        isAnalyzing = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("分析照片")
            }
        }
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
