package com.filmtracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.ui.components.*
import com.filmtracker.app.ai.BeautyAIAnalyzer
import com.filmtracker.app.util.BeautyParamsConverter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 主处理界面
 * 包含图像预览和参数控制面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    imageUri: String?,
    onExport: (FilmParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var filmParams by remember { mutableStateOf(FilmParams.portra400()) }
    var showAISuggestions by remember { mutableStateOf(false) }
    var showBeautyPanel by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val beautyAnalyzer = remember { BeautyAIAnalyzer() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FilmTracker") },
                actions = {
                    IconButton(onClick = { showAISuggestions = !showAISuggestions }) {
                        Text("AI")
                    }
                    IconButton(onClick = { showBeautyPanel = !showBeautyPanel }) {
                        Text("美颜")
                    }
                    IconButton(onClick = { onExport(filmParams) }) {
                        Text("导出")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 图像预览区域
            ImagePreviewSection(
                imageUri = imageUri,
                filmParams = filmParams,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // 美颜面板（如果启用）
            if (showBeautyPanel && imageUri != null) {
                BeautyPanel(
                    imageUri = imageUri,
                    onApplyBeauty = { suggestion ->
                        filmParams = BeautyParamsConverter.applyBeautySuggestion(filmParams, suggestion)
                        showBeautyPanel = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 参数控制面板
            ParameterControlPanel(
                filmParams = filmParams,
                onParamsChange = { filmParams = it },
                showAISuggestions = showAISuggestions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 图像预览区域
 */
@Composable
fun ImagePreviewSection(
    imageUri: String?,
    filmParams: FilmParams,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            // 使用 Coil 加载和显示图像
            // 实际应使用处理后的图像
            Text(
                "图像预览区域\n（实际应显示处理后的图像）",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                "请选择 RAW 图像",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 参数控制面板
 */
@Composable
fun ParameterControlPanel(
    filmParams: FilmParams,
    onParamsChange: (FilmParams) -> Unit,
    showAISuggestions: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 全局调整
        ParameterSection(title = "全局调整") {
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
        
        // 响应曲线
        ParameterSection(title = "响应曲线") {
            ResponseCurveControls(
                filmParams = filmParams,
                onParamsChange = onParamsChange
            )
        }
        
        // 颗粒控制
        ParameterSection(title = "颗粒") {
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
        
        // AI 建议（如果启用）
        if (showAISuggestions) {
            AISuggestionCard(
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
