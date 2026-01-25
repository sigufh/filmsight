package com.filmtracker.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmtracker.app.ai.AISettingsManager
import com.filmtracker.app.ai.ColorGradingSuggestion
import com.filmtracker.app.ui.theme.*
import com.filmtracker.app.ui.viewmodel.AIAssistantViewModel
import com.filmtracker.app.ui.viewmodel.AIAssistantViewModelFactory
import com.filmtracker.app.ui.viewmodel.ProcessingViewModel
import com.filmtracker.app.ui.viewmodel.ViewModelFactory
import com.filmtracker.app.util.ExifHelper
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AI 仿色界面 - 色彩匹配
 * 
 * 功能：
 * 1. 上传参考图（目标风格）
 * 2. 上传待修图（需要调整的图片）
 * 3. AI 自动分析图片色彩特点并应用调色
 * 4. 直接展示调色后的预览
 * 5. 点击预览进入专业修图界面
 */
@Composable
fun AIColorScreen(
    onBack: () -> Unit,
    onApplySuggestion: (ColorGradingSuggestion, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { AISettingsManager(context) }
    val aiViewModel: AIAssistantViewModel = viewModel(
        factory = AIAssistantViewModelFactory(settingsManager)
    )
    
    // 使用和专业调色相同的 ProcessingViewModel
    val processingViewModel: ProcessingViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )
    
    // 观察处理后的图像
    val processedImage by processingViewModel.processedImage.collectAsState()
    val isProcessingImage by processingViewModel.isProcessing.collectAsState()
    
    // 状态持久化
    val prefs = remember { context.getSharedPreferences("ai_color_state", Context.MODE_PRIVATE) }
    
    // 参考图（目标风格）
    var referenceImageUri by remember { 
        mutableStateOf<Uri?>(prefs.getString("reference_uri", null)?.let { Uri.parse(it) })
    }
    var referenceImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // 待修图（需要调整的图片）
    var targetImageUri by remember { 
        mutableStateOf<Uri?>(prefs.getString("target_uri", null)?.let { Uri.parse(it) })
    }
    var targetImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // AI 处理状态
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableStateOf(0f) }
    var hasProcessedResult by remember { mutableStateOf(false) }  // 新增：标记是否已处理完成
    
    var showSettings by remember { mutableStateOf(false) }
    var selectingImageType by remember { mutableStateOf<ImageType?>(null) }
    
    val isLoading by aiViewModel.isLoading.collectAsState()
    val currentSuggestion by aiViewModel.currentSuggestion.collectAsState()
    val apiConfig by aiViewModel.apiConfig.collectAsState()
    
    val isConfigured = apiConfig != null
    
    // 监听 AI 响应，使用 ProcessingViewModel 处理
    LaunchedEffect(currentSuggestion, isProcessing) {
        if (currentSuggestion != null && isProcessing && targetImageBitmap != null) {
            delay(500)
            processingProgress = 0.9f
            
            // 使用和专业调色相同的处理逻辑
            try {
                // 将 AI 建议转换为 Domain 参数
                val mapper = AdjustmentParamsMapper()
                val aiParams = com.filmtracker.app.data.BasicAdjustmentParams(
                    globalExposure = currentSuggestion!!.exposure,
                    contrast = currentSuggestion!!.contrast,
                    highlights = currentSuggestion!!.highlights,
                    shadows = currentSuggestion!!.shadows,
                    whites = currentSuggestion!!.whites,
                    blacks = currentSuggestion!!.blacks,
                    saturation = currentSuggestion!!.saturation,
                    vibrance = currentSuggestion!!.vibrance,
                    temperature = currentSuggestion!!.temperature,
                    tint = currentSuggestion!!.tint,
                    clarity = currentSuggestion!!.clarity,
                    sharpening = currentSuggestion!!.sharpness,
                    noiseReduction = currentSuggestion!!.denoise
                )
                
                // 转换为 Domain 参数并应用
                val domainParams = mapper.toDomain(aiParams)
                processingViewModel.updateParams(domainParams)
                
                processingProgress = 1f
                hasProcessedResult = true  // 标记处理完成
            } catch (e: Exception) {
                android.util.Log.e("AIColorScreen", "Failed to process image", e)
            }
            
            delay(300)
            isProcessing = false
        }
    }
    
    // 恢复图片（在后台线程加载）
    LaunchedEffect(referenceImageUri, targetImageUri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            referenceImageUri?.let { uri ->
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        referenceImageBitmap = bitmap
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AIColorScreen", "Failed to restore reference image", e)
                }
            }
            targetImageUri?.let { uri ->
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        targetImageBitmap = bitmap
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AIColorScreen", "Failed to restore target image", e)
                }
            }
        }
    }
    
    // 保存状态
    LaunchedEffect(referenceImageUri, targetImageUri) {
        prefs.edit().apply {
            putString("reference_uri", referenceImageUri?.toString())
            putString("target_uri", targetImageUri?.toString())
            apply()
        }
    }
    
    // 图片选择器（在后台线程加载）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectingImageType != null) {
            // 在后台线程加载图片
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        when (selectingImageType) {
                            ImageType.REFERENCE -> {
                                referenceImageUri = uri
                                referenceImageBitmap = bitmap
                            }
                            ImageType.TARGET -> {
                                targetImageUri = uri
                                targetImageBitmap = bitmap
                                // 设置到 ProcessingViewModel
                                if (bitmap != null) {
                                    processingViewModel.setOriginalImage(bitmap, uri, uri.toString())
                                }
                            }
                            null -> {}
                        }
                        selectingImageType = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AIColorScreen", "Failed to load image", e)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        selectingImageType = null
                    }
                }
            }
        }
    }
    
    // 设置对话框
    if (showSettings) {
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
                AISettingsScreen(
                    viewModel = aiViewModel,
                    onBack = { showSettings = false }
                )
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
        ) {
            // 顶部栏
            TopBar(
                onBack = onBack,
                onSettings = { showSettings = true },
                isConfigured = isConfigured
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))

            // 图片选择区域（上下布局）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 参考图
                ImagePickerCard(
                    title = "参考图",
                    subtitle = "目标风格",
                    icon = "🎨",
                    bitmap = referenceImageBitmap,
                    onClick = {
                        selectingImageType = ImageType.REFERENCE
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                
                // 向下箭头
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            "↓",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "色彩匹配",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 待修图 / 预览图
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (hasProcessedResult && processedImage != null) {
                        // 显示处理后的预览（使用 ProcessingViewModel 的结果）
                        ProcessedPreviewCard(
                            bitmap = processedImage!!,
                            onEdit = {
                                // 进入专业修图，传递当前参数
                                if (currentSuggestion != null && targetImageUri != null) {
                                    onApplySuggestion(currentSuggestion!!, targetImageUri.toString())
                                }
                            },
                            onExport = {
                                // 导出图片
                                if (processedImage != null) {
                                    scope.launch {
                                        try {
                                            val uri = saveImageToGallery(context, processedImage!!)
                                            if (uri != null) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "已保存到相册",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "保存失败",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("AIColorScreen", "Failed to save image", e)
                                            android.widget.Toast.makeText(
                                                context,
                                                "保存失败: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        // 显示待修图选择
                        ImagePickerCard(
                            title = "待修图",
                            subtitle = "需要调整",
                            icon = "📷",
                            bitmap = targetImageBitmap,
                            onClick = {
                                selectingImageType = ImageType.TARGET
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // 处理遮罩动画
                    if (isProcessing) {
                        ProcessingOverlay(progress = processingProgress)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // 操作按钮
            ActionButtons(
                hasReferenceImage = referenceImageBitmap != null,
                hasTargetImage = targetImageBitmap != null,
                hasProcessedImage = hasProcessedResult,
                isLoading = isLoading,
                isProcessing = isProcessing,
                isConfigured = isConfigured,
                onAnalyze = {
                    if (referenceImageBitmap != null && targetImageBitmap != null) {
                        scope.launch {
                            isProcessing = true
                            processingProgress = 0f
                            
                            // 获取图片信息
                            val exifInfo = targetImageUri?.let { uri ->
                                ExifHelper.extractExifInfo(context, uri)
                            }
                            
                            // 构建提示词，让 AI 自己分析
                            val prompt = buildString {
                                appendLine("请分析这两张图片的色彩特点：")
                                appendLine()
                                appendLine("第一张是参考图（目标风格），请分析它的：")
                                appendLine("- 整体色调和氛围")
                                appendLine("- 色彩饱和度和对比度")
                                appendLine("- 高光和阴影的处理")
                                appendLine("- 色温倾向")
                                appendLine()
                                appendLine("第二张是待修图，请分析它的当前状态。")
                                appendLine()
                                if (exifInfo != null) {
                                    appendLine("待修图的拍摄信息：")
                                    exifInfo.iso?.let { appendLine("ISO: $it") }
                                    exifInfo.exposureTime?.let { appendLine("快门: $it") }
                                    exifInfo.fNumber?.let { appendLine("光圈: f/$it") }
                                    exifInfo.focalLength?.let { appendLine("焦距: $it") }
                                    appendLine()
                                }
                                appendLine("【重要调色规则】")
                                appendLine("1. 调色策略必须温和，参数变化不要过大")
                                appendLine("2. 绝对不能失真或损失细节")
                                appendLine("3. 保持图片的自然感，避免过度处理")
                                appendLine("4. 曝光调整建议在 ±1.0 以内")
                                appendLine("5. 对比度调整建议在 ±20 以内")
                                appendLine("6. 饱和度调整建议在 ±15 以内")
                                appendLine("7. 高光/阴影调整建议在 ±30 以内")
                                appendLine("8. 色温调整建议在 ±10 以内")
                                appendLine()
                                appendLine("然后给出具体的调色参数，让待修图接近参考图的风格。")
                                appendLine("请直接给出参数数值，格式如：")
                                appendLine("曝光: +0.5")
                                appendLine("对比度: +15")
                                appendLine("饱和度: +10")
                                appendLine("色温: +5")
                                appendLine("高光: -20")
                                appendLine("阴影: +30")
                            }
                            
                            // 模拟进度
                            launch {
                                for (i in 1..3) {
                                    delay(300)
                                    processingProgress = i * 0.25f
                                }
                            }
                            
                            // 发送参考图
                            aiViewModel.sendMessage(
                                message = prompt,
                                image = referenceImageBitmap,
                                context = context
                            )
                            
                            // 等待后发送待修图
                            delay(1000)
                            processingProgress = 0.8f
                            
                            aiViewModel.sendMessage(
                                message = "这是待修图",
                                image = targetImageBitmap,
                                context = context
                            )
                        }
                    }
                },
                onReset = {
                    referenceImageBitmap = null
                    referenceImageUri = null
                    targetImageBitmap = null
                    targetImageUri = null
                    hasProcessedResult = false  // 重置处理标志
                    processingViewModel.setOriginalImage(android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888))
                    aiViewModel.clearConversation()
                    // 清除持久化状态
                    prefs.edit().clear().apply()
                }
            )
        }
    }
}

/**
 * 保存图片到相册
 */
private suspend fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "FilmTracker_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/FilmTracker")
        }
        
        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
        }
        
        uri
    } catch (e: Exception) {
        android.util.Log.e("AIColorScreen", "Failed to save image to gallery", e)
        null
    }
}

/**
 * 图片类型
 */
private enum class ImageType {
    REFERENCE,  // 参考图
    TARGET      // 待修图
}

/**
 * 顶部栏
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    isConfigured: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AI 仿色",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isConfigured) {
                Text(
                    text = "未配置",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        IconButton(onClick = onSettings) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 处理遮罩动画
 */
@Composable
private fun ProcessingOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // 动画图标
            val infiniteTransition = rememberInfiniteTransition(label = "processing")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Text(
                "🎨",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.scale(scale)
            )

            // 进度条
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(CornerRadius.xs)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )

                Text(
                    "AI 正在分析色彩特点...",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "${(progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * 处理后的预览卡片
 */
@Composable
private fun ProcessedPreviewCard(
    bitmap: Bitmap,
    onEdit: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(CornerRadius.lg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 预览图
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "调色预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // 底部操作区
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 完成提示
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(IconSize.md)
                    )
                    Text(
                        "仿色完成",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // 导出按钮
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier
                            .weight(1f)
                            .height(ComponentSize.buttonHeight),
                        shape = RoundedCornerShape(CornerRadius.xl),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            "导出",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // 继续调整按钮
                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(ComponentSize.buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(CornerRadius.xl)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(IconSize.sm)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            "继续调整",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片选择卡片
 */
@Composable
private fun ImagePickerCard(
    title: String,
    subtitle: String,
    icon: String,
    bitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.lg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (bitmap == null) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                    }
                )
        ) {
            if (bitmap != null) {
                // 显示图片
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 标签
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.md),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(CornerRadius.sm)
                ) {
                    Text(
                        title,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        icon,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSize.lg)
                    )
                }
            }
        }
    }
}

/**
 * 操作按钮
 */
@Composable
private fun ActionButtons(
    hasReferenceImage: Boolean,
    hasTargetImage: Boolean,
    hasProcessedImage: Boolean,
    isLoading: Boolean,
    isProcessing: Boolean,
    isConfigured: Boolean,
    onAnalyze: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(CornerRadius.xl),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                "重新选择",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            enabled = hasReferenceImage && hasTargetImage && isConfigured && !isLoading && !isProcessing && !hasProcessedImage,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(CornerRadius.xl)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                "开始仿色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
