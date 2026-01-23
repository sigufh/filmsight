package com.filmtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmtracker.app.ai.ColorGradingSuggestion
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.ui.screens.components.*
import com.filmtracker.app.ui.screens.models.PrimaryTool
import com.filmtracker.app.ui.screens.models.SecondaryTool
import com.filmtracker.app.ui.screens.panels.ColorAdjustmentPanel
import com.filmtracker.app.ui.screens.panels.other.*
import com.filmtracker.app.ui.viewmodel.ProcessingViewModel
import com.filmtracker.app.ui.viewmodel.ViewModelFactory
import com.filmtracker.app.util.ImageProcessor
import kotlinx.coroutines.launch

/**
 * 图像编辑屏幕（类似 Lightroom Mobile）
 * 
 * 重构说明：
 * - 原文件 1973 行已拆分为 20+ 个模块化组件
 * - 每个组件职责单一，易于维护和测试
 * - 主文件仅负责布局和状态管理（约 200 行）
 * 
 * 布局结构：
 * - 顶部栏：返回按钮、图像信息、导出、更多菜单
 * - 图片预览区：支持缩放、平移、双击复位
 * - 二级菜单和内容区：显示当前一级工具的子功能
 * - 一级菜单：AI协助、创意滤镜、裁剪旋转、调色、蒙版、修补消除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    imageUri: String?,
    onSelectImage: () -> Unit = {},
    aiSuggestion: ColorGradingSuggestion? = null,
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
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val editSession by viewModel.editSession.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    
    // 将 Domain 参数映射为 UI 参数（BasicAdjustmentParams）
    val mapper = remember { AdjustmentParamsMapper() }
    val basicParams = remember(domainParams) {
        mapper.toData(domainParams)
    }
    
    // UI 状态
    var selectedPrimaryTool by remember { mutableStateOf<PrimaryTool?>(null) }
    var selectedSecondaryTool by remember { mutableStateOf<SecondaryTool?>(null) }
    var isInitialLoading by remember { mutableStateOf(true) }
    var showImageInfoDialog by remember { mutableStateOf(false) }
    var copiedParams by remember { mutableStateOf<BasicAdjustmentParams?>(null) }
    var showCreatePresetDialog by remember { mutableStateOf(false) }
    var showPresetManagementDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportResultDialog by remember { mutableStateOf(false) }
    
    // 裁剪模式状态
    var isCropMode by remember { mutableStateOf(false) }
    
    // 监听工具切换
    LaunchedEffect(selectedPrimaryTool) {
        if (selectedPrimaryTool == PrimaryTool.CROP) {
            isCropMode = true
        } else if (isCropMode) {
            // 退出裁剪模式时才应用裁剪
            isCropMode = false
        }
    }
    
    // 预设管理器
    val presetManager = remember { com.filmtracker.app.data.PresetManager(context) }
    var userPresets by remember { mutableStateOf<List<com.filmtracker.app.data.Preset>>(emptyList()) }
    
    // 加载用户预设
    LaunchedEffect(Unit) {
        userPresets = presetManager.getAllPresets().filter { it.category == com.filmtracker.app.data.PresetCategory.USER }
    }
    
    // 监听导出结果
    LaunchedEffect(exportResult) {
        if (exportResult != null) {
            showExportResultDialog = true
        }
    }
    
    // 加载原始图像
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            android.util.Log.d("ProcessingScreen", "Loading image from URI: $imageUri")
            isInitialLoading = true
            val imageProcessor = ImageProcessor(context)
            val loadedImage = imageProcessor.loadOriginalImage(imageUri, previewMode = true)
            if (loadedImage != null) {
                android.util.Log.d("ProcessingScreen", "Image loaded successfully: ${loadedImage.width}x${loadedImage.height}")
                
                // 设置图像并传递 URI 和路径（用于导出）
                val uri = android.net.Uri.parse(imageUri)
                viewModel.setOriginalImage(loadedImage, uri, imageUri)
                
                android.util.Log.d("ProcessingScreen", "Image path set for export: $imageUri")
                
                // 如果有 AI 建议，应用参数
                if (aiSuggestion != null) {
                    android.util.Log.d("ProcessingScreen", "Applying AI suggestion")
                    // 注意：现在所有参数都使用 Adobe 标准（-100 到 +100 或 -5 到 +5）
                    // 不需要额外转换，直接使用即可
                    val aiParams = BasicAdjustmentParams(
                        globalExposure = aiSuggestion.exposure,  // Adobe 标准：-5.0 到 +5.0 EV
                        contrast = aiSuggestion.contrast,        // Adobe 标准：-100 到 +100
                        highlights = aiSuggestion.highlights,    // Adobe 标准：-100 到 +100
                        shadows = aiSuggestion.shadows,          // Adobe 标准：-100 到 +100
                        whites = aiSuggestion.whites,            // Adobe 标准：-100 到 +100
                        blacks = aiSuggestion.blacks,            // Adobe 标准：-100 到 +100
                        saturation = aiSuggestion.saturation,    // Adobe 标准：-100 到 +100
                        vibrance = aiSuggestion.vibrance,        // Adobe 标准：-100 到 +100
                        temperature = aiSuggestion.temperature,  // Adobe 标准：-100 到 +100
                        tint = aiSuggestion.tint,                // Adobe 标准：-100 到 +100
                        clarity = aiSuggestion.clarity,          // Adobe 标准：-100 到 +100
                        sharpening = aiSuggestion.sharpness,     // Adobe 标准：0 到 100
                        noiseReduction = aiSuggestion.denoise    // Adobe 标准：0 到 100
                    )
                    android.util.Log.d("ProcessingScreen", "AI params: exposure=${aiParams.globalExposure}, contrast=${aiParams.contrast}, saturation=${aiParams.saturation}")
                    val newDomainParams = mapper.toDomain(aiParams)
                    viewModel.updateParams(newDomainParams)
                }
            } else {
                android.util.Log.e("ProcessingScreen", "Failed to load image from URI: $imageUri")
            }
            isInitialLoading = false
        }
    }
    
    // 根据不同工具设置不同的面板高度
    val panelHeight = when (selectedPrimaryTool) {
        PrimaryTool.CROP -> 150.dp  // 裁剪面板更小
        PrimaryTool.COLOR -> 350.dp // 调色面板保持原样
        PrimaryTool.AI -> 350.dp    // AI面板保持原样
        else -> 300.dp              // 其他面板
    }
    
    Scaffold(
        topBar = {
            ProcessingTopBar(
                onBack = onSelectImage,
                onShowImageInfo = { showImageInfoDialog = true },
                onExport = {
                    // 显示导出对话框
                    android.util.Log.d("ProcessingScreen", "Export button clicked, showing dialog")
                    showExportDialog = true
                },
                onCopyParams = { copiedParams = basicParams },
                onPasteParams = {
                    copiedParams?.let {
                        val newDomainParams = mapper.toDomain(it)
                        viewModel.updateParams(newDomainParams)
                    }
                },
                onResetParams = {
                    val neutralParams = BasicAdjustmentParams.neutral()
                    val newDomainParams = mapper.toDomain(neutralParams)
                    viewModel.updateParams(newDomainParams)
                },
                onCreatePreset = { showCreatePresetDialog = true },
                onManagePresets = { showPresetManagementDialog = true },
                canPaste = copiedParams != null,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                canUndo = canUndo,
                canRedo = canRedo,
                isModified = editSession?.isModified ?: false
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 图像预览区（支持缩放、平移、双击复位）
            ImagePreview(
                processedImage = processedImage,
                isLoading = isInitialLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(
                        bottom = if (selectedPrimaryTool != null) {
                            panelHeight + 70.dp // 面板高度 + 一级菜单高度
                        } else {
                            70.dp // 只有一级菜单高度
                        }
                    ),
                cropEnabled = false,  // 裁剪模式下始终不实际裁剪
                cropLeft = 0f,
                cropTop = 0f,
                cropRight = 1f,
                cropBottom = 1f,
                onCropChange = { l, t, r, b ->
                    // 退出裁剪模式时应用
                    if (!isCropMode) {
                        val newParams = basicParams.copy(
                            cropLeft = l,
                            cropTop = t,
                            cropRight = r,
                            cropBottom = b,
                            cropEnabled = true
                        )
                        val newDomainParams = mapper.toDomain(newParams)
                        viewModel.updateParams(newDomainParams)
                    }
                },
                rotation = basicParams.rotation,
                onRotationChange = { rot ->
                    val newParams = basicParams.copy(rotation = rot, cropEnabled = false)
                    val newDomainParams = mapper.toDomain(newParams)
                    viewModel.updateParams(newDomainParams)
                },
                showCropOverlay = isCropMode
            )
            
            // 底部面板和工具栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // 二级面板（当选中一级工具时显示）
                if (selectedPrimaryTool != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(panelHeight)
                            .background(Color(0xFF1C1C1E))
                    ) {
                        when (selectedPrimaryTool) {
                            PrimaryTool.AI -> AIAssistPanel(
                                currentImage = processedImage,
                                imageIdentifier = imageUri,  // 使用 URI 作为稳定标识符
                                currentParams = basicParams,
                                onApplyParams = { newParams ->
                                    val newDomainParams = mapper.toDomain(newParams)
                                    viewModel.updateParams(newDomainParams)
                                }
                            )
                            PrimaryTool.FILTER -> CreativeFilterPanel(
                                currentParams = basicParams,
                                onApplyPreset = { presetParams ->
                                    val newDomainParams = mapper.toDomain(presetParams)
                                    viewModel.updateParams(newDomainParams)
                                }
                            )
                            PrimaryTool.CROP -> CropRotatePanel(
                                previewBitmap = processedImage,
                                params = basicParams,
                                onParamsChange = { newParams ->
                                    // 只更新旋转，不更新裁剪
                                    val paramsToUpdate = basicParams.copy(
                                        rotation = newParams.rotation,
                                        cropEnabled = false
                                    )
                                    val newDomainParams = mapper.toDomain(paramsToUpdate)
                                    viewModel.updateParams(newDomainParams)
                                }
                            )
                            PrimaryTool.COLOR -> ColorAdjustmentPanel(
                                params = basicParams,
                                onParamsChange = { newParams ->
                                    val newDomainParams = mapper.toDomain(newParams)
                                    viewModel.updateParams(newDomainParams)
                                },
                                selectedSecondaryTool = selectedSecondaryTool,
                                onSecondaryToolSelected = { selectedSecondaryTool = it }
                            )
                            PrimaryTool.MASK -> MaskPanel()
                            PrimaryTool.HEAL -> HealPanel()
                            null -> {} // 不应该到达这里，因为外层已经检查了 null
                        }
                    }
                }
                
                // 一级工具栏（始终显示在最底部）
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
        
        // 图像信息对话框
        if (showImageInfoDialog) {
            ImageInfoDialog(
                imageUri = imageUri,
                onDismiss = { showImageInfoDialog = false }
            )
        }
        
        // 创建预设对话框
        if (showCreatePresetDialog) {
            CreatePresetDialog(
                currentParams = basicParams,
                onDismiss = { showCreatePresetDialog = false },
                onConfirm = { name, category ->
                    coroutineScope.launch {
                        val preset = com.filmtracker.app.data.Preset(
                            name = name,
                            category = category,
                            params = basicParams
                        )
                        presetManager.savePreset(preset)
                        userPresets = presetManager.getAllPresets().filter { 
                            it.category == com.filmtracker.app.data.PresetCategory.USER 
                        }
                        showCreatePresetDialog = false
                    }
                }
            )
        }
        
        // 预设管理对话框
        if (showPresetManagementDialog) {
            PresetManagementDialog(
                presets = userPresets,
                onDismiss = { showPresetManagementDialog = false },
                onApplyPreset = { preset ->
                    val newDomainParams = mapper.toDomain(preset.params)
                    viewModel.updateParams(newDomainParams)
                    showPresetManagementDialog = false
                },
                onDeletePreset = { presetId ->
                    coroutineScope.launch {
                        presetManager.deletePreset(presetId)
                        userPresets = presetManager.getAllPresets().filter { 
                            it.category == com.filmtracker.app.data.PresetCategory.USER 
                        }
                    }
                },
                onRenamePreset = { presetId, newName ->
                    coroutineScope.launch {
                        presetManager.renamePreset(presetId, newName)
                        userPresets = presetManager.getAllPresets().filter { 
                            it.category == com.filmtracker.app.data.PresetCategory.USER 
                        }
                    }
                }
            )
        }
        
        // 导出对话框
        if (showExportDialog) {
            android.util.Log.d("ProcessingScreen", "Showing export dialog, imageUri=$imageUri")
            
            ExportDialog(
                onDismiss = { 
                    android.util.Log.d("ProcessingScreen", "Export dialog dismissed")
                    showExportDialog = false 
                },
                onConfirm = { config ->
                    android.util.Log.d("ProcessingScreen", "Export confirmed with config: format=${config.format}, quality=${config.quality}")
                    showExportDialog = false
                    viewModel.exportImage(config)
                }
            )
        }
        
        // 导出进度对话框
        if (isExporting) {
            AlertDialog(
                onDismissRequest = { /* 不允许取消 */ },
                title = { Text("正在导出") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("正在以完整分辨率处理图像...")
                        LinearProgressIndicator(
                            progress = exportProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(exportProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {}
            )
        }
        
        // 导出结果对话框
        if (showExportResultDialog && exportResult != null) {
            when (val result = exportResult) {
                is com.filmtracker.app.processing.ExportRenderingPipeline.ExportResult.Success -> {
                    AlertDialog(
                        onDismissRequest = {
                            showExportResultDialog = false
                            viewModel.clearExportResult()
                        },
                        title = { Text("导出成功") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("图像已成功保存到相册")
                                if (result.outputUri != null) {
                                    Text(
                                        text = "位置: 相册/FilmSight",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (result.outputFile != null) {
                                    Text(
                                        text = "路径: ${result.outputFile.absolutePath}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "耗时: ${result.totalTimeMs} ms",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showExportResultDialog = false
                                    viewModel.clearExportResult()
                                }
                            ) {
                                Text("确定")
                            }
                        }
                    )
                }
                is com.filmtracker.app.processing.ExportRenderingPipeline.ExportResult.Failure -> {
                    AlertDialog(
                        onDismissRequest = {
                            showExportResultDialog = false
                            viewModel.clearExportResult()
                        },
                        title = { Text("导出失败") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(result.message)
                                Text(
                                    text = "错误: ${result.error.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showExportResultDialog = false
                                    viewModel.clearExportResult()
                                }
                            ) {
                                Text("确定")
                            }
                        }
                    )
                }
                null -> {
                    // 不应该发生，因为外层已经检查了 exportResult != null
                }
            }
        }
    }
}
