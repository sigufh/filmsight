package com.filmtracker.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.ui.viewmodel.ProcessingViewModel
import com.filmtracker.app.ui.viewmodel.ViewModelFactory
import com.filmtracker.app.util.ImageProcessor
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * 图像编辑屏幕（类似 Lightroom Mobile）
 * 
 * 布局结构：
 * - 顶部栏：返回按钮、撤回、帮助、导出、更多
 * - 图片预览区：点击调色时自动向上退避
 * - 二级菜单和内容区：显示当前一级工具的子功能
 * - 一级菜单：AI协助、创意滤镜、裁剪旋转、调色、蒙版、修补消除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    imageUri: String?,
    onSelectImage: () -> Unit = {},
    onExport: (BasicAdjustmentParams) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 获取 ViewModel
    val viewModel: ProcessingViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )
    
    // 观察 ViewModel 状态
    val processedImage by viewModel.processedImage.collectAsState()
    val domainParams by viewModel.adjustmentParams.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    
    // 将 Domain 参数映射为 UI 参数（BasicAdjustmentParams）
    val mapper = remember { AdjustmentParamsMapper() }
    val basicParams = remember(domainParams) {
        mapper.toData(domainParams)
    }
    
    // UI 状态
    var selectedPrimaryTool by remember { mutableStateOf<PrimaryTool?>(null) }
    var selectedSecondaryTool by remember { mutableStateOf<SecondaryTool?>(null) }
    var isInitialLoading by remember { mutableStateOf(true) }
    
    // 图像缩放状态
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // 加载原始图像（使用旧的 ImageProcessor 临时方案）
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            isInitialLoading = true
            val imageProcessor = ImageProcessor(context)
            // 加载适合预览的分辨率（不是缩略图，而是适中的预览图）
            val loadedImage = imageProcessor.loadOriginalImage(imageUri, previewMode = true)
            if (loadedImage != null) {
                viewModel.setOriginalImage(loadedImage)
            }
            isInitialLoading = false
        }
    }
    
    // 面板高度
    val panelHeight = 350.dp
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onSelectImage) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // 撤回
                    IconButton(onClick = { /* TODO: 实现撤回功能 */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "撤回",
                            tint = Color.White
                        )
                    }
                    // 帮助
                    IconButton(onClick = { /* TODO: 实现帮助功能 */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "帮助",
                            tint = Color.White
                        )
                    }
                    // 导出
                    IconButton(
                        onClick = {
                            // 使用协程导出图像
                            coroutineScope.launch {
                                val result = viewModel.exportImageSuspend()
                                result.onSuccess { exportedBitmap ->
                                    // 导出成功，调用回调（可以在这里保存到文件）
                                    onExport(basicParams)
                                }
                            }
                        },
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "导出",
                                tint = Color.White
                            )
                        }
                    }
                    // 更多
                    IconButton(onClick = { /* TODO: 实现更多功能 */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 图像预览区（始终显示，但高度会根据面板状态调整）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(
                        bottom = if (selectedPrimaryTool != null) {
                            panelHeight + 70.dp // 面板高度 + 一级菜单高度
                        } else {
                            70.dp // 只有一级菜单高度
                        }
                    )
            ) {
                // 只在初始加载时显示加载动画
                if (isInitialLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                } else if (processedImage != null) {
                    // 图片始终显示，即使在调整参数时
                    // 支持双指缩放和双击复位
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(processedImage)
                            .build(),
                        contentDescription = "处理后的图像",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(Unit) {
                                // 双指缩放手势
                                detectTransformGestures { _, pan, zoom, _ ->
                                    // 更新缩放比例（限制在 1x 到 5x 之间）
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    
                                    // 如果缩放比例为 1，重置偏移
                                    if (scale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        // 更新偏移量（自由平移，无边界限制）
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                // 双击复位手势
                                detectTapGestures(
                                    onDoubleTap = {
                                        // 复位缩放和偏移
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }
                    )
                }
            }
            
            // 二级菜单和内容区域（固定在底部，一级菜单上方）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // 二级菜单和内容区（当选中一级工具时显示）
                if (selectedPrimaryTool != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(panelHeight)
                            .background(Color(0xFF1C1C1E))
                    ) {
                        when (selectedPrimaryTool) {
                            PrimaryTool.AI -> {
                                AIAssistPanel()
                            }
                            PrimaryTool.FILTER -> {
                                CreativeFilterPanel()
                            }
                            PrimaryTool.CROP -> {
                                CropRotatePanel()
                            }
                            PrimaryTool.COLOR -> {
                                ColorAdjustmentPanel(
                                    params = basicParams,
                                    onParamsChange = { newParams ->
                                        // 将 BasicAdjustmentParams 映射回 Domain 参数并更新 ViewModel
                                        val newDomainParams = mapper.toDomain(newParams)
                                        viewModel.updateParams(newDomainParams)
                                    },
                                    selectedSecondaryTool = selectedSecondaryTool,
                                    onSecondaryToolSelected = { selectedSecondaryTool = it }
                                )
                            }
                            PrimaryTool.MASK -> {
                                MaskPanel()
                            }
                            PrimaryTool.HEAL -> {
                                HealPanel()
                            }
                            null -> {
                                // 不应该到达这里，因为外层已经检查了 null
                            }
                        }
                    }
                }
                
                // 一级菜单（始终显示在最底部）
                PrimaryToolBar(
                    selectedTool = selectedPrimaryTool,
                    onToolSelected = { tool ->
                        selectedPrimaryTool = if (selectedPrimaryTool == tool) null else tool
                        // 切换到调色工具时，默认选择亮度子工具
                        if (tool == PrimaryTool.COLOR) {
                            selectedSecondaryTool = SecondaryTool.BRIGHTNESS
                        }
                    }
                )
            }
        }
    }
}

