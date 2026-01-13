package com.filmtracker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.ui.components.*
import com.filmtracker.app.ai.BeautyAIAnalyzer
import com.filmtracker.app.util.BeautyParamsConverter
import com.filmtracker.app.util.ImageProcessor
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.graphics.Bitmap

/**
 * 编辑模式枚举
 */
enum class EditMode {
    BASIC,      // 基础调整
    CURVE,      // 曲线
    HSL,        // HSL
    GRAIN,      // 颗粒
    AI_TONE,    // AI 调色
    AI_BEAUTY   // AI 美颜
}

/**
 * 主处理界面
 * 重新设计：底部模式切换 + 上拉调色器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    imageUri: String?,
    onSelectImage: () -> Unit = {},
    onExport: (FilmParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var filmParams by remember { mutableStateOf(FilmParams.portra400().apply {
        // 默认开启所有选项
        enableRgbCurve = true
        enableRedCurve = true
        enableGreenCurve = true
        enableBlueCurve = true
        enableHSL = true
    }) }
    var selectedMode by remember { mutableStateOf<EditMode?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageProcessor = remember { ImageProcessor(context) }
    val beautyAnalyzer = remember { BeautyAIAnalyzer() }
    
    // 上拉面板状态
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    // 当参数变化时，重新处理图像（实时预览）
    LaunchedEffect(filmParams, imageUri) {
        if (imageUri != null && !isProcessing) {
            isProcessing = true
            coroutineScope.launch {
                val result = imageProcessor.processImage(imageUri, filmParams)
                processedBitmap = result
                isProcessing = false
            }
        }
    }
    
    // LR移动版风格：深色背景，底部工具栏
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // LR移动版深色背景
    ) {
        // 图像预览区域（全屏，不包含padding）
        ImagePreviewSection(
            imageUri = imageUri,
            processedBitmap = processedBitmap,
            onSelectImage = onSelectImage,
            modifier = Modifier.fillMaxSize()
        )
        
        // 顶部栏（LR移动版风格：半透明，深色）
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = Color(0xFF1A1A1A).copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FilmTracker",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                IconButton(
                    onClick = { 
                        coroutineScope.launch {
                            if (processedBitmap != null) {
                                onExport(filmParams)
                            }
                        }
                    },
                    enabled = processedBitmap != null
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        "导出",
                        tint = if (processedBitmap != null) Color.White else Color.Gray
                    )
                }
            }
        }
        
        // 底部模式切换栏（LR移动版风格：半透明，深色）
        BottomModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { mode ->
                selectedMode = if (selectedMode == mode) null else mode
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
        
        // 处理中指示器
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
    
    // 上拉调色器面板（LR移动版风格：不遮挡图片，使用侧边栏或底部抽屉）
    if (selectedMode != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMode = null },
            sheetState = bottomSheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color(0xFF2A2A2A), // LR移动版深色背景
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .padding(vertical = 12.dp)
                )
            }
        ) {
            when (selectedMode) {
                EditMode.BASIC -> BasicTonePanel(
                    filmParams = filmParams,
                    onParamsChange = { filmParams = it },
                    onDismiss = { selectedMode = null }
                )
                EditMode.CURVE -> CurvePanel(
                    filmParams = filmParams,
                    onParamsChange = { filmParams = it },
                    onDismiss = { selectedMode = null }
                )
                EditMode.HSL -> HSLPanel(
                    filmParams = filmParams,
                    onParamsChange = { filmParams = it },
                    onDismiss = { selectedMode = null }
                )
                EditMode.GRAIN -> GrainPanel(
                    filmParams = filmParams,
                    onParamsChange = { filmParams = it },
                    onDismiss = { selectedMode = null }
                )
                EditMode.AI_TONE -> AITonePanel(
                    imageUri = imageUri,
                    onApplySuggestion = { suggestion ->
                        // 应用AI调色建议
                        filmParams = suggestion
                        selectedMode = null
                    },
                    onDismiss = { selectedMode = null }
                )
                EditMode.AI_BEAUTY -> AIBeautyPanel(
                    imageUri = imageUri,
                    currentParams = filmParams,
                    beautyAnalyzer = beautyAnalyzer,
                    onApplyBeauty = { suggestion ->
                        filmParams = BeautyParamsConverter.applyBeautySuggestion(filmParams, suggestion)
                        selectedMode = null
                    },
                    onDismiss = { selectedMode = null }
                )
                else -> {}
            }
        }
    }
}

/**
 * 图像预览区域（支持缩放和平移，LR移动版风格）
 */
@Composable
fun ImagePreviewSection(
    imageUri: String?,
    processedBitmap: Bitmap?,
    onSelectImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var lastScale by remember { mutableStateOf(1f) }
    var lastOffset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (lastScale * zoom).coerceIn(1f, 5f)
                    offset = lastOffset + pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                            lastScale = 1f
                            lastOffset = Offset.Zero
                        } else {
                            scale = 2f
                            lastScale = 2f
                        }
                    }
                )
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            processedBitmap != null -> {
                Image(
                    bitmap = processedBitmap.asImageBitmap(),
                    contentDescription = "处理后的图像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            imageUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "原始图像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .clickable { onSelectImage() }
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "点击选择图像",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 底部模式选择器（可左右滑动）
 */
@Composable
fun BottomModeSelector(
    selectedMode: EditMode?,
    onModeSelected: (EditMode) -> Unit,
    modifier: Modifier = Modifier
) {
    data class ModeItem(val mode: EditMode, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    
    val modes = listOf(
        ModeItem(EditMode.BASIC, "基础", Icons.Default.Settings),
        ModeItem(EditMode.CURVE, "曲线", Icons.Default.TrendingUp),
        ModeItem(EditMode.HSL, "HSL", Icons.Default.ColorLens),
        ModeItem(EditMode.GRAIN, "颗粒", Icons.Default.Grain),
        ModeItem(EditMode.AI_TONE, "AI调色", Icons.Default.AutoFixHigh),
        ModeItem(EditMode.AI_BEAUTY, "AI美颜", Icons.Default.Face)
    )
    
    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A1A).copy(alpha = 0.95f), // LR移动版深色
        shadowElevation = 8.dp
    ) {
        // 模式切换栏（可横向滑动）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { modeItem ->
                val isSelected = selectedMode == modeItem.mode
                
                Column(
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { onModeSelected(modeItem.mode) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modeItem.icon,
                        contentDescription = modeItem.label,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) 
                            Color(0xFF007AFF) // LR移动版蓝色
                        else 
                            Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        modeItem.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) 
                            Color(0xFF007AFF)
                        else 
                            Color.White.copy(alpha = 0.6f)
                    )
                    
                    // 选中指示器（LR移动版风格）
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF))
                        )
                    }
                }
            }
        }
    }
}
