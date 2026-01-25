package com.filmtracker.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.processing.ExportRenderingPipeline
import com.filmtracker.app.ui.screens.components.BatchExportConfigDialog
import com.filmtracker.app.ui.screens.components.BatchExportDialog
import com.filmtracker.app.ui.screens.components.FilmStripEnd
import com.filmtracker.app.ui.screens.components.FilmStripFrame
import com.filmtracker.app.ui.screens.components.FilmStripInfoMarker
import com.filmtracker.app.ui.theme.*
import com.filmtracker.app.ui.viewmodel.FilmWorkflowViewModel
import kotlinx.coroutines.launch

/**
 * 胶卷滚动预览页（胶卷仿拍流程第三步）
 * 
 * 设计特点：
 * - 反转片胶卷滚动风格
 * - 横向滚动展示所有图片
 * - 胶片齿孔、黑边、白边
 * - 帧编号标记
 * - 卷轴端部效果
 * 
 * 交互：
 * - 横向滑动浏览
 * - 点击图片进入详细调色页
 * - 显示胶卷信息（型号、张数）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmGridPreviewScreen(
    filmFormat: FilmFormat,
    filmStock: FilmStock?,
    images: List<ImageInfo>,
    onBack: () -> Unit,
    onImageClick: (ImageInfo) -> Unit,
    onAddMoreImages: () -> Unit,
    viewModel: FilmWorkflowViewModel,  // 添加 ViewModel 参数
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedImageIndex by remember { mutableStateOf(-1) }
    var showExportConfigDialog by remember { mutableStateOf(false) }
    
    // 观察批量导出状态
    val batchExportState by viewModel.batchExportState.collectAsState()
    
    // 自动滚动动画（初始展示效果）
    var autoScrollEnabled by remember { mutableStateOf(true) }
    
    LaunchedEffect(autoScrollEnabled) {
        if (autoScrollEnabled && images.isNotEmpty()) {
            // 初始自动滚动一小段距离，展示胶卷效果
            kotlinx.coroutines.delay(500)
            listState.animateScrollToItem(
                index = minOf(2, images.size - 1),
                scrollOffset = 0
            )
            kotlinx.coroutines.delay(1000)
            autoScrollEnabled = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text(
                                text = "🎞",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = filmFormat.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        filmStock?.let {
                            Text(
                                text = "${it.displayName} · ${images.size} 张",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // 导出按钮
                    IconButton(onClick = { showExportConfigDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "批量导出",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 添加更多图片按钮
                    IconButton(onClick = onAddMoreImages) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "添加图片",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val backgroundColor = MaterialTheme.colorScheme.background
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 背景渐变（模拟暗房环境）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor,
                                backgroundColor.copy(alpha = 0.95f),
                                surfaceVariant
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(Spacing.xl))

                // 标题提示
                Text(
                    text = "点击图片进入调色",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.lg))
                
                // 胶卷滚动区域（全屏宽度）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 胶卷阴影（底部）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .offset(y = 8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // 胶卷帧滚动区域（无边框，齿孔到边缘）
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)  // 移除左右padding
                    ) {
                        // 图片帧
                        itemsIndexed(images) { index, imageInfo ->
                            // 优先使用处理后的图片，否则使用预览图
                            val displayBitmap = imageInfo.processedBitmap ?: imageInfo.previewBitmap
                            
                            FilmStripFrame(
                                bitmap = displayBitmap,
                                frameNumber = index + 1,
                                isSelected = selectedImageIndex == index,
                                onClick = {
                                    selectedImageIndex = index
                                    onImageClick(imageInfo)
                                },
                                aspectRatio = filmFormat.aspectRatio,  // 使用画幅比例
                                frameWidth = 280.dp,
                                isModified = imageInfo.isModified  // 显示修改指示器
                            )
                            
                            // 帧间间隔（黑色连接部分）
                            if (index < images.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(Spacing.sm)
                                        .height(260.dp)
                                        .background(MaterialTheme.colorScheme.scrim)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                // 滚动提示
                if (images.size > 2) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "← 左右滑动浏览 →",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
                
                // 胶卷信息卡片
                FilmInfoCard(
                    filmFormat = filmFormat,
                    filmStock = filmStock,
                    imageCount = images.size
                )
                
                Spacer(modifier = Modifier.weight(1f))

                // 底部操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // 批量导出按钮
                    OutlinedButton(
                        onClick = { showExportConfigDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("批量导出")
                    }

                    // 开始调色按钮
                    Button(
                        onClick = {
                            if (images.isNotEmpty()) {
                                onImageClick(images[0])
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("开始调色")
                    }
                }
            }
        }
    }
    
    // 批量导出配置对话框
    if (showExportConfigDialog && batchExportState is FilmWorkflowViewModel.BatchExportState.Idle) {
        BatchExportConfigDialog(
            imageCount = images.size,
            onDismiss = { showExportConfigDialog = false },
            onConfirm = { config ->
                showExportConfigDialog = false
                viewModel.batchExportImages(config)
            }
        )
    }
    
    // 批量导出进度/结果对话框
    BatchExportDialog(
        exportState = batchExportState,
        onDismiss = {
            viewModel.clearBatchExportState()
        }
    )
}

/**
 * 胶卷信息卡片
 */
@Composable
private fun FilmInfoCard(
    filmFormat: FilmFormat,
    filmStock: FilmStock?,
    imageCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        shape = RoundedCornerShape(CornerRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md + Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "胶卷信息",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 画幅信息
            InfoRow(
                label = "画幅",
                value = filmFormat.displayName
            )

            // 胶卷型号
            filmStock?.let {
                InfoRow(
                    label = "型号",
                    value = it.displayName
                )
                InfoRow(
                    label = "类型",
                    value = it.type.displayName
                )
            }

            // 张数
            InfoRow(
                label = "张数",
                value = "$imageCount / ${filmFormat.availableCounts.maxOrNull() ?: 0}"
            )

            // 比例
            InfoRow(
                label = "比例",
                value = when (filmFormat.aspectRatio) {
                    1f -> "1:1 (正方形)"
                    3f / 2f -> "3:2 (经典)"
                    4f / 3f -> "4:3 (中画幅)"
                    7f / 6f -> "7:6 (理想)"
                    else -> String.format("%.2f:1", filmFormat.aspectRatio)
                }
            )
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