/**
 * 一级工具栏
 */
@Composable
fun PrimaryToolBar(
    selectedTool: PrimaryTool?,
    onToolSelected: (PrimaryTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PrimaryTool.entries.forEach { tool ->
            PrimaryToolButton(
                tool = tool,
                isSelected = selectedTool == tool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
fun PrimaryToolButton(
    tool: PrimaryTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .padding(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = if (isSelected) Color(0xFF0A84FF) else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0A84FF) else Color.White
        )
    }
}


/**
 * AI 协助面板
 */
@Composable
fun AIAssistPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI 协助功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 创意滤镜面板
 */
@Composable
fun CreativeFilterPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "创意滤镜功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 裁剪旋转面板
 */
@Composable
fun CropRotatePanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "裁剪旋转功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 蒙版面板
 */
@Composable
fun MaskPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "蒙版功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 修补消除面板
 */
@Composable
fun HealPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "修补消除功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


/**
 * 调色面板（包含二级菜单和内容）
 */
@Composable
fun ColorAdjustmentPanel(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    selectedSecondaryTool: SecondaryTool?,
    onSecondaryToolSelected: (SecondaryTool) -> Unit
) {
    var showCurveEditor by remember { mutableStateOf(false) }
    var selectedCurveChannel by remember { mutableStateOf(CurveChannel.RGB) }
    
    if (showCurveEditor) {
        // 曲线编辑器全屏模式
        CurveEditorFullScreen(
            selectedChannel = selectedCurveChannel,
            onChannelSelected = { selectedCurveChannel = it },
            onDismiss = { showCurveEditor = false },
            params = params,
            onParamsChange = onParamsChange
        )
    } else {
        // 正常的调色面板
        Column(modifier = Modifier.fillMaxSize()) {
            // 二级工具栏
            SecondaryToolBar(
                tools = listOf(
                    SecondaryTool.AUTO,
                    SecondaryTool.BRIGHTNESS,
                    SecondaryTool.COLOR_TEMP,
                    SecondaryTool.EFFECTS,
                    SecondaryTool.DETAIL
                ),
                selectedTool = selectedSecondaryTool,
                onToolSelected = onSecondaryToolSelected
            )
            
            // 二级工具内容区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedSecondaryTool) {
                    SecondaryTool.AUTO -> {
                        AutoAdjustContent()
                    }
                    SecondaryTool.BRIGHTNESS -> {
                        BrightnessAdjustContent(
                            params = params,
                            onParamsChange = onParamsChange,
                            onOpenCurveEditor = { showCurveEditor = true }
                        )
                    }
                    SecondaryTool.COLOR_TEMP -> {
                        ColorAdjustContent(params, onParamsChange)
                    }
                    SecondaryTool.EFFECTS -> {
                        EffectsAdjustContent(params, onParamsChange)
                    }
                    SecondaryTool.DETAIL -> {
                        DetailAdjustContent(params, onParamsChange)
                    }
                    SecondaryTool.SATURATION -> {
                        // 已移除，不应该到达这里
                    }
                    SecondaryTool.CURVE -> {
                        // 已移除，不应该到达这里
                    }
                    null -> {
                        // 默认显示
                    }
                }
            }
        }
    }
}

