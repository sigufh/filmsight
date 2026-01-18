package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmtracker.app.processing.ExportRenderingPipeline
import com.filmtracker.app.ui.viewmodel.FilmWorkflowViewModel

/**
 * 批量导出对话框
 * 
 * 显示批量导出的进度和结果
 */
@Composable
fun BatchExportDialog(
    exportState: FilmWorkflowViewModel.BatchExportState,
    onDismiss: () -> Unit
) {
    when (exportState) {
        is FilmWorkflowViewModel.BatchExportState.Idle -> {
            // 不显示对话框
        }
        
        is FilmWorkflowViewModel.BatchExportState.Exporting -> {
            BatchExportProgressDialog(
                currentIndex = exportState.currentIndex,
                totalCount = exportState.totalCount,
                currentFileName = exportState.currentFileName
            )
        }
        
        is FilmWorkflowViewModel.BatchExportState.Success -> {
            BatchExportSuccessDialog(
                exportedCount = exportState.exportedCount,
                totalTimeMs = exportState.totalTimeMs,
                onDismiss = onDismiss
            )
        }
        
        is FilmWorkflowViewModel.BatchExportState.Failure -> {
            BatchExportFailureDialog(
                message = exportState.message,
                exportedCount = exportState.exportedCount,
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * 批量导出进度对话框
 */
@Composable
private fun BatchExportProgressDialog(
    currentIndex: Int,
    totalCount: Int,
    currentFileName: String
) {
    AlertDialog(
        onDismissRequest = { /* 不允许取消 */ },
        title = { Text("批量导出中") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("正在导出第 ${currentIndex + 1} / $totalCount 张")
                
                LinearProgressIndicator(
                    progress = (currentIndex + 1).toFloat() / totalCount,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = currentFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "请勿关闭应用...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {}
    )
}

/**
 * 批量导出成功对话框
 */
@Composable
private fun BatchExportSuccessDialog(
    exportedCount: Int,
    totalTimeMs: Long,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出成功") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("成功导出 $exportedCount 张图片到相册")
                
                Text(
                    text = "位置: 相册/FilmSight",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "总耗时: ${totalTimeMs / 1000} 秒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 批量导出失败对话框
 */
@Composable
private fun BatchExportFailureDialog(
    message: String,
    exportedCount: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出失败") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(message)
                
                if (exportedCount > 0) {
                    Text(
                        text = "已成功导出 $exportedCount 张图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 批量导出配置对话框
 * 
 * 允许用户配置批量导出设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchExportConfigDialog(
    imageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (ExportRenderingPipeline.ExportConfig) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportRenderingPipeline.ExportFormat.JPEG) }
    var jpegQuality by remember { mutableStateOf(95) }
    var selectedBitDepth by remember { mutableStateOf(ExportRenderingPipeline.BitDepth.BIT_8) }
    var selectedColorSpace by remember { mutableStateOf(ExportRenderingPipeline.ColorSpace.SRGB) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导出 ($imageCount 张)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 格式选择
                Text(
                    text = "输出格式",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportRenderingPipeline.ExportFormat.values().forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.name) }
                        )
                    }
                }
                
                // JPEG 质量滑块
                if (selectedFormat == ExportRenderingPipeline.ExportFormat.JPEG) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "JPEG 质量: $jpegQuality",
                            style = MaterialTheme.typography.labelMedium
                        )
                        
                        Slider(
                            value = jpegQuality.toFloat(),
                            onValueChange = { jpegQuality = it.toInt() },
                            valueRange = 0f..100f,
                            steps = 99,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "低质量",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "高质量",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // TIFF 位深度选择
                if (selectedFormat == ExportRenderingPipeline.ExportFormat.TIFF) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "位深度",
                            style = MaterialTheme.typography.labelMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedBitDepth == ExportRenderingPipeline.BitDepth.BIT_8,
                                onClick = { selectedBitDepth = ExportRenderingPipeline.BitDepth.BIT_8 },
                                label = { Text("8-bit") }
                            )
                            FilterChip(
                                selected = selectedBitDepth == ExportRenderingPipeline.BitDepth.BIT_16,
                                onClick = { selectedBitDepth = ExportRenderingPipeline.BitDepth.BIT_16 },
                                label = { Text("16-bit") }
                            )
                        }
                    }
                }
                
                // 色彩空间选择
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "色彩空间",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedColorSpace == ExportRenderingPipeline.ColorSpace.SRGB,
                            onClick = { selectedColorSpace = ExportRenderingPipeline.ColorSpace.SRGB },
                            label = { Text("sRGB") }
                        )
                        FilterChip(
                            selected = selectedColorSpace == ExportRenderingPipeline.ColorSpace.ADOBE_RGB,
                            onClick = { selectedColorSpace = ExportRenderingPipeline.ColorSpace.ADOBE_RGB },
                            label = { Text("Adobe RGB") }
                        )
                    }
                }
                
                // 提示信息
                Text(
                    text = "所有图片将保存到相册的 FilmSight 文件夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "⚠️ 批量导出可能需要较长时间，请保持应用在前台运行",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = ExportRenderingPipeline.ExportConfig(
                        format = selectedFormat,
                        quality = jpegQuality,
                        bitDepth = selectedBitDepth,
                        colorSpace = selectedColorSpace,
                        saveToGallery = true
                    )
                    onConfirm(config)
                }
            ) {
                Text("开始导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
