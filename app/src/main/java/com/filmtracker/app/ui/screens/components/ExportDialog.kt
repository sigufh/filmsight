package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.filmtracker.app.processing.ExportRenderingPipeline
import com.filmtracker.app.ui.theme.ComponentSize
import com.filmtracker.app.ui.theme.Spacing

/**
 * Export dialog for configuring image export settings.
 *
 * Allows users to configure:
 * - Format selection (JPEG, PNG, TIFF)
 * - JPEG quality slider
 * - TIFF bit depth selection
 * - Color space selection
 *
 * Requirements: 6.3, 6.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportRenderingPipeline.ExportConfig) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportRenderingPipeline.ExportFormat.JPEG) }
    var jpegQuality by remember { mutableStateOf(95) }
    var selectedBitDepth by remember { mutableStateOf(ExportRenderingPipeline.BitDepth.BIT_8) }
    var selectedColorSpace by remember { mutableStateOf(ExportRenderingPipeline.ColorSpace.SRGB) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "导出图像",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = ComponentSize.panelMaxHeight),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Format selection section
                FormatSelectionSection(
                    selectedFormat = selectedFormat,
                    onFormatSelected = { selectedFormat = it }
                )

                // JPEG quality slider (only shown for JPEG format)
                if (selectedFormat == ExportRenderingPipeline.ExportFormat.JPEG) {
                    JpegQualitySection(
                        quality = jpegQuality,
                        onQualityChange = { jpegQuality = it }
                    )
                }

                // TIFF bit depth selection (only shown for TIFF format)
                if (selectedFormat == ExportRenderingPipeline.ExportFormat.TIFF) {
                    BitDepthSection(
                        selectedBitDepth = selectedBitDepth,
                        onBitDepthSelected = { selectedBitDepth = it }
                    )
                }

                // Color space selection
                ColorSpaceSection(
                    selectedColorSpace = selectedColorSpace,
                    onColorSpaceSelected = { selectedColorSpace = it }
                )

                // Info text
                Text(
                    text = "图像将保存到相册的 FilmSight 文件夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    android.util.Log.d("ExportDialog", "Export button clicked")
                    android.util.Log.d("ExportDialog", "selectedFormat=$selectedFormat, jpegQuality=$jpegQuality")

                    try {
                        val config = ExportRenderingPipeline.ExportConfig(
                            format = selectedFormat,
                            quality = jpegQuality,
                            bitDepth = selectedBitDepth,
                            colorSpace = selectedColorSpace,
                            saveToGallery = true
                        )
                        android.util.Log.d("ExportDialog", "Config created successfully")
                        onConfirm(config)
                    } catch (e: Exception) {
                        android.util.Log.e("ExportDialog", "Failed to create config", e)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "导出",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FormatSelectionSection(
    selectedFormat: ExportRenderingPipeline.ExportFormat,
    onFormatSelected: (ExportRenderingPipeline.ExportFormat) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "输出格式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            ExportRenderingPipeline.ExportFormat.values().forEach { format ->
                FilterChip(
                    selected = selectedFormat == format,
                    onClick = { onFormatSelected(format) },
                    label = {
                        Text(
                            text = format.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun JpegQualitySection(
    quality: Int,
    onQualityChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "JPEG 质量: $quality",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Slider(
            value = quality.toFloat(),
            onValueChange = { onQualityChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 99,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
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

@Composable
private fun BitDepthSection(
    selectedBitDepth: ExportRenderingPipeline.BitDepth,
    onBitDepthSelected: (ExportRenderingPipeline.BitDepth) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "位深度",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FilterChip(
                selected = selectedBitDepth == ExportRenderingPipeline.BitDepth.BIT_8,
                onClick = { onBitDepthSelected(ExportRenderingPipeline.BitDepth.BIT_8) },
                label = {
                    Text(
                        text = "8-bit",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            FilterChip(
                selected = selectedBitDepth == ExportRenderingPipeline.BitDepth.BIT_16,
                onClick = { onBitDepthSelected(ExportRenderingPipeline.BitDepth.BIT_16) },
                label = {
                    Text(
                        text = "16-bit",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
private fun ColorSpaceSection(
    selectedColorSpace: ExportRenderingPipeline.ColorSpace,
    onColorSpaceSelected: (ExportRenderingPipeline.ColorSpace) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "色彩空间",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FilterChip(
                selected = selectedColorSpace == ExportRenderingPipeline.ColorSpace.SRGB,
                onClick = { onColorSpaceSelected(ExportRenderingPipeline.ColorSpace.SRGB) },
                label = {
                    Text(
                        text = "sRGB",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            FilterChip(
                selected = selectedColorSpace == ExportRenderingPipeline.ColorSpace.ADOBE_RGB,
                onClick = { onColorSpaceSelected(ExportRenderingPipeline.ColorSpace.ADOBE_RGB) },
                label = {
                    Text(
                        text = "Adobe RGB",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}