/**
 * 二级工具栏
 */
@Composable
fun SecondaryToolBar(
    tools: List<SecondaryTool>,
    selectedTool: SecondaryTool?,
    onToolSelected: (SecondaryTool) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2E))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(tools) { tool ->
            SecondaryToolButton(
                tool = tool,
                isSelected = selectedTool == tool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
fun SecondaryToolButton(
    tool: SecondaryTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = if (isSelected) Color(0xFF0A84FF) else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0A84FF) else Color.White
        )
    }
}


/**
 * 自动调整内容
 */
@Composable
fun AutoAdjustContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { /* TODO: 实现自动调整 */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A84FF)
            )
        ) {
            Text("自动调整", color = Color.White)
        }
    }
}

/**
 * 亮度调整内容
 */
@Composable
fun BrightnessAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onOpenCurveEditor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 曝光度（-100 到 +100，映射到 -5 到 +5）
        AdjustmentSlider(
            label = "曝光度",
            value = params.globalExposure * 20f, // 转换为 -100~100 显示
            onValueChange = { onParamsChange(params.copy(globalExposure = it / 20f)) }, // 转换回 -5~5
            valueRange = -100f..100f
        )
        
        // 对比度（-100 到 +100）
        AdjustmentSlider(
            label = "对比度",
            value = (params.contrast - 1f) * 100f,
            onValueChange = { onParamsChange(params.copy(contrast = 1f + it / 100f)) },
            valueRange = -100f..100f
        )
        
        // 高光（-100 到 +100）
        AdjustmentSlider(
            label = "高光",
            value = params.highlights,
            onValueChange = { onParamsChange(params.copy(highlights = it)) },
            valueRange = -100f..100f
        )
        
        // 阴影（-100 到 +100）
        AdjustmentSlider(
            label = "阴影",
            value = params.shadows,
            onValueChange = { onParamsChange(params.copy(shadows = it)) },
            valueRange = -100f..100f
        )
        
        // 白场（-100 到 +100）
        AdjustmentSlider(
            label = "白场",
            value = params.whites,
            onValueChange = { onParamsChange(params.copy(whites = it)) },
            valueRange = -100f..100f
        )
        
        // 黑场（-100 到 +100）
        AdjustmentSlider(
            label = "黑场",
            value = params.blacks,
            onValueChange = { onParamsChange(params.copy(blacks = it)) },
            valueRange = -100f..100f
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 曲线按钮
        Button(
            onClick = onOpenCurveEditor,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2E)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "曲线",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("曲线", color = Color.White)
        }
    }
}

/**
 * 色温调整内容（改名为颜色调整）
 */
