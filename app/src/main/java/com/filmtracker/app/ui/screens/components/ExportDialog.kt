package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmtracker.app.processing.ExportRenderingPipeline

/**
 * 导出对话框
 * 
 * 允许用户配置导出设置：
 * - 格式选择（JPEG、PNG、TIFF）
 * - JPEG 质量滑块
 * - TIFF 位深度选择
 * - 输出路径选择
 * 
 * Requirements: 6.3, 6.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportRenderingPipeline.ExportConfig) -> Unit,
    defaultOutputPath: String = ""
) {
    var selectedFormat by remember { mutableStateOf(ExportRenderingPipeline.ExportFormat.JPEG) }
    var jpegQuality by remember { mutableStateOf(95) }
    var selectedBitDepth by remember { mutableStateOf(ExportRenderingPipeline.BitDepth.BIT_8) }
    var selectedColorSpace by remember { mutableStateOf(ExportRenderingPipeline.ColorSpace.SRGB) }
    var outputPath by remember { mutableStateOf(defaultOutputPath) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出图像") },
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
                
                // JPEG 质量滑块（仅对 JPEG 格式显示）
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
                
                // TIFF 位深度选择（仅对 TIFF 格式显示）
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
                
                // 输出路径
                OutlinedTextField(
                    value = outputPath,
                    onValueChange = { outputPath = it },
                    label = { Text("输出路径") },
                    placeholder = { Text("留空使用默认路径") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 提示信息
                Text(
                    text = "提示：导出将使用完整分辨率处理图像",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        outputPath = outputPath.ifBlank { defaultOutputPath }
                    )
                    onConfirm(config)
                }
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
