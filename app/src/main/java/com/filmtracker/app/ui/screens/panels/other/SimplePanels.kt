@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.filmtracker.app.ui.screens.panels.other

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.BuiltInPresets
import com.filmtracker.app.data.Preset
import com.filmtracker.app.data.PresetCategory
import com.filmtracker.app.ui.screens.components.AIDialogPanel
import com.filmtracker.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CreativeFilterPanel(
    currentParams: BasicAdjustmentParams,
    onApplyPreset: (BasicAdjustmentParams) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedCategory by remember { mutableStateOf(PresetCategory.CREATIVE) }
    var allPresets by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 加载预设（内置 + Assets）
    LaunchedEffect(Unit) {
        isLoading = true
        val builtInPresets = BuiltInPresets.getAll()
        val assetPresets = try {
            com.filmtracker.app.data.AssetPresetLoader(context).loadAllPresets()
        } catch (e: Exception) {
            android.util.Log.e("CreativeFilterPanel", "Failed to load asset presets", e)
            emptyList()
        }
        allPresets = builtInPresets + assetPresets
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md)
    ) {
        // 分类选择
        CategoryTabs(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(Spacing.md))
        
        // 预设网格
        val filteredPresets = remember(selectedCategory, allPresets) {
            if (selectedCategory == PresetCategory.CREATIVE) {
                allPresets
            } else {
                allPresets.filter { it.category == selectedCategory }
            }
        }
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            filteredPresets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无预设",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(filteredPresets) { preset ->
                        PresetCard(
                            preset = preset,
                            onClick = { onApplyPreset(preset.params) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: PresetCategory,
    onCategorySelected: (PresetCategory) -> Unit
) {
    val categories = listOf(
        PresetCategory.CREATIVE to "全部",
        PresetCategory.PORTRAIT to "人像",
        PresetCategory.LANDSCAPE to "风景",
        PresetCategory.BLACKWHITE to "黑白",
        PresetCategory.FILM to "胶片",
        PresetCategory.VINTAGE to "复古",
        PresetCategory.CINEMATIC to "电影"
    )
    
    ScrollableTabRow(
        selectedTabIndex = categories.indexOfFirst { it.first == selectedCategory },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 0.dp
    ) {
        categories.forEach { (category, label) ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 预设图标（根据分类显示不同图标）
                Icon(
                    imageVector = when (preset.category) {
                        PresetCategory.BLACKWHITE -> Icons.Default.Face
                        PresetCategory.VINTAGE -> Icons.Default.Star
                        PresetCategory.CINEMATIC -> Icons.Default.Create
                        PresetCategory.PORTRAIT -> Icons.Default.Face
                        PresetCategory.LANDSCAPE -> Icons.Default.Star
                        else -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.lg)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AIAssistPanel(
    currentImage: android.graphics.Bitmap? = null,
    imageIdentifier: String? = null,  // 新增：用于标识图片的稳定 ID（如 URI）
    currentParams: BasicAdjustmentParams = BasicAdjustmentParams.neutral(),
    onApplyParams: (BasicAdjustmentParams) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { com.filmtracker.app.ai.AISettingsManager(context) }
    val aiViewModel: com.filmtracker.app.ui.viewmodel.AIAssistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.filmtracker.app.ui.viewmodel.AIAssistantViewModelFactory(settingsManager)
    )
    
    var showSettings by remember { mutableStateOf(false) }
    val messages by aiViewModel.messages.collectAsState()
    val isLoading by aiViewModel.isLoading.collectAsState()
    val apiConfig by aiViewModel.apiConfig.collectAsState()
    
    // 检查是否已配置 API
    val isConfigured = apiConfig != null
    
    // 当图片标识符变化时切换到对应的聊天记录
    // 使用 imageIdentifier（如 URI）而不是 Bitmap 对象，避免图片重新处理时触发切换
    LaunchedEffect(imageIdentifier) {
        val imageHash = imageIdentifier?.hashCode()
        aiViewModel.switchToImage(imageHash)
    }
    
    if (showSettings) {
        // 显示设置对话框
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSettings = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(Spacing.md),
                color = MaterialTheme.colorScheme.surface
            ) {
                com.filmtracker.app.ui.screens.AISettingsScreen(
                    viewModel = aiViewModel,
                    onBack = { showSettings = false }
                )
            }
        }
    }
    
    // 主界面
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.md)
    ) {
        // 顶部：仅设置按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    Icons.Default.Settings,
                    "设置",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        
        if (!isConfigured) {
            // 未配置提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "首次使用",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        "请点击右上角设置按钮配置 AI API",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Button(
                        onClick = { showSettings = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("立即配置", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        } else {
            // AI 对话界面
            ProAIAssistantContent(
                viewModel = aiViewModel,
                currentImage = currentImage,
                currentParams = currentParams,
                onApplyParams = onApplyParams
            )
        }
    }
}

/**
 * 专业调色界面的 AI 助手内容
 */
@Composable
private fun ProAIAssistantContent(
    viewModel: com.filmtracker.app.ui.viewmodel.AIAssistantViewModel,
    currentImage: android.graphics.Bitmap?,
    currentParams: BasicAdjustmentParams,
    onApplyParams: (BasicAdjustmentParams) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 快捷操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Button(
                onClick = {
                    if (currentImage != null) {
                        viewModel.sendMessage(
                            message = "请分析这张图片并提供专业的调色建议",
                            image = currentImage,
                            context = context
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = currentImage != null && !isLoading
            ) {
                Text(
                    "分析图片",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Button(
                onClick = {
                    val paramsDesc = buildString {
                        append("当前参数：")
                        append("曝光${currentParams.globalExposure}, ")
                        append("对比度${currentParams.contrast}, ")
                        append("饱和度${currentParams.saturation}")
                        append("\n请帮我优化这些参数")
                    }
                    viewModel.sendMessage(
                        message = paramsDesc,
                        image = currentImage,
                        context = context
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                enabled = !isLoading
            ) {
                Text(
                    "优化参数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // 对话历史
        if (messages.isEmpty()) {
            // 欢迎界面
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "AI 调色助手",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    "分析图片获取专业调色建议",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(messages) { message ->
                    ProChatBubble(
                        message = message,
                        onApplySuggestion = { suggestion ->
                            // 转换为 BasicAdjustmentParams
                            val params = BasicAdjustmentParams(
                                globalExposure = suggestion.exposure,
                                contrast = suggestion.contrast,
                                highlights = suggestion.highlights,
                                shadows = suggestion.shadows,
                                whites = suggestion.whites,
                                blacks = suggestion.blacks,
                                saturation = suggestion.saturation,
                                vibrance = suggestion.vibrance,
                                temperature = suggestion.temperature,
                                tint = suggestion.tint,
                                clarity = suggestion.clarity,
                                sharpening = suggestion.sharpness,
                                noiseReduction = suggestion.denoise
                            )
                            onApplyParams(params)
                        }
                    )
                }
                
                if (isLoading) {
                    item {
                        ProLoadingIndicator()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 输入框
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "描述你的需求...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodySmall
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        viewModel.sendMessage(
                            message = inputText,
                            image = currentImage,
                            context = context
                        )
                        inputText = ""
                        scope.launch {
                            listState.animateScrollToItem(messages.size)
                        }
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (inputText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    "发送",
                    tint = if (inputText.isNotBlank())
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 专业版聊天气泡（不显示图片预览）
 */
@Composable
private fun ProChatBubble(
    message: com.filmtracker.app.ai.ChatMessage,
    onApplySuggestion: ((com.filmtracker.app.ai.ColorGradingSuggestion) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.widthIn(max = 250.dp)) {
            Surface(
                shape = RoundedCornerShape(CornerRadius.sm),
                color = if (message.isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(Spacing.sm)) {
                    // 仅显示文字内容，不显示图片
                    if (message.content.isNotBlank() && message.content != "[图片]") {
                        if (message.isUser) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            com.filmtracker.app.ui.components.MarkdownText(
                                markdown = message.content,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
            
            // AI 消息底部显示"应用参数"按钮
            if (!message.isUser && message.suggestion != null && onApplySuggestion != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Button(
                    onClick = { onApplySuggestion(message.suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(CornerRadius.sm),
                    contentPadding = PaddingValues(vertical = Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.sm),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        "应用参数",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * 专业版加载指示器
 */
@Composable
private fun ProLoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(CornerRadius.sm),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

@Composable
fun CropRotatePanel(
    @Suppress("UNUSED_PARAMETER") previewBitmap: android.graphics.Bitmap?,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        // 旋转滑条
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "旋转",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatAngle(params.rotation),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Slider(
            value = params.rotation.coerceIn(-180f, 180f),
            onValueChange = { v ->
                val snapped = snapRotation(normalizeRotation(v))
                onParamsChange(params.copy(
                    rotation = snapped,
                    cropEnabled = false  // 裁剪模式下不实际裁剪
                ))
            },
            valueRange = -180f..180f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 快捷旋转按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Button(
                onClick = { 
                    onParamsChange(params.copy(
                        rotation = normalizeRotation(params.rotation - 90f),
                        cropEnabled = false  // 裁剪模式下不实际裁剪
                    )) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "-90°")
            }
            Button(
                onClick = { 
                    onParamsChange(params.copy(
                        rotation = 0f,
                        cropEnabled = false  // 裁剪模式下不实际裁剪
                    )) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "重置")
            }
            Button(
                onClick = { 
                    onParamsChange(params.copy(
                        rotation = normalizeRotation(params.rotation + 90f),
                        cropEnabled = false  // 裁剪模式下不实际裁剪
                    )) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "+90°")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // 裁剪开关和重置
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "裁剪",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                FilterChip(
                    selected = params.cropEnabled,
                    onClick = { 
                        // 注意：这里只是UI状态，不会实际裁剪
                        // 实际裁剪会在退出裁剪模式时应用
                        onParamsChange(params.copy(cropEnabled = false)) 
                    },
                    label = { 
                        Text(text = "预览中") 
                    }
                )
            }
            
            TextButton(
                onClick = {
                    // 重置裁剪为自由裁剪（全图）
                    onParamsChange(
                        params.copy(
                            cropEnabled = false,
                            cropLeft = 0f, 
                            cropTop = 0f, 
                            cropRight = 1f, 
                            cropBottom = 1f
                        )
                    )
                }
            ) { 
                Text("重置裁剪") 
            }
        }
    }
}

private fun normalizeRotation(deg: Float): Float {
    var r = deg % 360f
    if (r > 180f) r -= 360f
    if (r < -180f) r += 360f
    return r
}

private fun snapRotation(deg: Float, threshold: Float = 2f): Float {
    val targets = floatArrayOf(-90f, -45f, 0f, 45f, 90f)
    val d = targets.minByOrNull { t -> kotlin.math.abs(deg - t) } ?: 0f
    return if (kotlin.math.abs(deg - d) <= threshold) d else deg
}

private fun formatAngle(deg: Float): String {
    return String.format("%.1f°", deg)
}

@Composable
fun MaskPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.xl)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "蒙版功能开发中",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun HealPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.xl)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "修补消除功能开发中",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 景深模拟面板
 * 使用 AI 视觉模型自动识别主体并生成精确轮廓
 */
@Composable
fun DepthOfFieldPanel(
    currentImage: android.graphics.Bitmap?,
    depthMap: android.graphics.Bitmap?,
    showMaskOverlay: Boolean,
    onDepthMapGenerated: (depthMap: android.graphics.Bitmap) -> Unit,
    onShowMaskOverlayChange: (Boolean) -> Unit,
    onApplyEffect: (blurAmount: Float, focusX: Float, focusY: Float, focusRadius: Float) -> Unit
) {
    var blurAmount by remember { mutableStateOf(50f) }
    var isProcessing by remember { mutableStateOf(false) }
    var useCloudAI by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 存储原始深度图和 AI 识别的焦点位置
    var rawDepthMap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var aiFocusX by remember { mutableStateOf(0.5f) }
    var aiFocusY by remember { mutableStateOf(0.5f) }
    var aiFocusDepth by remember { mutableStateOf(100) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 防抖：延迟应用效果，避免滑块拖动时频繁计算
    var applyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    fun scheduleApplyEffect() {
        if (rawDepthMap == null) return
        
        // 取消之前的任务
        applyJob?.cancel()
        
        // 延迟 300ms 后应用效果
        applyJob = scope.launch {
            kotlinx.coroutines.delay(300)
            android.util.Log.d("DepthOfFieldPanel", "Applying effect: blur=$blurAmount, AI focus=($aiFocusX, $aiFocusY)")
            // 使用 AI 识别的焦点位置，固定范围为 0.3
            onApplyEffect(blurAmount, aiFocusX, aiFocusY, 0.3f)
        }
    }
    
    // 生成深度图
    fun generateDepthMap() {
        if (currentImage == null) return
        
        isProcessing = true
        errorMessage = null
        
        scope.launch {
            try {
                if (useCloudAI) {
                    // 使用云端 AI 分析深度和主体位置
                    val settingsManager = com.filmtracker.app.ai.AISettingsManager(context)
                    val aiConfig = settingsManager.getAPIConfig()
                    
                    if (aiConfig != null) {
                        val cloudEstimator = com.filmtracker.app.processing.CloudVisionDepthEstimator(context, aiConfig)
                        
                        // 1. AI 分析深度和主体位置
                        val analysis = cloudEstimator.analyzeDepth(currentImage)
                        
                        // 2. 获取 AI 建议的焦点位置
                        val (suggestedX, suggestedY) = cloudEstimator.getSuggestedFocus(analysis)
                        aiFocusX = suggestedX
                        aiFocusY = suggestedY
                        aiFocusDepth = cloudEstimator.getFocusDepth(analysis, aiFocusX, aiFocusY)
                        
                        android.util.Log.d("DepthOfFieldPanel", "AI detected focus: ($aiFocusX, $aiFocusY), depth=$aiFocusDepth")
                        
                        // 3. 生成深度图
                        val generatedDepthMap = cloudEstimator.generateDepthMap(
                            analysis,
                            currentImage.width,
                            currentImage.height
                        )
                        rawDepthMap = generatedDepthMap
                        
                        // 4. 从深度图提取精确的主体蒙版（使用 AI 返回的深度值）
                        val depthEstimator = com.filmtracker.app.processing.DepthEstimator(context)
                        val subjectMask = depthEstimator.extractSubjectMaskByDepth(
                            generatedDepthMap,
                            aiFocusDepth,  // 使用 AI 返回的深度值
                            aiFocusX,
                            aiFocusY
                        )
                        
                        // 5. 传递主体蒙版
                        onDepthMapGenerated(subjectMask)
                        
                        android.util.Log.d("DepthOfFieldPanel", "Cloud AI depth analysis completed successfully")
                    } else {
                        errorMessage = "请先配置 AI API"
                        android.util.Log.w("DepthOfFieldPanel", "AI config not found")
                    }
                } else {
                    // 使用本地算法
                    val depthEstimator = com.filmtracker.app.processing.DepthEstimator(context)
                    val generatedDepthMap = depthEstimator.estimate(currentImage, useCloud = false)
                    rawDepthMap = generatedDepthMap
                    
                    // 使用默认焦点位置（图像中心）
                    aiFocusX = 0.5f
                    aiFocusY = 0.5f
                    
                    val subjectMask = depthEstimator.extractSubjectMask(
                        generatedDepthMap,
                        aiFocusX,
                        aiFocusY,
                        0.3f
                    )
                    
                    onDepthMapGenerated(subjectMask)
                    android.util.Log.d("DepthOfFieldPanel", "Local depth estimation completed")
                }
            } catch (e: Exception) {
                android.util.Log.e("DepthOfFieldPanel", "Failed to estimate depth", e)
                errorMessage = "深度分析失败: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        // 标题和模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "景深模拟",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = useCloudAI,
                    onClick = { useCloudAI = true },
                    label = { Text("云端 AI", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
                FilterChip(
                    selected = !useCloudAI,
                    onClick = { useCloudAI = false },
                    label = { Text("本地", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // 错误提示
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.sm)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
        
        if (isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "正在分析图像深度...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // 模糊强度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "模糊强度",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${blurAmount.toInt()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Slider(
                value = blurAmount,
                onValueChange = {
                    blurAmount = it
                    scheduleApplyEffect()
                },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 生成深度图按钮或控制按钮
            if (depthMap == null) {
                Button(
                    onClick = { generateDepthMap() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = currentImage != null && !isProcessing
                ) {
                    Text("分析深度", color = MaterialTheme.colorScheme.onSecondary)
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = if (useCloudAI) "使用 AI 自动识别主体并生成精确轮廓" else "使用本地算法生成深度图",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                // 显示主体范围开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "显示主体范围",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = showMaskOverlay,
                        onCheckedChange = onShowMaskOverlayChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // 应用按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedButton(
                        onClick = { 
                            // 清除深度图和蒙版，重新分析
                            rawDepthMap = null
                            onShowMaskOverlayChange(false)
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重新分析")
                    }
                    
                    Button(
                        onClick = {
                            // 使用 AI 识别的焦点位置
                            onApplyEffect(blurAmount, aiFocusX, aiFocusY, 0.3f)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("应用效果", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = if (showMaskOverlay) "AI 已自动识别主体，绿色区域为精确识别的主体轮廓" else "AI 已自动识别主体，调整模糊强度查看效果",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 抠图面板
 * 使用 AI 视觉模型实现智能抠图
 */
@Composable
fun CutoutPanel(
    currentImage: android.graphics.Bitmap?,
    segmentationMask: android.graphics.Bitmap?,
    showMaskOverlay: Boolean,
    onMaskGenerated: (mask: android.graphics.Bitmap) -> Unit,
    onShowMaskOverlayChange: (Boolean) -> Unit,
    onApplyCutout: (mask: android.graphics.Bitmap) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var selectedPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var cutoutMode by remember { mutableStateOf(CutoutMode.AUTO) }
    var useCloudAI by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var featherRadius by remember { mutableStateOf(5) } // 新增：羽化半径
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        // 标题和 AI 模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "智能抠图",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = useCloudAI,
                    onClick = { useCloudAI = true },
                    label = { Text("云端 AI", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
                FilterChip(
                    selected = !useCloudAI,
                    onClick = { useCloudAI = false },
                    label = { Text("本地", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // 错误提示
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.sm)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
        
        // 模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FilterChip(
                selected = cutoutMode == CutoutMode.AUTO,
                onClick = { cutoutMode = CutoutMode.AUTO },
                label = { Text("自动识别", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = cutoutMode == CutoutMode.MANUAL,
                onClick = { cutoutMode = CutoutMode.MANUAL },
                label = { Text("手动选择", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        
        when (cutoutMode) {
            CutoutMode.AUTO -> {
                // 自动识别模式
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "自动识别主体",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            if (currentImage != null) {
                                isProcessing = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val segmenter = com.filmtracker.app.processing.SubjectSegmenter(context)
                                        val mask = segmenter.segmentAuto(currentImage, useCloud = useCloudAI)
                                        onMaskGenerated(mask)
                                        android.util.Log.d("CutoutPanel", "Auto segmentation completed")
                                    } catch (e: Exception) {
                                        android.util.Log.e("CutoutPanel", "Failed to segment", e)
                                        errorMessage = "识别失败: ${e.message}"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = currentImage != null && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(IconSize.sm),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                        }
                        Text(
                            text = if (isProcessing) "识别中..." else "开始识别",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Text(
                        text = "自动识别图片中的主要物体并抠图",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            CutoutMode.MANUAL -> {
                // 手动选择模式
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "点击选择物体",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // 显示已选择的点数
                    if (selectedPoints.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已选择 ${selectedPoints.size} 个点",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = { selectedPoints = emptyList() }
                            ) {
                                Text("清除", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (currentImage != null && selectedPoints.isNotEmpty()) {
                                isProcessing = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val segmenter = com.filmtracker.app.processing.SubjectSegmenter(context)
                                        val mask = segmenter.segmentWithPoints(
                                            currentImage,
                                            selectedPoints,
                                            useCloud = useCloudAI
                                        )
                                        onMaskGenerated(mask)
                                        android.util.Log.d("CutoutPanel", "Point-based segmentation completed")
                                    } catch (e: Exception) {
                                        android.util.Log.e("CutoutPanel", "Failed to segment", e)
                                        errorMessage = "分割失败: ${e.message}"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = currentImage != null && selectedPoints.isNotEmpty() && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(IconSize.sm),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                        }
                        Text(
                            text = if (isProcessing) "处理中..." else "生成抠图",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Text(
                        text = if (useCloudAI) "使用 AI 识别点击位置的物体" else "使用本地算法生成蒙版",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        
        // 如果有分割结果，显示应用按钮
        if (segmentationMask != null) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "抠图完成",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // 显示蒙版开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "显示选区范围",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showMaskOverlay,
                    onCheckedChange = onShowMaskOverlayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // 边缘羽化
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "边缘羽化",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$featherRadius px",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Slider(
                value = featherRadius.toFloat(),
                onValueChange = { featherRadius = it.toInt() },
                valueRange = 0f..20f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = if (showMaskOverlay) "绿色区域为选中的主体" else "增加羽化值可使边缘更柔和",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
                        // 清除蒙版，重新开始
                        onShowMaskOverlayChange(false)
                        selectedPoints = emptyList()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("重新抠图")
                }

                Button(
                    onClick = {
                        segmentationMask?.let { mask ->
                            // 应用边缘优化
                            scope.launch {
                                try {
                                    val segmenter = com.filmtracker.app.processing.SubjectSegmenter(context)
                                    val refinedMask = if (featherRadius > 0) {
                                        segmenter.refineMask(mask, featherRadius)
                                    } else {
                                        mask
                                    }
                                    onApplyCutout(refinedMask)
                                } catch (e: Exception) {
                                    android.util.Log.e("CutoutPanel", "Failed to refine mask", e)
                                    onApplyCutout(mask) // 降级使用原蒙版
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("应用抠图", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/**
 * 抠图模式
 */
private enum class CutoutMode {
    AUTO,    // 自动识别
    MANUAL   // 手动选择
}