@Composable
fun ColorAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    // 子界面状态：null=主界面, "grading"=分级, "mixer"=混合
    var subScreen by remember { mutableStateOf<String?>(null) }
    
    when (subScreen) {
        "grading" -> {
            // 分级界面
            ColorGradingScreen(
                params = params,
                onParamsChange = onParamsChange,
                onBack = { subScreen = null }
            )
        }
        "mixer" -> {
            // 混合界面
            ColorMixerScreen(
                params = params,
                onParamsChange = onParamsChange,
                onBack = { subScreen = null }
            )
        }
        else -> {
            // 主界面
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 色温（-100 到 +100）
                AdjustmentSlider(
                    label = "色温",
                    value = params.temperature,
                    onValueChange = { onParamsChange(params.copy(temperature = it)) },
                    valueRange = -100f..100f
                )
                
                // 色调（-100 到 +100）
                AdjustmentSlider(
                    label = "色调",
                    value = params.tint,
                    onValueChange = { onParamsChange(params.copy(tint = it)) },
                    valueRange = -100f..100f
                )
                
                // 饱和度（-100 到 +100）
                AdjustmentSlider(
                    label = "饱和度",
                    value = (params.saturation - 1f) * 100f,
                    onValueChange = { onParamsChange(params.copy(saturation = 1f + it / 100f)) },
                    valueRange = -100f..100f
                )
                
                // 自然饱和度（-100 到 +100）
                AdjustmentSlider(
                    label = "自然饱和度",
                    value = params.vibrance,
                    onValueChange = { onParamsChange(params.copy(vibrance = it)) },
                    valueRange = -100f..100f
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 分级按钮
                Button(
                    onClick = { subScreen = "grading" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C2C2E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "分级",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分级", color = Color.White)
                }
                
                // 混合按钮
                Button(
                    onClick = { subScreen = "mixer" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C2C2E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "混合",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("混合", color = Color.White)
                }
            }
        }
    }
}

/**
 * 饱和度调整内容（已移除，合并到颜色调整中）
 */

/**
 * 效果调整内容
 */
@Composable
fun EffectsAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 纹理（-100 到 +100）
        AdjustmentSlider(
            label = "纹理",
            value = params.texture,
            onValueChange = { onParamsChange(params.copy(texture = it)) },
            valueRange = -100f..100f
        )
        
        // 去雾（-100 到 +100）
        AdjustmentSlider(
            label = "去雾",
            value = params.dehaze,
            onValueChange = { onParamsChange(params.copy(dehaze = it)) },
            valueRange = -100f..100f
        )
        
        // 晕影（-100 到 +100）
        AdjustmentSlider(
            label = "晕影",
            value = params.vignette,
            onValueChange = { onParamsChange(params.copy(vignette = it)) },
            valueRange = -100f..100f
        )
        
        // 颗粒（0 到 100）
        AdjustmentSlider(
            label = "颗粒",
            value = params.grain,
            onValueChange = { onParamsChange(params.copy(grain = it)) },
            valueRange = 0f..100f
        )
    }
}

/**
 * 细节调整内容
 */
@Composable
fun DetailAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 清晰度（-100 到 +100）
        AdjustmentSlider(
            label = "清晰度",
            value = params.clarity,
            onValueChange = { onParamsChange(params.copy(clarity = it)) },
            valueRange = -100f..100f
        )
        
        // 锐化（0 到 100）
        AdjustmentSlider(
            label = "锐化",
            value = params.sharpening,
            onValueChange = { onParamsChange(params.copy(sharpening = it)) },
            valueRange = 0f..100f
        )
        
        // 降噪（0 到 100）
        AdjustmentSlider(
            label = "降噪",
            value = params.noiseReduction,
            onValueChange = { onParamsChange(params.copy(noiseReduction = it)) },
            valueRange = 0f..100f
        )
    }
}

/**
 * 曲线调整内容（已移除，改为全屏曲线编辑器）
 */

/**
 * 曲线编辑器全屏界面
 */
