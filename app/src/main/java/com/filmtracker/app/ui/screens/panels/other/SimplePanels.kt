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
            .padding(16.dp)
    ) {
        // 分类选择
        CategoryTabs(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                    CircularProgressIndicator(color = Color.White)
                }
            }
            filteredPresets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无预设",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
        contentColor = Color.White,
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
            containerColor = Color(0xFF2C2C2E)
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
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
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
                shape = RoundedCornerShape(16.dp),
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
            .background(FilmTrackerDark)
            .padding(16.dp)
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
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (!isConfigured) {
            // 未配置提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = FilmTrackerSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "💡 首次使用",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请点击右上角设置按钮配置 AI API",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showSettings = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FilmTrackerPrimary
                        )
                    ) {
                        Text("立即配置", color = FilmTrackerDark)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    containerColor = FilmTrackerPrimary
                ),
                enabled = currentImage != null && !isLoading
            ) {
                Text("分析图片", fontSize = 12.sp, color = FilmTrackerDark)
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
                    containerColor = FilmTrackerAccent
                ),
                enabled = !isLoading
            ) {
                Text("优化参数", fontSize = 12.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
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
                Text("✨", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "AI 调色助手",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "分析图片获取专业调色建议",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 输入框
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("描述你的需求...", fontSize = 12.sp, color = Color.Gray) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FilmTrackerPrimary,
                    unfocusedBorderColor = FilmTrackerSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = FilmTrackerPrimary
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
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
                    .background(if (inputText.isNotBlank()) FilmTrackerPrimary else FilmTrackerSecondary)
            ) {
                Icon(
                    Icons.Default.Send,
                    "发送",
                    tint = if (inputText.isNotBlank()) FilmTrackerDark else Color.White,
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
                shape = RoundedCornerShape(12.dp),
                color = if (message.isUser) 
                    FilmTrackerSurface 
                else 
                    Color(0xFF3C3C3E),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // 仅显示文字内容，不显示图片
                    if (message.content.isNotBlank() && message.content != "[图片]") {
                        if (message.isUser) {
                            Text(
                                text = message.content,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        } else {
                            com.filmtracker.app.ui.components.MarkdownText(
                                markdown = message.content,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
            
            // AI 消息底部显示"应用参数"按钮
            if (!message.isUser && message.suggestion != null && onApplySuggestion != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onApplySuggestion(message.suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FilmTrackerPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = FilmTrackerDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("应用参数", color = FilmTrackerDark, fontSize = 11.sp)
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
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF3C3C3E),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(FilmTrackerPrimary.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

@Composable
fun CropRotatePanel(
    previewBitmap: android.graphics.Bitmap?,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    val aspectOptions = listOf(
        "自由" to null,
        "1:1" to 1f / 1f,
        "3:2" to 3f / 2f,
        "4:3" to 4f / 3f,
        "16:9" to 16f / 9f
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 旋转
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "旋转", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(text = formatAngle(params.rotation), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = params.rotation.coerceIn(-180f, 180f),
            onValueChange = { v ->
                val snapped = snapRotation(normalizeRotation(v))
                onParamsChange(params.copy(rotation = snapped))
            },
            valueRange = -180f..180f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onParamsChange(params.copy(rotation = normalizeRotation(params.rotation - 90f))) }) {
                Text(text = "-90°")
            }
            Button(onClick = { onParamsChange(params.copy(rotation = 0f)) }) {
                Text(text = "重置旋转")
            }
            Button(onClick = { onParamsChange(params.copy(rotation = normalizeRotation(params.rotation + 90f))) }) {
                Text(text = "+90°")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 裁剪
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "裁剪", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            FilterChip(
                selected = params.cropEnabled,
                onClick = { onParamsChange(params.copy(cropEnabled = !params.cropEnabled)) },
                label = { Text(text = if (params.cropEnabled) "开启" else "关闭") }
            )
            TextButton(onClick = {
                // 重置裁剪
                onParamsChange(
                    params.copy(
                        cropEnabled = false,
                        cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f
                    )
                )
            }) { Text("重置裁剪") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 常用比例
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(aspectOptions) { (label, ratio) ->
                ElevatedButton(onClick = {
                    if (ratio == null) {
                        onParamsChange(
                            params.copy(
                                cropEnabled = true,
                                cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f
                            )
                        )
                    } else {
                        val updated = computeCenteredCrop(previewBitmap, ratio, params)
                        onParamsChange(updated)
                    }
                }) {
                    Text(label)
                }
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

private fun computeCenteredCrop(
    previewBitmap: android.graphics.Bitmap?,
    targetRatio: Float,
    params: BasicAdjustmentParams
): BasicAdjustmentParams {
    val w = previewBitmap?.width ?: return params
    val h = previewBitmap.height
    if (w <= 0 || h <= 0) return params
    val imageRatio = w.toFloat() / h.toFloat()
    return if (imageRatio > targetRatio) {
        val widthNorm = targetRatio / imageRatio
        val left = (1f - widthNorm) / 2f
        params.copy(
            cropEnabled = true,
            cropLeft = left,
            cropTop = 0f,
            cropRight = 1f - left,
            cropBottom = 1f
        )
    } else {
        val heightNorm = imageRatio / targetRatio
        val top = (1f - heightNorm) / 2f
        params.copy(
            cropEnabled = true,
            cropLeft = 0f,
            cropTop = top,
            cropRight = 1f,
            cropBottom = 1f - top
        )
    }
}

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
