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
    
    // 预设管理器
    val presetManager = remember { com.filmtracker.app.data.PresetManager(context) }
    var userPresets by remember { mutableStateOf<List<com.filmtracker.app.data.Preset>>(emptyList()) }
    
    // 加载用户预设
    LaunchedEffect(Unit) {
        userPresets = presetManager.getAllPresets().filter { it.category == com.filmtracker.app.data.PresetCategory.USER }
    }
    
    // 加载原始图像
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            isInitialLoading = true
            val imageProcessor = ImageProcessor(context)
            val loadedImage = imageProcessor.loadOriginalImage(imageUri, previewMode = true)
            if (loadedImage != null) {
                viewModel.setOriginalImage(loadedImage)
            }
            isInitialLoading = false
        }
    }
    
    val panelHeight = 350.dp
    
    Scaffold(
        topBar = {
            ProcessingTopBar(
                onBack = onSelectImage,
                onShowImageInfo = { showImageInfoDialog = true },
                onExport = {
                    coroutineScope.launch {
                        val result = viewModel.exportImageSuspend()
                        result.onSuccess { onExport(basicParams) }
                    }
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
                canPaste = copiedParams != null
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
                    )
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
                            PrimaryTool.AI -> AIAssistPanel()
                            PrimaryTool.FILTER -> CreativeFilterPanel(
                                currentParams = basicParams,
                                onApplyPreset = { presetParams ->
                                    val newDomainParams = mapper.toDomain(presetParams)
                                    viewModel.updateParams(newDomainParams)
                                }
                            )
                            PrimaryTool.CROP -> CropRotatePanel()
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
    }
}