@Composable
fun CurveEditorFullScreen(
    selectedChannel: CurveChannel,
    onChannelSelected: (CurveChannel) -> Unit,
    onDismiss: () -> Unit,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)  // 完全透明，显示下方图片
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 曲线图区域（占据大部分空间，移除内边距）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // 移除 padding，让曲线全屏
            ) {
                InteractiveCurveEditor(
                    channel = selectedChannel,
                    params = params,
                    onParamsChange = onParamsChange
                )
            }
            
            // 底部控制栏（半透明）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))  // 半透明背景
                    .padding(16.dp)
            ) {
                // 通道选择和完成按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：曲线标签
                    Text(
                        text = "曲线",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // 右侧：完成按钮
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) {
                        Text("完成", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 通道选择按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CurveChannel.entries.forEach { channel ->
                        CurveChannelButton(
                            channel = channel,
                            isSelected = selectedChannel == channel,
                            onClick = { onChannelSelected(channel) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 可交互的曲线编辑器（Adobe 风格）
 * 支持点击添加控制点，拖动调整，使用平滑样条曲线
 */
@Composable
fun InteractiveCurveEditor(
    channel: CurveChannel,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    // 获取当前通道的控制点列表
    val currentCurvePoints = when (channel) {
        CurveChannel.RGB -> params.rgbCurvePoints
        CurveChannel.RED -> params.redCurvePoints
        CurveChannel.GREEN -> params.greenCurvePoints
        CurveChannel.BLUE -> params.blueCurvePoints
    }
    
    // 转换为 Offset 列表用于绘制
    val initialPoints = remember(channel, currentCurvePoints) {
        currentCurvePoints.map { Offset(it.first, it.second) }
    }
    
    // 控制点列表
    var controlPoints by remember(channel) { mutableStateOf(initialPoints) }
    
    // 当前拖动的点索引
    var draggingPointIndex by remember { mutableStateOf<Int?>(null) }
    
    // 更新参数（直接使用控制点列表）
    fun updateCurvePoints() {
        val newPoints = controlPoints.map { Pair(it.x, it.y) }
        
        val newParams = when (channel) {
            CurveChannel.RGB -> params.copy(
                enableRgbCurve = true,
                rgbCurvePoints = newPoints
            )
            CurveChannel.RED -> params.copy(
                enableRedCurve = true,
                redCurvePoints = newPoints
            )
            CurveChannel.GREEN -> params.copy(
                enableGreenCurve = true,
                greenCurvePoints = newPoints
            )
            CurveChannel.BLUE -> params.copy(
                enableBlueCurve = true,
                blueCurvePoints = newPoints
            )
        }
        onParamsChange(newParams)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)  // 改为完全透明
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)  // 从 16dp 减少到 8dp，增大画布
                .pointerInput(channel, controlPoints) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        
                        val normalizedX = (offset.x / canvasWidth).coerceIn(0f, 1f)
                        val normalizedY = (1f - offset.y / canvasHeight).coerceIn(0f, 1f)
                        
                        // 检查是否点击了现有控制点 - 增大点击范围
                        val clickedPointIndex = controlPoints.indexOfFirst { point ->
                            val pointX = point.x * canvasWidth
                            val pointY = (1f - point.y) * canvasHeight
                            val distance = kotlin.math.sqrt(
                                (pointX - offset.x).pow(2) + (pointY - offset.y).pow(2)
                            )
                            distance < 60f // 从 30 增加到 60 像素
                        }
                        
                        if (clickedPointIndex == -1) {
                            // 没有点击到控制点，添加新的控制点
                            val newPoints = controlPoints.toMutableList()
                            
                            // 找到插入位置（按 x 坐标排序）
                            val insertIndex = newPoints.indexOfFirst { it.x > normalizedX }
                            if (insertIndex != -1) {
                                newPoints.add(insertIndex, Offset(normalizedX, normalizedY))
                            } else {
                                newPoints.add(Offset(normalizedX, normalizedY))
                            }
                            
                            controlPoints = newPoints
                            updateCurvePoints()
                        }
                    }
                }
                .pointerInput(channel, controlPoints) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            
                            // 查找被拖动的控制点 - 增大触摸范围
                            val pointIndex = controlPoints.indexOfFirst { point ->
                                val pointX = point.x * canvasWidth
                                val pointY = (1f - point.y) * canvasHeight
                                val distance = kotlin.math.sqrt(
                                    (pointX - offset.x).pow(2) + (pointY - offset.y).pow(2)
                                )
                                distance < 60f  // 从 30 增加到 60 像素
                            }
                            
                            if (pointIndex != -1) {
                                draggingPointIndex = pointIndex
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            
                            if (draggingPointIndex != null) {
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                
                                val normalizedY = (1f - change.position.y / canvasHeight).coerceIn(0f, 1f)
                                
                                // 更新控制点位置（起点和终点只能垂直移动）
                                val newPoints = controlPoints.toMutableList()
                                val currentIndex = draggingPointIndex!!
                                
                                // 确保索引有效
                                if (currentIndex >= 0 && currentIndex < newPoints.size) {
                                    val point = newPoints[currentIndex]
                                    
                                    if (currentIndex == 0 || currentIndex == controlPoints.size - 1) {
                                        // 起点和终点只能垂直移动
                                        newPoints[currentIndex] = Offset(point.x, normalizedY)
                                    } else {
                                        // 中间点可以水平和垂直移动
                                        val normalizedX = (change.position.x / canvasWidth).coerceIn(0f, 1f)
                                        
                                        // 限制 X 坐标范围，避免超过相邻点
                                        // 减小最小间距，提高精细度控制
                                        val prevX = if (currentIndex > 0) newPoints[currentIndex - 1].x else 0f
                                        val nextX = if (currentIndex < newPoints.size - 1) newPoints[currentIndex + 1].x else 1f
                                        val clampedX = normalizedX.coerceIn(prevX + 0.001f, nextX - 0.001f)  // 从 0.01f 减小到 0.001f
                                        
                                        newPoints[currentIndex] = Offset(clampedX, normalizedY)
                                    }
                                    
                                    controlPoints = newPoints
                                    // 实时更新，提高响应速度
                                    updateCurvePoints()
                                }
                            }
                        },
                        onDragEnd = {
                            draggingPointIndex = null
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // 绘制网格
            val gridColor = Color.Gray.copy(alpha = 0.3f)
            val gridLines = 4
            
            for (i in 1 until gridLines) {
                val x = canvasWidth * i / gridLines
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
            }
            
            for (i in 1 until gridLines) {
                val y = canvasHeight * i / gridLines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }
            
            // 绘制对角线参考线
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, 0f),
                strokeWidth = 2f
            )
            
            // 绘制平滑曲线
            val path = Path()
            val steps = 100 // 曲线平滑度
            
            for (i in 0..steps) {
                val x = i / steps.toFloat()
                val y = interpolateSpline(controlPoints, x)
                
                val canvasX = x * canvasWidth
                val canvasY = (1f - y) * canvasHeight
                
                if (i == 0) {
                    path.moveTo(canvasX, canvasY)
                } else {
                    path.lineTo(canvasX, canvasY)
                }
            }
            
            drawPath(
                path = path,
                color = channel.color,
                style = Stroke(width = 5f)  // 从 3f 增加到 5f
            )
            
            // 绘制控制点 - 增大尺寸
            for ((index, point) in controlPoints.withIndex()) {
                val canvasX = point.x * canvasWidth
                val canvasY = (1f - point.y) * canvasHeight
                
                val isBeingDragged = draggingPointIndex == index
                val isEndpoint = index == 0 || index == controlPoints.size - 1
                
                // 外圈（白色边框）- 增大尺寸
                drawCircle(
                    color = Color.White,
                    radius = if (isBeingDragged) 20f else if (isEndpoint) 14f else 16f,  // 增大
                    center = Offset(canvasX, canvasY)
                )
                
                // 内圈（填充色）- 增大尺寸
                drawCircle(
                    color = channel.color,  // 使用通道颜色而不是黑色
                    radius = if (isBeingDragged) 16f else if (isEndpoint) 10f else 12f,  // 增大
                    center = Offset(canvasX, canvasY)
                )
            }
        }
    }
}

