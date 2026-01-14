package com.filmtracker.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.util.ImageProcessor

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
    // 状态管理
    var basicParams by remember { mutableStateOf(BasicAdjustmentParams.neutral()) }
    var selectedPrimaryTool by remember { mutableStateOf<PrimaryTool?>(null) }
    var selectedSecondaryTool by remember { mutableStateOf<SecondaryTool?>(null) }
    var processedImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isInitialLoading by remember { mutableStateOf(true) } // 只在初始加载时为 true
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 加载原始图像（只在初始时显示加载动画）
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            isInitialLoading = true
            val imageProcessor = ImageProcessor(context)
            processedImage = imageProcessor.loadOriginalImage(imageUri, previewMode = true)
            isInitialLoading = false
        }
    }
    
    // 应用参数变化（后台处理，不显示加载动画）
    LaunchedEffect(basicParams) {
        if (imageUri != null && processedImage != null && !isInitialLoading) {
            // 在后台处理，不改变 isInitialLoading 状态
            val imageProcessor = ImageProcessor(context)
            val originalImage = imageProcessor.loadOriginalImage(imageUri, previewMode = true)
            if (originalImage != null) {
                val newProcessedImage = imageProcessor.applyBasicAdjustmentsToOriginal(originalImage, basicParams)
                if (newProcessedImage != null) {
                    processedImage = newProcessedImage
                }
            }
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
                    IconButton(onClick = { onExport(basicParams) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "导出",
                            tint = Color.White
                        )
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
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(processedImage)
                            .build(),
                        contentDescription = "处理后的图像",
                        modifier = Modifier.fillMaxSize()
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
                                    onParamsChange = { basicParams = it },
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
    Column(modifier = Modifier.fillMaxSize()) {
        // 二级工具栏
        SecondaryToolBar(
            tools = listOf(
                SecondaryTool.AUTO,
                SecondaryTool.BRIGHTNESS,
                SecondaryTool.COLOR_TEMP,
                SecondaryTool.SATURATION,
                SecondaryTool.EFFECTS,
                SecondaryTool.DETAIL,
                SecondaryTool.CURVE
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
                    BrightnessAdjustContent(params, onParamsChange)
                }
                SecondaryTool.COLOR_TEMP -> {
                    ColorTempAdjustContent(params, onParamsChange)
                }
                SecondaryTool.SATURATION -> {
                    SaturationAdjustContent(params, onParamsChange)
                }
                SecondaryTool.EFFECTS -> {
                    EffectsAdjustContent(params, onParamsChange)
                }
                SecondaryTool.DETAIL -> {
                    DetailAdjustContent(params, onParamsChange)
                }
                SecondaryTool.CURVE -> {
                    CurveAdjustContent()
                }
                null -> {
                    // 默认显示
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
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 曝光度
        AdjustmentSlider(
            label = "曝光度",
            value = params.globalExposure,
            onValueChange = { onParamsChange(params.copy(globalExposure = it)) },
            valueRange = -5f..5f
        )
        
        // 对比度
        AdjustmentSlider(
            label = "对比度",
            value = (params.contrast - 1f) * 100f,
            onValueChange = { onParamsChange(params.copy(contrast = 1f + it / 100f)) },
            valueRange = -50f..100f
        )
        
        // 高光
        AdjustmentSlider(
            label = "高光",
            value = params.highlights,
            onValueChange = { onParamsChange(params.copy(highlights = it)) },
            valueRange = -100f..100f
        )
        
        // 阴影
        AdjustmentSlider(
            label = "阴影",
            value = params.shadows,
            onValueChange = { onParamsChange(params.copy(shadows = it)) },
            valueRange = -100f..100f
        )
        
        // 白场
        AdjustmentSlider(
            label = "白场",
            value = params.whites,
            onValueChange = { onParamsChange(params.copy(whites = it)) },
            valueRange = -100f..100f
        )
        
        // 黑场
        AdjustmentSlider(
            label = "黑场",
            value = params.blacks,
            onValueChange = { onParamsChange(params.copy(blacks = it)) },
            valueRange = -100f..100f
        )
    }
}

/**
 * 色温调整内容
 */
@Composable
fun ColorTempAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "色温调整功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 饱和度调整内容
 */
@Composable
fun SaturationAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 饱和度
        AdjustmentSlider(
            label = "饱和度",
            value = (params.saturation - 1f) * 100f,
            onValueChange = { onParamsChange(params.copy(saturation = 1f + it / 100f)) },
            valueRange = -100f..100f
        )
        
        // 自然饱和度
        AdjustmentSlider(
            label = "自然饱和度",
            value = params.vibrance,
            onValueChange = { onParamsChange(params.copy(vibrance = it)) },
            valueRange = -100f..100f
        )
    }
}

/**
 * 效果调整内容
 */
@Composable
fun EffectsAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "效果调整功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 清晰度
        AdjustmentSlider(
            label = "清晰度",
            value = params.clarity,
            onValueChange = { onParamsChange(params.copy(clarity = it)) },
            valueRange = -100f..100f
        )
    }
}

/**
 * 曲线调整内容
 */
@Composable
fun CurveAdjustContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "曲线调整功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
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
    SATURATION("饱和", Icons.Default.Star),
    EFFECTS("效果", Icons.Default.Star),
    DETAIL("细节", Icons.Default.Star),
    CURVE("曲线", Icons.Default.Star)
}