/**
 * 使用 Hermite 样条插值
 */
fun interpolateSpline(points: List<Offset>, x: Float): Float {
    if (points.isEmpty()) return x
    if (points.size == 1) return points[0].y
    
    // 边界情况
    if (x <= points.first().x) return points.first().y
    if (x >= points.last().x) return points.last().y
    
    // 找到 x 所在的区间
    var i1 = 0
    var i2 = 1
    
    for (i in 0 until points.size - 1) {
        if (x >= points[i].x && x <= points[i + 1].x) {
            i1 = i
            i2 = i + 1
            break
        }
    }
    
    val p1 = points[i1]
    val p2 = points[i2]
    
    // 防止除零
    val dx = p2.x - p1.x
    if (dx < 0.0001f) return p1.y
    
    // 归一化 t
    val t = ((x - p1.x) / dx).coerceIn(0f, 1f)
    val t2 = t * t
    val t3 = t2 * t
    
    // Hermite 基函数
    val h00 = 2 * t3 - 3 * t2 + 1
    val h10 = t3 - 2 * t2 + t
    val h01 = -2 * t3 + 3 * t2
    val h11 = t3 - t2
    
    // 计算切线（使用相邻点）
    val m0 = if (i1 > 0) {
        val prevDx = p2.x - points[i1 - 1].x
        if (prevDx > 0.0001f) {
            (p2.y - points[i1 - 1].y) / prevDx
        } else {
            (p2.y - p1.y) / dx
        }
    } else {
        (p2.y - p1.y) / dx
    }
    
    val m1 = if (i2 < points.size - 1) {
        val nextDx = points[i2 + 1].x - p1.x
        if (nextDx > 0.0001f) {
            (points[i2 + 1].y - p1.y) / nextDx
        } else {
            (p2.y - p1.y) / dx
        }
    } else {
        (p2.y - p1.y) / dx
    }
    
    val result = h00 * p1.y + h10 * dx * m0 + h01 * p2.y + h11 * dx * m1
    return result.coerceIn(0f, 1f)
}

/**
 * 曲线通道按钮
 */
@Composable
fun CurveChannelButton(
    channel: CurveChannel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isSelected) channel.color else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
                .then(
                    if (!isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = channel.color,
                            shape = MaterialTheme.shapes.small
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channel == CurveChannel.RGB) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = channel.label,
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


/**
 * 调整滑块组件
 */
@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "%.0f".format(value),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF0A84FF),
                inactiveTrackColor = Color(0xFF3A3A3C)
            )
        )
    }
}

/**
 * 一级工具枚举
 */
enum class PrimaryTool(val label: String, val icon: ImageVector) {
    AI("AI协助", Icons.Default.Star),
    FILTER("创意滤镜", Icons.Default.Face),
    CROP("裁剪", Icons.Default.Create),
    COLOR("调色", Icons.Default.Settings),
    MASK("蒙版", Icons.Default.Edit),
    HEAL("修补", Icons.Default.Build)
}

/**
 * 二级工具枚举（调色功能的子工具）
 */
enum class SecondaryTool(val label: String, val icon: ImageVector) {
    AUTO("自动", Icons.Default.Star),
    BRIGHTNESS("亮度", Icons.Default.Star),
    COLOR_TEMP("颜色", Icons.Default.Star),
    SATURATION("饱和", Icons.Default.Star), // 已废弃，保留以兼容
    EFFECTS("效果", Icons.Default.Star),
    DETAIL("细节", Icons.Default.Star),
    CURVE("曲线", Icons.Default.Star) // 已废弃，保留以兼容
}

/**
 * 曲线通道枚举
 */
enum class CurveChannel(val label: String, val color: Color) {
    RGB("RGB", Color.White),
    RED("红", Color.Red),
    GREEN("绿", Color.Green),
    BLUE("蓝", Color.Blue)
}

/**
 * 分级界面（Color Grading）
 * 类似 Lightroom 的分级功能，可以分别调整高光、中间调、阴影的色调
 */
@Composable
fun ColorGradingScreen(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部返回栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "分级",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            // 占位，保持标题居中
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "高光",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            
            // 高光色温
            AdjustmentSlider(
                label = "色温",
                value = params.gradingHighlightsTemp,
                onValueChange = { onParamsChange(params.copy(gradingHighlightsTemp = it)) },
                valueRange = -100f..100f
            )
            
            // 高光色调
            AdjustmentSlider(
                label = "色调",
                value = params.gradingHighlightsTint,
                onValueChange = { onParamsChange(params.copy(gradingHighlightsTint = it)) },
                valueRange = -100f..100f
            )
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Text(
                text = "中间调",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            
            // 中间调色温
            AdjustmentSlider(
                label = "色温",
                value = params.gradingMidtonesTemp,
                onValueChange = { onParamsChange(params.copy(gradingMidtonesTemp = it)) },
                valueRange = -100f..100f
            )
            
            // 中间调色调
            AdjustmentSlider(
                label = "色调",
                value = params.gradingMidtonesTint,
                onValueChange = { onParamsChange(params.copy(gradingMidtonesTint = it)) },
                valueRange = -100f..100f
            )
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Text(
                text = "阴影",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            
            // 阴影色温
            AdjustmentSlider(
                label = "色温",
                value = params.gradingShadowsTemp,
                onValueChange = { onParamsChange(params.copy(gradingShadowsTemp = it)) },
                valueRange = -100f..100f
            )
            
            // 阴影色调
            AdjustmentSlider(
                label = "色调",
                value = params.gradingShadowsTint,
                onValueChange = { onParamsChange(params.copy(gradingShadowsTint = it)) },
                valueRange = -100f..100f
            )
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            // 全局调整
            Text(
                text = "全局",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            
            AdjustmentSlider(
                label = "混合",
                value = params.gradingBlending,
                onValueChange = { onParamsChange(params.copy(gradingBlending = it)) },
                valueRange = 0f..100f
            )
            
            AdjustmentSlider(
                label = "平衡",
                value = params.gradingBalance,
                onValueChange = { onParamsChange(params.copy(gradingBalance = it)) },
                valueRange = -100f..100f
            )
        }
    }
}

/**
 * 混合界面（Color Mixer）
 * 类似 Lightroom 的颜色混合功能，可以调整特定颜色的色相、饱和度、明度
 */
@Composable
fun ColorMixerScreen(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onBack: () -> Unit
) {
    // 当前选择的颜色通道（0-7: 红、橙、黄、绿、青、蓝、紫、品红）
    var selectedChannel by remember { mutableStateOf(0) }
    
    val channelNames = listOf("红", "橙", "黄", "绿", "青", "蓝", "紫", "品红")
    val channelColors = listOf(
        Color(0xFFFF0000), // 红
        Color(0xFFFF8800), // 橙
        Color(0xFFFFFF00), // 黄
        Color(0xFF00FF00), // 绿
        Color(0xFF00FFFF), // 青
        Color(0xFF0000FF), // 蓝
        Color(0xFF8800FF), // 紫
        Color(0xFFFF00FF)  // 品红
    )
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部返回栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "混合",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            // 占位，保持标题居中
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        // 颜色通道选择
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(channelNames.size) { index ->
                ColorChannelButton(
                    label = channelNames[index],
                    color = channelColors[index],
                    isSelected = selectedChannel == index,
                    onClick = { selectedChannel = index }
                )
            }
        }
        
        // 调整区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 色相偏移
            AdjustmentSlider(
                label = "色相",
                value = params.hslHueShift[selectedChannel],
                onValueChange = { 
                    val newArray = params.hslHueShift.copyOf()
                    newArray[selectedChannel] = it
                    onParamsChange(params.copy(
                        enableHSL = true,
                        hslHueShift = newArray
                    ))
                },
                valueRange = -180f..180f
            )
            
            // 饱和度
            AdjustmentSlider(
                label = "饱和度",
                value = params.hslSaturation[selectedChannel],
                onValueChange = { 
                    val newArray = params.hslSaturation.copyOf()
                    newArray[selectedChannel] = it
                    onParamsChange(params.copy(
                        enableHSL = true,
                        hslSaturation = newArray
                    ))
                },
                valueRange = -100f..100f
            )
            
            // 明度
            AdjustmentSlider(
                label = "明度",
                value = params.hslLuminance[selectedChannel],
                onValueChange = { 
                    val newArray = params.hslLuminance.copyOf()
                    newArray[selectedChannel] = it
                    onParamsChange(params.copy(
                        enableHSL = true,
                        hslLuminance = newArray
                    ))
                },
                valueRange = -100f..100f
            )
        }
    }
}

/**
 * 颜色通道按钮
 */
@Composable
fun ColorChannelButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) color else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .then(
                        if (!isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = color,
                                shape = MaterialTheme.shapes.small
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0A84FF) else Color.White
        )
    }
}
